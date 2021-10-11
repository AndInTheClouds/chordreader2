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

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.handmadeideas.chordreader2.data.ColorScheme;
import org.handmadeideas.chordreader2.helper.ChordDictionary;
import org.handmadeideas.chordreader2.helper.PreferenceHelper;

public class MainBaseActivity extends DrawerBaseActivity implements OnClickListener {

    //private static final UtilLogger log = new UtilLogger(MainBaseActivity.class);

    private LinearLayout mainView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setContentView(R.layout.activity_main);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contentView = inflater.inflate(R.layout.activity_main, null, false);
        mDrawerLayout.addView(contentView, 0);

        mainView = (LinearLayout) findViewById(R.id.main_activity_main_view);

        Button buttonWebSearch = (Button) findViewById(R.id.button_web_search);
        buttonWebSearch.setOnClickListener(this);
        Button buttonLocalSongs = (Button) findViewById(R.id.button_local_songs);
        buttonLocalSongs.setOnClickListener(this);
        Button buttonPlayList = (Button) findViewById(R.id.button_playlists);
        buttonPlayList.setOnClickListener(this);
        buttonPlayList.setVisibility(View.GONE); //TODO: implement it first

        applyColorScheme();

        initializeChordDictionary();

        showInitialMessage();

    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Update drawer selection
        super.selectItem(0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // came back from the settings activity; need to update the colors
        PreferenceHelper.clearCache();
        applyColorScheme();

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.button_web_search) {
            startWebViewActivity();
        } else if (id == R.id.button_local_songs) {
            startSongListActivity();
        } else if (id == R.id.button_playlists) {
            startPlayListActivity();
        }
    }

    private void startWebViewActivity() {

        Intent intent = new Intent(this, WebViewActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void startSongListActivity() {

        Intent intent = new Intent(this, SongListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void startPlayListActivity() {
        Intent intent = new Intent(this, PlayListsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void showInitialMessage() {

        boolean isFirstRun = PreferenceHelper.getFirstRunPreference(getApplicationContext());
        if (isFirstRun) {

            View view = View.inflate(this, R.layout.intro_dialog, null);
            TextView textView = (TextView) view.findViewById(R.id.first_run_text_view);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            textView.setText(R.string.first_run_message);
            textView.setLinkTextColor(ColorStateList.valueOf(getResources().getColor(R.color.linkColorBlue)));
            new AlertDialog.Builder(this)
                    .setTitle(R.string.first_run_title)
                    .setView(view)
                    .setPositiveButton(android.R.string.ok,
                            (dialog, which) -> PreferenceHelper.setFirstRunPreference(getApplicationContext(), false))
                    .setCancelable(false)
                    .setIcon(R.mipmap.chord_reader_icon).show();
        }
    }

    private void initializeChordDictionary() {
        // do in the background to avoid jank

        HandlerThread handlerThread = new HandlerThread("InitializeChordDictionary");
        handlerThread.start();

        Handler asyncHandler = new Handler(handlerThread.getLooper()) {};

        Runnable runnable = () -> {
            // your async code goes here.
            ChordDictionary.initialize(MainBaseActivity.this);
            handlerThread.quit();
        };
        asyncHandler.post(runnable);
    }

    private void applyColorScheme() {

        ColorScheme colorScheme = PreferenceHelper.getColorScheme(this);

        mainView.setBackgroundColor(colorScheme.getBackgroundColor(this));
    }
}