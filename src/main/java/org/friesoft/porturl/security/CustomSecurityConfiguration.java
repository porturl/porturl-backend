package org.friesoft.porturl.security;

import org.friesoft.porturl.config.PorturlProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(PorturlProperties.class)
public class CustomSecurityConfiguration implements WebMvcConfigurer {

    private final PorturlProperties portUrlProperties;
    private final CustomGrantedAuthoritiesExtractor authoritiesExtractor;

    @org.springframework.beans.factory.annotation.Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    public CustomSecurityConfiguration(PorturlProperties portUrlProperties, CustomGrantedAuthoritiesExtractor authoritiesExtractor) {
        this.portUrlProperties = portUrlProperties;
        this.authoritiesExtractor = authoritiesExtractor;
    }

    public static class YamlHttpMessageConverter extends AbstractHttpMessageConverter<Object> {
        private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

        public YamlHttpMessageConverter() {
            super(new MediaType("application", "x-yaml"), new MediaType("application", "yaml"));
        }

        @Override
        protected boolean supports(Class<?> clazz) {
            return true;
        }

        @Override
        protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException {
            return yamlMapper.readValue(inputMessage.getBody(), clazz);
        }

        @Override
        protected void writeInternal(Object o, HttpOutputMessage outputMessage) throws IOException {
            yamlMapper.writeValue(outputMessage.getBody(), o);
        }
    }
@Bean
public YamlHttpMessageConverter yamlHttpMessageConverter() {
    return new YamlHttpMessageConverter();
}

@Bean
ServerHttpMessageConvertersCustomizer jackson3HttpMessageConverter(JsonMapper jsonMapper) {
    return builder -> builder.withJsonConverter(
            new JacksonJsonHttpMessageConverter(jsonMapper.rebuild()
                    .build())
        );
}

@Bean
public org.springframework.boot.web.servlet.FilterRegistrationBean<org.springframework.web.filter.OncePerRequestFilter> openApiAcceptHeaderFilter() {
    org.springframework.boot.web.servlet.FilterRegistrationBean<org.springframework.web.filter.OncePerRequestFilter> registrationBean = new org.springframework.boot.web.servlet.FilterRegistrationBean<>();
    registrationBean.setFilter(new org.springframework.web.filter.OncePerRequestFilter() {
        @Override
        protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, jakarta.servlet.FilterChain filterChain)
                throws jakarta.servlet.ServletException, java.io.IOException {
            if (request.getRequestURI().contains("/v3/api-docs")) {
                jakarta.servlet.http.HttpServletRequest wrapper = new jakarta.servlet.http.HttpServletRequestWrapper(request) {
                    @Override
                    public String getHeader(String name) {
                        if ("Accept".equalsIgnoreCase(name)) {
                            return "application/json";
                        }
                        return super.getHeader(name);
                    }

                    @Override
                    public java.util.Enumeration<String> getHeaders(String name) {
                        if ("Accept".equalsIgnoreCase(name)) {
                            return java.util.Collections.enumeration(java.util.Collections.singletonList("application/json"));
                        }
                        return super.getHeaders(name);
                    }
                };
                filterChain.doFilter(wrapper, response);
            } else {
                filterChain.doFilter(request, response);
            }
        }
    });
    registrationBean.setOrder(org.springframework.core.Ordered.HIGHEST_PRECEDENCE);
    return registrationBean;
}

@Bean
public JwtDecoder jwtDecoder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(java.net.HttpURLConnection connection, String httpMethod) throws IOException {
                if (connection instanceof HttpsURLConnection httpsConnection) {
                    configureSsl(httpsConnection);
                }
                super.prepareConnection(connection, httpMethod);
            }
        };
        requestFactory.setConnectTimeout(30000);
        requestFactory.setReadTimeout(30000);
        RestTemplate restTemplate = new RestTemplate(requestFactory);

        String jwkSetUri = issuerUri + (issuerUri.endsWith("/") ? "" : "/") + "protocol/openid-connect/certs";
        
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .restOperations(restTemplate)
                .build();

        if (portUrlProperties.getSecurity().isValidateIssuer()) {
            jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
        } else {
            OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator());
            jwtDecoder.setJwtValidator(validator);
        }
        
        return jwtDecoder;
    }

    private void configureSsl(HttpsURLConnection connection) {
        try {
            String trustStorePath = portUrlProperties.getKeycloak().getTruststorePath();
            if (trustStorePath != null) {
                Path path = Paths.get(trustStorePath);
                if (Files.exists(path)) {
                    String password = portUrlProperties.getKeycloak().getTruststorePassword();
                    KeyStore trustStore = KeyStore.getInstance("JKS");
                    try (FileInputStream fis = new FileInputStream(path.toFile())) {
                        trustStore.load(fis, password.toCharArray());
                    }

                    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    tmf.init(trustStore);

                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, tmf.getTrustManagers(), null);
                    
                    connection.setSSLSocketFactory(sslContext.getSocketFactory());
                    connection.setHostnameVerifier((hostname, session) -> 
                        hostname.equals("localhost") || hostname.equals("127.0.0.1") || hostname.equals("10.0.2.2")
                    );
                }
            }
        } catch (Exception e) {
            // Log or ignore
        }
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
                    .requestMatchers("/v3/api-docs/**", "/v3/api-docs").permitAll()
                    .requestMatchers(HttpMethod.POST, "/otlp/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/images/**").permitAll()
                    .requestMatchers("/auth/bridge").permitAll()
                    .requestMatchers("/api/auth/ticket").authenticated()
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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(portUrlProperties.getCors().getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
