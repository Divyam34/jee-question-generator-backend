
package com.example.demo.dto;

public class TopicResponse {

    private Long id;
    private String name;
    private String syllabusType;

    // Default constructor
    public TopicResponse() {
    }

    // Parameterized constructor
    public TopicResponse(Long id, String name, String syllabusType) {
        this.id = id;
        this.name = name;
        this.syllabusType = syllabusType;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSyllabusType() {
        return syllabusType;
    }

    public void setSyllabusType(String syllabusType) {
        this.syllabusType = syllabusType;
    }
}
