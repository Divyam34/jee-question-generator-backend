
package com.example.demo.dto;

public class QuestionResponse {

    private Long id;
    private String questionText;

    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;

    private String difficulty;
    private String questionType;

    private Integer pageNumber;
    private Integer questionNumber;

    private String imagePath;
    private String diagramPath;

    // Mathematical expressions recognized by pix2tex
    private String mathExpressionsJson;

    private Long paperId;

    public QuestionResponse() {
    }

    public QuestionResponse(
            Long id,
            String questionText,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            String difficulty,
            String questionType,
            Integer pageNumber,
            Integer questionNumber,
            String imagePath,
            String diagramPath,
            String mathExpressionsJson,
            Long paperId
    ) {
        this.id = id;
        this.questionText = questionText;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.difficulty = difficulty;
        this.questionType = questionType;
        this.pageNumber = pageNumber;
        this.questionNumber = questionNumber;
        this.imagePath = imagePath;
        this.diagramPath = diagramPath;
        this.mathExpressionsJson = mathExpressionsJson;
        this.paperId = paperId;
    }

    public Long getId() {
        return id;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getOptionA() {
        return optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getQuestionType() {
        return questionType;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public Integer getQuestionNumber() {
        return questionNumber;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getDiagramPath() {
        return diagramPath;
    }

    public String getMathExpressionsJson() {
        return mathExpressionsJson;
    }

    public Long getPaperId() {
        return paperId;
    }
}
