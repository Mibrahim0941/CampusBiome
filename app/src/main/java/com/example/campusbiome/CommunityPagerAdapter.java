package com.example.campusbiome;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.campusbiome.studyGroups.StudyGroupsFragment;

public class CommunityPagerAdapter extends FragmentStateAdapter {

    // IMPORTANT: Pass the *parent fragment* (CommunityHubFragment), not an Activity.
    // FragmentStateAdapter uses the fragment's childFragmentManager internally,
    // which is correct for nested fragments (fragment inside fragment).
    public CommunityPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new StudyGroupsFragment();
            case 1: return new SocietiesFragment();   // TODO: Create this fragment
            case 2: return new EventsFragment();       // TODO: Create this fragment
            default: return new StudyGroupsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}