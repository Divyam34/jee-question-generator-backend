
package com.example.demo.dto;

public class PdfPageImage {

    private int pageNumber;
    private String imagePath;

    public PdfPageImage() {
    }

    public PdfPageImage(int pageNumber, String imagePath) {
        this.pageNumber = pageNumber;
        this.imagePath = imagePath;
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
