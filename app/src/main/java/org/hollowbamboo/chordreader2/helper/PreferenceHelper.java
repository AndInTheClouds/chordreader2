package org.hollowbamboo.chordreader2.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import org.hollowbamboo.chordreader2.R;
import org.hollowbamboo.chordreader2.chords.NoteNaming;
import org.hollowbamboo.chordreader2.data.ColorScheme;

public class PreferenceHelper {

	private static ColorScheme colorScheme = null;
	private static NoteNaming noteNaming = null;
	private static String searchEngineURL = null;
	private static String storageLocation = null;

	public static void clearCache() {
		colorScheme = null;
		noteNaming = null;
		searchEngineURL = null;
		storageLocation = null;
	}

	public static void setFirstRunPreference(Context context, boolean bool) {

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = sharedPrefs.edit();
		
		editor.putBoolean(context.getString(R.string.pref_first_run), bool);
		
		editor.apply();

	}
	public static boolean getFirstRunPreference(Context context) {

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPrefs.getBoolean(context.getString(R.string.pref_first_run), true);

	}
	
	public static ColorScheme getColorScheme(Context context) {
		
		if(colorScheme == null) {
			String automaticColorSchemeName = context.getText(R.string.pref_scheme_system).toString();
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
			String colorSchemeName = sharedPrefs.getString(
					context.getText(R.string.pref_scheme).toString(), 
					automaticColorSchemeName);

			if (colorSchemeName.equals(automaticColorSchemeName)) {
				colorScheme = isNightModeActive(context) ? ColorScheme.Dark : ColorScheme.Light;
			} else {
				colorScheme = ColorScheme.findByPreferenceName(colorSchemeName, context);
			}
		}
		
		return colorScheme;
	}

	private static boolean isNightModeActive(Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			return context.getResources().getConfiguration().isNightModeActive();
		} else {
			int nightModeFlags = context.getResources().getConfiguration().uiMode &
					Configuration.UI_MODE_NIGHT_MASK;

			return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
		}
	}

	public static NoteNaming getNoteNaming(Context context) {
		
		if(noteNaming == null) {
		
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
			String pref = sharedPrefs.getString(context.getString(R.string.pref_note_naming), 
					context.getString(R.string.pref_note_naming_default));
			noteNaming = NoteNaming.valueOf(pref);
		}
		
		return noteNaming;
	}

	public static void setNoteNaming(Context context, String noteNameValue) {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = sharedPrefs.edit();
		editor.putString(context.getString(R.string.pref_note_naming), noteNameValue);
		editor.apply();
		
	}

	public static String getSearchEngineURL(Context context) {

		if(searchEngineURL == null) {

			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
			searchEngineURL = sharedPrefs.getString(
					context.getString(R.string.pref_search_engine),
					context.getString(R.string.pref_search_engine_default));
		}

		return searchEngineURL;
	}

	public static Uri getStorageLocation(Context context) {

		if(storageLocation == null) {

			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
			storageLocation = sharedPrefs.getString(
					context.getString(R.string.pref_storage_location),
					context.getString(R.string.pref_storage_location_default));
		}

		return Uri.parse(storageLocation);
	}

	public static void setStorageLocation(Context context, Uri uri) {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = sharedPrefs.edit();
		editor.putString(context.getString(R.string.pref_storage_location), uri.toString());
		editor.apply();
	}
}
