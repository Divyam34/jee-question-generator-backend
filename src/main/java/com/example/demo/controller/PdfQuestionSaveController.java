
package com.example.demo.controller;

import com.example.demo.service.PdfQuestionSaveService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:4200")
public class PdfQuestionSaveController {

    private final PdfQuestionSaveService questionSaveService;

    public PdfQuestionSaveController(
            PdfQuestionSaveService questionSaveService) {
        this.questionSaveService = questionSaveService;
    }

    @PostMapping("/save-extracted-questions/{paperId}")
    public ResponseEntity<?> saveExtractedQuestions(
            @PathVariable Long paperId,
            @RequestParam String examType,
            @RequestParam Long subjectId,
            @RequestParam Long topicId,
            @RequestParam String difficulty) {

        try {
            int savedCount = questionSaveService.extractAndSaveQuestions(
                    paperId,
                    examType,
                    subjectId,
                    topicId,
                    difficulty
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Questions saved successfully",
                    "paperId", paperId,
                    "savedCount", savedCount
            ));

        } catch (Exception e) {

            // Print the actual error in Spring Boot console
            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "message", "Failed to save extracted questions",
                            "error", e.getMessage() != null
                                    ? e.getMessage()
                                    : e.getClass().getSimpleName()
                    )
            );
        }
    }
}
