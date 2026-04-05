package com.example.eventsapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Full-screen branded splash with a logo assembly animation:
 *   1. Logo scales up + fades in with overshoot bounce
 *   2. App name slides up + fades in
 *   3. Tagline fades in
 *   4. Auto-navigates to MainActivity
 */
public class SplashActivity extends AppCompatActivity {

    /**
     * Builds and runs the three-phase logo assembly animation, then schedules navigation to
     * {@link MainActivity} via a 600 ms post-animation delay.
     *
     * <p>Animation phases:
     * <ol>
     *   <li>Logo scales from 0.3× to 1× with an overshoot bounce while fading in (700 ms).</li>
     *   <li>App name slides up 40 dp and fades in (500 ms).</li>
     *   <li>Tagline fades in (400 ms).</li>
     * </ol>
     *
     * @param savedInstanceState Unused; splash screens are never recreated from saved state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.iv_splash_logo);
        TextView name = findViewById(R.id.tv_splash_name);
        TextView tagline = findViewById(R.id.tv_splash_tagline);

        // Phase 1 – logo scales from 0.3 → 1.0 and fades in (overshoot bounce)
        ObjectAnimator logoAlpha = ObjectAnimator.ofFloat(logo, View.ALPHA, 0f, 1f);
        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(logo, View.SCALE_X, 0.3f, 1f);
        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(logo, View.SCALE_Y, 0.3f, 1f);
        AnimatorSet logoSet = new AnimatorSet();
        logoSet.playTogether(logoAlpha, logoScaleX, logoScaleY);
        logoSet.setDuration(700);
        logoSet.setInterpolator(new OvershootInterpolator(1.2f));

        // Phase 2 – app name slides up + fades in
        ObjectAnimator nameAlpha = ObjectAnimator.ofFloat(name, View.ALPHA, 0f, 1f);
        ObjectAnimator nameTransY = ObjectAnimator.ofFloat(name, View.TRANSLATION_Y, 40f, 0f);
        AnimatorSet nameSet = new AnimatorSet();
        nameSet.playTogether(nameAlpha, nameTransY);
        nameSet.setDuration(500);
        nameSet.setStartDelay(150);

        // Phase 3 – tagline fades in
        ObjectAnimator taglineAlpha = ObjectAnimator.ofFloat(tagline, View.ALPHA, 0f, 1f);
        taglineAlpha.setDuration(400);
        taglineAlpha.setStartDelay(100);

        // Chain: logo → name → tagline → navigate
        AnimatorSet full = new AnimatorSet();
        full.playSequentially(logoSet, nameSet, taglineAlpha);
        full.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                logo.postDelayed(() -> {
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                }, 600);
            }
        });
        full.start();
    }
}
