package org.handmadeideas.chordreader.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import org.handmadeideas.chordreader.R;
import org.handmadeideas.chordreader.chords.NoteNaming;
import org.handmadeideas.chordreader.data.ColorScheme;
import org.handmadeideas.chordreader.util.UtilLogger;

public class PreferenceHelper {
	
	private static float textSize = -1;
	private static ColorScheme colorScheme = null;
	private static NoteNaming noteNaming = null;
	
	private static UtilLogger log = new UtilLogger(org.handmadeideas.chordreader.helper.PreferenceHelper.class);
		
	public static float getTextSizePreference(Context context) {
		
		if (textSize == -1) {
		
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
			
			String textSizePref = sharedPrefs.getString(
					context.getText(R.string.pref_text_size).toString(), 
					context.getText(R.string.text_size_medium_value).toString());

			if (textSizePref.equals(context.getText(R.string.text_size_xsmall_value))) {
				cacheTextsize(context, R.dimen.text_size_xsmall);
			} else if (textSizePref.equals(context.getText(R.string.text_size_small_value))) {
				cacheTextsize(context, R.dimen.text_size_small);
			} else if (textSizePref.equals(context.getText(R.string.text_size_medium_value))) {
				cacheTextsize(context, R.dimen.text_size_medium);
			} else if (textSizePref.equals(context.getText(R.string.text_size_large_value))) {
				cacheTextsize(context, R.dimen.text_size_large);
			} else { // xlarge
				cacheTextsize(context, R.dimen.text_size_xlarge);
			}
		}
		
		return textSize;
		
	}
	
	public static void clearCache() {
		textSize = -1;
		colorScheme = null;
		noteNaming = null;
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
		
		editor.commit();

	}
	public static boolean getFirstRunPreference(Context context) {

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPrefs.getBoolean(context.getString(R.string.pref_first_run), true);

	}
	
	public static ColorScheme getColorScheme(Context context) {
		
		if (colorScheme == null) {
		
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
		
		editor.commit();
		
	}

	public static NoteNaming getNoteNaming(Context context) {
		
		if (noteNaming == null) {
		
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
		editor.commit();
		
	}

	
}
