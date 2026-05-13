package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.UserEntity;
import com.example.myapplication.data.repository.UserRepository;
import com.example.myapplication.util.NotificationHelper;
import com.example.myapplication.util.SessionManager;
import com.example.myapplication.util.ThemeManager;

public class MainActivity extends AppCompatActivity {

    private LinearLayout bottomBar;
    private NavController navController;
    private SessionManager sessionManager;

    private LinearLayout[] tabs;
    private ImageView[] tabIcons;
    private TextView[] tabLabels;
    private int[] tabDestIds;
    private int currentTabIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nav_host_fragment), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        sessionManager = new SessionManager(this);
        bottomBar = findViewById(R.id.bottom_bar);

        navController = ((androidx.navigation.fragment.NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment))
                .getNavController();

        tabs = new LinearLayout[]{
                findViewById(R.id.tab_home),
                findViewById(R.id.tab_calendar),
                findViewById(R.id.tab_focus),
                findViewById(R.id.tab_shop),
                findViewById(R.id.tab_profile)
        };
        tabIcons = new ImageView[]{
                findViewById(R.id.tab_icon_home),
                findViewById(R.id.tab_icon_calendar),
                findViewById(R.id.tab_icon_focus),
                findViewById(R.id.tab_icon_shop),
                findViewById(R.id.tab_icon_profile)
        };
        tabLabels = new TextView[]{
                findViewById(R.id.tab_label_home),
                findViewById(R.id.tab_label_calendar),
                findViewById(R.id.tab_label_focus),
                findViewById(R.id.tab_label_shop),
                findViewById(R.id.tab_label_profile)
        };
        tabDestIds = new int[]{
                R.id.homeFragment,
                R.id.calendarFragment,
                R.id.focusFragment,
                R.id.shopFragment,
                R.id.profileFragment
        };

        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            final int destId = tabDestIds[i];
            tabs[i].setOnClickListener(v -> {
                if (index == currentTabIndex) return;
                animateTabClick(v);
                NavOptions options = new NavOptions.Builder()
                        .setPopUpTo(R.id.homeFragment, false)
                        .setLaunchSingleTop(true)
                        .setEnterAnim(R.anim.tab_enter)
                        .setExitAnim(R.anim.tab_exit)
                        .setPopEnterAnim(R.anim.tab_enter)
                        .setPopExitAnim(R.anim.tab_exit)
                        .build();
                navController.navigate(destId, null, options);
            });
        }

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            boolean showBottomNav = id == R.id.homeFragment
                    || id == R.id.calendarFragment
                    || id == R.id.focusFragment
                    || id == R.id.shopFragment
                    || id == R.id.profileFragment;
            bottomBar.setVisibility(showBottomNav ? View.VISIBLE : View.GONE);
            updateTabSelection(id);
        });

        ensureLocalUser();
    }

    private void animateTabClick(View tab) {
        tab.animate()
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(100)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> tab.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start())
                .start();
    }

    private void updateTabSelection(int destId) {
        int accentColor = ThemeManager.getThemePrimaryInt(this);
        int mutedColor = getResources().getColor(R.color.md_on_surface_variant, null);
        ColorStateList accentTint = ColorStateList.valueOf(accentColor);
        ColorStateList mutedTint = ColorStateList.valueOf(mutedColor);

        for (int i = 0; i < tabDestIds.length; i++) {
            boolean selected = tabDestIds[i] == destId;
            if (selected) currentTabIndex = i;

            tabIcons[i].setImageTintList(selected ? accentTint : mutedTint);
            tabLabels[i].setTextColor(selected ? accentColor : mutedColor);
            tabLabels[i].setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

            if (selected) {
                ImageView icon = tabIcons[i];
                icon.animate()
                        .scaleX(1.15f).scaleY(1.15f)
                        .setDuration(150)
                        .setInterpolator(new DecelerateInterpolator())
                        .withEndAction(() -> icon.animate()
                                .scaleX(1f).scaleY(1f)
                                .setDuration(100)
                                .setInterpolator(new AccelerateDecelerateInterpolator())
                                .start())
                        .start();
            }
        }
    }

    private void observeTheme() {
        long userId = sessionManager.getLocalUserId();
        if (userId == -1) return;
        AppDatabase db = AppDatabase.getInstance(this);
        db.userDao().observeUser(userId).observe(this, user -> {
            if (user != null) {
                ThemeManager.applyTheme(this, user.getThemeId());
                int currentDest = navController.getCurrentDestination() != null ? navController.getCurrentDestination().getId() : -1;
                updateTabSelection(currentDest);
            }
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
    }

    private void ensureLocalUser() {
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            long existingId = sessionManager.getLocalUserId();
            AppDatabase db = AppDatabase.getInstance(this);

            if (existingId != -1) {
                UserEntity user = db.userDao().getUserById(existingId);
                if (user != null) {
                    latch.countDown();
                    runOnUiThread(this::onUserReady);
                    return;
                }
            }

            UserEntity existing = db.userDao().getFirstUser();
            if (existing != null) {
                sessionManager.setLocalUserId(existing.getId());
            } else {
                long id = new UserRepository(db, AppDatabase.databaseWriteExecutor).createDefaultUser();
                sessionManager.setLocalUserId(id);
            }

            latch.countDown();
            runOnUiThread(this::onUserReady);
        });

        try {
            latch.await(3, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}
    }

    private void onUserReady() {
        NotificationHelper.ensureChannel(this);
        requestNotificationPermission();
        observeTheme();
    }
}
