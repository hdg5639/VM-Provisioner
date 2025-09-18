package cloud.gamja.identity_bridge.downstream;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "downstream")
public class DownstreamRoutesProperties {
    // ex) routes.vm=http://vm-service:8080
    private Map<String, String> routes = new HashMap<>();
}