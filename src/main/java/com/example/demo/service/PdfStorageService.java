package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class PdfStorageService {

    private final Path storageLocation =
            Paths.get("uploads/papers").toAbsolutePath().normalize();

    public String storePdf(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a PDF file.");
        }

        if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new IllegalArgumentException("Only PDF files are allowed.");
        }

        Files.createDirectories(storageLocation);

        String storedFileName =
                UUID.randomUUID() + ".pdf";

        Path targetLocation =
                storageLocation.resolve(storedFileName);

        Files.copy(file.getInputStream(), targetLocation);

        return targetLocation.toString();
    }
}