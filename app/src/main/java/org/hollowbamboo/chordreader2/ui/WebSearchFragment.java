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


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import org.hollowbamboo.chordreader2.R;
import org.hollowbamboo.chordreader2.data.ColorScheme;
import org.hollowbamboo.chordreader2.databinding.FragmentWebSearchBinding;
import org.hollowbamboo.chordreader2.db.ChordReaderDBHelper;
import org.hollowbamboo.chordreader2.helper.PreferenceHelper;
import org.hollowbamboo.chordreader2.model.DataViewModel;
import org.hollowbamboo.chordreader2.model.WebSearchViewModel;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WebSearchFragment extends Fragment implements TextView.OnEditorActionListener, View.OnClickListener, TextWatcher {

    private static final String LOG_TAG = "WebSearchFragment";
    private static final long HISTORY_WINDOW = TimeUnit.SECONDS.toMillis(60 * 60 * 24 * 360); // about one year

    private WebSearchViewModel webSearchViewModel;
    private FragmentWebSearchBinding binding;

    private AutoCompleteTextView searchEditText;
    private View messageSecondaryView;
    private LinearLayout mainView;
    private TextView messageTextView;
    private ProgressBar progressBar;
    private ImageView infoIconImageView;
    private ImageButton searchButton;
    private WebView webView;
    private LinearLayout webViewFrame;

    private ArrayAdapter<String> queryAdapter;

    private final Handler handler = new Handler(Looper.getMainLooper());

    public WebSearchFragment() {
        setArguments(new Bundle());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        webSearchViewModel =
                new ViewModelProvider(this).get(WebSearchViewModel.class);

        DataViewModel dataViewModel =
                new ViewModelProvider(requireActivity()).get(DataViewModel.class);
        dataViewModel.resetData();

        binding = FragmentWebSearchBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        //getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mainView = binding.findChordsFindingView;
        searchEditText = binding.findChordsEditText;

        webView = binding.findChordsWebView;
        webViewFrame = binding.webViewFrame;

        progressBar = binding.findChordsProgressBar;
        infoIconImageView = binding.findChordsImageView;
        searchButton = binding.findChordsSearchButton;
        searchButton.setOnClickListener(this);
        searchEditText = binding.findChordsEditText;
        searchEditText.setOnEditorActionListener(this);
        searchEditText.addTextChangedListener(this);
        searchEditText.setOnClickListener(this);

        messageSecondaryView = binding.findChordsMessageSecondaryView;
        messageSecondaryView.setOnClickListener(this);
        messageSecondaryView.setEnabled(false);

        messageTextView = binding.findChordsMessageTextView;

        applyPreferences();

        handleBackButton();
        prepareQuerySaver();
        setUpInstanceData();
        setUpMenu();
        setUpWebView();

        setObserversForLiveData();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        applyPreferences();
    }

    @Override
    public void onPause() {
        super.onPause();

        Bundle bundle = new Bundle();
        webView.saveState(bundle);

        assert getArguments() != null;
        getArguments().putBundle("webViewState", bundle);

        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
    }

    private void setUpMenu() {
        MenuHost menuHost = requireActivity();
        MenuProvider menuProvider = new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.web_view_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int itemId = menuItem.getItemId();
                if(itemId == R.id.menu_stop) {
                    stopWebView();
                    return true;
                } else if(itemId == R.id.menu_refresh) {
                    refreshWebView();
                    return true;
                } else if(itemId == android.R.id.home) {
                    if(webView.copyBackForwardList().getCurrentIndex() > 0) {
                        webView.goBack();
                    } else if (getParentFragment() != null) {
                        Navigation.findNavController(getParentFragment().requireView()).popBackStack();
                    }
                    return true;
                }
                return false;
            }
        };

        menuHost.addMenuProvider(menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void setUpWebView() {
        webView.setWebViewClient(webSearchViewModel.getClient());

        assert getArguments() != null;
        webView.restoreState(getArguments().getBundle("webViewState"));

        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(this, "HTMLOUT");

        ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(requireContext(), new MyScaleListener());

        View.OnTouchListener touchListener = (v, event) -> {

            if(event.getPointerCount() == 2) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                    case MotionEvent.ACTION_MOVE:
                        // Disallow ScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        scaleGestureDetector.onTouchEvent(event);
                        break;

                    case MotionEvent.ACTION_UP:
                        // Allow ScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }
                return true;
            }
            return false;
        };

        webView.setOnTouchListener(touchListener);
    }

    @Override
    public void afterTextChanged(Editable s) {
        searchButton.setVisibility(TextUtils.isEmpty(s) ? View.GONE : View.VISIBLE);

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // do nothing
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // do nothing
    }


    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

        if(event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
            performSearch();
            return true;
        }


        return false;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if(id == R.id.find_chords_search_button) {
            performSearch();
        } else if(id == R.id.find_chords_message_secondary_view) {
            showConfirmChordChartDialog(webSearchViewModel.analyzeHtml());
        } else if(id == R.id.find_chords_edit_text) {
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            searchEditText.requestFocus();

            // show keyboard

            imm.showSoftInput(searchEditText, 0);
        }
    }

    private void setObserversForLiveData() {

        webSearchViewModel.getUrlMLD().observe(getViewLifecycleOwner(), this::loadUrl);

        webSearchViewModel.getHTMLurlMLD().observe(getViewLifecycleOwner(), this::loadUrl);

        webSearchViewModel.getIsUrlLoadingMLD().observe(getViewLifecycleOwner(), aBoolean -> urlLoading());
    }

    private void prepareQuerySaver() {
        long queryLimit = System.currentTimeMillis() - HISTORY_WINDOW;
        try (ChordReaderDBHelper dbHelper = new ChordReaderDBHelper(requireContext())) {
            List<String> queries = dbHelper.findAllQueries(queryLimit, "");
            queryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, queries);
            searchEditText.setAdapter(queryAdapter);
        }
    }

    private void handleBackButton() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if(webView.copyBackForwardList().getCurrentIndex() > 0) {
                    webView.goBack();
                } else if (getParentFragment() != null) {
                    Navigation.findNavController(getParentFragment().requireView()).popBackStack();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);

    }

    private void setUpInstanceData() {

        String searchText = null;
        // If activity is started from song list
        if (getArguments() != null) {
            searchText = WebSearchFragmentArgs.fromBundle(getArguments()).getSearchText();
        }

        if (!(searchText == null)) {
            searchEditText.setText(searchText);
            performSearch();
        } else {
            requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE); //show keyboard immediately
        }

    }

    public void refreshWebView() {
        webView.reload();
    }

    public void stopWebView() {
        Toast.makeText(getActivity(), getResources().getString(R.string.stopping), Toast.LENGTH_SHORT).show();
        webView.stopLoading();
        progressBar.setVisibility(View.GONE);
        infoIconImageView.setVisibility(View.VISIBLE);
        messageTextView.setText(R.string.find_chords_intro_message);
        messageSecondaryView.setEnabled(false);
    }

    private void loadUrl(String url) {
        Log.d(LOG_TAG,"url is: " + url);

        webView.loadUrl(url);
    }

    @SuppressWarnings("unused")
    @JavascriptInterface
    public void showHTML(String html) {

        Log.d(LOG_TAG,"html is %s..." + (html != null ? (html.substring(0, Math.min(html.length(), 30))) : null));

        webSearchViewModel.setHtml(html);

        handler.post(this::urlAndHtmlLoaded);

    }

    private void urlAndHtmlLoaded() {

        progressBar.setVisibility(View.GONE);
        infoIconImageView.setVisibility(View.VISIBLE);

        webViewFrame.setVisibility(View.VISIBLE);
        webView.setVisibility(View.VISIBLE);

        Log.d(LOG_TAG,"chordWebpage is: " + webSearchViewModel.getChordWebpage());

        if((webSearchViewModel.getChordWebpage() != null && webSearchViewModel.checkHtmlOfKnownWebpage())
                || webSearchViewModel.getChordWebpage() == null && webSearchViewModel.checkHtmlOfUnknownWebpage()) {
            messageTextView.setText(R.string.chords_found);
            messageSecondaryView.setBackgroundResource(R.drawable.focused_shape);
            animationBlink(infoIconImageView);

        } else {
            messageTextView.setText(R.string.find_chords_second_message);
        }
        messageSecondaryView.setEnabled(true);
    }

    public void urlLoading() {
        progressBar.setVisibility(View.VISIBLE);
        infoIconImageView.setVisibility(View.GONE);
        animationBlink(infoIconImageView);
        messageTextView.setText(R.string.loading);
        messageSecondaryView.setEnabled(false);
        messageSecondaryView.setBackgroundResource(android.R.drawable.title_bar);
    }

    public void performSearch() {


        // dismiss soft keyboard
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);

        String searchText = (searchEditText.getText() == null ? "" : searchEditText.getText().toString().trim());

        if(TextUtils.isEmpty(searchText)) {
            return;
        }

        webSearchViewModel.setSearchText(searchText);

        // save the query, add it to the auto suggest text view
        saveQuery(searchText);


        searchText = searchText + " " + getText(R.string.chords_keyword);

        String urlEncoded = null;
        try {
            urlEncoded = URLEncoder.encode(searchText, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(LOG_TAG, e + "this should never happen");
        }

        loadUrl(webSearchViewModel.getSearchEngineURL() + urlEncoded);

    }

    public void saveQuery(String searchText) {

        Log.d(LOG_TAG,"saving: '%s' " + searchText);

        try (ChordReaderDBHelper dbHelper = new ChordReaderDBHelper(requireContext())) {
            boolean newQuerySaved = dbHelper.saveQuery(searchText);

            // don't add duplicates
            if (newQuerySaved) {
                queryAdapter.insert(searchText, 0); // add first so it shows up first
            }
        }

    }

    private void applyPreferences() {

        webSearchViewModel.setSearchEngineURL(PreferenceHelper.getSearchEngineURL(requireContext()));
        webSearchViewModel.setNoteNaming(PreferenceHelper.getNoteNaming(requireContext()));

        ColorScheme colorScheme = PreferenceHelper.getColorScheme(getActivity());

        mainView.setBackgroundColor(colorScheme.getBackgroundColor(getActivity()));
    }


    private void showConfirmChordChartDialog(String chordInputText) {

        LayoutInflater inflater = (LayoutInflater) requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final EditText editText = (EditText) inflater.inflate(R.layout.confirm_chords_edit_text, null);
        editText.setText(chordInputText);
        editText.setTypeface(Typeface.MONOSPACE);
        editText.setBackgroundColor(PreferenceHelper.getColorScheme(requireContext()).getBackgroundColor(requireContext()));
        editText.setTextColor(PreferenceHelper.getColorScheme(requireContext()).getForegroundColor(requireContext()));

        //set AlertDialog theme according app theme
        int alertDialogTheme;
        if(PreferenceHelper.getColorScheme(requireContext()) == ColorScheme.Dark)
            alertDialogTheme = AlertDialog.THEME_DEVICE_DEFAULT_DARK;
        else
            alertDialogTheme = AlertDialog.THEME_DEVICE_DEFAULT_LIGHT;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), alertDialogTheme);
        builder.setTitle(R.string.confirm_chordchart)
                .setInverseBackgroundForced(true)
                .setView(editText)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String chordText = editText.getText().toString();

                    WebSearchFragmentDirections.ActionNavWebSearchToNavSongView action =
                            WebSearchFragmentDirections.actionNavWebSearchToNavSongView(webSearchViewModel.getSearchText(), chordText);
                    action.setBpm(webSearchViewModel.getBPM());
                    if (getParentFragment() != null) {
                        Navigation.findNavController(getParentFragment().requireView()).navigate(action);
                    }
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(alertDialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        alertDialog.getWindow().setAttributes(lp);
    }

    protected void animationBlink(ImageView imageView) {
        Animation animButtonBlink = AnimationUtils.loadAnimation(requireContext(), R.anim.blink_infinite_anim);
        if (imageView.isShown())
            imageView.startAnimation(animButtonBlink);
        else
            imageView.clearAnimation();
    }

    private class MyScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {

            float scaleFactor = scaleGestureDetector.getScaleFactor();
            int textSize = webView.getSettings().getTextZoom();

            webView.getSettings().setTextZoom((int)(textSize * scaleFactor));

            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {

            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
        }
    }
}