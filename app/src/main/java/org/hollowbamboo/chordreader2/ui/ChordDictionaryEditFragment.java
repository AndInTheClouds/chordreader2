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


import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import org.hollowbamboo.chordreader2.R;
import org.hollowbamboo.chordreader2.chords.Chord;
import org.hollowbamboo.chordreader2.chords.NoteNaming;
import org.hollowbamboo.chordreader2.databinding.FragmentChordDictionaryEditBinding;
import org.hollowbamboo.chordreader2.helper.ChordDictionary;
import org.hollowbamboo.chordreader2.helper.PreferenceHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChordDictionaryEditFragment extends Fragment implements View.OnClickListener {

    private TableLayout chordVarEditView;
    private TextView chordTitleTextView;

    private Chord CHORD;
    private int TOTAL_VAR_NO = 0;

    private final Map<Integer, ArrayList<Spinner>> allChordVarSpinnersMap = new LinkedHashMap<>();
    private final Map<Integer, TableRow> allChordVarTableRows = new LinkedHashMap<>();

    private FragmentChordDictionaryEditBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentChordDictionaryEditBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        chordVarEditView = binding.chordVarView;
        chordTitleTextView = binding.chordEditChordTextView;

        ImageButton addChordVarButton = binding.addChordVarButton;
        addChordVarButton.setOnClickListener(this);
        Button saveButton = binding.chordEditSaveButton;
        saveButton.setOnClickListener(this);
        Button cancelButton = binding.chordEditCancelButton;
        cancelButton.setOnClickListener(this);

        initializeChordEditView();

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle(R.string.chord_variation_editing);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.chord_edit_save_button) {
            updateChordDictionary();
            if (getParentFragment() != null)
                Navigation.findNavController(getParentFragment().requireView()).popBackStack();
        } else if(id == R.id.chord_edit_cancel_button) {
            if (getParentFragment() != null) {
                Navigation.findNavController(getParentFragment().requireView()).popBackStack();
            }
        } else if(id == R.id.add_chord_var_button) {
            addChordVar();
        }

    }

    private void initializeChordEditView() {
        CHORD = ChordDictionaryEditFragmentArgs.fromBundle(getArguments()).getChord();
        NoteNaming noteNaming = PreferenceHelper.getNoteNaming(requireContext());

        chordTitleTextView.setText(new StringBuilder().append("* * *  ").append(CHORD.toPrintableString(noteNaming)).append("  * * *").toString());
        //chordTitleTextView.setTextSize(R.dimen.drawer_text_size);

        List<String> guitarChords = ChordDictionary.getGuitarChordsForChord(CHORD);

        if(guitarChords.size() == 0) {
            TextView textView = new TextView(requireContext());
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
        final TableRow tableRow = new TableRow(requireContext());
        tableRow.setId(varNo);
        TextView textView = new TextView(requireContext());
        textView.setText(new StringBuilder().append(getString(R.string.variation)).append(varNo).append(":").toString());
        textView.setPadding(5,0,15,0);
        tableRow.addView(textView);

        //keep spinner objects temporarily for later saving
        ArrayList<Spinner> spinnersList = new ArrayList<>();

        if(chord.isEmpty()) {
            for (int i = 0; i < 6; i++) {
                Spinner spinner = createSpinner("");
                spinnersList.add(spinner);
                tableRow.addView(spinner);
            }
        } else {
            // extract single tab per string and build spinner
            Pattern chordTabsPattern = Pattern.compile("(\\d{1,2}|x)-(\\d{1,2}|x)-(\\d{1,2}|x)-(\\d{1,2}|x)-(\\d{1,2}|x)-(\\d{1,2}|x)");
            Matcher matcher = chordTabsPattern.matcher(chord);

            if(matcher.find()) {
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
        Spinner spinner = (Spinner) View.inflate(requireContext(), R.layout.spinner_custom_style, null);
        ArrayAdapter<CharSequence> spinnerArrayAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.chord_tabs, R.layout.spinner_chord_edit);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);
        if(initialTab.isEmpty() || initialTab.equals("x"))
            spinner.setSelection(0);
        else
            spinner.setSelection(Integer.parseInt(initialTab)+1);
        return spinner;
    }

    private ImageButton createDeleteButton(final int varNo) {
        ImageButton chordVarDeleteButton = new ImageButton(requireContext());
        chordVarDeleteButton.setImageResource(R.drawable.ic_btn_delete);
        chordVarDeleteButton.setBackgroundColor(Color.TRANSPARENT);
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
        ChordDictionary.setGuitarChordsForChord(requireContext(), CHORD, newGuitarChords);
    }
}