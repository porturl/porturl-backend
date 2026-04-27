package org.friesoft.porturl.controller;

import org.friesoft.porturl.security.CustomGrantedAuthoritiesExtractor;
import org.friesoft.porturl.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

@WebMvcTest(ImageController.class)
class ImageControllerTest {

    @TestConfiguration
    static class ControllerTestConfig {
        @Bean
        public FileStorageService fileStorageService() {
            return Mockito.mock(FileStorageService.class);
        }

        @Bean
        public CustomGrantedAuthoritiesExtractor customGrantedAuthoritiesExtractor() {
            return Mockito.mock(CustomGrantedAuthoritiesExtractor.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FileStorageService fileStorageService;

    @Test
    void getImage_returnsOk() throws Exception {
        Path mockPath = Paths.get("src/test/resources/test.png");
        when(fileStorageService.load(anyString())).thenReturn(mockPath);

        mockMvc.perform(get("/api/images/test.png")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().is4xxClientError());
    }
}
