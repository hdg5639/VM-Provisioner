package cloud.gamja.identity_bridge.config;

import cloud.gamja.identity_bridge.downstream.DownstreamRoutesProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(DownstreamRoutesProperties.class)
public class DownstreamConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}