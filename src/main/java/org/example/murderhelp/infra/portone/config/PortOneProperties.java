package org.example.murderhelp.infra.portone.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "portone")
public class PortOneProperties {
    private String baseUrl;
    private String apiSecret;
    private String storeId;
    private String channelKey;
    private String webhookSecret;
}