package khanh.careercoach.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class SaveAssessmentRequest {
    @JsonProperty("questions")
    private List<QuizQuestion> questions;

    @JsonProperty("userAnswers")
    private List<String> userAnswers;

    @JsonProperty("score")
    private Double score;

    public SaveAssessmentRequest() {}

    // Getters Setters
    public List<QuizQuestion> getQuestions() { return questions; }
    public void setQuestions(List<QuizQuestion> questions) { this.questions = questions; }
    public List<String> getUserAnswers() { return userAnswers; }
    public void setUserAnswers(List<String> userAnswers) { this.userAnswers = userAnswers; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
}