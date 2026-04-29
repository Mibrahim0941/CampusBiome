package com.example.campusbiome.studyGroups.models;

public class StudySession {

    public String title;
    public String date;
    public String time;
    public String location;
    public String groupId;      // which group this session belongs to

    // ── Required no-arg constructor for Firebase ──
    public StudySession() {}

    public StudySession(String title, String date, String time,
                        String location, String groupId) {
        this.title    = title;
        this.date     = date;
        this.time     = time;
        this.location = location;
        this.groupId  = groupId;
    }

    public String getTitle()    { return title; }
    public String getDate()     { return date; }
    public String getTime()     { return time; }
    public String getLocation() { return location; }
    public String getGroupId()  { return groupId; }

    public void setTitle(String title)       { this.title = title; }
    public void setDate(String date)         { this.date = date; }
    public void setTime(String time)         { this.time = time; }
    public void setLocation(String location) { this.location = location; }
    public void setGroupId(String groupId)   { this.groupId = groupId; }
}