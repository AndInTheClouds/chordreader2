package org.hollowbamboo.chordreader2.model;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ListFragmentViewModel extends ViewModel {



    private final MutableLiveData<String> urlMLD = new MutableLiveData<>();

    public MutableLiveData<String> getUrlMLD() { return urlMLD; }



}