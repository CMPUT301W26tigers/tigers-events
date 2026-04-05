package com.example.eventsapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Host activity for the administrator section of the application.
 *
 * <p>Wires the admin-specific {@link androidx.navigation.fragment.NavHostFragment} to the
 * admin bottom-navigation bar ({@code R.id.adminBottomNav}), giving administrators their own
 * navigation graph that is entirely separate from the main entrant/organizer flow.
 */
public class AdminActivity extends AppCompatActivity {

    /**
     * Sets up the admin navigation graph and bottom-navigation bar.
     *
     * @param savedInstanceState Previously saved instance state, or {@code null} on first launch.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        BottomNavigationView bottomNav = findViewById(R.id.adminBottomNav);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.admin_nav_host_fragment);

        if (navHostFragment == null) {
            return;
        }

        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(bottomNav, navController);
    }
}
