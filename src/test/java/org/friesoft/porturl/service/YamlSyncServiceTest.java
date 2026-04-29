package org.friesoft.porturl.service;

import org.friesoft.porturl.config.PorturlProperties;
import org.friesoft.porturl.dto.ExportData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class YamlSyncServiceTest {

    @Mock
    private PorturlProperties properties;

    @Mock
    private AdminService adminService;

    @InjectMocks
    private YamlSyncService yamlSyncService;

    @TempDir
    Path tempDir;

    private Path yamlFile;

    @BeforeEach
    void setUp() {
        yamlFile = tempDir.resolve("porturl.yml");
    }

    @AfterEach
    void tearDown() {
        yamlSyncService.cleanup();
    }

    @Test
    void testInit_SqlStorage_DoesNothing() {
        PorturlProperties.Storage storage = new PorturlProperties.Storage();
        ReflectionTestUtils.setField(storage, "type", PorturlProperties.StorageType.SQL);
        when(properties.getStorage()).thenReturn(storage);

        yamlSyncService.init();

        verify(adminService, never()).syncData(any(), any());
        verify(adminService, never()).updateSyncHash(any());
    }

    @Test
    void testInit_YamlStorage_FileDoesNotExist() {
        PorturlProperties.Storage storage = new PorturlProperties.Storage();
        storage.setYamlPath(yamlFile.toAbsolutePath().toString());
        ReflectionTestUtils.setField(storage, "type", PorturlProperties.StorageType.YAML);
        when(properties.getStorage()).thenReturn(storage);

        yamlSyncService.init();

        verify(adminService, never()).syncData(any(), any());
        verify(adminService, never()).updateSyncHash(any());
    }

    @Test
    void testInit_YamlStorage_FileExistsAndSyncs() throws IOException {
        String yamlContent = "version: 1\n"; // basic yaml that can be parsed as ExportData (even if empty)
        Files.writeString(yamlFile, yamlContent);

        PorturlProperties.Storage storage = new PorturlProperties.Storage();
        storage.setYamlPath(yamlFile.toAbsolutePath().toString());
        ReflectionTestUtils.setField(storage, "type", PorturlProperties.StorageType.YAML);
        when(properties.getStorage()).thenReturn(storage);

        yamlSyncService.init();

        // Verify that initial sync happened
        verify(adminService, times(1)).syncData(any(ExportData.class), eq(null));
        verify(adminService, times(1)).updateSyncHash(any(ExportData.class));
    }

    @Test
    void testInit_YamlStorage_InvalidFile() throws IOException {
        String yamlContent = "invalid: yaml: ["; 
        Files.writeString(yamlFile, yamlContent);

        PorturlProperties.Storage storage = new PorturlProperties.Storage();
        storage.setYamlPath(yamlFile.toAbsolutePath().toString());
        ReflectionTestUtils.setField(storage, "type", PorturlProperties.StorageType.YAML);
        when(properties.getStorage()).thenReturn(storage);

        yamlSyncService.init();

        // Parse should fail, so syncData shouldn't be called
        verify(adminService, never()).syncData(any(), any());
        verify(adminService, never()).updateSyncHash(any());
    }
}
