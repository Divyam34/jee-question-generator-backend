
package com.example.demo.service;

import com.example.demo.dto.PdfQuestionRegion;
import com.example.demo.dto.PdfTextLine;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfQuestionRegionDetectionService {

    // Supports: 1. Question, 1) Question, Q1. Question
    private static final Pattern QUESTION_PATTERN =
            Pattern.compile(
                    "^\\s*(?:Q\\s*)?(\\d{1,3})\\s*[.)]\\s*(.*)$",
                    Pattern.CASE_INSENSITIVE
            );

    // Supports the PDF's MCQ options: (a), (b), (c), (d)
    private static final Pattern OPTION_PATTERN =
            Pattern.compile(
                    "\\(\\s*([a-d])\\s*\\)",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern SOLUTION_PATTERN =
            Pattern.compile(
                    "^\\s*(?:ANSWERS\\s+AND\\s+SOLUTIONS|ANSWER\\s+KEY|SOLUTIONS)\\s*$",
                    Pattern.CASE_INSENSITIVE
            );

    private static final float SAME_LINE_TOLERANCE = 3.0f;

    public List<PdfQuestionRegion> detectQuestions(
            List<PdfTextLine> extractedLines) {

        List<PdfQuestionRegion> questions = new ArrayList<>();

        if (extractedLines == null || extractedLines.isEmpty()) {
            System.out.println("PDF extraction returned no text lines.");
            return questions;
        }

        System.out.println("========== QUESTION DETECTION ==========");
        System.out.println("Input text lines: " + extractedLines.size());

        List<PdfTextLine> sortedLines = new ArrayList<>(extractedLines);

        sortedLines.removeIf(line ->
                line == null ||
                        line.getText() == null ||
                        line.getText().trim().isEmpty()
        );

        sortedLines.sort(
                Comparator.comparingInt(PdfTextLine::getPageNumber)
                        .thenComparingDouble(PdfTextLine::getY)
                        .thenComparingDouble(PdfTextLine::getX)
        );

        // Combine PDFBox chunks belonging to the same visual line.
        List<PdfTextLine> lines = combineVisualLines(sortedLines);

        System.out.println("Combined visual lines: " + lines.size());

        QuestionBuilder currentQuestion = null;
        int expectedQuestionNumber = 1;
        int previousPage = -1;

        for (PdfTextLine line : lines) {

            String text = line.getText().trim();

            if (text.isEmpty()) {
                continue;
            }

            // Stop before answer/solution pages.
            if (SOLUTION_PATTERN.matcher(text).matches()) {

                if (currentQuestion != null) {
                    addCompletedQuestion(questions, currentQuestion);
                    currentQuestion = null;
                }

                System.out.println(
                        "Solution section found on page "
                                + line.getPageNumber()
                );

                break;
            }

            // Finish the previous question when the page changes.
            if (previousPage != -1 &&
                    line.getPageNumber() != previousPage) {

                if (currentQuestion != null) {
                    addCompletedQuestion(questions, currentQuestion);
                    currentQuestion = null;
                }
            }

            previousPage = line.getPageNumber();

            Matcher matcher = QUESTION_PATTERN.matcher(text);

            if (matcher.matches()) {

                int number = Integer.parseInt(matcher.group(1));

                String remainingText = matcher.group(2) == null
                        ? ""
                        : matcher.group(2).trim();

                boolean hasQPrefix = text.matches(
                        "(?i)^\\s*Q\\s*\\d{1,3}\\s*[.)].*"
                );

                /*
                 * This PDF numbers its questions sequentially:
                 * 1, 2, 3 ... 75.
                 *
                 * The sequence check prevents most ordinary numbered
                 * values inside a question from becoming new questions.
                 */
                boolean validStart =
                        hasQPrefix || number == expectedQuestionNumber;

                if (validStart) {

                    if (currentQuestion != null) {
                        addCompletedQuestion(questions, currentQuestion);
                    }

                    currentQuestion = new QuestionBuilder(
                            number,
                            line.getPageNumber()
                    );

                    // Keep the question text after its number.
                    if (!remainingText.isEmpty()) {
                        currentQuestion.addLine(
                                new PdfTextLine(
                                        line.getPageNumber(),
                                        remainingText,
                                        line.getX(),
                                        line.getY(),
                                        line.getWidth(),
                                        line.getHeight()
                                )
                        );
                    }

                    expectedQuestionNumber = number + 1;

                    System.out.println(
                            "Detected question " + number
                                    + " on page " + line.getPageNumber()
                    );

                    continue;
                }
            }

            // Add this line to the current question.
            if (currentQuestion != null) {
                currentQuestion.addLine(line);
            }
        }

        // Finish the final question.
        if (currentQuestion != null) {
            addCompletedQuestion(questions, currentQuestion);
        }

        System.out.println("----------------------------------------");
        System.out.println(
                "Total detected question regions: " + questions.size()
        );

        for (PdfQuestionRegion question : questions) {

            System.out.println(
                    "Q" + question.getQuestionNumber()
                            + " | Page: " + question.getPageNumber()
            );

            System.out.println(
                    "Question: " + question.getQuestionText()
            );

            System.out.println("A: " + question.getOptionA());
            System.out.println("B: " + question.getOptionB());
            System.out.println("C: " + question.getOptionC());
            System.out.println("D: " + question.getOptionD());

            System.out.println("----------------------------------------");
        }

        System.out.println("========== DETECTION FINISHED ==========");

        return questions;
    }

    private void addCompletedQuestion(
            List<PdfQuestionRegion> questions,
            QuestionBuilder builder) {

        PdfQuestionRegion region = builder.build();

        if (region == null) {
            return;
        }

        if (region.getQuestionText() == null ||
                region.getQuestionText().trim().isEmpty()) {

            System.out.println(
                    "Warning: Q" + region.getQuestionNumber()
                            + " has empty extracted text."
            );
        }

        questions.add(region);
    }

    /*
     * PDFBox can split one visual line into several text chunks.
     * This combines chunks with approximately the same Y coordinate.
     */
    private List<PdfTextLine> combineVisualLines(
            List<PdfTextLine> sortedLines) {

        List<PdfTextLine> result = new ArrayList<>();

        PdfTextLine firstLine = null;
        StringBuilder text = new StringBuilder();

        float lineY = 0;
        float minX = 0;
        float minY = 0;
        float maxX = 0;
        float maxY = 0;

        for (PdfTextLine line : sortedLines) {

            boolean newVisualLine =
                    firstLine == null ||
                            firstLine.getPageNumber() != line.getPageNumber() ||
                            Math.abs(lineY - line.getY()) > SAME_LINE_TOLERANCE;

            if (newVisualLine) {

                if (firstLine != null) {
                    result.add(createVisualLine(
                            firstLine.getPageNumber(),
                            text.toString(),
                            minX,
                            minY,
                            maxX,
                            maxY
                    ));
                }

                firstLine = line;
                lineY = line.getY();

                text = new StringBuilder(line.getText().trim());

                minX = line.getX();
                minY = line.getY();
                maxX = line.getX() + line.getWidth();
                maxY = line.getY() + line.getHeight();

            } else {

                String chunk = line.getText().trim();

                if (!chunk.isEmpty()) {
                    if (text.length() > 0) {
                        text.append(" ");
                    }

                    text.append(chunk);
                }

                minX = Math.min(minX, line.getX());
                minY = Math.min(minY, line.getY());

                maxX = Math.max(
                        maxX,
                        line.getX() + line.getWidth()
                );

                maxY = Math.max(
                        maxY,
                        line.getY() + line.getHeight()
                );
            }
        }

        if (firstLine != null) {
            result.add(createVisualLine(
                    firstLine.getPageNumber(),
                    text.toString(),
                    minX,
                    minY,
                    maxX,
                    maxY
            ));
        }

        return result;
    }

    private PdfTextLine createVisualLine(
            int pageNumber,
            String text,
            float minX,
            float minY,
            float maxX,
            float maxY) {

        return new PdfTextLine(
                pageNumber,
                text.trim(),
                minX,
                minY,
                Math.max(0, maxX - minX),
                Math.max(0, maxY - minY)
        );
    }

    private static class QuestionBuilder {

        private final int questionNumber;
        private final int pageNumber;

        private final StringBuilder questionText =
                new StringBuilder();

        private final StringBuilder[] options = {
                new StringBuilder(),
                new StringBuilder(),
                new StringBuilder(),
                new StringBuilder()
        };

        private int currentOption = -1;

        private float minX = Float.MAX_VALUE;
        private float minY = Float.MAX_VALUE;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;

        QuestionBuilder(int questionNumber, int pageNumber) {
            this.questionNumber = questionNumber;
            this.pageNumber = pageNumber;
        }

        void addLine(PdfTextLine line) {

            if (line == null || line.getText() == null) {
                return;
            }

            String value = line.getText().trim();

            if (value.isEmpty()) {
                return;
            }

            updateCoordinates(line);

            Matcher matcher = OPTION_PATTERN.matcher(value);

            int previousIndex = 0;
            boolean optionFound = false;

            while (matcher.find()) {

                optionFound = true;

                String beforeOption = value
                        .substring(previousIndex, matcher.start())
                        .trim();

                appendText(beforeOption);

                char optionLetter =
                        matcher.group(1).toUpperCase().charAt(0);

                currentOption = optionLetter - 'A';

                previousIndex = matcher.end();
            }

            String remaining = value.substring(previousIndex).trim();

            if (optionFound) {
                appendText(remaining);
            } else {
                appendText(value);
            }
        }

        private void appendText(String value) {

            if (value == null || value.isBlank()) {
                return;
            }

            StringBuilder destination;

            if (currentOption >= 0 && currentOption < 4) {
                destination = options[currentOption];
            } else {
                destination = questionText;
            }

            if (destination.length() > 0) {
                destination.append(" ");
            }

            destination.append(value);
        }

        private void updateCoordinates(PdfTextLine line) {

            minX = Math.min(minX, line.getX());
            minY = Math.min(minY, line.getY());

            maxX = Math.max(
                    maxX,
                    line.getX() + line.getWidth()
            );

            maxY = Math.max(
                    maxY,
                    line.getY() + line.getHeight()
            );
        }

        PdfQuestionRegion build() {

            if (minX == Float.MAX_VALUE ||
                    minY == Float.MAX_VALUE) {
                return null;
            }

            PdfQuestionRegion region = new PdfQuestionRegion();

            region.setQuestionNumber(questionNumber);
            region.setPageNumber(pageNumber);

            region.setQuestionText(questionText.toString().trim());

            region.setOptionA(getOption(0));
            region.setOptionB(getOption(1));
            region.setOptionC(getOption(2));
            region.setOptionD(getOption(3));

            region.setX(minX);
            region.setY(minY);
            region.setWidth(Math.max(0, maxX - minX));
            region.setHeight(Math.max(0, maxY - minY));

            return region;
        }

        private String getOption(int index) {

            String value = options[index].toString().trim();

            return value.isEmpty() ? null : value;
        }
    }
}
