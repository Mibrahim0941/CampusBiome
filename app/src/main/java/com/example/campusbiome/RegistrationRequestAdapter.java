package com.example.campusbiome;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RegistrationRequestAdapter extends RecyclerView.Adapter<RegistrationRequestAdapter.ViewHolder> {

    private List<RegistrationRequest> requestList;
    private OnRequestActionListener listener;

    // ✅ Interface for handling actions in Fragment
    public interface OnRequestActionListener {
        void onAction(RegistrationRequest request, boolean accepted);
    }

    // ✅ Constructor with callback
    public RegistrationRequestAdapter(List<RegistrationRequest> requestList,
                                      OnRequestActionListener listener) {
        this.requestList = requestList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_registration_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RegistrationRequest request = requestList.get(position);

        // ✅ Safe data binding
        String name = request.getApplicantName() != null ? request.getApplicantName() : "Unknown";
        String role = request.getAppliedFor() != null ? request.getAppliedFor() : "N/A";
        String gpa = request.getGpa() != null ? request.getGpa() : "N/A";

        holder.tvName.setText(name);
        holder.tvDetails.setText(role + " • GPA: " + gpa);

        // ✅ Approve button
        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAction(request, true);
            }
        });

        // ✅ Reject button
        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAction(request, false);
            }
        });
    }

    @Override
    public int getItemCount() {
        return requestList != null ? requestList.size() : 0;
    }

    // ✅ ViewHolder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDetails;
        ImageView btnApprove, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvApplicantName);
            tvDetails = itemView.findViewById(R.id.tvApplicantDetails);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}