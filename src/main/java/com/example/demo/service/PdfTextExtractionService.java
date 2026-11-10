
package com.example.demo.service;

import com.example.demo.dto.PdfPageText;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfTextExtractionService {

    public List<PdfPageText> extractPages(String filePath)
            throws IOException {

        File pdfFile = new File(filePath);

        if (!pdfFile.exists() || !pdfFile.isFile()) {
            throw new IOException("PDF file not found: " + filePath);
        }

        List<PdfPageText> pages = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdfFile)) {

            PDFTextStripper stripper = new PDFTextStripper();

            // Extract text according to its position on the page
            stripper.setSortByPosition(true);

            // Avoid extracting overlapping text multiple times
            stripper.setSuppressDuplicateOverlappingText(true);

            // Preserve paragraph and line formatting
            stripper.setLineSeparator("\n");
            stripper.setParagraphStart("\n");
            stripper.setParagraphEnd("\n");

            // Extract each page separately
            for (int page = 1;
                 page <= document.getNumberOfPages();
                 page++) {

                stripper.setStartPage(page);
                stripper.setEndPage(page);

                String extractedText = stripper.getText(document);

                // Clean unnecessary whitespace without removing
                // mathematical symbols or line breaks
                extractedText = cleanText(extractedText);

                pages.add(new PdfPageText(page, extractedText));

                System.out.println(
                        "Extracted page: " + page
                );

                System.out.println(extractedText);

                System.out.println(
                        "-----------------------------------"
                );
            }
        }

        return pages;
    }

    private String cleanText(String text) {

        if (text == null || text.isEmpty()) {
            return "";
        }

        // Normalize Windows line endings
        text = text.replace("\r\n", "\n");

        // Remove excessive blank lines
        text = text.replaceAll("\n{3,}", "\n\n");

        // Remove unnecessary spaces at line boundaries
        text = text.replaceAll("[\\t ]+\\n", "\n");

        return text.trim();
    }
}
