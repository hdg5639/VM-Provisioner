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
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;

import java.net.http.HttpTimeoutException;
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

    /*-----------------------Node 조회-----------------------*/
    public Mono<Map<String,Object>> getNodes() {
        return webClient.get()
                .uri("/nodes")
                .header(HttpHeaders.AUTHORIZATION,
                        "PVEAPIToken=" + tokenId + "=" + tokenValue)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<>() {});
    }

    /*-----------------------VM 생성-----------------------*/
    public Mono<Map<String, Object>> createVmOptimize(
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
            vm.setScsi0(disk.toString()+"G");
            // Template 방식으로 변경되어 현재는 사용하지 않지만 추후 OS 추가될 수도 있기 때문에 놔둠
            vm.setIde0("local:iso/" + ide + ",media=cdrom");
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
                    Mono<Map<String, Object>> result = cloneFromTemplate(vm, vmType);

                    return Mono.zip(result, Mono.just(vm))
                            .flatMap(body -> {
                                Map<String, Object> map = body.getT1();
                                map.put("vm", body.getT2());
                                return Mono.just(map);
                            });
                });
    }

    // 템플릿 클론
    private Mono<Map<String, Object>> cloneFromTemplate(VmCreate vm, VmType vmType) {
        log.info("Cloning VM {} from template {}", vm.getVmid(), vmType);

        MultiValueMap<String, String> cloneParams = new LinkedMultiValueMap<>();
        cloneParams.add("newid", vm.getVmid().toString());
        cloneParams.add("name", vm.getName());
        cloneParams.add("full", "1");
        cloneParams.add("pool", vm.getPool());

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
                .flatMap(result -> customizeClonedVm(vm));
    }

    // 클론된 VM 세부 설정 수정
    private Mono<Map<String, Object>> customizeClonedVm(VmCreate vm) {
        log.info("Customizing cloned VM {}", vm.getVmid());

        MultiValueMap<String, String> configParams = getStringStringMultiValueMap(vm);

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
                .flatMap(result -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("vmid", vm.getVmid());
                    response.put("status", "created");
                    response.put("message", "VM cloned and configured successfully");
                    return resizeDisk(vm.getVmid(), vm.getScsi0())
                            .thenReturn(response);
                });
    }

    private static MultiValueMap<String, String> getStringStringMultiValueMap(VmCreate vm) {
        MultiValueMap<String, String> configParams = new LinkedMultiValueMap<>();

        // 리소스 조정
        configParams.add("cores", String.valueOf(vm.getCores()));
        configParams.add("memory", vm.getMemory());
        configParams.add("cpu", vm.getCpu());

        // SSH 키 설정
        configParams.add("sshkeys", UriUtils.encode(vm.getSshkeys(), "UTF-8"));
        log.info("ssh key: {}", configParams.get("sshkeys"));

        configParams.add("ipconfig0", vm.getIpconfig0());

        // 추가 설정들
        if (vm.getCpulimit() > 0) {
            configParams.add("cpulimit", String.valueOf(vm.getCpulimit()));
        }
        if (vm.getCpuunits() != 1024) {
            configParams.add("cpuunits", String.valueOf(vm.getCpuunits()));
        }
        return configParams;
    }

    // 디스크 크기 변경
    private Mono<Map<String, Object>> resizeDisk(Integer vmid, String size) {
        log.info("Resizing disk scsi0 of VM {} to {}", vmid, size);

        MultiValueMap<String, String> resizeParams = new LinkedMultiValueMap<>();
        resizeParams.add("disk", "scsi0");
        resizeParams.add("size", size);

        return webClient.put()
                .uri("/nodes/{node}/qemu/{vmid}/resize", "pve", vmid)
                .header(HttpHeaders.AUTHORIZATION, "PVEAPIToken " + tokenId + "=" + tokenValue)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(resizeParams))
                .exchangeToMono(res -> {
                    if (res.statusCode().is2xxSuccessful()) {
                        log.info("Success resizing disk scsi0 of VM {} to {}", vmid, size);
                        return res.bodyToMono(new ParameterizedTypeReference<>() {});
                    }
                    return res.bodyToMono(String.class)
                            .flatMap(body -> Mono.error(new IllegalStateException(
                                    "Resize disk failed: " + res.statusCode().value() + ": " + body)));
                });
    }

    // 사용 가능한 다음 VMID 조회
    private Mono<Map<String, Integer>> nextId() {
        return webClient.get()
                .uri("/cluster/nextid")
                .header(HttpHeaders.AUTHORIZATION,
                        "PVEAPIToken=" + tokenId + "=" + tokenValue)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<>() {});
    }

    // SSH key fingerprint 기반 조회
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
