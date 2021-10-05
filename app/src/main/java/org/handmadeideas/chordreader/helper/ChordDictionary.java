package org.handmadeideas.chordreader.helper;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.handmadeideas.chordreader.R;
import org.handmadeideas.chordreader.chords.Chord;
import org.handmadeideas.chordreader.chords.NoteNaming;
import org.handmadeideas.chordreader.chords.regex.ChordParser;
import org.handmadeideas.chordreader.util.StringUtil;
import org.handmadeideas.chordreader.util.UtilLogger;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChordDictionary {
	private static final String LOG_TAG = "ChordDictionary";

	// maps chords to finger positions on guitar frets, e.g. 133211
	private static Map<Chord, List<String>> chordsToGuitarChords = null;
	private static String customChordVars;
	
	public static void initialize(Context context) {
		Map<Chord, List<String>> dictionary = new HashMap<>();

		// load custom chord variations
		try {
			customChordVars = SaveFileHelper.openFile("customChordVariations_DO_NOT_EDIT.crd");

			if (!customChordVars.isEmpty()) {
				loadIntoChordDictionary(context, -1, NoteNaming.English, dictionary);
			} else
				throw new IOException();

		} catch (IOException e) {
			//no customChordVariations_DO_NOT_EDIT.crd - ignore
			Log.e(LOG_TAG, e + " - No customChordVariations file, load default");

			try {
				loadIntoChordDictionary(context, R.raw.chords1, NoteNaming.English, dictionary);
				loadIntoChordDictionary(context, R.raw.chords2, NoteNaming.NorthernEuropean, dictionary);

			} catch (IOException f) {
				Log.e(LOG_TAG, f + " - unexpected exception, couldn't initialize ChordDictionary");
			} catch (Exception f) {
				Log.e(LOG_TAG, f + " - unknown exception, couldn't initialize ChordDictionary");
			}
		}
		if (!dictionary.isEmpty())
			Log.i(LOG_TAG, "Chord Dictionary initialized");
		chordsToGuitarChords = dictionary;
	}
	
	private static void loadIntoChordDictionary(Context context, int resId, NoteNaming noteNaming, Map<Chord, List<String>> dictionary) throws IOException {
		InputStream inputStream;
		
		if (resId == -1) {
			inputStream =  new ByteArrayInputStream(customChordVars.getBytes());
		} else {
			inputStream = context.getResources().openRawResource(resId);
		}
		
		BufferedReader bufferedReader = null;
		
		try {
			bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
			while (bufferedReader.ready()) {
				String line = bufferedReader.readLine();
				line = line.trim();
				String[] tokens = StringUtil.split(line, ":");
				
				String chordText = tokens[0].trim();
				String guitarChord = tokens[1].trim();
				
				Chord chord = ChordParser.parseChord(chordText, noteNaming);
				
				if (chord == null) {
					Log.w(LOG_TAG, "Unable to parse chord text - skipping:" + chordText);
					continue;
				}
				
				// map chords to their string guitar chord representations
				// note that there may be multiples - e.g. there are several ways
				// to play a G chord
				List<String> existingValue = dictionary.get(chord);
				if (existingValue == null) {
					dictionary.put(chord, new ArrayList<>(Collections.singleton(guitarChord)));
				} else if (!existingValue.contains(guitarChord)) {
					existingValue.add(guitarChord);
				}
				
			}
		} finally {
			if (bufferedReader != null) {
				bufferedReader.close();
			}
		}
	}
	
	public static boolean isInitialized() {
		return chordsToGuitarChords != null;
	}

	public static List<String> getGuitarChordsForChord(Chord chord) {
		List<String> result = chordsToGuitarChords.get(chord);
		return result != null ? result : Collections.emptyList();
	}

	public static void setGuitarChordsForChord(Chord chord, List<String> newGuitarChords) {
		List<String> existingValue = chordsToGuitarChords.get(chord);
		if (existingValue != null) {
			chordsToGuitarChords.remove(chord);
		}
		chordsToGuitarChords.put(chord, newGuitarChords);

		saveChordDictionaryToFile();
	}

	private static void saveChordDictionaryToFile() {
		StringBuilder stringBuilder = new StringBuilder();
		for (Object key : chordsToGuitarChords.keySet()) {
			String chord = ((Chord) key).toPrintableString(NoteNaming.English);
			List<String> chordVarsList = chordsToGuitarChords.get(key);

			for (String chordVar : chordVarsList) {
				stringBuilder
						.append(chord)
						.append(": ")
						.append(chordVar)
						.append('\n');
			}
		}

		final String result = stringBuilder.substring(0, stringBuilder.length() - 1); // cut off final newline

		// do in background to avoid jankiness
		AsyncTask<Void,Void,Boolean> saveTask = new AsyncTask<Void, Void, Boolean>(){

			@Override
			protected Boolean doInBackground(Void... params) {
				return SaveFileHelper.saveFile(result, "customChordVariations_DO_NOT_EDIT.crd");
			}
		};

		saveTask.execute((Void)null);
	}
}
