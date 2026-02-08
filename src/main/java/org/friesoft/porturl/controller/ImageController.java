package org.friesoft.porturl.controller;

import org.friesoft.porturl.api.ImageApi;
import org.friesoft.porturl.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
public class ImageController implements ImageApi {

    private final FileStorageService fileStorageService;

    public ImageController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public ResponseEntity<org.friesoft.porturl.dto.ImageUploadResponse> uploadImage(MultipartFile file) {
        String filename = fileStorageService.store(file);
        org.friesoft.porturl.dto.ImageUploadResponse response = new org.friesoft.porturl.dto.ImageUploadResponse();
        response.setFilename(filename);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<org.springframework.core.io.Resource> serveImage(String filename) {
        Path filePath = fileStorageService.load(filename);
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() || resource.isReadable()) {
                String contentType = null;
                try {
                    contentType = Files.probeContentType(filePath);
                } catch (IOException _) { }

                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }
}
