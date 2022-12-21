package org.hollowbamboo.chordreader2;

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
import static android.os.Build.VERSION.SDK_INT;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;

import org.hollowbamboo.chordreader2.databinding.ActivityMainBinding;
import org.hollowbamboo.chordreader2.helper.ChordDictionary;
import org.hollowbamboo.chordreader2.helper.PreferenceHelper;
import org.hollowbamboo.chordreader2.model.DataViewModel;

import java.util.Objects;

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

        if(!areStoragePermissionsGranted())
            requestPermission();

        showInitialMessage();

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
        } else if(itemId == R.id.nav_list_view_setlists) {
            MobileNavigationDirections.ActionDrawerToListFragment action =
                    MobileNavigationDirections.actionDrawerToListFragment("Setlists");
            Navigation.findNavController(this, R.id.nav_host_fragment_content_main).navigate(action);
        } else if(itemId == R.id.nav_help) {
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);

            String sectionID = "";

            Fragment f = navHostFragment.getChildFragmentManager().getFragments().get(0);
            String tag = f.toString();

            if (tag.contains("List")) {
                if (tag.contains("DraggableList"))
                    sectionID = ". Setlists";
                else {
                    Bundle bundle = f.getArguments();
                    if (bundle != null) {
                        String mode = bundle.getString("mode");
                        if (mode.equals("Songs"))
                            sectionID = ". Song";
                        else if (mode.equals("Setlists") ||
                                mode.equals("SetlistSongsSelection"))
                            sectionID = ". Setlists";
                    }
                }
            } else if (tag.contains("SongView"))
                sectionID = ". Song";
            else if (tag.contains("WebSearch"))
                sectionID = ". " + getString(R.string.web_search);

            MobileNavigationDirections.ActionDrawerToHelpFragment action =
                    MobileNavigationDirections.actionDrawerToHelpFragment(sectionID);
            Navigation.findNavController(this, R.id.nav_host_fragment_content_main).navigate(action);
        } else if(itemId == R.id.nav_settings) {
            Navigation.findNavController(this, R.id.nav_host_fragment_content_main).navigate(R.id.nav_settings);
        } else if(itemId == R.id.nav_about) {
            Navigation.findNavController(this, R.id.nav_host_fragment_content_main).navigate(R.id.nav_about);
        }

        drawer.closeDrawer(GravityCompat.START);

        return true;
    }

    private void showInitialMessage() {

        int versionCode = 0;

        try {
            versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (prefs.getInt("lastUpdate", 0) != versionCode) {
            try {
                PreferenceHelper.setFirstRunPreference(getApplicationContext(), true);

                // Commiting in the preferences, that the update was successful.
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("lastUpdate", versionCode);
                editor.apply();
            } catch(Throwable t) {
                // update failed, or cancelled
            }
        }

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
        if(SDK_INT >= Build.VERSION_CODES.O) {
            Uri storageLocationSummary = PreferenceHelper.getStorageLocation(this);
            int permissions = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

            try {
                getContentResolver().takePersistableUriPermission(storageLocationSummary, permissions);
                return true;
            } catch (Exception e) {
                return false;
            }
        } else {
            int result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            int result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermission() {

        ActivityResultLauncher<Intent> directoryPickerResultLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                        new ActivityResultCallback<ActivityResult>() {
                            @Override
                            public void onActivityResult(ActivityResult result) {
                                if (result.getResultCode() == Activity.RESULT_OK) {

                                    if (result.getData() != null) {
                                        Uri uri = result.getData().getData();

                                        grantUriPermission(getPackageName(), uri,
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                        getContentResolver().takePersistableUriPermission(uri,
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                                        PreferenceHelper.setStorageLocation(getApplicationContext(), uri);
                                        PreferenceHelper.clearCache();
                                    }
                                }
                            }
                        });


        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                if(SDK_INT >= Build.VERSION_CODES.O) {

                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                            PreferenceHelper.getStorageLocation(getApplicationContext()));

                    intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    directoryPickerResultLauncher.launch(intent);
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
                }
            }
        };

        //check if folder chord_reader_2 already exist or first time install
        DocumentFile base_folder;
        String message;

        try {
            base_folder = Objects.requireNonNull(DocumentFile.fromTreeUri(getApplicationContext(),
                    PreferenceHelper.getStorageLocation(getApplicationContext())));
        } catch (Exception e) {
            base_folder = null;
        }

        if(base_folder == null || !base_folder.exists()) {
            message = getString(R.string.select_storage_location);
        } else {
            message = getString(R.string.grant_storage_access);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.storage_access)
                .setPositiveButton(android.R.string.yes, onClickListener)
                .setMessage(message);

        builder.show();
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
