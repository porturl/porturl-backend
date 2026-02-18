package org.friesoft.porturl.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.filter.FilterFunctions.stripPrefix;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.path;

@Configuration
@ConditionalOnProperty(name = "porturl.otel.enabled", havingValue = "true")
public class GatewayConfig {

    private final PorturlProperties portUrlProperties;

    public GatewayConfig(PorturlProperties portUrlProperties) {
        this.portUrlProperties = portUrlProperties;
    }

    @Bean
    public RouterFunction<ServerResponse> otlpRoute() {
        return route("otlp_proxy")
                .POST("/otlp/**", http())
                .before(uri(URI.create(portUrlProperties.getOtel().getAlloyUrl())))
                .filter(stripPrefix(1))
                .build();
    }
}