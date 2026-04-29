package com.example.campusbiome.studyGroups.models;

import java.io.Serializable;
import java.util.List;

public class StudyGroup implements Serializable {

    // ── All fields must be public (or have getters+setters) for Firebase ──
    public String id;           // we set this manually after fetching
    public String name;
    public String course;
    public String description;
    public int    currentMembers;
    public int    maxMembers;
    private boolean isOpen;
    public List<String> tags;
    public String createdBy;    // uid of creator — present in Firebase, must exist in model

    // ── Required no-arg constructor for Firebase deserialization ──
    public StudyGroup() {}

    public StudyGroup(String name, String course, String description,
                      int currentMembers, int maxMembers,
                      boolean isOpen, List<String> tags, String createdBy) {
        this.name           = name;
        this.course         = course;
        this.description    = description;
        this.currentMembers = currentMembers;
        this.maxMembers     = maxMembers;
        this.isOpen         = isOpen;
        this.tags           = tags;
        this.createdBy      = createdBy;
    }

    // ── Getters ──
    public String getId()            { return id; }
    public String getName()          { return name; }
    public String getCourse()        { return course; }
    public String getDescription()   { return description; }
    public int    getCurrentMembers(){ return currentMembers; }
    public int    getMaxMembers()    { return maxMembers; }
    public boolean isOpen()          { return isOpen; }
    public List<String> getTags()    { return tags; }
    public String getCreatedBy()     { return createdBy; }

    // ── Setters (Firebase also uses these) ──
    public void setId(String id)                     { this.id = id; }
    public void setName(String name)                 { this.name = name; }
    public void setCourse(String course)             { this.course = course; }
    public void setDescription(String description)   { this.description = description; }
    public void setCurrentMembers(int currentMembers){ this.currentMembers = currentMembers; }
    public void setMaxMembers(int maxMembers)        { this.maxMembers = maxMembers; }
    public void setOpen(boolean open) { this.isOpen = open; }
    public void setTags(List<String> tags)           { this.tags = tags; }
    public void setCreatedBy(String createdBy)       { this.createdBy = createdBy; }
}