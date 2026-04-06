package com.example.eventsapp;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Minimal debug-only activity used by instrumentation tests that need a stable
 * fragment container without MainActivity's navigation shell.
 */
public class TestHostActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setId(android.R.id.content);
        setContentView(root);
    }
}
