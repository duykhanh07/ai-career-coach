package khanh.careercoach.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CoverLetterRequest {

    @JsonProperty("jobTitle")
    private String jobTitle;

    @JsonProperty("companyName")
    private String companyName;

    @JsonProperty("jobDescription")
    private String jobDescription;

    public CoverLetterRequest() {}

    public CoverLetterRequest(String jobTitle, String companyName, String jobDescription) {
        this.jobTitle = jobTitle;
        this.companyName = companyName;
        this.jobDescription = jobDescription;
    }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getJobDescription() { return jobDescription; }
    public void setJobDescription(String jobDescription) { this.jobDescription = jobDescription; }

    @Override
    public String toString() {
        return "CoverLetterRequest{jobTitle='" + jobTitle + "', companyName='" + companyName + "'}";
    }
}