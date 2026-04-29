package com.example.campusbiome;

public class TimetableSlot implements Comparable<TimetableSlot> {
    public String courseCode;
    public String courseTitle;
    public String instructor;
    public String day;
    public String timeSlot;
    public String venue;
    public int durationMinutes;

    public TimetableSlot(String courseCode, String courseTitle, String instructor, String day, String timeSlot, String venue, int durationMinutes) {
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.instructor = instructor;
        this.day = day;
        this.timeSlot = timeSlot;
        this.venue = venue;
        this.durationMinutes = durationMinutes;
    }

    @Override
    public int compareTo(TimetableSlot other) {
        return this.timeSlot.compareTo(other.timeSlot);
    }
}
