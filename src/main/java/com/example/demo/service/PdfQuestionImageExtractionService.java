
package com.example.demo.service;

import com.example.demo.dto.PdfQuestionImage;
import com.example.demo.dto.PdfQuestionRegion;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.util.Matrix;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PdfQuestionImageExtractionService {

    private static final int MIN_IMAGE_WIDTH = 60;
    private static final int MIN_IMAGE_HEIGHT = 40;
    private static final float REGION_MARGIN = 45f;

    public List<PdfQuestionImage> extractQuestionImages(
            String filePath,
            Long paperId,
            List<PdfQuestionRegion> regions) throws IOException {

        List<PdfQuestionImage> extractedImages = new ArrayList<>();

        if (regions == null || regions.isEmpty()) {
            return extractedImages;
        }

        Path outputDirectory = Paths.get(
                "uploads",
                "images",
                "paper-" + paperId,
                "diagrams"
        ).toAbsolutePath().normalize();

        Files.createDirectories(outputDirectory);

        try (PDDocument document =
                     Loader.loadPDF(new File(filePath))) {

            Map<Integer, List<PdfQuestionRegion>> regionsByPage =
                    new HashMap<>();

            for (PdfQuestionRegion region : regions) {
                regionsByPage
                        .computeIfAbsent(
                                region.getPageNumber(),
                                key -> new ArrayList<>()
                        )
                        .add(region);
            }

            for (Map.Entry<Integer, List<PdfQuestionRegion>> entry
                    : regionsByPage.entrySet()) {

                int pageNumber = entry.getKey();

                if (pageNumber < 1 ||
                        pageNumber > document.getNumberOfPages()) {
                    continue;
                }

                PDPage page = document.getPage(pageNumber - 1);

                if (page.getRotation() % 360 != 0) {
                    System.out.println(
                            "Skipping rotated page: " + pageNumber
                    );
                    continue;
                }

                List<PdfQuestionRegion> pageRegions = entry.getValue();

                pageRegions.sort(
                        Comparator.comparingDouble(
                                PdfQuestionRegion::getY
                        )
                );

                ImagePlacementEngine engine =
                        new ImagePlacementEngine(page);

                engine.processPage(page);

                int[] imageIndexes = new int[pageRegions.size()];

                for (ImagePlacement placement : engine.getPlacements()) {

                    BufferedImage image = placement.image();

                    if (image == null) {
                        continue;
                    }

                    try {
                        if (image.getWidth() < MIN_IMAGE_WIDTH ||
                                image.getHeight() < MIN_IMAGE_HEIGHT) {
                            continue;
                        }

                        int matchedIndex = findMatchingRegion(
                                placement,
                                pageRegions
                        );

                        if (matchedIndex == -1) {
                            continue;
                        }

                        PdfQuestionRegion matchedRegion =
                                pageRegions.get(matchedIndex);

                        int imageIndex = imageIndexes[matchedIndex]++;

                        String imageName =
                                "page-" + pageNumber
                                        + "-question-"
                                        + matchedRegion.getQuestionNumber()
                                        + "-diagram-"
                                        + imageIndex
                                        + ".png";

                        Path imagePath =
                                outputDirectory.resolve(imageName);

                        ImageIO.write(
                                image,
                                "PNG",
                                imagePath.toFile()
                        );

                        extractedImages.add(
                                new PdfQuestionImage(
                                        matchedRegion.getQuestionNumber(),
                                        pageNumber,
                                        imagePath.toString()
                                )
                        );

                    } finally {
                        image.flush();
                    }
                }
            }
        }

        return extractedImages;
    }

    private int findMatchingRegion(
            ImagePlacement placement,
            List<PdfQuestionRegion> regions) {

        int bestIndex = -1;
        double bestOverlap = 0;

        float imageCenterX =
                (placement.minX() + placement.maxX()) / 2f;

        float imageCenterY =
                (placement.minY() + placement.maxY()) / 2f;

        for (int i = 0; i < regions.size(); i++) {

            PdfQuestionRegion region = regions.get(i);

            float left = region.getX() - REGION_MARGIN;
            float top = region.getY() - REGION_MARGIN;

            float right =
                    region.getX() + region.getWidth() + REGION_MARGIN;

            float bottom =
                    region.getY() + region.getHeight() + REGION_MARGIN;

            if (imageCenterX < left || imageCenterX > right ||
                    imageCenterY < top || imageCenterY > bottom) {
                continue;
            }

            float intersectionWidth = Math.max(
                    0,
                    Math.min(placement.maxX(), right)
                            - Math.max(placement.minX(), left)
            );

            float intersectionHeight = Math.max(
                    0,
                    Math.min(placement.maxY(), bottom)
                            - Math.max(placement.minY(), top)
            );

            double overlap =
                    intersectionWidth * intersectionHeight;

            if (overlap > bestOverlap) {
                bestOverlap = overlap;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    private record ImagePlacement(
            BufferedImage image,
            float minX,
            float minY,
            float maxX,
            float maxY
    ) {
    }

    private static class ImagePlacementEngine
            extends PDFGraphicsStreamEngine {

        private final List<ImagePlacement> placements =
                new ArrayList<>();

        private final PDPage page;

        ImagePlacementEngine(PDPage page) {
            super(page);
            this.page = page;
        }

        List<ImagePlacement> getPlacements() {
            return placements;
        }

        @Override
        public void drawImage(PDImage pdImage) throws IOException {

            Matrix matrix =
                    getGraphicsState().getCurrentTransformationMatrix();

            Point2D p1 = matrix.transformPoint(0, 0);
            Point2D p2 = matrix.transformPoint(1, 0);
            Point2D p3 = matrix.transformPoint(0, 1);
            Point2D p4 = matrix.transformPoint(1, 1);

            float minPdfX = (float) Math.min(
                    Math.min(p1.getX(), p2.getX()),
                    Math.min(p3.getX(), p4.getX())
            );

            float maxPdfX = (float) Math.max(
                    Math.max(p1.getX(), p2.getX()),
                    Math.max(p3.getX(), p4.getX())
            );

            float minPdfY = (float) Math.min(
                    Math.min(p1.getY(), p2.getY()),
                    Math.min(p3.getY(), p4.getY())
            );

            float maxPdfY = (float) Math.max(
                    Math.max(p1.getY(), p2.getY()),
                    Math.max(p3.getY(), p4.getY())
            );

            PDRectangle cropBox = page.getCropBox();

            float minX = minPdfX - cropBox.getLowerLeftX();
            float maxX = maxPdfX - cropBox.getLowerLeftX();

            float minY = cropBox.getHeight()
                    - (maxPdfY - cropBox.getLowerLeftY());

            float maxY = cropBox.getHeight()
                    - (minPdfY - cropBox.getLowerLeftY());

            BufferedImage image = pdImage.getImage();

            if (image == null) {
                return;
            }

            placements.add(
                    new ImagePlacement(
                            image,
                            minX,
                            minY,
                            maxX,
                            maxY
                    )
            );
        }

        @Override
        public void appendRectangle(
                Point2D p0,
                Point2D p1,
                Point2D p2,
                Point2D p3) {
        }

        @Override
        public void clip(int windingRule) {
        }

        @Override
        public void moveTo(float x, float y) {
        }

        @Override
        public void lineTo(float x, float y) {
        }

        @Override
        public void curveTo(
                float x1,
                float y1,
                float x2,
                float y2,
                float x3,
                float y3) {
        }

        @Override
        public Point2D getCurrentPoint() {
            return null;
        }

        @Override
        public void closePath() {
        }

        @Override
        public void endPath() {
        }

        @Override
        public void strokePath() {
        }

        @Override
        public void fillPath(int windingRule) {
        }

        @Override
        public void fillAndStrokePath(int windingRule) {
        }

        @Override
        public void shadingFill(COSName shadingName) {
        }
    }
}
