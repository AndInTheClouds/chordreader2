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


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import org.hollowbamboo.chordreader2.R;
import org.hollowbamboo.chordreader2.databinding.AboutBinding;
import org.hollowbamboo.chordreader2.helper.PackageHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

public class AboutFragment extends Fragment implements View.OnClickListener {

    AboutBinding binding;
    private Button okButton;
    private WebView aboutWebView;
    private ProgressBar progressBar;
    private final Handler handler = new Handler();
    private View topPanel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        assert actionBar != null;
        actionBar.hide();

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = AboutBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        topPanel = binding.topPanel;
        topPanel.setVisibility(View.GONE);
        okButton = binding.okButton;
        okButton.setOnClickListener(this);
        okButton.setVisibility(View.GONE);

        aboutWebView = binding.aboutTextWebView;
        aboutWebView.setVisibility(View.GONE);
        aboutWebView.setBackgroundColor(0);
        aboutWebView.setWebViewClient(new AboutWebClient());

        progressBar = binding.aboutProgressBar;

        initializeWebView();

        return root;

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


    @Override
    public void onClick(View view) {
        assert getParentFragment() != null;
        Navigation.findNavController(getParentFragment().requireView()).popBackStack();
    }

    public void initializeWebView() {
        String newhtml_code = Base64.encodeToString(loadTextFile().getBytes(), Base64.NO_PADDING);
        aboutWebView.loadData(newhtml_code,"text/html", "base64");
    }




    private String loadTextFile() {

        InputStream inputStream;
        Locale locale = Locale.getDefault();

        if(locale.equals(Locale.GERMAN))
            inputStream = getResources().openRawResource(R.raw.about_body_de);
        else if(locale.equals(Locale.FRENCH))
            inputStream = getResources().openRawResource(R.raw.about_body_fr);
        else
            inputStream = getResources().openRawResource(R.raw.about_body_en);

        BufferedReader buff = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();

        try {
            while (buff.ready()) {
                sb.append(buff.readLine()).append("\n");
            }
        } catch (IOException e) {
            Log.e("AboutActivity","This should not happen",e);
        }

        String result = sb.toString();

        // format the version into the string
        return String.format(result, PackageHelper.getVersionName(requireContext()));
    }

    private void loadExternalUrl(String url) {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.setData(Uri.parse(url));

        startActivity(intent);
    }

    private class AboutWebClient extends WebViewClient {



        @Override
        public boolean shouldOverrideUrlLoading(WebView view, final String url) {
            Log.d("AboutFragment","shouldOverrideUrlLoading");

            // XXX hack to make the webview go to an external url if the hyperlink is
            // in my own HTML file - otherwise it says "Page not available" because I'm not calling
            // loadDataWithBaseURL.  But if I call loadDataWithBaseUrl using a fake URL, then
            // the links within the page itself don't work!!  Arggggh!!!

            if(url.startsWith("http") || url.startsWith("mailto") || url.startsWith("market")) {
                handler.post(() -> loadExternalUrl(url));
                return true;
            }
            return false;
        }


        @Override
        public void onPageFinished(WebView view, String url) {
            // dismiss the loading bar when the page has finished loading
            handler.post(() -> {
                progressBar.setVisibility(View.GONE);
                aboutWebView.setVisibility(View.VISIBLE);
                topPanel.setVisibility(View.VISIBLE);
                okButton.setVisibility(View.VISIBLE);

            });
            super.onPageFinished(view, url);
        }
    }
}