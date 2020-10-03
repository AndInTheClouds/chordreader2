package org.handmadeideas.chordreader;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.KeyEvent;
import android.widget.ListAdapter;
import org.handmadeideas.chordreader.adapter.BasicTwoLineAdapter;
import org.handmadeideas.chordreader.helper.PreferenceHelper;

import java.util.Arrays;
import java.util.List;

public class SettingsActivity extends PreferenceActivity 
		implements OnPreferenceChangeListener, OnPreferenceClickListener {
	
	public static final String EXTRA_NOTE_NAMING_CHANGED = "noteNamingChanged";
	
	private ListPreference textSizePreference, themePreference;
	private Preference noteNamingPreference;
	private boolean noteNamingChanged;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.settings);
		
		setUpPreferences();
	}
	
	private void setUpPreferences() {
		
		
		textSizePreference = (ListPreference) findPreference(getString(R.string.pref_text_size));
		textSizePreference.setSummary(textSizePreference.getEntry());
		textSizePreference.setOnPreferenceChangeListener(this);
		
		themePreference = (ListPreference) findPreference(getString(R.string.pref_scheme));
		themePreference.setOnPreferenceChangeListener(this);
		
		CharSequence themeSummary = getString(PreferenceHelper.getColorScheme(this).getNameResource());
		themePreference.setSummary(themeSummary);
		
		noteNamingPreference = findPreference(getString(R.string.pref_note_naming));
		noteNamingPreference.setOnPreferenceClickListener(this);
		
		CharSequence noteNamingSummary = getString(PreferenceHelper.getNoteNaming(this).getPrintableNameResource());
		noteNamingPreference.setSummary(noteNamingSummary);
		
	}
	
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		
			
		if (preference.getKey().equals(getString(R.string.pref_text_size))) { // text size pref
			
			int index = Arrays.asList(textSizePreference.getEntryValues()).indexOf(newValue);
			CharSequence newEntry = textSizePreference.getEntries()[index];
			
			textSizePreference.setSummary(newEntry);
			return true;
		} else if (preference.getKey().equals(getString(R.string.pref_scheme))) {
			themePreference.setSummary(newValue.toString());
			return true;	
		} else { // show ads
			// TODO
			return true;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
		
	    if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0 ) {
	    	
	    	// set result and finish
	    	Intent data = new Intent();
	    	data.putExtra(EXTRA_NOTE_NAMING_CHANGED, noteNamingChanged);
	    	setResult(RESULT_OK, data);
	    	finish();
	    	return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		PreferenceHelper.clearCache();
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		
		// show note naming convention popup
		
		final List<String> noteNameDisplays = Arrays.asList(getResources().getStringArray(R.array.note_namings));
		final List<String> noteNameValues = Arrays.asList(getResources().getStringArray(R.array.note_namings_values));
		final List<String> noteNameExplanations = Arrays.asList(getResources().getStringArray(R.array.note_namings_explanations));
		
		int currentValueIndex = noteNameValues.indexOf(PreferenceHelper.getNoteNaming(this).name());
		
		ListAdapter adapter = new BasicTwoLineAdapter(this, noteNameDisplays, noteNameExplanations, currentValueIndex);
		
		new AlertDialog.Builder(this)
			.setTitle(noteNamingPreference.getTitle())
			.setNegativeButton(android.R.string.cancel, null)
			.setSingleChoiceItems(adapter, currentValueIndex, new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				PreferenceHelper.setNoteNaming(SettingsActivity.this, noteNameValues.get(which));
				PreferenceHelper.clearCache();
				noteNamingPreference.setSummary(noteNameDisplays.get(which));
				noteNamingChanged = true;
				dialog.dismiss();
				
			}})
			.show();

		return true;
	}
}
