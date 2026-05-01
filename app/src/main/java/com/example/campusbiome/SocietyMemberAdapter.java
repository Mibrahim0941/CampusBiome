package com.example.campusbiome;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SocietyMemberAdapter extends RecyclerView.Adapter<SocietyMemberAdapter.ViewHolder> {

    private final List<SocietyMember> list;
    private final DatabaseReference usersRef;

    // 🔥 Cache to avoid repeated Firebase calls
    private final Map<String, User> userCache = new HashMap<>();

    public SocietyMemberAdapter(List<SocietyMember> list) {
        this.list = list;
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dashboard_member, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        SocietyMember m = list.get(pos);

        // Default values
        h.tvName.setText("Loading...");
        h.tvEmail.setText("");
        h.tvInitials.setText("?");

        String uid = m.getUid();

        if (uid != null && userCache.containsKey(uid)) {
            bindUserData(h, m, userCache.get(uid));
        } else if (uid != null) {

            usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);

                    if (user != null) {
                        userCache.put(uid, user); // cache it
                        bindUserData(h, m, user);
                    } else {
                        h.tvName.setText("Unknown");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        // Role (from SocietyMembers)
        String role = m.getRole() != null ? m.getRole() : "student";
        switch (role) {
            case "society_manager":
                h.tvRole.setText("Manager");
                h.tvRole.setBackgroundResource(R.drawable.bg_badge_approved);
                break;
            case "faculty":
                h.tvRole.setText("Faculty");
                h.tvRole.setBackgroundResource(R.drawable.bg_badge_pending);
                break;
            default:
                h.tvRole.setText("Student");
                h.tvRole.setBackgroundResource(R.drawable.bg_role_student);
                break;
        }
    }

    private void bindUserData(ViewHolder h, SocietyMember m, User user) {

        String name = user.getName() != null ? user.getName() : "Unknown";
        String email = user.getEmail() != null ? user.getEmail() : "";
        String gpa = user.getGpa() != null ? user.getGpa() : "N/A";

        h.tvName.setText(name);
        h.tvEmail.setText(email + " • GPA: " + gpa);

        // initials
        h.tvInitials.setText(getInitials(name));
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.split(" ");
        String initials = "";
        for (String p : parts) {
            if (!p.isEmpty()) initials += p.charAt(0);
        }
        return initials.toUpperCase();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInitials, tvName, tvEmail, tvRole;

        ViewHolder(View v) {
            super(v);
            tvInitials = v.findViewById(R.id.tvMemberInitials);
            tvName     = v.findViewById(R.id.tvMemberName);
            tvEmail    = v.findViewById(R.id.tvMemberEmail);
            tvRole     = v.findViewById(R.id.tvMemberRole);
        }
    }
}