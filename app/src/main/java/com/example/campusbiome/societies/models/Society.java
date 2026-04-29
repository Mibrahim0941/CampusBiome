package com.example.campusbiome.societies.models;

import java.io.Serializable;
import java.util.Map;

public class Society implements Serializable {

    // ── Fields match Firebase keys exactly ───────────────────────────────────
    private String id;          // set manually from snapshot key, not stored in Firebase
    private String name;
    private String description;
    private String managerId;
    private String category;
    private String status;      // "approved" / "pending" / "rejected"
    private Map<String, Object> members;      // uid → member object
    private Map<String, Object> events;
    private Map<String, Object> announcements;

    // ── Required no-arg constructor for Firebase ─────────────────────────────
    public Society() {}

    // ── Getters ──────────────────────────────────────────────────────────────
    public String getId()          { return id; }
    public String getName()        { return name; }
    public String getDescription() { return description; }
    public String getManagerId()   { return managerId; }
    public String getCategory()    { return category; }
    public String getStatus()      { return status; }
    public Map<String, Object> getMembers()       { return members; }
    public Map<String, Object> getEvents()        { return events; }
    public Map<String, Object> getAnnouncements() { return announcements; }

    // ── Setters ──────────────────────────────────────────────────────────────
    public void setId(String id)                   { this.id = id; }
    public void setName(String name)               { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setManagerId(String managerId)     { this.managerId = managerId; }
    public void setCategory(String category)       { this.category = category; }
    public void setStatus(String status)           { this.status = status; }
    public void setMembers(Map<String, Object> members)             { this.members = members; }
    public void setEvents(Map<String, Object> events)               { this.events = events; }
    public void setAnnouncements(Map<String, Object> announcements) { this.announcements = announcements; }

    // ── Helpers ──────────────────────────────────────────────────────────────
    public int getMemberCount() {
        return members != null ? members.size() : 0;
    }

    public boolean isMember(String uid) {
        return members != null && members.containsKey(uid);
    }

    public int getEventCount() {
        return events != null ? events.size() : 0;
    }
}