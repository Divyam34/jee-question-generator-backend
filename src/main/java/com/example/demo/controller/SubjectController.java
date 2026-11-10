
package com.example.demo.controller;

import com.example.demo.dto.SubjectResponse;
import com.example.demo.dto.TopicResponse;
import com.example.demo.service.SubjectService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class SubjectController {

    private final SubjectService subjectService;

    // Constructor Injection
    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    // API 1: Get all subjects
    @GetMapping("/subjects")
    public List<SubjectResponse> getSubjects() {

        return subjectService.getAllSubjects();
    }

    // API 2: Get topics by subject ID
    @GetMapping("/topics/subject/{subjectId}")
    public List<TopicResponse> getTopics(
            @PathVariable Long subjectId
    ) {

        return subjectService.getTopicsBySubject(subjectId);
    }
}
