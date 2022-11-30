package org.hollowbamboo.chordreader2.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import org.hollowbamboo.chordreader2.R;
import org.hollowbamboo.chordreader2.chords.NoteNaming;
import org.hollowbamboo.chordreader2.data.ColorScheme;
import org.hollowbamboo.chordreader2.util.UtilLogger;

public class PreferenceHelper {
	
	private static float textSize = -1;
	private static ColorScheme colorScheme = null;
	private static NoteNaming noteNaming = null;
	private static String searchEngineURL = null;

	private static UtilLogger log = new UtilLogger(org.hollowbamboo.chordreader2.helper.PreferenceHelper.class);
	
	public static void clearCache() {
		textSize = -1;
		colorScheme = null;
		noteNaming = null;
		searchEngineURL = null;
	}
	
	private static void cacheTextsize(Context context, int dimenId) {
		
		float unscaledSize = context.getResources().getDimension(dimenId);
		
		log.d("unscaledSize is %g", unscaledSize);
		
		textSize = unscaledSize;
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
		
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
			String colorSchemeName = sharedPrefs.getString(
					context.getText(R.string.pref_scheme).toString(), 
					context.getText(ColorScheme.Dark.getNameResource()).toString());
			
			colorScheme = ColorScheme.findByPreferenceName(colorSchemeName, context);
		}
		
		return colorScheme;
		
	}
		
	public static void setColorScheme(Context context, ColorScheme colorScheme) {
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = sharedPrefs.edit();
		
		editor.putString(context.getString(R.string.pref_scheme).toString(), 
				context.getText(colorScheme.getNameResource()).toString());
		
		editor.apply();
		
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

	public static void setSearchEngineURL(Context context, String searchEngineURL) {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = sharedPrefs.edit();
		editor.putString(context.getString(R.string.pref_search_engine), searchEngineURL);
		editor.apply();

	}


}
