
package com.example.demo.controller;

import com.example.demo.dto.QuestionResponse;
import com.example.demo.entity.Question;
import com.example.demo.service.QuestionService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@CrossOrigin(origins = "http://localhost:4200")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("/generate")
    public ResponseEntity<List<QuestionResponse>> generateQuestions(
            @RequestParam String examType,
            @RequestParam Long subjectId,
            @RequestParam Long topicId,
            @RequestParam String difficulty,
            @RequestParam int questionCount) {

        List<Question> questions = questionService.generateQuestions(
                examType,
                subjectId,
                topicId,
                difficulty,
                questionCount
        );

        return ResponseEntity.ok(convertToResponse(questions));
    }

    // Get every saved question for a particular PDF
    @GetMapping("/paper/{paperId}")
    public ResponseEntity<List<QuestionResponse>> getQuestionsByPaper(
            @PathVariable Long paperId) {

        List<Question> questions =
                questionService.getQuestionsByPaperId(paperId);

        return ResponseEntity.ok(convertToResponse(questions));
    }

    private List<QuestionResponse> convertToResponse(
            List<Question> questions) {

        return questions.stream()
                .map(question -> new QuestionResponse(
                        question.getId(),
                        question.getQuestionText(),

                        question.getOptionA(),
                        question.getOptionB(),
                        question.getOptionC(),
                        question.getOptionD(),

                        question.getDifficulty(),
                        question.getQuestionType(),

                        question.getPageNumber(),
                        question.getQuestionNumber(),

                        question.getImagePath(),
                        question.getDiagramPath(),

                        question.getMathExpressionsJson(),

                        question.getQuestionPaper() != null
                                ? question.getQuestionPaper().getId()
                                : null
                ))
                .toList();
    }
}
