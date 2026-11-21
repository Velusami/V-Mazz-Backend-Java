package com.example.catalog.vmazz.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class UploadStorageService {
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private final Path root;

    public UploadStorageService(@Value("${vmazz.upload-dir:./uploads}") String directory) throws IOException {
        this.root = Path.of(directory).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public String saveImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Please select an image to upload");
        if (file.getSize() > 5 * 1024 * 1024) throw new IllegalArgumentException("Image size must not exceed 5 MB");
        if (!ALLOWED.contains(file.getContentType())) throw new IllegalArgumentException("Only JPG, PNG, WEBP and GIF images are allowed");
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "image" : file.getOriginalFilename());
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')).toLowerCase() : ".jpg";
        if (!Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif").contains(ext)) throw new IllegalArgumentException("Unsupported image extension");
        String name = UUID.randomUUID() + ext;
        Path folderPath = root.resolve(folder).normalize();
        if (!folderPath.startsWith(root)) throw new IllegalArgumentException("Invalid upload path");
        try {
            Files.createDirectories(folderPath);
            try (var input = file.getInputStream()) {
                Files.copy(input, folderPath.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not save image", e);
        }
        return "/uploads/" + folder + "/" + name;
    }
}
