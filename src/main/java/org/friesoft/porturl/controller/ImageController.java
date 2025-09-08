package org.friesoft.porturl.controller;

import org.friesoft.porturl.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final FileStorageService fileStorageService;

    public ImageController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * Endpoint for uploading a new image. The client is responsible for resizing.
     * @param file The image file from the multipart request.
     * @return A JSON object containing the unique identifier for the uploaded image.
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> handleFileUpload(@RequestParam("file") MultipartFile file) {
        String filename = fileStorageService.store(file);
        // The key is now just 'filename' for clarity
        return ResponseEntity.ok().body(Map.of("filename", filename));
    }

    /**
     * Endpoint for serving an image by its unique filename.
     * @param filename The unique identifier of the image.
     * @return The image file as a resource.
     */
    @GetMapping("/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        Path filePath = fileStorageService.load(filename);
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                // Return a 404 Not Found if the file doesn't exist
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }
}

