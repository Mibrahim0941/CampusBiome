package com.example.campusbiome;

/**
 * Mirrors the Firebase "Events" node:
 * {
 *   "day":         "22",
 *   "month":       "NOV",
 *   "title":       "Annual Book Fair",
 *   "description": "Literacy Society is holding a book fair! Be there. :)"
 * }
 */
public class SocietyEvent {

    private String day;
    private String month;
    private String title;
    private String description;

    /** Required empty constructor for Firebase */
    public SocietyEvent() {}

    public SocietyEvent(String day, String month, String title, String description) {
        this.day         = day;
        this.month       = month;
        this.title       = title;
        this.description = description;
    }

    public String getDay()         { return day; }
    public String getMonth()       { return month; }
    public String getTitle()       { return title; }
    public String getDescription() { return description; }

    public void setDay(String day)                 { this.day = day; }
    public void setMonth(String month)             { this.month = month; }
    public void setTitle(String title)             { this.title = title; }
    public void setDescription(String description) { this.description = description; }
}
