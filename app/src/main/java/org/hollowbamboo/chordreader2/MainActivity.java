package org.hollowbamboo.chordreader2;

import static android.os.Build.VERSION.SDK_INT;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;

import org.hollowbamboo.chordreader2.databinding.ActivityMainBinding;
import org.hollowbamboo.chordreader2.helper.ChordDictionary;
import org.hollowbamboo.chordreader2.helper.PreferenceHelper;
import org.hollowbamboo.chordreader2.model.DataViewModel;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final int STORAGE_PERMISSION_CODE = 100;

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    DrawerLayout drawer;
    DataViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_start)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        navigationView.setNavigationItemSelectedListener(this);

        initializeChordDictionary();

        showInitialMessage();

        if(!areStoragePermissionsGranted())
            requestPermission();

        viewModel = new ViewModelProvider(this).get(DataViewModel.class);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        int itemId = item.getItemId();
        if(itemId == R.id.nav_web_search) {
            MobileNavigationDirections.ActionDrawerToWebSearchFragment action =
                    MobileNavigationDirections.actionDrawerToWebSearchFragment("");
            Navigation.findNavController(this, R.id.nav_host_fragment_content_main).navigate(action);
        } else if(itemId == R.id.nav_list_view) {
            MobileNavigationDirections.ActionDrawerToListFragment action =
                    MobileNavigationDirections.actionDrawerToListFragment("Songs");
            Navigation.findNavController(this, R.id.nav_host_fragment_content_main).navigate(action);
        } else if(itemId == R.id.nav_list_view_playlists) {
            MobileNavigationDirections.ActionDrawerToListFragment action =
                    MobileNavigationDirections.actionDrawerToListFragment("Playlists");
            Navigation.findNavController(this, R.id.nav_host_fragment_content_main).navigate(action);
        } else if(itemId == R.id.nav_help) {
            Navigation.findNavController(this, R.id.nav_host_fragment_content_main).navigate(R.id.nav_help);
        } else if(itemId == R.id.nav_settings) {
            Navigation.findNavController(this, R.id.nav_host_fragment_content_main).navigate(R.id.nav_settings);
        } else if(itemId == R.id.nav_about) {
            Navigation.findNavController(this, R.id.nav_host_fragment_content_main).navigate(R.id.nav_about);
        }

        drawer.closeDrawer(GravityCompat.START);

        return true;
    }

    public void setLocale(String language) {
        Resources resources = getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        Configuration configuration = resources.getConfiguration();
        configuration.locale = new Locale(language);
        resources.updateConfiguration(configuration, metrics);
    }

    private void showInitialMessage() {

        boolean isFirstRun = PreferenceHelper.getFirstRunPreference(getApplicationContext());
        if(isFirstRun) {

            View view = View.inflate(this, R.layout.intro_dialog, null);
            TextView textView = (TextView) view.findViewById(R.id.first_run_text_view);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            textView.setText(R.string.first_run_message);
            textView.setLinkTextColor(ColorStateList.valueOf(getResources().getColor(R.color.linkColorBlue)));
            new AlertDialog.Builder(this)
                    .setTitle(R.string.first_run_title)
                    .setView(view)
                    .setPositiveButton(android.R.string.ok,
                            (dialog, which) -> PreferenceHelper.setFirstRunPreference(getApplicationContext(), false))
                    .setCancelable(false)
                    .setIcon(R.mipmap.chord_reader_icon).show();
        }
    }

    private boolean areStoragePermissionsGranted() {
        if(SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            int result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            int result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermission()
    {
        if(SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                startActivityForResult(intent, 2296);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, 2296);
            }
        } else {
            //below android 11
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);

        if(requestCode == STORAGE_PERMISSION_CODE) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Storage Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void initializeChordDictionary() {
        // do in the background to avoid jank

        HandlerThread handlerThread = new HandlerThread("InitializeChordDictionary");
        handlerThread.start();

        Handler asyncHandler = new Handler(handlerThread.getLooper()) {};

        Runnable runnable = () -> {
            // your async code goes here.
            ChordDictionary.initialize(this);
            handlerThread.quit();
        };
        asyncHandler.post(runnable);
    }
}
