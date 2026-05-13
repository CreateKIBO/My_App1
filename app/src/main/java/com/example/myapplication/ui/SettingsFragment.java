package com.example.myapplication.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_NIGHT_MODE = "night_mode";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupDarkModeSwitch();
        setupVersionText();
    }

    private void setupDarkModeSwitch() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);
        int savedMode = prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(savedMode);
        binding.switchDarkMode.setChecked(savedMode == AppCompatDelegate.MODE_NIGHT_YES);

        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int mode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            AppCompatDelegate.setDefaultNightMode(mode);
            prefs.edit().putInt(KEY_NIGHT_MODE, mode).apply();
        });
    }

    private void setupVersionText() {
        try {
            String versionName = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            binding.tvVersion.setText(getString(R.string.settings_version, versionName));
        } catch (Exception e) {
            binding.tvVersion.setText(getString(R.string.settings_version, "1.0"));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}