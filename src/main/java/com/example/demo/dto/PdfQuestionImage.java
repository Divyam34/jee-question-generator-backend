
package com.example.demo.dto;

public class PdfQuestionImage {

    private int questionNumber;
    private int pageNumber;
    private String imagePath;

    public PdfQuestionImage() {
    }

    public PdfQuestionImage(
            int questionNumber,
            int pageNumber,
            String imagePath) {

        this.questionNumber = questionNumber;
        this.pageNumber = pageNumber;
        this.imagePath = imagePath;
    }

    public int getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(int questionNumber) {
        this.questionNumber = questionNumber;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}
