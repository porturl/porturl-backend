package org.friesoft.porturl.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class CustomSecurityConfiguration {

    @Value("${application.security.enabled:true}")
    private boolean securityEnabled;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        if (!securityEnabled) {
            http.authorizeHttpRequests((authz) -> authz
                    .anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable);
        } else {
            http.authorizeHttpRequests((authz) -> authz
                    .anyRequest().authenticated())
                    .oauth2ResourceServer((oauth2) -> oauth2
                        .jwt(Customizer.withDefaults())
                    ).cors(Customizer.withDefaults());
        }
        return http.build();
    }
}