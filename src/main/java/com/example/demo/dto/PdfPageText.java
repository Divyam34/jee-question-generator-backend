package com.example.demo.dto;

public class PdfPageText {

    private int pageNumber;
    private String extractedText;

    public PdfPageText() {
    }

    public PdfPageText(int pageNumber, String extractedText) {
        this.pageNumber = pageNumber;
        this.extractedText = extractedText;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }
}