package com.akylas.hisensea9;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.akylas.hisensea9.lsposed.R;

public class SettingsActivity extends AppCompatActivity {
    public static String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @SuppressLint("WorldReadableFiles")
        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
            PreferenceManager manager = getPreferenceManager();
            try {
                manager.setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
                // Fail here rather than deep inside addPreferencesFromResource: MODE_WORLD_READABLE
                // is banned since API 24 and only works because LSPosed lifts the ban for the
                // processes of modules it has hooked.
                manager.getSharedPreferences();
            } catch (SecurityException e) {
                // Not hooked, so the module is not enabled in LSPosed (a fresh install resets
                // that). Stay usable instead of crashing, but say the values will not be read.
                manager.setSharedPreferencesMode(Context.MODE_PRIVATE);
                Toast.makeText(
                        getContext(),
                        "Module not enabled in LSPosed: settings will not be applied",
                        Toast.LENGTH_LONG
                ).show();
            }
            addPreferencesFromResource(R.xml.prefs);

        }

    }
}
