package com.example.demo.controller;

import com.example.demo.entity.QuestionPaper;
import com.example.demo.repository.QuestionPaperRepository;
import com.example.demo.service.PdfStorageService;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:4200")
public class PdfUploadController {

    private final PdfStorageService pdfStorageService;
    private final QuestionPaperRepository questionPaperRepository;

    public PdfUploadController(
            PdfStorageService pdfStorageService,
            QuestionPaperRepository questionPaperRepository) {
        this.pdfStorageService = pdfStorageService;
        this.questionPaperRepository = questionPaperRepository;
    }

    @PostMapping(
            value = "/upload-pdf",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> uploadPdf(
            @RequestParam("file") MultipartFile file) throws IOException {

        String filePath = pdfStorageService.storePdf(file);

        QuestionPaper paper = new QuestionPaper();
        paper.setFileName(file.getOriginalFilename());
        paper.setFilePath(filePath);
        paper.setUploadedAt(LocalDateTime.now());

        QuestionPaper savedPaper =
                questionPaperRepository.save(paper);

        return ResponseEntity.ok(Map.of(
                "message", "PDF uploaded successfully",
                "paperId", savedPaper.getId(),
                "fileName", savedPaper.getFileName()
        ));
    }
}