package cloud.gamja.vm.client;

import cloud.gamja.vm.client.enums.Audience;
import cloud.gamja.vm.client.record.KeyDto;
import cloud.gamja.vm.client.record.VmCreate;
import cloud.gamja.vm.vms.enums.VmType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Component
@RequiredArgsConstructor
public class ProxmoxClient {
    private final WebClient webClient;
    private final UserServiceClient userServiceClient;
    private final TokenExchangeClient tokenExchangeClient;

    @Value("${custom.proxmox.token-id}")
    private String tokenId;
    @Value("${custom.proxmox.token-value}")
    private String tokenValue;
    @Value("${custom.proxmox.template.free}")
    private String freeVm;
    @Value("${custom.proxmox.template.pro}")
    private String proVm;

    public Mono<Map<String,Object>> getNodes() {
        return webClient.get()
                .uri("/nodes")
                .header(HttpHeaders.AUTHORIZATION,
                        "PVEAPIToken=" + tokenId + "=" + tokenValue)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<>() {});
    }

//    private Mono<Map<String,Object>> createVmRequest(VmCreate vm) {
//        MultiValueMap<String, String> q = getStringStringMultiValueMap(vm);
//
//        return webClient.post()
//                .uri(uri -> uri.path("/nodes/{node}/qemu")
//                        .queryParams(q).build(vm.getNode()))
//                .header(HttpHeaders.AUTHORIZATION, "PVEAPIToken " + tokenId + "=" + tokenValue)
//                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                .body(BodyInserters.fromFormData(q))
//                .retrieve()
//                .bodyToMono(new ParameterizedTypeReference<>() {})
//                .flatMap(result ->
//                        importCloudImage(vm.getVmid(), "noble-server-cloudimg-amd64.img"));
//    }

//    private static MultiValueMap<String, String> getStringStringMultiValueMap(VmCreate vm) {
//        MultiValueMap<String,String> q = new LinkedMultiValueMap<>();
//        q.add("vmid", String.valueOf(vm.getVmid()));
//        q.add("name", vm.getName());
//        q.add("cores", String.valueOf(vm.getCores()));
//        q.add("cpu", vm.getCpu());
//        q.add("cpulimit", String.valueOf(vm.getCpulimit()));
//        q.add("cpuunits", String.valueOf(vm.getCpuunits()));
//        // affinity 임시 제거
////        q.add("affinity", vm.getAffinity());
//        q.add("memory", vm.getMemory());
//        q.add("ostype", vm.getOstype());
//        q.add("pool", vm.getPool());
//        q.add("agent", vm.getAgent());
//        q.add("net0", vm.getNet0());
//        q.add("scsihw", vm.getScsihw());
//        q.add("scsi0", vm.getScsi0());
////        q.add("ide0", vm.getIde0());
////        q.add("ide2", vm.getIde2());
//        q.add("ciuser", vm.getCiuser());
//        q.add("ipconfig0", vm.getIpconfig0());
//        return q;
//    }

    public Mono<Map<String,Object>> createVmOptimize(
            String subjectToken, String fingerprint, VmType vmType, String name, Integer disk, String ide) {
        // VmId
        log.info("vmId start");
        Mono<Integer> vmIdMono = nextId()
                .map(response -> response.get("data"))
                .timeout(Duration.ofSeconds(10))
                .onErrorMap(e -> new HttpTimeoutException("NextId failed: " + e.getMessage()));
        log.info("vmId end");
        // VmTemplate
        log.info("vmTemplate start");
        Mono<VmCreate> vmTemplateMono = Mono.defer(() -> {
            VmCreate vm = new VmCreate(vmType);
            vm.setName(name);
            vm.setScsi0("local-lvm:" + disk);
            vm.setIde0("local:iso/" + ide + ",media=cdrom");
            vm.setCiuser(getCiuser(ide));
            return Mono.just(vm);
        });
        log.info("vmTemplate end");
        // SSH key
        log.info("ssh key start");
        Mono<String> sshKey = tokenExchangeClient.exchange(subjectToken, Audience.USER)
                .map((response) -> response.get("access_token"))
                .flatMap(token -> findByFingerprint(token, fingerprint));
        log.info("sshKey end");

        log.info("Create vm start");
        return Mono.zip(vmIdMono, vmTemplateMono, sshKey)
                .flatMap(tuple -> {
                    VmCreate vm = tuple.getT2();
                    vm.setVmid(tuple.getT1());
                    vm.setSshkeys(tuple.getT3());
                    return cloneFromTemplate(vm, vmType);
                });
    }

    private Mono<Map<String, Object>> cloneFromTemplate(VmCreate vm, VmType vmType) {
        log.info("Cloning VM {} from template {}", vm.getVmid(), vmType);

        MultiValueMap<String, String> cloneParams = new LinkedMultiValueMap<>();
        cloneParams.add("newid", vm.getVmid().toString());
        cloneParams.add("name", vm.getName());
        cloneParams.add("full", "1"); // Full clone (독립적인 디스크)

        return webClient.post()
                .uri("/nodes/{node}/qemu/{vmid}/clone", "pve", vmType == VmType.FREE ? freeVm : proVm)
                .header(HttpHeaders.AUTHORIZATION, "PVEAPIToken " + tokenId + "=" + tokenValue)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(cloneParams))
                .exchangeToMono(res -> {
                    if (res.statusCode().is2xxSuccessful()) {
                        return res.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
                    }
                    return res.bodyToMono(String.class)
                            .flatMap(body -> Mono.error(new IllegalStateException(
                                    "Clone failed: " + res.statusCode().value() + ": " + body)));
                })
                // Clone 완료 후 VM 설정 커스터마이징
                .flatMap(result -> customizeClonedVm(vm));
    }

    // Clone된 VM을 요청사항에 맞게 커스터마이징
    private Mono<Map<String, Object>> customizeClonedVm(VmCreate vm) {
        log.info("Customizing cloned VM {}", vm.getVmid());

        MultiValueMap<String, String> configParams = new LinkedMultiValueMap<>();

        // VmType에 따른 리소스 조정
        configParams.add("cores", String.valueOf(vm.getCores()));
        configParams.add("memory", vm.getMemory());
        configParams.add("cpu", vm.getCpu());

        // SSH 키 설정
        // configParams.add("sshkeys", URLEncoder.encode(vm.getSshkeys(), StandardCharsets.UTF_8));

        // 네트워크 설정 (DHCP 또는 고정 IP)
        configParams.add("ipconfig0", vm.getIpconfig0());

        // 추가 설정들 (필요에 따라)
        if (vm.getCpulimit() > 0) {
            configParams.add("cpulimit", String.valueOf(vm.getCpulimit()));
        }
        if (vm.getCpuunits() != 1024) {
            configParams.add("cpuunits", String.valueOf(vm.getCpuunits()));
        }

        return webClient.put()
                .uri("/nodes/{node}/qemu/{vmid}/config", "pve", vm.getVmid())
                .header(HttpHeaders.AUTHORIZATION, "PVEAPIToken " + tokenId + "=" + tokenValue)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(configParams))
                .exchangeToMono(res -> {
                    if (res.statusCode().is2xxSuccessful()) {
                        return res.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
                    }
                    return res.bodyToMono(String.class)
                            .flatMap(body -> Mono.error(new IllegalStateException(
                                    "Customize VM failed: " + res.statusCode().value() + ": " + body)));
                })
                // 설정 완료 후 VM 시작 (선택사항)
                .flatMap(result -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("vmid", vm.getVmid());
                    response.put("status", "created");
                    response.put("message", "VM cloned and configured successfully");
                    return Mono.just(response);
                });
    }
