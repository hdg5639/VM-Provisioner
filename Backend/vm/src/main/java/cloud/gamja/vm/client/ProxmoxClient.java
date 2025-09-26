package cloud.gamja.vm.client;

import cloud.gamja.vm.client.enums.Audience;
import cloud.gamja.vm.client.record.KeyDto;
import cloud.gamja.vm.client.record.VmCreate;
import cloud.gamja.vm.vms.enums.VmType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;


@Component
@RequiredArgsConstructor
public class ProxmoxClient {
    private final WebClient webClient = WebClient.builder().build();
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
        return webClient.post()
                    .uri(uriBuilder ->
                            uriBuilder.path("/nodes/pve/qemu")
                                    .queryParam("vmid", vm.getVmid())
                                    .queryParam("name", vm.getName())
                                    .queryParam("cores", vm.getCores())
                                    .queryParam("cpu", vm.getCpu())
                                    .queryParam("cpulimit", vm.getCpulimit())
                                    .queryParam("cpuunits", vm.getCpuunits())
                                    .queryParam("affinity", vm.getAffinity())
                                    .queryParam("memory", vm.getMemory())
                                    .queryParam("ostype", vm.getOstype())
                                    .queryParam("pool", vm.getPool())
                                    .queryParam("agent", vm.getAgent())
                                    .queryParam("scsihw", vm.getScsihw())
                                    .queryParam("scsi0", vm.getScsi0())
                                    .queryParam("net0", vm.getNet0())
                                    .queryParam("ide2", vm.getIde2())
                                    .queryParam("ciuser", vm.getCiuser())
                                    .queryParam("sshkeys", vm.getSshkeys())
                                    .queryParam("ipconfig0", vm.getIpconfig0())
                                    .queryParam("nameserver", vm.getNameserver())
                    .build())
                    .header(HttpHeaders.AUTHORIZATION,
                            "PVEAPIToken=" + tokenId + "=" + tokenValue)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<>() {});
    }

    public Mono<Map<String,Object>> createVmOptimize(
            String subjectToken, String userId, String fingerprint, VmType vmType, String name, Integer disk, String ide) {
        // VmId
        Mono<Integer> vmIdMono = nextId()
                .map(response -> response.get("data"))
                .timeout(Duration.ofSeconds(10))
                .onErrorMap(e -> new HttpTimeoutException("NextId failed: " + e.getMessage()));
        // VmTemplate
        Mono<VmCreate> vmTemplateMono = Mono.defer(() -> {
            VmCreate vm = new VmCreate(vmType);
            vm.setName(name);
            vm.setScsi0("local-lvm:" + disk);
            vm.setIde2("local:iso/" + ide + ",media=cdrom");
            vm.setCiuser(getCiuser(ide));
            return Mono.just(vm);
        });
        // SSH key
        Mono<String> exchangedToken = tokenExchangeClient.exchange(subjectToken, Audience.USER)
                .map((response) -> response.get("access_token"));
        Mono<String> sshKey = Mono.zip(exchangedToken, Mono.just(userId))
                .flatMap(tuple -> userServiceClient.getSshKeys(tuple.getT1(), tuple.getT2())
                        .map(response -> {
                            KeyDto key = response.stream()
                                    .filter(v -> v.fingerprint().equals(fingerprint))
                                    .findFirst()
                                    .orElse(null);
                            return key!=null?key.publicKey():null;
                        })
                        .flatMap(pk -> pk != null
                                ? Mono.just(pk)
                                :Mono.error(new IllegalArgumentException("SshKey not found"))
                        )
                );

        return Mono.zip(vmIdMono, vmTemplateMono, sshKey)
                .map(tuple -> {
                    VmCreate vm = tuple.getT2();
                    vm.setVmid(tuple.getT1());
                    vm.setSshkeys(tuple.getT3());
                    return vm;
                })
                .flatMap(this::createVmRequest);
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
}
