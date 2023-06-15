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

import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import org.hollowbamboo.chordreader2.R;
import org.hollowbamboo.chordreader2.databinding.FragmentStartBinding;

public class StartFragment extends Fragment implements View.OnClickListener, TextureView.SurfaceTextureListener, MediaPlayer.OnCompletionListener  {

    private FragmentStartBinding binding;

    private MediaPlayer mediaPlayer;
    private TextureView textureView;
    Surface surface;
    Uri videoUri;

    public StartFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentStartBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Button buttonWebSearch = binding.buttonWebSearch;
        buttonWebSearch.setOnClickListener(this);
        Button buttonLocalSongs = binding.buttonLocalSongs;
        buttonLocalSongs.setOnClickListener(this);
        Button buttonSetList = binding.buttonSetLists;
        buttonSetList.setOnClickListener(this);

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);

        textureView = binding.videoView;
        textureView.setSurfaceTextureListener(this);

        videoUri = Uri.parse("android.resource://"+ getActivity().getPackageName() +"/"+ R.raw.bgrd_vid);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;

        try {
            if(mediaPlayer != null) {
                // Make sure we stop video and release resources when activity is destroyed.
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
        } catch (Exception e) {
            //do nothing
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.button_web_search) {
            StartFragmentDirections.ActionNavStartToWebSearchFragment action =
                    StartFragmentDirections.actionNavStartToWebSearchFragment("");
            Navigation.findNavController(view).navigate(action);
        } else if(id == R.id.button_local_songs) {
            StartFragmentDirections.ActionNavStartToListFragment action =
                    StartFragmentDirections.actionNavStartToListFragment("Songs");
            Navigation.findNavController(view).navigate(action);
        } else if(id == R.id.button_set_lists) {
            StartFragmentDirections.ActionNavStartToListFragment action =
                    StartFragmentDirections.actionNavStartToListFragment("Setlists");
            Navigation.findNavController(view).navigate(action);
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

        Log.d("StartFragment TA",width + " x " + height);
        updateTextureViewSize(width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {

        surface = new Surface(surfaceTexture);
        mediaPlayer.setSurface(surface);

        Log.d("StartFragment TSC",width + " x " + height);

        updateTextureViewSize(width, height);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // necessary to recalculate media player textureview correctly after orientation change
        textureView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

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
                    mediaPlayer.setDataSource(requireContext(),videoUri);
                    mediaPlayer.setLooping(true);
                    mediaPlayer.prepareAsync();
                    // Play video when the media source is ready for playback.
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mediaPlayer) {
                            double duration = mediaPlayer.getDuration();
                            mediaPlayer.seekTo((int) (Math.random() * duration));
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

        if(viewHeight < viewWidth / videoRatio) {
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
}