package org.friesoft.porturl.service;

import org.friesoft.porturl.config.PorturlProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileStorageServiceTest {

    private FileStorageService fileStorageService;
    private Path tempRoot;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        tempRoot = tempDir.resolve("uploads");
        PorturlProperties properties = mock(PorturlProperties.class, Answers.RETURNS_DEEP_STUBS);
        when(properties.getStorage().getLocation()).thenReturn(tempRoot.toString());
        
        fileStorageService = new FileStorageService(properties);
    }

    @Test
    void store_savesMultipartFile() throws IOException {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", "some image data".getBytes());

        // Act
        String filename = fileStorageService.store(file);

        // Assert
        assertNotNull(filename);
        assertTrue(filename.endsWith(".png"));
        assertTrue(Files.exists(tempRoot.resolve(filename)));
        assertEquals("some image data", Files.readString(tempRoot.resolve(filename)));
    }

    @Test
    void store_throwsOnEmptyFile() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "", "image/png", new byte[0]);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> fileStorageService.store(file));
    }

    @Test
    void storeBytes_savesByteArray() throws IOException {
        // Arrange
        byte[] bytes = "raw bytes".getBytes();
        String filename = "raw.bin";

        // Act
        String savedFilename = fileStorageService.storeBytes(bytes, filename);

        // Assert
        assertEquals(filename, savedFilename);
        assertTrue(Files.exists(tempRoot.resolve(filename)));
        assertArrayEquals(bytes, Files.readAllBytes(tempRoot.resolve(filename)));
    }

    @Test
    void delete_removesFile() throws IOException {
        // Arrange
        Path file = tempRoot.resolve("delete-me.txt");
        Files.writeString(file, "content");
        assertTrue(Files.exists(file));

        // Act
        fileStorageService.delete("delete-me.txt");

        // Assert
        assertFalse(Files.exists(file));
    }

    @Test
    void listAllFiles_returnsFiles() throws IOException {
        // Arrange
        Files.writeString(tempRoot.resolve("file1.txt"), "c1");
        Files.writeString(tempRoot.resolve("file2.txt"), "c2");

        // Act
        List<Path> files = fileStorageService.listAllFiles().collect(Collectors.toList());

        // Assert
        assertEquals(2, files.size());
        assertTrue(files.contains(Path.of("file1.txt")));
        assertTrue(files.contains(Path.of("file2.txt")));
    }
}
