package com.example.campusbiome;

public class RegistrationRequest {
    public String requestId, applicantUid, applicantName, appliedFor, gpa, status, appliedDate;

    public RegistrationRequest() {} // Required for Firebase

    public RegistrationRequest(String applicantName, String appliedFor, String status) {
        this.applicantName = applicantName;
        this.appliedFor = appliedFor;
        this.status = status;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getApplicantUid() { return applicantUid; }
    public String getApplicantName() { return applicantName; }
    public String getAppliedFor() { return appliedFor; }
    public String getGpa() { return gpa; }
    public String getStatus() { return status; }
    public String getAppliedDate() { return appliedDate; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public void setAppliedFor(String appliedFor) { this.appliedFor = appliedFor; }
    public void setStatus(String status) { this.status = status; }
    public void setApplicantUid(String applicantUid) { this.applicantUid = applicantUid; }

    public void setGpa(String gpa) {
        this.gpa = gpa;
    }

    public void setAppliedDate(String appliedDate) {
        this.appliedDate = appliedDate;
    }
}
