package org.friesoft.porturl.security;

import org.friesoft.porturl.config.PorturlProperties;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(PorturlProperties.class)
public class CustomSecurityConfiguration implements WebMvcConfigurer {

    private final PorturlProperties portUrlProperties;
    private final CustomGrantedAuthoritiesExtractor authoritiesExtractor;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    public CustomSecurityConfiguration(PorturlProperties portUrlProperties, CustomGrantedAuthoritiesExtractor authoritiesExtractor) {
        this.portUrlProperties = portUrlProperties;
        this.authoritiesExtractor = authoritiesExtractor;
    }

    public static class YamlHttpMessageConverter implements HttpMessageConverter<Object> {
        private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        private final List<MediaType> supportedMediaTypes = List.of(new MediaType("application", "x-yaml"));

        @Override
        public boolean canRead(@NonNull Class<?> clazz, MediaType mediaType) {
            return supportedMediaTypes.getFirst().isCompatibleWith(mediaType);
        }

        @Override
        public boolean canWrite(@NonNull Class<?> clazz, MediaType mediaType) {
            return supportedMediaTypes.getFirst().isCompatibleWith(mediaType);
        }

        @Override
        @NullMarked
        public List<MediaType> getSupportedMediaTypes() {
            return supportedMediaTypes;
        }

        @Override
        public Object read(@NonNull Class<?> clazz, HttpInputMessage inputMessage) throws IOException {
            return yamlMapper.readValue(inputMessage.getBody(), clazz);
        }

        @Override
        public void write(Object o, MediaType contentType, HttpOutputMessage outputMessage) throws IOException {
            yamlMapper.writeValue(outputMessage.getBody(), o);
        }
    }

    @Bean
    public YamlHttpMessageConverter yamlHttpMessageConverter() {
        return new YamlHttpMessageConverter();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(30000);
        requestFactory.setReadTimeout(30000);
        RestTemplate restTemplate = new RestTemplate(requestFactory);

        return NimbusJwtDecoder.withIssuerLocation(issuerUri)
                .restOperations(restTemplate)
                .build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        if (!portUrlProperties.getSecurity().isEnabled()) {
            http
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests((authz) -> authz
                    .anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable);
        } else {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests((authz) -> authz
                    .requestMatchers("/actuator/info").permitAll()
                    .requestMatchers("/swagger-ui/**").permitAll()
                    .requestMatchers("/v3/api-docs*/**").permitAll()
                    // Allow telemetry from the mobile app
                    .requestMatchers(HttpMethod.POST, "/otlp/**").permitAll()
                    // Allow public, unauthenticated GET requests to the image serving endpoint.
                    // Security is maintained because the filenames are unguessable UUIDs.
                    .requestMatchers(HttpMethod.GET, "/api/images/**").permitAll()
                    .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                        .anyRequest().authenticated())
                        .oauth2ResourceServer((oauth2) -> oauth2
                            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

            return http.build();
        }
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesExtractor::extractAuthorities);
        return converter;
    }

    /**
     * This new @Bean explicitly defines the CORS configuration for Spring Security.
     * It's the standard and most reliable way to configure CORS for a secured application.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Use the allowed origins from your application properties
        configuration.setAllowedOrigins(portUrlProperties.getCors().getAllowedOrigins());
        // Set the allowed HTTP methods
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Set the allowed headers
        configuration.setAllowedHeaders(List.of("*"));
        // Allow credentials (e.g., cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Apply this configuration to all endpoints in your application
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
