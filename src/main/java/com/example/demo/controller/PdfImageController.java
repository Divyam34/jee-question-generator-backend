
package com.example.demo.controller;

import com.example.demo.repository.QuestionPaperRepository;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:4200")
public class PdfImageController {

    private final QuestionPaperRepository paperRepository;

    public PdfImageController(QuestionPaperRepository paperRepository) {
        this.paperRepository = paperRepository;
    }

    // Full PDF page image
    @GetMapping("/images/{paperId}/{fileName:.+}")
    public ResponseEntity<Resource> getPageImage(
            @PathVariable Long paperId,
            @PathVariable String fileName) throws Exception {

        if (!fileName.matches("page-[0-9]+\\.png")) {
            return ResponseEntity.badRequest().build();
        }

        return getImage(
                paperId,
                "paper-" + paperId,
                fileName
        );
    }

    // Question images and diagrams
    @GetMapping("/question-images/{paperId}/{fileName:.+}")
    public ResponseEntity<Resource> getQuestionImage(
            @PathVariable Long paperId,
            @PathVariable String fileName) throws Exception {

        if (!fileName.matches(
                "page-[0-9]+-question-[0-9]+(-diagram-[0-9]+)?\\.png")) {
            return ResponseEntity.badRequest().build();
        }

        String directory;

        if (fileName.contains("-diagram-")) {
            directory = "paper-" + paperId + "/diagrams";
        } else {
            directory = "paper-" + paperId + "/questions";
        }

        return getImage(paperId, directory, fileName);
    }

    // Common image loading method
    private ResponseEntity<Resource> getImage(
            Long paperId,
            String directory,
            String fileName) throws Exception {

        paperRepository.findById(paperId)
                .orElseThrow(() ->
                        new IllegalArgumentException("PDF not found"));

        Path imageDirectory = Paths.get(
                "uploads",
                "images",
                directory
        ).toAbsolutePath().normalize();

        Path imagePath = imageDirectory
                .resolve(fileName)
                .normalize();

        if (!imagePath.startsWith(imageDirectory)) {
            return ResponseEntity.badRequest().build();
        }

        Resource resource = new UrlResource(imagePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(resource);
    }
}
