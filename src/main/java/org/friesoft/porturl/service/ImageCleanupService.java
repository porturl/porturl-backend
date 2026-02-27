package org.friesoft.porturl.service;

import org.friesoft.porturl.config.PorturlProperties;
import org.friesoft.porturl.entities.Application;
import org.friesoft.porturl.entities.User;
import org.friesoft.porturl.repositories.ApplicationRepository;
import org.friesoft.porturl.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Service
public class ImageCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(ImageCleanupService.class);

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final PorturlProperties properties;

    public ImageCleanupService(ApplicationRepository applicationRepository,
                               UserRepository userRepository,
                               FileStorageService fileStorageService,
                               PorturlProperties properties) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.properties = properties;
    }

    /**
     * A scheduled task that periodically cleans up orphaned image files.
     * The schedule is defined by the 'porturl.cleanup.cron' property.
     * The task is only enabled if 'porturl.cleanup.enabled' is true.
     */
    @Scheduled(cron = "${porturl.cleanup.cron}")
    public void cleanupOrphanedImages() {
        if (!properties.getCleanup().isEnabled()) {
            logger.info("Image cleanup task is disabled by configuration.");
            return;
        }

        logger.debug("Starting orphaned image cleanup task...");

        // 1. Get all active image identifiers from the database
        Set<String> activeImageFiles = new HashSet<>();
        Iterable<Application> applications = applicationRepository.findAll();
        applications.forEach(app -> {
            if (app.getIcon() != null) activeImageFiles.add(app.getIcon());
        });

        Iterable<User> users = userRepository.findAll();
        users.forEach(user -> {
            if (user.getImage() != null) activeImageFiles.add(user.getImage());
        });

        logger.debug("Found {} active image references in the database.", activeImageFiles.size());

        // 2. Compare with files on disk and delete orphans
        AtomicInteger deletedCount = new AtomicInteger();
        try (Stream<Path> storedFiles = fileStorageService.listAllFiles()) {
            storedFiles.forEach(filePath -> {
                String filename = filePath.getFileName().toString();
                if (!activeImageFiles.contains(filename)) {
                    try {
                        fileStorageService.delete(filename);
                        logger.debug("Deleted orphaned image file: {}", filename);
                        deletedCount.getAndIncrement();
                    } catch (IOException e) {
                        logger.debug("Failed to delete orphaned file: {}", filename, e);
                    }
                }
            });
        } catch (IOException e) {
            logger.error("Failed to read stored files for cleanup.", e);
        }

        logger.debug("Orphaned image cleanup task finished. Deleted {} files.", deletedCount);
    }
}
