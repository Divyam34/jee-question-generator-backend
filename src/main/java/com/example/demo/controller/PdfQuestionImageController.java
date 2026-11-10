
package com.example.demo.controller;

import com.example.demo.dto.PdfTextLine;
import com.example.demo.dto.PdfQuestionRegion;
import com.example.demo.dto.PdfQuestionImage;

import com.example.demo.entity.QuestionPaper;
import com.example.demo.repository.QuestionPaperRepository;

import com.example.demo.service.PdfTextLineExtractionService;
import com.example.demo.service.PdfQuestionRegionDetectionService;
import com.example.demo.service.PdfQuestionImageExtractionService;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:4200")
public class PdfQuestionImageController {

    private final QuestionPaperRepository paperRepository;
    private final PdfTextLineExtractionService lineExtractionService;
    private final PdfQuestionRegionDetectionService regionDetectionService;
    private final PdfQuestionImageExtractionService imageExtractionService;

    public PdfQuestionImageController(
            QuestionPaperRepository paperRepository,
            PdfTextLineExtractionService lineExtractionService,
            PdfQuestionRegionDetectionService regionDetectionService,
            PdfQuestionImageExtractionService imageExtractionService) {

        this.paperRepository = paperRepository;
        this.lineExtractionService = lineExtractionService;
        this.regionDetectionService = regionDetectionService;
        this.imageExtractionService = imageExtractionService;
    }

    @GetMapping("/extract-question-images/{paperId}")
    public ResponseEntity<?> extractQuestionImages(
            @PathVariable Long paperId) throws IOException {

        QuestionPaper paper = paperRepository.findById(paperId)
                .orElseThrow(() ->
                        new IllegalArgumentException("PDF not found"));

        List<PdfTextLine> lines =
                lineExtractionService.extractLines(paper.getFilePath());

        List<PdfQuestionRegion> regions =
                regionDetectionService.detectQuestions(lines);

        List<PdfQuestionImage> images =
                imageExtractionService.extractQuestionImages(
                        paper.getFilePath(),
                        paperId,
                        regions
                );

        return ResponseEntity.ok(Map.of(
                "paperId", paperId,
                "fileName", paper.getFileName(),
                "questions", regions,
                "images", images
        ));
    }

    // Display an extracted diagram image
    @GetMapping(
            value = "/question-diagrams/{paperId}/{fileName:.+}",
            produces = MediaType.IMAGE_PNG_VALUE
    )
    public ResponseEntity<Resource> getQuestionDiagram(
            @PathVariable Long paperId,
            @PathVariable String fileName) throws IOException {

        // Prevent directory traversal
        if (fileName.contains("/") || fileName.contains("\\")
                || fileName.equals(".") || fileName.equals("..")) {
            return ResponseEntity.badRequest().build();
        }

        Path imagePath = Paths.get(
                "uploads",
                "images",
                "paper-" + paperId,
                "diagrams",
                fileName
        ).toAbsolutePath().normalize();

        if (!Files.exists(imagePath) || !Files.isRegularFile(imagePath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(imagePath);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(resource);
    }
}
