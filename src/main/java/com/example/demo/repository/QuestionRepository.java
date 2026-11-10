
package com.example.demo.repository;

import com.example.demo.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query(value = """
        SELECT *
        FROM questions
        WHERE exam_type = :examType
          AND subject_id = :subjectId
          AND topic_id = :topicId
          AND difficulty = :difficulty
        ORDER BY RANDOM()
        LIMIT :questionCount
        """, nativeQuery = true)
    List<Question> generateQuestions(
            @Param("examType") String examType,
            @Param("subjectId") Long subjectId,
            @Param("topicId") Long topicId,
            @Param("difficulty") String difficulty,
            @Param("questionCount") int questionCount
    );

    // Retrieve questions belonging to a particular uploaded PDF
    List<Question> findByQuestionPaper_Id(Long paperId);
}
