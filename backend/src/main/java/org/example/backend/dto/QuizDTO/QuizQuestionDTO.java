package org.example.backend.dto.QuizDTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.example.backend.enums.QuestionType;

import java.util.List;

@Setter
@Getter
public class QuizQuestionDTO {

    private String questionText;
    private QuestionType questionType;
    private List<QuestionOptionDTO> options;
    private String correctAnswer;
    @JsonProperty("score")
    private int points;
}
