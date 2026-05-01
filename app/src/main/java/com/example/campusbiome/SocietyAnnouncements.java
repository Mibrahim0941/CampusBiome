package com.example.campusbiome;

public class SocietyAnnouncements {
    public String title, message, createdAt;

    public SocietyAnnouncements() {} // Required

    public SocietyAnnouncements(String title, String message, String createdAt) {
        this.title = title;
        this.message = message;
        this.createdAt = createdAt;
    }
    public String getTitle() {
        return title;
    }
    public String getMessage() {
        return message;
    }
    public String getCreatedAt() {
        return createdAt;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
