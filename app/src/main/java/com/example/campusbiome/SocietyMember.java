package com.example.campusbiome;

/**
 * Mirrors the Firebase "Users" node:
 * {
 *   "name":  "Bisma Amir",
 *   "email": "bismaamir368@gmail.com",
 *   "role":  "student"
 * }
 */
public class SocietyMember {
    private String uid;
    private String name;
    private String email;
    private String role;
    private String joinedDate;


    /** Required empty constructor for Firebase */
    public SocietyMember() {}

    public SocietyMember(String uid,String name, String email, String role, String joinedDate) {
        this.uid=uid;
        this.name  = name;
        this.email = email;
        this.role  = role;
        this.joinedDate = joinedDate;
    }
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getName()  { return name; }
    public String getEmail() { return email; }
    public String getRole()  { return role; }

    public void setName(String name)   { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(String role)   { this.role = role; }
    public String getJoinedDate() { return joinedDate; }
    public void setJoinedDate(String joinedDate) { this.joinedDate = joinedDate; }


    /** Returns initials (up to 2 chars) for the avatar placeholder */
    public String getInitials() {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }
}
