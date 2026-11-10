
package com.example.demo.service;

import com.example.demo.dto.PdfQuestionRegion;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PdfQuestionCropService {

    private static final float DPI = 150f;
    private static final float SCALE = DPI / 72f;
    private static final float PADDING = 15f;

    public Map<String, String> cropQuestions(
            String pdfPath,
            Long paperId,
            List<PdfQuestionRegion> regions) throws Exception {

        Map<String, String> imagePaths = new HashMap<>();

        if (regions == null || regions.isEmpty()) {
            return imagePaths;
        }

        Path outputDirectory = Path.of(
                "uploads",
                "images",
                "paper-" + paperId,
                "questions"
        );

        Files.createDirectories(outputDirectory);

        try (PDDocument document =
                     Loader.loadPDF(new File(pdfPath))) {

            PDFRenderer renderer = new PDFRenderer(document);

            for (PdfQuestionRegion region : regions) {

                int pageNumber = region.getPageNumber();
                int questionNumber = region.getQuestionNumber();

                int pageIndex = pageNumber - 1;

                if (pageIndex < 0 ||
                        pageIndex >= document.getNumberOfPages()) {
                    System.out.println(
                            "Invalid page number: " + pageNumber
                    );
                    continue;
                }

                if (questionNumber < 1) {
                    System.out.println(
                            "Invalid question number on page: " + pageNumber
                    );
                    continue;
                }

                String key = pageNumber + "-" + questionNumber;

                // Do not silently overwrite another region.
                if (imagePaths.containsKey(key)) {
                    throw new IllegalStateException(
                            "Duplicate question region detected: " + key
                    );
                }

                PDPage page = document.getPage(pageIndex);

                if (page.getRotation() != 0) {
                    System.out.println(
                            "Skipping rotated page: " + pageNumber
                    );
                    continue;
                }

                BufferedImage pageImage =
                        renderer.renderImageWithDPI(pageIndex, DPI);

                float regionX = region.getX();
                float regionY = region.getY();
                float regionWidth = region.getWidth();
                float regionHeight = region.getHeight();

                if (!Float.isFinite(regionX) ||
                        !Float.isFinite(regionY) ||
                        !Float.isFinite(regionWidth) ||
                        !Float.isFinite(regionHeight) ||
                        regionWidth <= 0 ||
                        regionHeight <= 0) {

                    System.out.println(
                            "Invalid crop coordinates for " + key
                    );
                    continue;
                }

                /*
                 * Coordinates are assumed to be top-left-origin
                 * PDF points from PdfTextLineExtractionService.
                 *
                 * This conversion must be validated against the
                 * rendered page before using these crops for saving.
                 */
                int x = (int) Math.floor(
                        (regionX - PADDING) * SCALE
                );

                int y = (int) Math.floor(
                        (regionY - PADDING) * SCALE
                );

                int right = (int) Math.ceil(
                        (regionX + regionWidth + PADDING) * SCALE
                );

                int bottom = (int) Math.ceil(
                        (regionY + regionHeight + PADDING) * SCALE
                );

                // Keep crop coordinates within the rendered page.
                x = Math.max(0, x);
                y = Math.max(0, y);

                right = Math.min(pageImage.getWidth(), right);
                bottom = Math.min(pageImage.getHeight(), bottom);

                int width = right - x;
                int height = bottom - y;

                if (width <= 0 || height <= 0) {
                    System.out.println(
                            "Crop is outside page boundaries: " + key
                    );
                    continue;
                }

                BufferedImage cropped =
                        pageImage.getSubimage(x, y, width, height);

                // Create an independent image instead of retaining
                // a reference to the complete rendered page.
                BufferedImage questionImage = new BufferedImage(
                        width,
                        height,
                        BufferedImage.TYPE_INT_RGB
                );

                Graphics2D graphics = questionImage.createGraphics();

                try {
                    graphics.drawImage(cropped, 0, 0, null);
                } finally {
                    graphics.dispose();
                }

                String fileName =
                        "page-" + pageNumber
                                + "-question-" + questionNumber
                                + ".png";

                Path outputPath = outputDirectory.resolve(fileName);

                boolean written = ImageIO.write(
                        questionImage,
                        "PNG",
                        outputPath.toFile()
                );

                if (!written) {
                    throw new IllegalStateException(
                            "Could not write question image: " + fileName
                    );
                }

                imagePaths.put(key, fileName);

                System.out.println(
                        "Cropped " + key
                                + " | x=" + x
                                + " | y=" + y
                                + " | width=" + width
                                + " | height=" + height
                                + " | image=" + fileName
                );
            }
        }

        return imagePaths;
    }
}
