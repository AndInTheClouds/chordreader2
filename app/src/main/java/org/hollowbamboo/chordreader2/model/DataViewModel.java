package org.hollowbamboo.chordreader2.model;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.hollowbamboo.chordreader2.helper.SaveFileHelper;

import java.util.ArrayList;

public class DataViewModel extends ViewModel {

    public String mode;
    public ArrayList<String> playlistSongs;

    private MutableLiveData<String> playlistMLD;
    public MutableLiveData<ArrayList<String>> playlistSongsMLD;

    public void setPlaylistMLD(String playlist) {
        playlistMLD = new MutableLiveData<>();
        playlistMLD.setValue(playlist);

        playlistSongs = (ArrayList<String>) SaveFileHelper.openPlaylist(playlist);

        playlistSongsMLD = new MutableLiveData<>();
        playlistSongsMLD.setValue(playlistSongs);
    }

    public MutableLiveData<String> getPlaylistMLD() {
        return playlistMLD;
    }

    public MutableLiveData<ArrayList<String>> getPlaylistSongsMLD() { return playlistSongsMLD; }

    public void setPlaylistSongs(ArrayList<String> playlistSongs) {
        this.playlistSongs = playlistSongs;
        playlistSongsMLD.setValue(playlistSongs);
    }

    public void resetData() {
        playlistSongs = null;
        playlistMLD = null;
        playlistSongsMLD = null;
    }
}