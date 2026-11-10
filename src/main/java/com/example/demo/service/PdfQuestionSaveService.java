
package com.example.demo.service;

import com.example.demo.dto.PdfQuestionRegion;
import com.example.demo.entity.Question;
import com.example.demo.entity.QuestionPaper;
import com.example.demo.entity.Subject;
import com.example.demo.entity.Topic;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.repository.QuestionPaperRepository;
import com.example.demo.repository.SubjectRepository;
import com.example.demo.repository.TopicRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PdfQuestionSaveService {

    private final QuestionRepository questionRepository;
    private final QuestionPaperRepository paperRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;

    private final PdfTextLineExtractionService lineExtractionService;
    private final PdfQuestionRegionDetectionService regionDetectionService;
    private final PdfQuestionCropService questionCropService;

    public PdfQuestionSaveService(
            QuestionRepository questionRepository,
            QuestionPaperRepository paperRepository,
            SubjectRepository subjectRepository,
            TopicRepository topicRepository,
            PdfTextLineExtractionService lineExtractionService,
            PdfQuestionRegionDetectionService regionDetectionService,
            PdfQuestionCropService questionCropService) {

        this.questionRepository = questionRepository;
        this.paperRepository = paperRepository;
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.lineExtractionService = lineExtractionService;
        this.regionDetectionService = regionDetectionService;
        this.questionCropService = questionCropService;
    }

    @Transactional
    public int extractAndSaveQuestions(
            Long paperId,
            String examType,
            Long subjectId,
            Long topicId,
            String difficulty) throws Exception {

        System.out.println("Starting question extraction...");

        // 1. Load uploaded PDF.
        QuestionPaper paper = paperRepository.findById(paperId)
                .orElseThrow(() ->
                        new IllegalArgumentException("PDF not found"));

        // 2. Load subject.
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Subject not found"));

        // 3. Load topic.
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Topic not found"));

        if (topic.getSubject() == null ||
                !topic.getSubject().getId().equals(subjectId)) {

            throw new IllegalArgumentException(
                    "Selected topic does not belong to selected subject"
            );
        }

        // 4. Extract original PDF text lines.
        System.out.println("Extracting PDF text lines...");

        var lines = lineExtractionService.extractLines(
                paper.getFilePath()
        );

        System.out.println("Extracted lines: " + lines.size());

        // 5. Detect question regions.
        System.out.println("Detecting questions...");

        List<PdfQuestionRegion> detectedRegions =
                regionDetectionService.detectQuestions(lines);

        if (detectedRegions.isEmpty()) {
            throw new IllegalStateException(
                    "No questions detected in this PDF"
            );
        }

        System.out.println(
                "Detected question regions: " + detectedRegions.size()
        );

        // 6. Load already-saved questions for this PDF.
        List<Question> existingQuestions =
                questionRepository.findByQuestionPaper_Id(paperId);

        Set<String> existingKeys = new HashSet<>();

        for (Question existing : existingQuestions) {

            String key = createKey(
                    existing.getPageNumber(),
                    existing.getQuestionNumber()
            );

            existingKeys.add(key);
        }

        // 7. Validate regions and exclude existing questions.
        List<PdfQuestionRegion> newRegions = new ArrayList<>();

        Set<String> currentExtractionKeys = new HashSet<>();

        for (PdfQuestionRegion region : detectedRegions) {

            if (region == null) {
                continue;
            }

            Integer pageNumber = region.getPageNumber();
            Integer questionNumber = region.getQuestionNumber();

            if (pageNumber == null || pageNumber < 1 ||
                    questionNumber == null || questionNumber < 1) {

                throw new IllegalStateException(
                        "Invalid page or question number detected"
                );
            }

            String questionText = region.getQuestionText();

            if (questionText == null || questionText.isBlank()) {
                System.out.println(
                        "Skipping empty question: " + questionNumber
                );
                continue;
            }

            String key = createKey(pageNumber, questionNumber);

            // Stop if the detector returns duplicate question regions.
            if (!currentExtractionKeys.add(key)) {
                throw new IllegalStateException(
                        "Duplicate question detected in extraction: " + key
                );
            }

            // Never overwrite existing saved questions.
            if (existingKeys.contains(key)) {
                System.out.println(
                        "Skipping already-saved question: " + key
                );
                continue;
            }

            newRegions.add(region);
        }

        if (newRegions.isEmpty()) {
            System.out.println(
                    "No new questions to save. Existing records untouched."
            );
            return 0;
        }

        // 8. Generate images only for new questions.
        System.out.println("Generating question images...");

        Map<String, String> imageMap =
                questionCropService.cropQuestions(
                        paper.getFilePath(),
                        paperId,
                        newRegions
                );

        System.out.println(
                "Generated question images: " + imageMap.size()
        );

        // 9. Prepare database entities.
        List<Question> questionsToSave = new ArrayList<>();

        for (PdfQuestionRegion region : newRegions) {

            String key = createKey(
                    region.getPageNumber(),
                    region.getQuestionNumber()
            );

            String imagePath = imageMap.get(key);

            // Do not save a question if its original image is missing.
            if (imagePath == null || imagePath.isBlank()) {
                throw new IllegalStateException(
                        "Question image was not generated for: " + key
                );
            }

            Question question = new Question();

            question.setExamType(examType);
            question.setQuestionText(region.getQuestionText());

            // These are populated only when actually extracted.
            question.setOptionA(region.getOptionA());
            question.setOptionB(region.getOptionB());
            question.setOptionC(region.getOptionC());
            question.setOptionD(region.getOptionD());

            question.setQuestionNumber(region.getQuestionNumber());
            question.setPageNumber(region.getPageNumber());

            // Complete question crop is the primary visual record.
            question.setImagePath(imagePath);

            // OCR remains disabled. It must not replace PDF content.
            question.setMathExpressionsJson(null);

            // Separate diagram path is optional.
            // Diagrams inside the question crop are preserved in imagePath.
            question.setDiagramPath(null);

            question.setDifficulty(difficulty);
            question.setSubject(subject);
            question.setTopic(topic);
            question.setQuestionPaper(paper);

            questionsToSave.add(question);
        }

        if (questionsToSave.isEmpty()) {
            throw new IllegalStateException(
                    "No valid questions available to save"
            );
        }

        // 10. Save all validated questions.
        System.out.println(
                "Saving " + questionsToSave.size() + " new questions..."
        );

        questionRepository.saveAllAndFlush(questionsToSave);

        System.out.println(
                "Successfully saved " + questionsToSave.size()
                        + " new questions."
        );

        return questionsToSave.size();
    }

    private String createKey(
            Integer pageNumber,
            Integer questionNumber) {

        return pageNumber + "-" + questionNumber;
    }
}
