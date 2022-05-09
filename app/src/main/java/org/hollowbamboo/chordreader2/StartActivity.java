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

import static android.os.Build.VERSION.SDK_INT;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.hollowbamboo.chordreader2.helper.ChordDictionary;
import org.hollowbamboo.chordreader2.helper.PreferenceHelper;

public class StartActivity extends DrawerBaseActivity implements OnClickListener, TextureView.SurfaceTextureListener, MediaPlayer.OnCompletionListener {

    private static final int STORAGE_PERMISSION_CODE = 100;

    private final Context context = this;
    private MediaPlayer mediaPlayer;
    private TextureView textureView;
    Surface surface;
    Uri videoUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contentView = inflater.inflate(R.layout.activity_start, null, false);
        mDrawerLayout.addView(contentView, 0);

        Button buttonWebSearch = (Button) findViewById(R.id.button_web_search);
        buttonWebSearch.setOnClickListener(this);
        Button buttonLocalSongs = (Button) findViewById(R.id.button_local_songs);
        buttonLocalSongs.setOnClickListener(this);
        Button buttonPlayList = (Button) findViewById(R.id.button_playlists);
        buttonPlayList.setOnClickListener(this);
        buttonPlayList.setVisibility(View.GONE); //TODO: implement it first

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);

        textureView = (TextureView) findViewById(R.id.video_view);
        textureView.setSurfaceTextureListener(this);

        videoUri = Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.bgrd_vid);


        initializeChordDictionary();

        showInitialMessage();

        if (!areStoragePermissionsGranted())
            requestPermission();
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
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            // Make sure we stop video and release resources when activity is destroyed.
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        textureView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
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

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mediaPlayer.stop();
        mediaPlayer.release();
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {

        surface = new Surface(surfaceTexture);
        mediaPlayer.setSurface(surface);

        updateTextureViewSize(width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {

        surface = new Surface(surfaceTexture);
        mediaPlayer.setSurface(surface);

        updateTextureViewSize(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

    }

    private void playVideo() {
        //its a big file - use separate thread
        new Thread(new Runnable() {
            public void run() {
                try {
                    mediaPlayer.setDataSource(context,videoUri);
                    mediaPlayer.setLooping(true);
                    mediaPlayer.prepareAsync();
                    // Play video when the media source is ready for playback.
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mediaPlayer) {
                            mediaPlayer.start();
                        }
                    });
                } catch (Exception e) {
                    Log.e("Error: ", e.toString());
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //uses the view width to determine best crop to fit the screen
    //@param int viewWidth width of viewport
    //@param int viewHeight height of viewport

    private void updateTextureViewSize(int viewWidth, int viewHeight) {
        float scaleX = 1.0f;
        float scaleY = 1.0f;

        float videoWidth = 1920;
        float videoHeight = 1080;
        float videoRatio = videoWidth / videoHeight;

        if (viewHeight < viewWidth / videoRatio) {
            scaleY = viewWidth / videoRatio / viewHeight;
        } else
            scaleX = viewHeight * videoRatio / viewWidth;

        // Calculate pivot points, in our case crop from center
        int pivotPointX = viewWidth / 2;
        int pivotPointY = viewHeight / 2;

        Matrix matrix = new Matrix();
        matrix.setScale(scaleX, scaleY, pivotPointX, pivotPointY);
        //transform the video viewing size
        textureView.setTransform(matrix);
        //set the width and height of playing view
        textureView.setLayoutParams(new RelativeLayout.LayoutParams(viewWidth, viewHeight));
        //finally, play the video
        playVideo();
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

    private boolean areStoragePermissionsGranted() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            int result = ContextCompat.checkSelfPermission(StartActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
            int result1 = ContextCompat.checkSelfPermission(StartActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermission()
    {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                startActivityForResult(intent, 2296);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, 2296);
            }
        } else {
            //below android 11
            ActivityCompat.requestPermissions(StartActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,@NonNull String[] permissions,@NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(StartActivity.this, "Storage Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(StartActivity.this, "Storage Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void initializeChordDictionary() {
        // do in the background to avoid jank

        HandlerThread handlerThread = new HandlerThread("InitializeChordDictionary");
        handlerThread.start();

        Handler asyncHandler = new Handler(handlerThread.getLooper()) {};

        Runnable runnable = () -> {
            // your async code goes here.
            ChordDictionary.initialize(StartActivity.this);
            handlerThread.quit();
        };
        asyncHandler.post(runnable);
    }


}