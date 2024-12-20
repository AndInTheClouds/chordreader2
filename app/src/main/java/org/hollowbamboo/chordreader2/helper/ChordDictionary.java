package org.hollowbamboo.chordreader2.helper;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.widget.Toast;

import org.hollowbamboo.chordreader2.R;
import org.hollowbamboo.chordreader2.chords.Chord;
import org.hollowbamboo.chordreader2.chords.NoteNaming;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ChordDictionary {
    private static final String LOG_TAG = "ChordDictionary";

    // maps chords to finger positions on guitar frets, e.g. 133211
    private static final Map<Chord, List<String>> chordsToGuitarChords = null;


    public static List<String> getFingerPositionsForChord(Context context, Chord chord, String instrument) {

        String laterality = PreferenceHelper.getLaterality(context);

        if (instrument == null) {
            instrument = PreferenceHelper.getInstrument(context);
        }

        List<String[]> chordList = new ArrayList<>();

        switch (instrument) {
            case ("Guitar"):
                chordList = getGuitarChordFromJSON(context,
                        chord.toPrintableString(NoteNaming.English),
                        chord.toPrintableString(NoteNaming.NorthernEuropean));
                break;
            case ("Ukulele"):
                chordList = getUkuleleChordFromJSON(context, chord);
        }

        if (chordList.isEmpty())
            return Collections.emptyList();

        List<String> result = new ArrayList<>();

        for (String[] chordPositions : chordList) {

            StringBuilder sb = new StringBuilder();

            if (laterality.equals("left"))
                Collections.reverse(Arrays.asList(chordPositions));

            for (String s : chordPositions) {
                sb.append(s).append("-");
            }
            sb.deleteCharAt(sb.length() - 1);

            result.add(sb.toString());
        }

        return result;
    }

    public static void setGuitarChordsForChord(Context context, Chord chord, List<String> newGuitarChords) {
        List<String> existingValue = chordsToGuitarChords.get(chord);
        if (existingValue != null) {
            chordsToGuitarChords.remove(chord);
        }
        chordsToGuitarChords.put(chord, newGuitarChords);

        saveChordDictionaryToFile(context);
    }

    private static void saveChordDictionaryToFile(Context context) {
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

        boolean successfullySavedLog = SaveFileHelper.saveFile(
                context, result, "customChordVariations_DO_NOT_EDIT.crd");

        String toastMessage;

        if (successfullySavedLog) {
            toastMessage = context.getString(R.string.file_saved);
        } else {
            toastMessage = context.getString(R.string.unable_to_save_file);
        }

        // must be called on the main thread
        Handler mainThread = new Handler(Looper.getMainLooper());
        mainThread.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show();
            }
        });

    }

    public static List<String[]> getGuitarChordFromJSON(Context context, String chord, String chord2) {

        List<String[]> result = new ArrayList<>();
        BufferedReader bufferedReader = null;
        JsonReader reader = null;

        try {
            bufferedReader = new BufferedReader(
                    new InputStreamReader(context.getResources().openRawResource(R.raw.guitar_chords)));
            reader = new JsonReader(bufferedReader);

            reader.beginObject();
            while (reader.hasNext()) {
                String chordName = reader.nextName();

                if (chord.equals(chordName) || chord2.equals(chordName)) {
                    Log.i(LOG_TAG, "Chord " + chordName);


                    if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                        reader.beginArray();
                        while (reader.hasNext()) {
                            reader.beginObject();

                            String[] positions = null;

                            try {
                                while (reader.hasNext()) {
                                    String name = reader.nextName();
                                    if (name.equals("positions")) {
                                        reader.beginArray();
                                        positions = new String[6];
                                        int i = 0;
                                        while (reader.hasNext()) {
                                            positions[i] = reader.nextString();
                                            i++;
                                        }
                                        reader.endArray();
                                    } else {
                                        reader.skipValue();
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            if (positions != null) {
                                result.add(positions);
                            }

                            reader.endObject();
                        }
                        reader.endArray();
                    } else {
                        reader.skipValue();
                    }

                    return result;
                } else {
                    reader.skipValue();
                }
            }

            reader.endObject();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            assert bufferedReader != null;
            try {
                bufferedReader.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, " unable to close BufferedReader");
            }

            assert reader != null;
            try {
                reader.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, " unable to close JSONReader");
            }
        }

        return new ArrayList<>();
    }

    private static List<String[]> getUkuleleChordFromJSON(Context context, Chord chord) {

        String chordStr1 = chord.toPrintableString(NoteNaming.English);
        String chordStr2 = chord.toPrintableString(NoteNaming.NorthernEuropean);
        String chordRoot = String.valueOf(chord.getRoot());

        List<String[]> result = new ArrayList<>();
        BufferedReader bufferedReader = null;
        JsonReader reader = null;

        try {
            bufferedReader = new BufferedReader(
                    new InputStreamReader(context.getResources().openRawResource(R.raw.ukulele_chords)));
            reader = new JsonReader(bufferedReader);

            reader.beginObject();

            while (reader.hasNext()) {
                String key = reader.nextName();
                if (key.equals("chords")) {
                    reader.beginObject();

                    while (reader.hasNext()) {
                        String chordName = reader.nextName();
                        if (chordName.equals(chordRoot)) {
                            reader.beginArray();

                            while (reader.hasNext()) {
                                reader.beginObject();

                                String chordKey = null;
                                String chordSuffix = null;

                                while (reader.hasNext()) {
                                    String name = reader.nextName();
                                    switch (name) {
                                        case "key":
                                            chordKey = reader.nextString();
                                            break;
                                        case "suffix":
                                            chordSuffix = reader.nextString();
                                            break;
                                        case "positions":
                                            if (chordStr1.equals(chordKey + chordSuffix) || chordStr2.equals(chordKey + chordSuffix)) {
                                                reader.beginArray();

                                                while (reader.hasNext()) {
                                                    reader.beginObject();

                                                    int[] positions = null;
                                                    int baseFret = 1;

                                                    try {
                                                        while (reader.hasNext()) {
                                                            String innerName = reader.nextName();
                                                            if (innerName.equals("frets")) {
                                                                if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                                                                    reader.beginArray();
                                                                    positions = new int[4];
                                                                    int i = 0;
                                                                    while (reader.hasNext()) {
                                                                        positions[i] = Integer.parseInt(reader.nextString());
                                                                        i++;
                                                                    }
                                                                    reader.endArray();
                                                                }
                                                            } else if (innerName.equals("baseFret")) {
//                                                            if (reader.peek() == JsonToken.BEGIN_ARRAY) {
//                                                                reader.beginArray();
                                                                baseFret = Integer.parseInt(reader.nextString());
//                                                            }
//                                                            reader.endArray();
                                                            } else {
                                                                reader.skipValue();
                                                            }
                                                        }
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }

                                                    if (positions != null) {

                                                        String[] fretPos = new String[4];

                                                        for (int i = 0; i < positions.length; i++) {
                                                            fretPos[i] = String.valueOf(positions[i] + baseFret - 1);
                                                        }

                                                        result.add(fretPos);
                                                    }

                                                    reader.endObject();
                                                }

                                                reader.endArray();

                                                return result;
                                            } else {
                                                reader.skipValue();
                                            }
                                            break;
                                        default:
                                            reader.skipValue();
                                            break;
                                    }
                                }

                                reader.endObject();
                            }

                            reader.endArray();
                        } else {
                            reader.skipValue();
                        }
                    }

                    reader.endObject();
                } else {
                    reader.skipValue();
                }
            }

            reader.endObject();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            assert bufferedReader != null;
            try {
                bufferedReader.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, " unable to close BufferedReader");
            }

            assert reader != null;
            try {
                reader.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, " unable to close JSONReader");
            }
        }

        return new ArrayList<>();
    }
}
