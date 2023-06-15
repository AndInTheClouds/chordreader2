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

import android.os.Bundle;
import android.text.Html;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.hollowbamboo.chordreader2.R;
import org.hollowbamboo.chordreader2.databinding.FragmentHelpBinding;


public class HelpFragment extends Fragment {

    ScrollView scrollView;
    TextView textView;

    public HelpFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        org.hollowbamboo.chordreader2.databinding.FragmentHelpBinding binding = FragmentHelpBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        textView = binding.helpPageIntro;
        scrollView = binding.scrollView;

        String sectionID = HelpFragmentArgs.fromBundle(getArguments()).getSectionID();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
            textView.setText(Html.fromHtml(getString(R.string.help_page),Html.FROM_HTML_MODE_LEGACY));
        else
            textView.setText (Html.fromHtml (getString (R.string.help_page)));

        scrollToSection(sectionID);

        return root;
    }

    private void scrollToSection(String sectionID) {

        textView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int lineNo = 0;
                int start = 0;
                int end;

                Layout layout = textView.getLayout();
                CharSequence text = textView.getText();

                if (!sectionID.isEmpty()) {
                    for (int i = 0; i < textView.getLineCount(); i++) {
                        end = layout.getLineEnd(i);
                        String slice = text.subSequence(start, end).toString();
                        if (slice.contains(sectionID)) {
                            lineNo = i;
                            break;
                        }
                        start = end;
                    }
                }

                int y = layout.getLineTop(lineNo);
                scrollView.scrollTo(0, y);
            }
        });


    }

}
