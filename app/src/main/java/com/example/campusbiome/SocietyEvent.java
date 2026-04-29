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


    public String day, month, year;
    public String title, description;
    public String time, ampm;
    public String venue;

    public SocietyEvent() {}

    public SocietyEvent(String day, String month, String year,
                        String title, String description,
                        String time, String ampm,
                        String venue) {
        this.day = day;
        this.month = month;
        this.year = year;
        this.title = title;
        this.description = description;
        this.time = time;
        this.ampm = ampm;
        this.venue = venue;
    }

    public String getDay()         { return day; }
    public String getMonth()       { return month; }
    public String getTitle()       { return title; }
    public String getDescription() { return description; }

    public void setDay(String day)                 { this.day = day; }
    public void setMonth(String month)             { this.month = month; }
    public void setTitle(String title)             { this.title = title; }
    public void setDescription(String description) { this.description = description; }

    public String getAmpm() {
        return ampm;
    }
    public void setAmpm(String ampm) {
        this.ampm = ampm;
    }
    public String getTime() {
        return time;
    }
    public void setTime(String time) {
        this.time = time;
    }
    public String getVenue() {
        return venue;
    }
    public void setVenue(String venue) {
        this.venue = venue;
    }
    public String getYear() {
        return year;
    }
    public void setYear(String year) {
        this.year = year;
    }
}
