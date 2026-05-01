package com.example.campusbiome;

import java.util.Map;

/**
 * Mirrors Firebase: Societies/{societyId}/events/{eventId}
 * Fields match Firebase keys exactly so Firebase can deserialize correctly.
 */
public class SocietyEvent {

    // ── Core fields (already exist in Firebase) ───────────────────────────────
    public String day, month, year;
    public String title, description;
    public String time, ampm;
    public String venue;
    public String createdAt;
    public String createdBy;

    // ── Registrations: uid → true ─────────────────────────────────────────────
    // Firebase map — Firebase deserializes "registrations": {"uid1": true, ...}
    public Map<String, Object> registrations;

    // ── Set manually after fetching (NOT stored in Firebase) ──────────────────
    private transient String id;         // event's Firebase key
    private transient String societyId;  // which society this belongs to
    private transient String societyName;

    // ── Required no-arg constructor for Firebase ──────────────────────────────
    public SocietyEvent() {}

    public SocietyEvent(String day, String month, String year,
                        String title, String description,
                        String time, String ampm,
                        String venue) {
        this.day         = day;
        this.month       = month;
        this.year        = year;
        this.title       = title;
        this.description = description;
        this.time        = time;
        this.ampm        = ampm;
        this.venue       = venue;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String getId()          { return id; }
    public String getSocietyId()   { return societyId; }
    public String getSocietyName() { return societyName; }
    public String getDay()         { return day; }
    public String getMonth()       { return month; }
    public String getYear()        { return year; }
    public String getTitle()       { return title; }
    public String getDescription() { return description; }
    public String getTime()        { return time; }
    public String getAmpm()        { return ampm; }
    public String getVenue()       { return venue; }
    public String getCreatedAt()   { return createdAt; }
    public String getCreatedBy()   { return createdBy; }
    public Map<String, Object> getRegistrations() { return registrations; }

    public int getRegistrationCount() {
        return registrations != null ? registrations.size() : 0;
    }

    public boolean isRegistered(String uid) {
        return registrations != null && registrations.containsKey(uid);
    }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setId(String id)                   { this.id = id; }
    public void setSocietyId(String societyId)     { this.societyId = societyId; }
    public void setSocietyName(String societyName) { this.societyName = societyName; }
    public void setDay(String day)                 { this.day = day; }
    public void setMonth(String month)             { this.month = month; }
    public void setYear(String year)               { this.year = year; }
    public void setTitle(String title)             { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setTime(String time)               { this.time = time; }
    public void setAmpm(String ampm)               { this.ampm = ampm; }
    public void setVenue(String venue)             { this.venue = venue; }
    public void setCreatedAt(String createdAt)     { this.createdAt = createdAt; }
    public void setCreatedBy(String createdBy)     { this.createdBy = createdBy; }
    public void setRegistrations(Map<String, Object> registrations) {
        this.registrations = registrations;
    }

    // ── Helper: formatted time string ─────────────────────────────────────────
    public String getFormattedTime() {
        if (time != null && ampm != null) return time + " " + ampm;
        if (time != null) return time;
        return null;
    }
}