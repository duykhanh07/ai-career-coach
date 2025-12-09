package khanh.careercoach.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class QuizQuestion {
    @JsonProperty("question")
    private String question;

    @JsonProperty("options")
    private List<String> options;

    @JsonProperty("correctAnswer")
    private String correctAnswer;

    @JsonProperty("explanation")
    private String explanation;

    // Constructors, Getters, Setters
    public QuizQuestion() {}

    // ... (Generate Getters/Setters standard)
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}