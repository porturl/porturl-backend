package org.friesoft.porturl.service;

import org.friesoft.porturl.config.PorturlProperties;
import org.friesoft.porturl.entities.Application;
import org.friesoft.porturl.entities.User;
import org.friesoft.porturl.repositories.ApplicationRepository;
import org.friesoft.porturl.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageCleanupServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private PorturlProperties properties;

    @Mock
    private PorturlProperties.CleanupProperties cleanupProperties;

    @InjectMocks
    private ImageCleanupService imageCleanupService;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getCleanup()).thenReturn(cleanupProperties);
    }

    @Test
    void testCleanupOrphanedImages_Disabled() {
        when(cleanupProperties.isEnabled()).thenReturn(false);

        imageCleanupService.cleanupOrphanedImages();

        verify(applicationRepository, never()).findAll();
        verify(userRepository, never()).findAll();
        try {
            verify(fileStorageService, never()).listAllFiles();
        } catch (IOException e) {
            // Should not happen
        }
    }

    @Test
    void testCleanupOrphanedImages_Enabled_WithOrphans() throws IOException {
        when(cleanupProperties.isEnabled()).thenReturn(true);

        // Mock applications
        Application app1 = new Application();
        app1.setIcon("app-icon.png");
        Application app2 = new Application();
        app2.setIcon(null); // Should handle null

        when(applicationRepository.findAll()).thenReturn(Arrays.asList(app1, app2));

        // Mock users
        User user1 = new User();
        user1.setImage("user-image.png");
        User user2 = new User(); // image is null

        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        // Mock files on disk
        Path activeAppFile = Paths.get("data/app-icon.png");
        Path activeUserFile = Paths.get("data/user-image.png");
        Path orphanedFile = Paths.get("data/orphaned.png");

        when(fileStorageService.listAllFiles()).thenReturn(Stream.of(activeAppFile, activeUserFile, orphanedFile));

        // Execute
        imageCleanupService.cleanupOrphanedImages();

        // Verify
        verify(fileStorageService).delete("orphaned.png");
        verify(fileStorageService, never()).delete("app-icon.png");
        verify(fileStorageService, never()).delete("user-image.png");
    }

    @Test
    void testCleanupOrphanedImages_HandlesIOExceptionOnList() throws IOException {
        when(cleanupProperties.isEnabled()).thenReturn(true);
        when(applicationRepository.findAll()).thenReturn(Collections.emptyList());
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        when(fileStorageService.listAllFiles()).thenThrow(new IOException("Cannot read directory"));

        // Execute
        imageCleanupService.cleanupOrphanedImages(); // Should not throw exception, just log error

        verify(fileStorageService, never()).delete(anyString());
    }

    @Test
    void testCleanupOrphanedImages_HandlesIOExceptionOnDelete() throws IOException {
        when(cleanupProperties.isEnabled()).thenReturn(true);
        when(applicationRepository.findAll()).thenReturn(Collections.emptyList());
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        Path orphanedFile = Paths.get("data/orphaned.png");
        when(fileStorageService.listAllFiles()).thenReturn(Stream.of(orphanedFile));

        doThrow(new IOException("Cannot delete")).when(fileStorageService).delete("orphaned.png");

        // Execute
        imageCleanupService.cleanupOrphanedImages(); // Should catch exception and log it

        verify(fileStorageService).delete("orphaned.png");
    }
}
