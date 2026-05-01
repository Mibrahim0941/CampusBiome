package com.example.campusbiome;

/**
 * Mirrors Firebase: Societies/{societyId}/tasks/{taskId}
 * Fields match Firebase keys exactly so Firebase can deserialise correctly.
 */
public class SocietyTask {

    // ── Core fields (stored in Firebase) ─────────────────────────────────────
    public String title;
    public String description;
    public String assignedTo;       // uid of the member assigned
    public String assignedToName;   // display name (denormalised for convenience)
    public String createdBy;        // uid of manager/admin
    public String dueDate;          // "YYYY-MM-DD" or free-text
    public String dueDay;           // spinner value, e.g. "28"
    public String dueMonth;         // spinner value, e.g. "March"
    public String dueYear;          // spinner value, e.g. "2026"
    public String dueTime;          // e.g. "11:30"
    public String dueAmPm;          // "AM" or "PM"
    public String priority;         // "High" | "Medium" | "Low"
    public String status;           // "pending" | "in_progress" | "completed"
    public String createdAt;        // ISO timestamp string

    // ── Transient fields (NOT stored in Firebase) ─────────────────────────────
    private transient String id;         // Firebase key
    private transient String societyId;  // which society this belongs to

    // ── Required no-arg constructor for Firebase ──────────────────────────────
    public SocietyTask() {}

    public SocietyTask(String title, String description,
                       String assignedTo, String assignedToName,
                       String createdBy,
                       String dueDay, String dueMonth, String dueYear,
                       String dueTime, String dueAmPm,
                       String priority, String status) {
        this.title          = title;
        this.description    = description;
        this.assignedTo     = assignedTo;
        this.assignedToName = assignedToName;
        this.createdBy      = createdBy;
        this.dueDay         = dueDay;
        this.dueMonth       = dueMonth;
        this.dueYear        = dueYear;
        this.dueTime        = dueTime;
        this.dueAmPm        = dueAmPm;
        this.priority       = priority;
        this.status         = status != null ? status : "pending";
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String getId()             { return id; }
    public String getSocietyId()      { return societyId; }
    public String getTitle()          { return title; }
    public String getDescription()    { return description; }
    public String getAssignedTo()     { return assignedTo; }
    public String getAssignedToName() { return assignedToName; }
    public String getCreatedBy()      { return createdBy; }
    public String getDueDate()        { return dueDate; }
    public String getDueDay()         { return dueDay; }
    public String getDueMonth()       { return dueMonth; }
    public String getDueYear()        { return dueYear; }
    public String getDueTime()        { return dueTime; }
    public String getDueAmPm()        { return dueAmPm; }
    public String getPriority()       { return priority; }
    public String getStatus()         { return status; }
    public String getCreatedAt()      { return createdAt; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setId(String id)                       { this.id = id; }
    public void setSocietyId(String societyId)         { this.societyId = societyId; }
    public void setTitle(String title)                 { this.title = title; }
    public void setDescription(String description)     { this.description = description; }
    public void setAssignedTo(String assignedTo)       { this.assignedTo = assignedTo; }
    public void setAssignedToName(String n)            { this.assignedToName = n; }
    public void setCreatedBy(String createdBy)         { this.createdBy = createdBy; }
    public void setDueDate(String dueDate)             { this.dueDate = dueDate; }
    public void setDueDay(String dueDay)               { this.dueDay = dueDay; }
    public void setDueMonth(String dueMonth)           { this.dueMonth = dueMonth; }
    public void setDueYear(String dueYear)             { this.dueYear = dueYear; }
    public void setDueTime(String dueTime)             { this.dueTime = dueTime; }
    public void setDueAmPm(String dueAmPm)             { this.dueAmPm = dueAmPm; }
    public void setPriority(String priority)           { this.priority = priority; }
    public void setStatus(String status)               { this.status = status; }
    public void setCreatedAt(String createdAt)         { this.createdAt = createdAt; }

    // ── Helpers ───────────────────────────────────────────────────────────────
    /** Human-readable due date: "28 March 2026" */
    public String getFormattedDueDate() {
        if (dueDay != null && dueMonth != null && dueYear != null) {
            return dueDay + " " + dueMonth + " " + dueYear;
        }
        return dueDate != null ? dueDate : "No date";
    }

    /** Human-readable due time: "11:30 AM" */
    public String getFormattedDueTime() {
        if (dueTime != null && dueAmPm != null) return dueTime + " " + dueAmPm;
        if (dueTime != null) return dueTime;
        return null;
    }

    /** True if status is "completed" */
    public boolean isCompleted() {
        return "completed".equalsIgnoreCase(status);
    }
}