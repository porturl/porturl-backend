package org.friesoft.porturl.service;

import org.friesoft.porturl.config.PorturlProperties;
import org.friesoft.porturl.dto.ExportData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class YamlSyncServiceTest {

    @Mock
    private PorturlProperties properties;

    @Mock
    private AdminService adminService;

    @InjectMocks
    private YamlSyncService yamlSyncService;

    private Path tempYamlFile;

    @BeforeEach
    void setUp() throws IOException {
        tempYamlFile = Files.createTempFile("test-config", ".yaml");
        Files.writeString(tempYamlFile, "{}");
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempYamlFile);
        yamlSyncService.cleanup();
    }

    @Test
    void testInit_SqlStorage_DoesNothing() {
        PorturlProperties.Storage storage = new PorturlProperties.Storage();
        when(properties.getStorage()).thenReturn(storage);

        yamlSyncService.init();

        verify(adminService, never()).syncData(any(), any());
    }

    @Test
    void testInit_YamlStorage_PerformsSync() throws Exception {
        PorturlProperties.Storage storage = new PorturlProperties.Storage();
        storage.setYamlPath(tempYamlFile.toString());
        ReflectionTestUtils.setField(storage, "type", PorturlProperties.StorageType.YAML);

        when(properties.getStorage()).thenReturn(storage);

        yamlSyncService.init();

        verify(adminService, times(1)).syncData(any(ExportData.class), eq(null));
    }
}
