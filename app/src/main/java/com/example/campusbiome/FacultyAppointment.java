package com.example.campusbiome;

import java.io.Serializable;

public class FacultyAppointment implements Serializable {
    private String id;
    private String facultyId;
    private String createdAt;
    private String description;
    private String status;
    private StudentInfo student;

    public FacultyAppointment() {
    }

    public FacultyAppointment(String id, String facultyId, String createdAt, String description, String status, StudentInfo student) {
        this.id = id;
        this.facultyId = facultyId;
        this.createdAt = createdAt;
        this.description = description;
        this.status = status;
        this.student = student;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFacultyId() { return facultyId; }
    public void setFacultyId(String facultyId) { this.facultyId = facultyId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public StudentInfo getStudent() { return student; }
    public void setStudent(StudentInfo student) { this.student = student; }

    public static class StudentInfo implements Serializable {
        private String email;
        private String name;
        private String program;
        private String section;
        private int semester;
        private String uid;

        public StudentInfo() { }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getProgram() { return program; }
        public void setProgram(String program) { this.program = program; }
        public String getSection() { return section; }
        public void setSection(String section) { this.section = section; }
        public int getSemester() { return semester; }
        public void setSemester(int semester) { this.semester = semester; }
        public String getUid() { return uid; }
        public void setUid(String uid) { this.uid = uid; }
    }
}
