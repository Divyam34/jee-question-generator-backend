
package com.example.demo.service;

import com.example.demo.dto.PdfPageImage;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfPageImageExtractionService {

    public List<PdfPageImage> extractPageImages(
            String filePath, Long paperId) throws IOException {

        File pdfFile = new File(filePath);

        Path imageDirectory = Paths.get(
                "uploads",
                "images",
                "paper-" + paperId
        ).toAbsolutePath().normalize();

        Files.createDirectories(imageDirectory);

        List<PdfPageImage> images = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdfFile)) {

            PDFRenderer renderer = new PDFRenderer(document);

            for (int page = 0;
                 page < document.getNumberOfPages();
                 page++) {

                // Render PDF page as an image at 150 DPI
                BufferedImage bufferedImage =
                        renderer.renderImageWithDPI(
                                page,
                                150,
                                ImageType.RGB
                        );

                int pageNumber = page + 1;

                Path imagePath = imageDirectory.resolve(
                        "page-" + pageNumber + ".png"
                );

                // Save the image as PNG
                boolean saved = ImageIO.write(
                        bufferedImage,
                        "PNG",
                        imagePath.toFile()
                );

                if (!saved) {
                    throw new IOException(
                            "Unable to save image for page " + pageNumber
                    );
                }

                images.add(
                        new PdfPageImage(
                                pageNumber,
                                imagePath.toString()
                        )
                );

                bufferedImage.flush();
            }
        }

        return images;
    }
}
