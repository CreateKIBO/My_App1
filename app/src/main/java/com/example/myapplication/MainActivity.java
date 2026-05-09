package com.example.myapplication;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.ui.NavigationUI;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.UserEntity;
import com.example.myapplication.data.repository.UserRepository;
import com.example.myapplication.util.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private NavController navController;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        sessionManager = new SessionManager(this);
        bottomNav = findViewById(R.id.bottom_nav);

        navController = ((androidx.navigation.fragment.NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment))
                .getNavController();

        NavigationUI.setupWithNavController(bottomNav, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            boolean showBottomNav = id == R.id.homeFragment
                    || id == R.id.calendarFragment
                    || id == R.id.shopFragment
                    || id == R.id.profileFragment;
            bottomNav.setVisibility(showBottomNav ? View.VISIBLE : View.GONE);
        });

        ensureLocalUser();
    }

    private void ensureLocalUser() {
        if (sessionManager.getLocalUserId() != -1) return;
        // DB callback (onCreate/onDestructiveMigration) already created a user,
        // but it runs on a background thread. Query synchronously to get it.
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
            waitThread[0].join(5000); // wait up to 5s for DB init
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
