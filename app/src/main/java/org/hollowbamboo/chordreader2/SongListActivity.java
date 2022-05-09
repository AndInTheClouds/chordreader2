package org.hollowbamboo.chordreader2;

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

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.hollowbamboo.chordreader2.adapter.SelectableFilterAdapter;
import org.hollowbamboo.chordreader2.data.ColorScheme;
import org.hollowbamboo.chordreader2.helper.PreferenceHelper;
import org.hollowbamboo.chordreader2.helper.SaveFileHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SongListActivity extends DrawerBaseActivity implements TextWatcher {

    private LinearLayout songListMainView;
    private EditText filterEditText;
    private ListView fileList;
    SelectableFilterAdapter fileListAdapter;
    private Menu menu;

    private int indexCurrentPosition, top;
    private boolean IsSelectionModeActive;

    //private static UtilLogger log = new UtilLogger(SongListActivity.class);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setContentView(R.layout.activity_song_list);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contentView = inflater.inflate(R.layout.activity_song_list, null, false);
        mDrawerLayout.addView(contentView, 0);

        fileList = (ListView) findViewById(R.id.file_list);
        fileList.setTextFilterEnabled(true);

        songListMainView = (LinearLayout) findViewById(R.id.song_list_main_view);

        filterEditText = (EditText) findViewById(R.id.song_list_filter_text_view);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // save index and top position
        indexCurrentPosition = fileList.getFirstVisiblePosition();
        View v = fileList.getChildAt(0);
        top = (v == null) ? 0 : (v.getTop() - fileList.getPaddingTop());
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpWidgets();

        filterEditText.setText("");

        //Update drawer selection
        super.selectItem(2);

        // restore index and position
        fileList.setSelectionFromTop(indexCurrentPosition, top);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.song_list_menu, menu);

        this.menu = menu;

        if (fileListAdapter == null)
            menu.findItem(R.id.menu_manage_files).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemId = item.getItemId();
        if (itemId == R.id.menu_manage_files) {
            startSelectionMode(0);
            return true;
        } else if (itemId == R.id.menu_new_file) {
            startChordViewActivity(null);
            return true;
        } else if (itemId == R.id.menu_cancel_selection) {
            cancelSelectionMode();
            return true;
        } else if (itemId == R.id.menu_select_all) {
            fileListAdapter.selectAll();
            return true;
        } else if (itemId == R.id.menu_delete) {
            verifyDelete();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // came back from the settings activity; need to update the colours
        PreferenceHelper.clearCache();

        setUpWidgets();
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        //do nothing
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        this.fileListAdapter.getFilter().filter(charSequence);
    }

    @Override
    public void afterTextChanged(Editable editable) {
        //do nothing
    }

    private void setUpWidgets() {

        //apply color scheme
        ColorScheme colorScheme = PreferenceHelper.getColorScheme(this);
        songListMainView.setBackgroundColor(colorScheme.getBackgroundColor(this));
        final int foregroundColor = colorScheme.getForegroundColor(this);
        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, new int[]{0, foregroundColor});
        gradientDrawable.setAlpha(120);
        fileList.setDivider(gradientDrawable);
        fileList.setDividerHeight(1);

        if (!checkSdCard()) {
            return;
        }

        final List<String> filenames = new ArrayList<>(SaveFileHelper.getSavedFilenames());

        if (filenames.isEmpty()) {
            return;
        }

        Collections.sort(filenames, (Comparator<CharSequence>) (first, second) -> first.toString().toLowerCase().compareTo(second.toString().toLowerCase()));

        fileListAdapter = new SelectableFilterAdapter(this, filenames) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setTextColor(foregroundColor);
                return textView;
            }
        };

        fileList.setAdapter(fileListAdapter);

        fileList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String filename = (String) adapterView.getAdapter().getItem(i);
                view.setBackgroundColor(Color.GRAY);

                if (SongListActivity.this.IsSelectionModeActive) {
                    fileListAdapter.switchSelectionForIndex(i);
                } else
                    startChordViewActivity(filename);
            }
        });

        fileList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int pos, long arg3) {
                startSelectionMode(pos);
                fileListAdapter.switchSelectionForIndex(pos);
                return true;
            }
        });

        filterEditText.addTextChangedListener(this);
    }

    private void startSelectionMode(int pos) {
        IsSelectionModeActive = true;
        fileList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        menu.findItem(R.id.menu_manage_files).setVisible(false);
        menu.findItem(R.id.menu_new_file).setVisible(false);
        menu.findItem(R.id.menu_delete).setVisible(true);
        menu.findItem(R.id.menu_cancel_selection).setVisible(true);
        menu.findItem(R.id.menu_select_all).setVisible(true);
    }


    private void cancelSelectionMode() {
        IsSelectionModeActive = false;
        fileList.setChoiceMode(ListView.CHOICE_MODE_NONE);

        menu.findItem(R.id.menu_manage_files).setVisible(true);
        menu.findItem(R.id.menu_new_file).setVisible(true);
        menu.findItem(R.id.menu_delete).setVisible(false);
        menu.findItem(R.id.menu_cancel_selection).setVisible(false);
        menu.findItem(R.id.menu_select_all).setVisible(false);

        fileListAdapter.unselectAll();
    }

    private boolean checkSdCard() {

        boolean result = SaveFileHelper.checkIfSdCardExists();

        if (!result) {
            super.showToastLong(getResources().getString(R.string.sd_card_not_found));
        }
        return result;
    }


    protected void verifyDelete() {

        if (!checkSdCard()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        final CharSequence[] filenameArray = fileListAdapter.getSelectedFiles();
        Log.d("SongListActivity", Arrays.toString(filenameArray));
        final int finalDeleteCount = filenameArray.length;

        if (finalDeleteCount > 0) {

            builder.setTitle(R.string.delete_saved_file)
                    .setCancelable(true)
                    .setMessage(String.format(getText(R.string.are_you_sure).toString(), finalDeleteCount))
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        // ok, delete

                        for (CharSequence charSequence : filenameArray) {
                            SaveFileHelper.deleteFile(charSequence.toString());
                        }
                        setUpWidgets();

                        String toastText = String.format(getText(R.string.files_deleted).toString(), finalDeleteCount);
                        super.showToastShort(toastText);

                        dialog.dismiss();

                    });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.show();
        }
    }

    private void startChordViewActivity(String filename) {
        Intent intent = new Intent(this, SongViewActivity.class);
        Bundle b = new Bundle();
        b.putString("filename", filename);
        intent.putExtras(b);
        startActivity(intent);
    }
}