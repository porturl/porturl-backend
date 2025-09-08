package org.friesoft.porturl.service;

import org.friesoft.porturl.config.PorturlProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path rootLocation;

    public FileStorageService(PorturlProperties properties) {
        String uploadDir = properties.getStorage().getLocation();
        if (uploadDir == null || uploadDir.isBlank()) {
            uploadDir = "upload-dir";
        }
        this.rootLocation = Paths.get(uploadDir);
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location: " + uploadDir, e);
        }
    }

    /**
     * Stores an uploaded file with a unique name, without any resizing.
     * @param file The uploaded image file.
     * @return The unique identifier (filename) for the stored image.
     */
    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Failed to store empty file.");
        }

        try {
            // Generate a unique filename to prevent conflicts
            String extension = getFileExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID().toString() + "." + extension;

            Path destinationFile = this.rootLocation.resolve(filename);
            Files.copy(file.getInputStream(), destinationFile);

            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }
    }

    /**
     * Loads a file by its unique filename.
     * @param filename The unique identifier of the image.
     * @return The Path to the requested image file.
     */
    public Path load(String filename) {
        return this.rootLocation.resolve(filename);
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return ""; // No extension
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}

