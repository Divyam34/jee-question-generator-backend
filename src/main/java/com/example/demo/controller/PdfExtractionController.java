
package com.example.demo.controller;

import com.example.demo.dto.PdfPageText;
import com.example.demo.dto.PdfPageImage;

import com.example.demo.entity.QuestionPaper;
import com.example.demo.repository.QuestionPaperRepository;

import com.example.demo.service.PdfTextExtractionService;
import com.example.demo.service.PdfPageImageExtractionService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:4200")
public class PdfExtractionController {

    private final PdfTextExtractionService extractionService;
    private final PdfPageImageExtractionService imageExtractionService;
    private final QuestionPaperRepository paperRepository;

    public PdfExtractionController(
            PdfTextExtractionService extractionService,
            PdfPageImageExtractionService imageExtractionService,
            QuestionPaperRepository paperRepository) {

        this.extractionService = extractionService;
        this.imageExtractionService = imageExtractionService;
        this.paperRepository = paperRepository;
    }

    @GetMapping("/extract-text/{paperId}")
    public ResponseEntity<?> extractText(
            @PathVariable Long paperId) throws IOException {

        QuestionPaper paper = paperRepository.findById(paperId)
                .orElseThrow(() ->
                        new IllegalArgumentException("PDF not found"));

        List<PdfPageText> pages =
                extractionService.extractPages(paper.getFilePath());

        List<PdfPageImage> images =
                imageExtractionService.extractPageImages(
                        paper.getFilePath(),
                        paperId
                );

        return ResponseEntity.ok(
                Map.of(
                        "paperId", paperId,
                        "fileName", paper.getFileName(),
                        "pages", pages,
                        "images", images
                )
        );
    }
}
