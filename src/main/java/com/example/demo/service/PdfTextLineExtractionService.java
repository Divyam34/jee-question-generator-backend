
package com.example.demo.service;

import com.example.demo.dto.PdfTextLine;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfTextLineExtractionService {

    public List<PdfTextLine> extractLines(String filePath)
            throws IOException {

        List<PdfTextLine> lines = new ArrayList<>();

        try (PDDocument document =
                     Loader.loadPDF(new File(filePath))) {

            PDFTextStripper stripper = new PDFTextStripper() {

                @Override
                protected void writeString(
                        String text,
                        List<TextPosition> positions) throws IOException {

                    if (positions == null || positions.isEmpty()
                            || text == null || text.isBlank()) {
                        return;
                    }

                    float minX = Float.MAX_VALUE;
                    float minY = Float.MAX_VALUE;
                    float maxX = -Float.MAX_VALUE;
                    float maxY = -Float.MAX_VALUE;

                    for (TextPosition position : positions) {

                        float x = position.getXDirAdj();
                        float y = position.getYDirAdj();

                        float width = position.getWidthDirAdj();
                        float height = position.getHeightDir();

                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);

                        maxX = Math.max(maxX, x + width);
                        maxY = Math.max(maxY, y + height);
                    }

                    if (minX == Float.MAX_VALUE
                            || minY == Float.MAX_VALUE) {
                        return;
                    }

                    lines.add(new PdfTextLine(
                            getCurrentPageNo(),
                            text.trim(),
                            minX,
                            minY,
                            maxX - minX,
                            maxY - minY
                    ));
                }
            };

            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(document.getNumberOfPages());

            stripper.getText(document);
        }

        return lines;
    }
}
