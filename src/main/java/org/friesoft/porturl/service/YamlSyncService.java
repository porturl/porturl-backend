package org.friesoft.porturl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.friesoft.porturl.config.PorturlProperties;
import org.friesoft.porturl.dto.ExportData;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class YamlSyncService {

    private final PorturlProperties properties;
    private final AdminService adminService;
    private final ObjectMapper yamlMapper;
    private final ExecutorService watcherExecutor = Executors.newSingleThreadExecutor();
    private WatchService watchService;

    public YamlSyncService(PorturlProperties properties, AdminService adminService) {
        this.properties = properties;
        this.adminService = adminService;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (properties.getStorage().getType() == PorturlProperties.StorageType.YAML) {
            log.info("YAML Storage mode enabled. Initializing sync from: {}", properties.getStorage().getYamlPath());
            
            // 1. Initial Sync
            syncFromFile();

            // 2. Start Watcher
            startFileWatcher();
        }
    }

    @PreDestroy
    public void cleanup() {
        watcherExecutor.shutdownNow();
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                log.error("Error closing watch service", e);
            }
        }
    }

    private void syncFromFile() {
        File file = new File(properties.getStorage().getYamlPath());
        log.info("Reading YAML from: {}", file.getAbsolutePath());
        if (!file.exists()) {
            log.warn("YAML config file not found at {}. Skipping initial sync.", file.getAbsolutePath());
            return;
        }

        try {
            log.info("Syncing data from YAML file...");
            ExportData data = yamlMapper.readValue(file, ExportData.class);
            adminService.syncData(data, null);
            adminService.updateSyncHash(data);
            log.info("YAML sync completed successfully.");
        } catch (Exception e) {
            log.error("Failed to sync from YAML config file", e);
        }
    }

    private void startFileWatcher() {
        Path path = Paths.get(properties.getStorage().getYamlPath()).toAbsolutePath();
        Path directory = path.getParent();
        if (directory == null) {
            directory = Paths.get(".");
        }
        String filename = path.getFileName().toString();

        final Path finalDirectory = directory;
        watcherExecutor.submit(() -> {
            try {
                this.watchService = FileSystems.getDefault().newWatchService();
                finalDirectory.register(watchService, 
                    StandardWatchEventKinds.ENTRY_MODIFY, 
                    StandardWatchEventKinds.ENTRY_CREATE);

                log.info("Watching directory for YAML changes: {}", finalDirectory);

                while (!Thread.currentThread().isInterrupted()) {
                    WatchKey key = watchService.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        Path context = (Path) event.context();
                        if (context.toString().equals(filename)) {
                            log.info("YAML file change detected ({}): {}", event.kind(), filename);
                            // Small delay to let Puppet/K8s finish writing the file
                            Thread.sleep(1000);
                            syncFromFile();
                        }
                    }
                    if (!key.reset()) {
                        log.warn("WatchKey no longer valid, stopping watcher.");
                        break;
                    }
                }
            } catch (IOException | InterruptedException e) {
                if (!(e instanceof InterruptedException)) {
                    log.error("File watcher error", e);
                }
                Thread.currentThread().interrupt();
            }
        });
    }
}
