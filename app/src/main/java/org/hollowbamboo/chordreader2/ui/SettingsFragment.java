package org.hollowbamboo.chordreader2.ui;

/*
Chord Reader 2 - fetch and display chords for your favorite songs from the Internet
Copyright (C) 2021 AndInTheClouds

This program is free software: you can redistribute it and/or modify it under the terms
of the GNU General Public License as published by the Free Software Foundation, either
version 3 of the License, or any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program.
If not, see <https://www.gnu.org/licenses/>.

*/


import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
import static android.os.Build.VERSION.SDK_INT;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.widget.ListAdapter;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.hollowbamboo.chordreader2.R;
import org.hollowbamboo.chordreader2.adapter.BasicTwoLineAdapter;
import org.hollowbamboo.chordreader2.helper.PreferenceHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SettingsFragment extends PreferenceFragmentCompat
        implements androidx.preference.Preference.OnPreferenceChangeListener, androidx.preference.Preference.OnPreferenceClickListener {

    public static final String EXTRA_NOTE_NAMING_CHANGED = "noteNamingChanged";

    private ListPreference themePreference;
    private Preference noteNamingPreference;
    private EditTextPreference searchEnginePreference;
    private Preference storageLocationPreference;

    private ActivityResultLauncher<Intent> directoryPickerResultLauncher;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);

        setUpPreferences();
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        directoryPickerResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {

                            if (result.getData() != null) {
                                Uri uri = result.getData().getData();
                                setStorageLocationPreference(uri);
                            }
                        }
                    }
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PreferenceHelper.clearCache();
    }


    private void setUpPreferences() {

        themePreference = findPreference(getString(R.string.pref_scheme));
        themePreference.setOnPreferenceChangeListener(this);

        CharSequence themeSummary = getString(PreferenceHelper.getColorScheme(requireContext()).getNameResource());
        themePreference.setSummary(themeSummary);

        noteNamingPreference = findPreference(getString(R.string.pref_note_naming));
        noteNamingPreference.setOnPreferenceClickListener(this);
        CharSequence noteNamingSummary = getString(PreferenceHelper.getNoteNaming(requireContext()).getPrintableNameResource());
        noteNamingPreference.setSummary(noteNamingSummary);

        searchEnginePreference = findPreference(getString(R.string.pref_search_engine));
        searchEnginePreference.setOnPreferenceChangeListener(this);
        CharSequence searchEngineSummary = PreferenceHelper.getSearchEngineURL(requireContext());
        searchEnginePreference.setSummary(searchEngineSummary);

        storageLocationPreference = findPreference(getString(R.string.pref_storage_location));
        storageLocationPreference.setOnPreferenceClickListener(this);
        Uri storageLocationSummary = PreferenceHelper.getStorageLocation(requireContext());
        storageLocationPreference.setSummary(storageLocationSummary.toString());

        if (SDK_INT < Build.VERSION_CODES.O)
            storageLocationPreference.setVisible(false);
    }

    @Override
    public boolean onPreferenceChange(@NonNull androidx.preference.Preference preference, Object newValue) {

        if(preference.getKey().equals(getString(R.string.pref_scheme))) {
            themePreference.setSummary(newValue.toString());
            return true;
        } else if(preference.getKey().equals(getString(R.string.pref_search_engine))) {
            searchEnginePreference.setSummary(newValue.toString());
            return true;
        } else if(preference.getKey().equals(getString(R.string.pref_storage_location))) {
            storageLocationPreference.setSummary(newValue.toString());
            return true;
        }else {
            return true;
        }
    }

    @Override
    public boolean onPreferenceClick(@NonNull androidx.preference.Preference preference) {

        if (preference.getKey() == getString(R.string.pref_note_naming)) {
            // show note naming convention popup

            final List<String> noteNameDisplays = Arrays.asList(getResources().getStringArray(R.array.note_namings));
            final List<String> noteNameValues = Arrays.asList(getResources().getStringArray(R.array.note_namings_values));
            final List<String> noteNameExplanations = Arrays.asList(getResources().getStringArray(R.array.note_namings_explanations));

            int currentValueIndex = noteNameValues.indexOf(PreferenceHelper.getNoteNaming(requireContext()).name());

            ListAdapter adapter = new BasicTwoLineAdapter(requireContext(), noteNameDisplays, noteNameExplanations, currentValueIndex);

            new AlertDialog.Builder(requireContext())
                    .setTitle(noteNamingPreference.getTitle())
                    .setNegativeButton(android.R.string.cancel, null)
                    .setSingleChoiceItems(adapter, currentValueIndex, (dialog, which) -> {
                        PreferenceHelper.setNoteNaming(requireContext(), noteNameValues.get(which));
                        PreferenceHelper.clearCache();
                        noteNamingPreference.setSummary(noteNameDisplays.get(which));
                        dialog.dismiss();

                    })
                    .show();

            return true;
        } else if (Objects.equals(preference.getKey(), getString(R.string.pref_storage_location))) {

            openDirectoryPicker();
            return true;
        }

        return false;
    }

    @SuppressLint("InlinedApi")
    public void openDirectoryPicker() {
        // Choose a directory using the system's file picker.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, PreferenceHelper.getStorageLocation(requireContext()));

        directoryPickerResultLauncher.launch(intent);
    }

    private void setStorageLocationPreference(Uri uri) {
        PreferenceHelper.setStorageLocation(requireContext(), uri);
        PreferenceHelper.clearCache();
        storageLocationPreference.setSummary(uri.toString());
    }

}