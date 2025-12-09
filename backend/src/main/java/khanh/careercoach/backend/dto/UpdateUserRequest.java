package khanh.careercoach.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class UpdateUserRequest {

    @JsonProperty("industry")
    private String industry;

    @JsonProperty("bio")
    private String bio;

    @JsonProperty("experience")
    private Integer experience;

    @JsonProperty("skills")
    private List<String> skills;

    // 1. Constructor rỗng (BẮT BUỘC cho Jackson)
    public UpdateUserRequest() {
    }

    // 2. Constructor đầy đủ
    public UpdateUserRequest(String industry, String bio, Integer experience, List<String> skills) {
        this.industry = industry;
        this.bio = bio;
        this.experience = experience;
        this.skills = skills;
    }

    // 3. Getters và Setters (Viết tay để chắc chắn tồn tại)
    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public Integer getExperience() { return experience; }
    public void setExperience(Integer experience) { this.experience = experience; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    // 4. Hàm toString() để in log debug cho dễ nhìn
    @Override
    public String toString() {
        return "UpdateUserRequest{" +
                "industry='" + industry + '\'' +
                ", bio='" + bio + '\'' +
                ", experience=" + experience +
                ", skills=" + skills +
                '}';
    }
}