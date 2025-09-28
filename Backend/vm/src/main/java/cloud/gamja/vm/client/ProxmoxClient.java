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

    public Mono<Map<String,Object>> getNodes() {
        return webClient.get()
                .uri("/nodes")
                .header(HttpHeaders.AUTHORIZATION,
                        "PVEAPIToken=" + tokenId + "=" + tokenValue)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<>() {});
    }

    private Mono<Map<String,Object>> createVmRequest(VmCreate vm) {
        MultiValueMap<String, String> q = getStringStringMultiValueMap(vm);

        return webClient.post()
                .uri(uri -> uri.path("/nodes/{node}/qemu")
                        .queryParams(q)
                        .queryParam("sshkeys", URLEncoder.encode(vm.getSshkeys(), StandardCharsets.UTF_8)).build(vm.getNode()))
                .header(HttpHeaders.AUTHORIZATION, "PVEAPIToken " + tokenId + "=" + tokenValue)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(q))
                .exchangeToMono(res -> {
                    if (res.statusCode().is2xxSuccessful()) {
                        return res.bodyToMono(new ParameterizedTypeReference<>() {});
                    }
                    return res.bodyToMono(String.class).defaultIfEmpty("")
                            .flatMap(body -> Mono.error(new IllegalStateException("PVE " + res.statusCode().value() + ": " + body)));
                });
    }

    private static MultiValueMap<String, String> getStringStringMultiValueMap(VmCreate vm) {
        MultiValueMap<String,String> q = new LinkedMultiValueMap<>();
        q.add("vmid", String.valueOf(vm.getVmid()));
        q.add("name", vm.getName());
        q.add("cores", String.valueOf(vm.getCores()));
        q.add("cpu", vm.getCpu());
        q.add("cpulimit", String.valueOf(vm.getCpulimit()));
        q.add("cpuunits", String.valueOf(vm.getCpuunits()));
        // affinity 임시 제거
        //q.add("affinity", vm.getAffinity());
        q.add("memory", vm.getMemory());
        q.add("ostype", vm.getOstype());
        q.add("pool", vm.getPool());
        q.add("agent", vm.getAgent());
        q.add("net0", vm.getNet0());
        q.add("scsihw", vm.getScsihw());
        q.add("scsi0", vm.getScsi0());
//        q.add("ide0", vm.getIde0());
//        q.add("ide2", vm.getIde2());
        q.add("ciuser", vm.getCiuser());
        q.add("ipconfig0", vm.getIpconfig0());
        return q;
    }

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
                .map(tuple -> {
                    VmCreate vm = tuple.getT2();
                    vm.setVmid(tuple.getT1());
                    vm.setSshkeys(tuple.getT3());
                    return vm;
                })
                .flatMap(this::createVmRequest)
                .flatMap(result -> {
                    Integer vmid = (Integer) result.get("vmid");
                    return importCloudImage(vmid, "noble-server-cloudimg-amd64.img")
                            .thenReturn(result);
                });
    }

    private Mono<Map<String, Object>> importCloudImage(Integer vmid, String cloudImageName) {
        log.info("Importing cloud image for VM {}: {}", vmid, cloudImageName);

        return executeQmImportDisk(vmid, cloudImageName)
                .then(attachImportedDisk(vmid));
    }

    private Mono<Void> executeQmImportDisk(Integer vmid, String cloudImageName) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("command", "qm");
        params.add("args", String.join(" ",
                "importdisk",
                vmid.toString(),
                "/var/lib/vz/template/iso/" + cloudImageName,
                "local-lvm"
        ));

        return webClient.post()
                .uri("/nodes/{node}/tasks", "pve")
                .header(HttpHeaders.AUTHORIZATION, "PVEAPIToken " + tokenId + "=" + tokenValue)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(params))
                .exchangeToMono(res -> {
                    if (res.statusCode().is2xxSuccessful()) {
                        return res.bodyToMono(Void.class);
                    }
                    return res.bodyToMono(String.class)
                            .flatMap(body -> Mono.error(new IllegalStateException(
                                    "Import disk failed: " + res.statusCode().value() + ": " + body)));
                });
    }

    private Mono<Map<String, Object>> attachImportedDisk(Integer vmid) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("scsi0", "local-lvm:vm-" + vmid + "-disk-0,cache=writeback,discard=on");

        return webClient.put()
                .uri("/nodes/{node}/qemu/{vmid}/config", "pve", vmid)
                .header(HttpHeaders.AUTHORIZATION, "PVEAPIToken " + tokenId + "=" + tokenValue)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(params))
                .exchangeToMono(res -> {
                    if (res.statusCode().is2xxSuccessful()) {
                        return res.bodyToMono(new ParameterizedTypeReference<>() {});
                    }
                    return res.bodyToMono(String.class)
                            .flatMap(body -> Mono.error(new IllegalStateException(
                                    "Attach disk failed: " + res.statusCode().value() + ": " + body)));
                });
    }

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
