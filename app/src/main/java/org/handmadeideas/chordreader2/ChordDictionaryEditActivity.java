package org.handmadeideas.chordreader2;

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

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.handmadeideas.chordreader2.chords.Chord;
import org.handmadeideas.chordreader2.chords.NoteNaming;
import org.handmadeideas.chordreader2.helper.ChordDictionary;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChordDictionaryEditActivity extends Activity implements View.OnClickListener {

    private TableLayout chordVarEditView;
    private TextView chordTitleTextView;

    private Chord CHORD;
    private int TOTAL_VAR_NO = 0;

    private final Map<Integer, ArrayList<Spinner>> allChordVarSpinnersMap = new LinkedHashMap<>();
    private final Map<Integer, TableRow> allChordVarTableRows = new LinkedHashMap<>();

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.chord_var_edit);

        chordVarEditView = (TableLayout) findViewById(R.id.chord_var_view);
        chordTitleTextView = (TextView) findViewById(R.id.chord_edit_chord_TextView);

        ImageButton addChordVarButton = (ImageButton) findViewById(R.id.add_chord_var_button);
        addChordVarButton.setOnClickListener(this);
        Button saveButton = (Button) findViewById(R.id.chord_edit_save_button);
        saveButton.setOnClickListener(this);
        Button cancelButton = (Button) findViewById(R.id.chord_edit_cancel_button);
        cancelButton.setOnClickListener(this);

        initializeChordEditView();
    }



    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.chord_edit_save_button) {
            updateChordDictionary();
            finish();
        } else if (id == R.id.chord_edit_cancel_button) {
            finish();
        } else if (id == R.id.add_chord_var_button) {
            addChordVar();
        }

    }

    private void initializeChordEditView() {
        CHORD = (Chord) getIntent().getSerializableExtra("CHORD");
        NoteNaming NOTENAMING = (NoteNaming) getIntent().getSerializableExtra("NOTENAMING");

        chordTitleTextView.setText(new StringBuilder().append("* * *  ").append(CHORD.toPrintableString(NOTENAMING)).append("  * * *").toString());

        List<String> guitarChords = ChordDictionary.getGuitarChordsForChord(CHORD);

        if (guitarChords.size() == 0) {
            TextView textView = new TextView(this);
            textView.setText(R.string.no_chord_available_add);
            chordVarEditView.addView(textView);
            chordVarEditView.addView(createChordVar("", 1));
        } else {
            for (int i = 0; i < guitarChords.size(); i++) {
                chordVarEditView.addView(createChordVar(guitarChords.get(i), i+1));
                TOTAL_VAR_NO += 1;
            }
        }
    }

    private void addChordVar () {
        chordVarEditView.addView(createChordVar("", TOTAL_VAR_NO + 1));
        TOTAL_VAR_NO += 1;
    }

    private TableRow createChordVar(String chord, int varNo) {
        final TableRow tableRow = new TableRow(this);
        tableRow.setId(varNo);
        TextView textView = new TextView(this);
        textView.setText(new StringBuilder().append(getString(R.string.variation)).append(varNo).toString());
        textView.setTextColor(getResources().getColor(R.color.scheme_light_foreground) );
        textView.setPadding(5,0,5,0);
        tableRow.addView(textView);

        //keep spinner objects temporarily for later saving
        ArrayList<Spinner> spinnersList = new ArrayList<>();

        if (chord.isEmpty()) {
            for (int i = 0; i < 6; i++) {
                Spinner spinner = createSpinner("");
                spinnersList.add(spinner);
                tableRow.addView(spinner);
            }
        } else {
            // extract single tab per string and build spinner
            Pattern chordTabsPattern = Pattern.compile("(\\d{1,2}|x)-(\\d{1,2}|x)-(\\d{1,2}|x)-(\\d{1,2}|x)-(\\d{1,2}|x)-(\\d{1,2}|x)");
            Matcher matcher = chordTabsPattern.matcher(chord);

            if (matcher.find()) {
                for (int i = 0; i < 6; i++) {
                    Spinner spinner = createSpinner(matcher.group(i + 1));
                    spinnersList.add(spinner);
                    tableRow.addView(spinner);
                }
            }
        }

        ImageButton chordVarDeleteButton = createDeleteButton(varNo);
        tableRow.addView(chordVarDeleteButton);

        allChordVarSpinnersMap.put(varNo, spinnersList);
        allChordVarTableRows.put(varNo, tableRow);
        return tableRow;
    }

    private Spinner createSpinner(String initialTab) {
        Spinner spinner = (Spinner) View.inflate(this, R.layout.spinner_custom_style, null);
        ArrayAdapter<CharSequence> spinnerArrayAdapter = ArrayAdapter.createFromResource(this, R.array.chord_tabs, android.R.layout.simple_spinner_item);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinner.setAdapter(spinnerArrayAdapter);
        if (initialTab.isEmpty() || initialTab.equals("x"))
            spinner.setSelection(0);
        else
            spinner.setSelection(Integer.parseInt(initialTab)+1);
        return spinner;
    }

    private ImageButton createDeleteButton(final int varNo) {
        ImageButton chordVarDeleteButton = new ImageButton(this);
        chordVarDeleteButton.setImageResource(R.drawable.ic_btn_delete);
        chordVarDeleteButton.setBackgroundColor(Color.TRANSPARENT); //R.drawable.popup_background
        chordVarDeleteButton.setOnClickListener(view -> removeChordVar(varNo));
        chordVarDeleteButton.setId(varNo);
        chordVarDeleteButton.setScaleX(0.5f);
        chordVarDeleteButton.setScaleY(0.5f);
        chordVarDeleteButton.setPadding(0,0,0,0);

        return chordVarDeleteButton;
    }

    private void removeChordVar(int varNo) {
        TableRow tableRow = allChordVarTableRows.get(varNo);
        chordVarEditView.removeView(tableRow);
        allChordVarSpinnersMap.remove(varNo);
    }

    private void updateChordDictionary() {
        List<String> newGuitarChords = new ArrayList<>();

        for (Integer key : allChordVarSpinnersMap.keySet()) {
            ArrayList<Spinner> spinnersList = allChordVarSpinnersMap.get(key);
            StringBuilder stringBuilder = new StringBuilder();
            for (Spinner spinner : spinnersList) {
                stringBuilder.append(spinner.getSelectedItem().toString());
                stringBuilder.append("-");
            }
            newGuitarChords.add(stringBuilder.substring(0, stringBuilder.length() - 1)); //remove last '-'
        }
        ChordDictionary.setGuitarChordsForChord(CHORD, newGuitarChords);
    }
}