//
//    private Mono<Map<String, Object>> importCloudImage(Integer vmid, String cloudImageName) {
//        log.info("Importing cloud image for VM {}: {}", vmid, cloudImageName);
//
//        return executeQmImportDisk(vmid, cloudImageName)
//                .then(attachImportedDisk(vmid));
//    }
//
//    private Mono<Void> executeQmImportDisk(Integer vmid, String cloudImageName) {
//        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//        params.add("command", "qm");
//        params.add("args", String.join(" ",
//                "importdisk",
//                vmid.toString(),
//                "/var/lib/vz/template/iso/" + cloudImageName,
//                "local-lvm"
//        ));
//
//        return webClient.post()
//                .uri("/nodes/{node}/tasks", "pve")
//                .header(HttpHeaders.AUTHORIZATION, "PVEAPIToken " + tokenId + "=" + tokenValue)
//                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                .body(BodyInserters.fromFormData(params))
//                .exchangeToMono(res -> {
//                    if (res.statusCode().is2xxSuccessful()) {
//                        return res.bodyToMono(Void.class);
//                    }
//                    return res.bodyToMono(String.class)
//                            .flatMap(body -> Mono.error(new IllegalStateException(
//                                    "Import disk failed: " + res.statusCode().value() + ": " + body)));
//                });
//    }
//
//    private Mono<Map<String, Object>> attachImportedDisk(Integer vmid) {
//        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//        params.add("scsi0", "local-lvm:vm-" + vmid + "-disk-0,cache=writeback,discard=on");
//
//        return webClient.put()
//                .uri("/nodes/{node}/qemu/{vmid}/config", "pve", vmid)
//                .header(HttpHeaders.AUTHORIZATION, "PVEAPIToken " + tokenId + "=" + tokenValue)
//                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                .body(BodyInserters.fromFormData(params))
//                .exchangeToMono(res -> {
//                    if (res.statusCode().is2xxSuccessful()) {
//                        return res.bodyToMono(new ParameterizedTypeReference<>() {});
//                    }
//                    return res.bodyToMono(String.class)
//                            .flatMap(body -> Mono.error(new IllegalStateException(
//                                    "Attach disk failed: " + res.statusCode().value() + ": " + body)));
//                });
//    }

    private Mono<Map<String, Integer>> nextId() {
        return webClient.get()
                .uri("/cluster/nextid")
                .header(HttpHeaders.AUTHORIZATION,
                        "PVEAPIToken=" + tokenId + "=" + tokenValue)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<>() {});
    }

    private String getCiuser(String ide) {
        if (ide.contains("ubuntu"))
            return "ubuntu";
        return "default";
    }

    private Mono<String> findByFingerprint(String userAccessToken, String fingerprint) {
        return userServiceClient.getSshKeys(userAccessToken)
                .map(list -> list.stream()
                        .filter(k -> fingerprint.equals(k.fingerprint()))
                        .findFirst()
                        .map(KeyDto::publicKey)
                        .orElse(null))
                .flatMap(pk -> pk != null
                        ? Mono.just(pk)
                        : Mono.error(new IllegalArgumentException("Fingerprint not found: " + fingerprint)));
    }
}
