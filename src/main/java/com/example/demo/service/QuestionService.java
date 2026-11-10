
package com.example.demo.service;

import com.example.demo.entity.Question;
import com.example.demo.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;

    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public List<Question> generateQuestions(
            String examType,
            Long subjectId,
            Long topicId,
            String difficulty,
            int questionCount) {

        if (questionCount < 1 || questionCount > 20) {
            throw new IllegalArgumentException(
                    "Question count must be between 1 and 20"
            );
        }

        return questionRepository.generateQuestions(
                examType,
                subjectId,
                topicId,
                difficulty,
                questionCount
        );
    }

    // Fetch all extracted questions
    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }

    // Fetch questions belonging to a particular PDF
    public List<Question> getQuestionsByPaperId(Long paperId) {
        return questionRepository.findByQuestionPaper_Id(paperId);
    }
}
