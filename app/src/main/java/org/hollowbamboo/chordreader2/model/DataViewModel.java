package org.hollowbamboo.chordreader2.model;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.hollowbamboo.chordreader2.helper.SaveFileHelper;

import java.util.ArrayList;

public class DataViewModel extends ViewModel {

    public String mode;
    public ArrayList<String> setListSongs;

    private MutableLiveData<String> setListMLD = new MutableLiveData<>();
    public MutableLiveData<ArrayList<String>> setListSongsMLD;

    public void setSetListMLD(String setlist) {
        if (setListMLD == null)
            setListMLD = new MutableLiveData<>();

        setListMLD.setValue(setlist);

        setListSongsMLD.setValue(setListSongs);
    }

    public MutableLiveData<String> getSetListMLD() {
        return setListMLD;
    }

    public MutableLiveData<ArrayList<String>> getSetListSongsMLD() { return setListSongsMLD; }

    public void setSetListSongs(ArrayList<String> setListSongs) {
        this.setListSongs = setListSongs;

        if (setListSongsMLD == null)
            setListSongsMLD = new MutableLiveData<>();

        setListSongsMLD.setValue(setListSongs);
    }

    public void resetData() {
        setListSongs = null;
        setListMLD = null;
        setListSongsMLD = null;
    }
}