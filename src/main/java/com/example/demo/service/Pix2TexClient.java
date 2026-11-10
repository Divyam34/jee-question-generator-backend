
package com.example.demo.service;

import com.example.demo.dto.Pix2TexResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class Pix2TexClient {

    @Value("${math.ocr.url}")
    private String ocrUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String recognizeMath(
            byte[] imageBytes,
            String fileName
    ) {

        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Image cannot be empty");
        }

        ByteArrayResource imageResource = new ByteArrayResource(imageBytes) {

            @Override
            public String getFilename() {
                return fileName;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("file", imageResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> request =
                new HttpEntity<>(body, headers);

        try {

            ResponseEntity<Pix2TexResponse> response =
                    restTemplate.postForEntity(
                            ocrUrl,
                            request,
                            Pix2TexResponse.class
                    );

            if (response.getBody() == null ||
                    response.getBody().getLatex() == null) {

                throw new IllegalStateException(
                        "Python OCR returned an empty response"
                );
            }

            return response.getBody().getLatex();

        } catch (RestClientException e) {

            throw new IllegalStateException(
                    "Unable to communicate with Python OCR service",
                    e
            );
        }
    }
}
