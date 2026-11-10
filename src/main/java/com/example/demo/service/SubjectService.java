
package com.example.demo.service;

import com.example.demo.dto.SubjectResponse;
import com.example.demo.dto.TopicResponse;

import com.example.demo.entity.Subject;
import com.example.demo.entity.Topic;

import com.example.demo.repository.SubjectRepository;
import com.example.demo.repository.TopicRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;

    // Constructor Injection
    public SubjectService(
            SubjectRepository subjectRepository,
            TopicRepository topicRepository
    ) {
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
    }

    // Get all subjects
    public List<SubjectResponse> getAllSubjects() {

        List<Subject> subjects =
                subjectRepository.findAllByOrderByNameAsc();

        return subjects.stream()
                .map(subject -> new SubjectResponse(
                        subject.getId(),
                        subject.getName()
                ))
                .toList();
    }

    // Get topics by subject ID
    public List<TopicResponse> getTopicsBySubject(Long subjectId) {

        List<Topic> topics =
                topicRepository
                        .findBySubject_IdAndIsActiveOrderByNameAsc(
                                subjectId, 1
                        );

        return topics.stream()
                .map(topic -> new TopicResponse(
                        topic.getId(),
                        topic.getName(),
                        topic.getSyllabusType()
                ))
                .toList();
    }
}
