package com.example.campusbiome;

import java.io.Serializable;

public class FacultyAnnouncement implements Serializable {
    private String id;
    private String title;
    private String description;
    private String createdAt;
    private String createdby;

    public FacultyAnnouncement() {}

    public FacultyAnnouncement(String title, String description, String createdAt, String createdby) {
        this.title = title;
        this.description = description;
        this.createdAt = createdAt;
        this.createdby = createdby;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getCreatedby() { return createdby; }
    public void setCreatedby(String createdby) { this.createdby = createdby; }
}
