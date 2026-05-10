package com.example.myapplication;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.UserEntity;
import com.example.myapplication.data.repository.UserRepository;
import com.example.myapplication.util.SessionManager;
import com.example.myapplication.util.ThemeManager;

public class MainActivity extends AppCompatActivity {

    private LinearLayout bottomBar;
    private NavController navController;
    private SessionManager sessionManager;

    // Tab views
    private LinearLayout[] tabs;
    private ImageView[] tabIcons;
    private TextView[] tabLabels;
    private int[] tabDestIds;

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

        // Setup 6 tabs
        tabs = new LinearLayout[]{
                findViewById(R.id.tab_home),
                findViewById(R.id.tab_calendar),
                findViewById(R.id.tab_streak),
                findViewById(R.id.tab_reward),
                findViewById(R.id.tab_shop),
                findViewById(R.id.tab_profile)
        };
        tabIcons = new ImageView[]{
                findViewById(R.id.tab_icon_home),
                findViewById(R.id.tab_icon_calendar),
                findViewById(R.id.tab_icon_streak),
                findViewById(R.id.tab_icon_reward),
                findViewById(R.id.tab_icon_shop),
                findViewById(R.id.tab_icon_profile)
        };
        tabLabels = new TextView[]{
                findViewById(R.id.tab_label_home),
                findViewById(R.id.tab_label_calendar),
                findViewById(R.id.tab_label_streak),
                findViewById(R.id.tab_label_reward),
                findViewById(R.id.tab_label_shop),
                findViewById(R.id.tab_label_profile)
        };
        tabDestIds = new int[]{
                R.id.homeFragment,
                R.id.calendarFragment,
                R.id.streakFragment,
                R.id.rewardFragment,
                R.id.shopFragment,
                R.id.profileFragment
        };

        for (int i = 0; i < tabs.length; i++) {
            final int destId = tabDestIds[i];
            tabs[i].setOnClickListener(v -> {
                NavOptions options = new NavOptions.Builder()
                        .setPopUpTo(R.id.homeFragment, false)
                        .setLaunchSingleTop(true)
                        .build();
                navController.navigate(destId, null, options);
            });
        }

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            boolean showBottomNav = id == R.id.homeFragment
                    || id == R.id.calendarFragment
                    || id == R.id.streakFragment
                    || id == R.id.rewardFragment
                    || id == R.id.shopFragment
                    || id == R.id.profileFragment;
            bottomBar.setVisibility(showBottomNav ? View.VISIBLE : View.GONE);
            updateTabSelection(id);
        });

        ensureLocalUser();
        observeTheme();
    }

    private void updateTabSelection(int destId) {
        int accentColor = ThemeManager.getThemePrimaryInt(this);
        int mutedColor = getResources().getColor(R.color.md_on_surface_variant, null);
        ColorStateList accentTint = ColorStateList.valueOf(accentColor);
        ColorStateList mutedTint = ColorStateList.valueOf(mutedColor);

        for (int i = 0; i < tabDestIds.length; i++) {
            boolean selected = tabDestIds[i] == destId;
            tabIcons[i].setImageTintList(selected ? accentTint : mutedTint);
            tabLabels[i].setTextColor(selected ? accentColor : mutedColor);
            tabLabels[i].setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
    }

    private void observeTheme() {
        long userId = sessionManager.getLocalUserId();
        if (userId == -1) return;
        AppDatabase db = AppDatabase.getInstance(this);
        db.userDao().observeUser(userId).observe(this, user -> {
            if (user != null) {
                ThemeManager.applyTheme(this, user.getThemeId());
                // Re-apply tab colors when theme changes
                int currentDest = navController.getCurrentDestination() != null ? navController.getCurrentDestination().getId() : -1;
                updateTabSelection(currentDest);
            }
        });
    }

    private void ensureLocalUser() {
        if (sessionManager.getLocalUserId() != -1) return;
        try {
            Thread[] waitThread = new Thread[1];
            waitThread[0] = new Thread(() -> {
                AppDatabase db = AppDatabase.getInstance(this);
                UserEntity existing = db.userDao().getFirstUser();
                if (existing != null) {
                    sessionManager.setLocalUserId(existing.getId());
                } else {
                    long id = new UserRepository(db, AppDatabase.databaseWriteExecutor).createDefaultUser();
                    sessionManager.setLocalUserId(id);
                }
            });
            waitThread[0].start();
            waitThread[0].join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}