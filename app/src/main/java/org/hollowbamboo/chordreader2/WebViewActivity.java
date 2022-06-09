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
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.hollowbamboo.chordreader2.chords.NoteNaming;
import org.hollowbamboo.chordreader2.chords.regex.ChordParser;
import org.hollowbamboo.chordreader2.data.ColorScheme;
import org.hollowbamboo.chordreader2.db.ChordReaderDBHelper;
import org.hollowbamboo.chordreader2.helper.PreferenceHelper;
import org.hollowbamboo.chordreader2.helper.WebPageExtractionHelper;
import org.hollowbamboo.chordreader2.util.UtilLogger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebViewActivity extends DrawerBaseActivity implements TextView.OnEditorActionListener, View.OnClickListener, TextWatcher {

    private static final long HISTORY_WINDOW = TimeUnit.SECONDS.toMillis(60 * 60 * 24 * 360); // about one year
    private static final long PAGE_WAIT_TIME = 3000;

    private String html = null;
    private String url = null;
    private String searchEngineURL = null;
    private volatile String chordText;

    private AutoCompleteTextView searchEditText;

    private View messageSecondaryView;
    private LinearLayout mainView;
    private TextView messageTextView;
    private ProgressBar progressBar;
    private ImageView infoIconImageView;
    private ImageButton searchButton;

    private WebView webView;
    private final CustomWebViewClient client = new CustomWebViewClient();
    private ChordWebpage chordWebpage;

    private ArrayAdapter<String> queryAdapter;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private static final UtilLogger log = new UtilLogger(WebViewActivity.class);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contentView = inflater.inflate(R.layout.activity_web_view, null, false);
        mDrawerLayout.addView(contentView, 0);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mainView = (LinearLayout) findViewById(R.id.find_chords_finding_view);
        searchEditText = (AutoCompleteTextView) findViewById(R.id.find_chords_edit_text);

        webView = (WebView) findViewById(R.id.find_chords_web_view);
        webView.setWebViewClient(client);
        //webView.getSettings().setUserAgentString(DESKTOP_USERAGENT);

        /* JavaScript must be enabled if you want it to work, obviously */
        webView.getSettings().setJavaScriptEnabled(true);

        /* Register a new JavaScript interface called HTMLOUT */
        webView.addJavascriptInterface(this, "HTMLOUT");

        progressBar = (ProgressBar) findViewById(R.id.find_chords_progress_bar);
        infoIconImageView = (ImageView) findViewById(R.id.find_chords_image_view);
        searchButton = (ImageButton) findViewById(R.id.find_chords_search_button);
        searchButton.setOnClickListener(this);
        searchEditText = (AutoCompleteTextView) findViewById(R.id.find_chords_edit_text);
        searchEditText.setOnEditorActionListener(this);
        searchEditText.addTextChangedListener(this);
        searchEditText.setOnClickListener(this);

        messageSecondaryView = findViewById(R.id.find_chords_message_secondary_view);
        messageSecondaryView.setOnClickListener(this);
        messageSecondaryView.setEnabled(false);

        messageTextView = (TextView) findViewById(R.id.find_chords_message_text_view);

        long queryLimit = System.currentTimeMillis() - HISTORY_WINDOW;
        ChordReaderDBHelper dbHelper = null;
        try {
            dbHelper = new ChordReaderDBHelper(this);
            List<String> queries = dbHelper.findAllQueries(queryLimit, "");
            queryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, queries);
            searchEditText.setAdapter(queryAdapter);
        } finally {
            if (dbHelper != null) {
                dbHelper.close();
            }
        }

        setSearchEngineURL(PreferenceHelper.getSearchEngineURL(this));

        applyColorScheme();

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE); //show keyboard immediately
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Update drawer selection
        super.selectItem(1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.web_view_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemId = item.getItemId();
        if (itemId == R.id.menu_stop) {
            stopWebView();
            return true;
        } else if (itemId == R.id.menu_refresh) {
            refreshWebView();
            return true;
        }

        return super.onOptionsItemSelected(item);
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

        if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
            performSearch();
            return true;
        }


        return false;
    }
    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.find_chords_search_button) {
            performSearch();
        } else if (id == R.id.find_chords_message_secondary_view) {
            analyzeHtml();
// TODO: remove evtl.       } else if (id == R.id.find_chords_edit_text) {// I think it's intuitive to select the whole text when you click here
//            if (!TextUtils.isEmpty(searchEditText.getText())) {
//                searchEditText.setSelection(0, searchEditText.getText().length());
//            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (handleBackButton()) {
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_SEARCH && event.getRepeatCount() == 0) {

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            searchEditText.requestFocus();

            // show keyboard

            imm.showSoftInput(searchEditText, 0);

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // came back from the settings activity; need to update the colours
        PreferenceHelper.clearCache();
        setSearchEngineURL(PreferenceHelper.getSearchEngineURL(this));
        applyColorScheme();
    }

    private void startChordViewActivity() {

        String searchText = (searchEditText.getText() == null ? "" : searchEditText.getText().toString().trim());

        Intent intent = new Intent(this, SongViewActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("searchText", searchText.replace(" ","_"));
        bundle.putString("chordText",chordText);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private boolean handleBackButton() {
        if (webView.copyBackForwardList().getCurrentIndex() > 0) {
            webView.goBack();
            return true;
        } else
            finish();
        return false;
    }

    private void applyColorScheme() {

        ColorScheme colorScheme = PreferenceHelper.getColorScheme(this);

        mainView.setBackgroundColor(colorScheme.getBackgroundColor(this));
    }


    private void refreshWebView() {
        webView.reload();
    }

    private void stopWebView() {
        super.showToastShort(getResources().getString(R.string.stopping));
        webView.stopLoading();
        progressBar.setVisibility(View.GONE);
        infoIconImageView.setVisibility(View.VISIBLE);
        messageTextView.setText(R.string.find_chords_intro_message);
        messageSecondaryView.setEnabled(false);
    }


    private void loadUrl(String url) {
        log.d("url is: %s", url);

        webView.loadUrl(url);
    }

    public void getHtmlFromWebView() {
        webView.loadUrl("" +
                "javascript:window.HTMLOUT.showHTML(" +
                "'<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");
        log.d("loadURL per JS");
    }

    @JavascriptInterface
    public void showHTML(String html) {

        log.d("html is %s...", html != null ? (html.substring(0, Math.min(html.length(), 30))) : html);

        this.html = html;

        handler.post(() -> urlAndHtmlLoaded());

    }

    public void setSearchEngineURL(String searchEngineURL) {
        this.searchEngineURL = searchEngineURL;
    }

    public void urlLoading(String url) {
        progressBar.setVisibility(View.VISIBLE);
        infoIconImageView.setVisibility(View.GONE);
        messageTextView.setText(R.string.loading);
        messageSecondaryView.setEnabled(false);
        messageSecondaryView.setBackgroundResource(android.R.drawable.title_bar);
    }

    public void urlLoaded(String url) {

        this.url = url;
        this.chordWebpage = findKnownWebpage(url);

        handler.post(() -> getHtmlFromWebView());

    }

    private void urlAndHtmlLoaded() {

        progressBar.setVisibility(View.GONE);
        infoIconImageView.setVisibility(View.VISIBLE);
        webView.setVisibility(View.VISIBLE);

        log.d("chordWebpage is: %s", chordWebpage);

        if ((chordWebpage != null && checkHtmlOfKnownWebpage())
                || chordWebpage == null && checkHtmlOfUnknownWebpage()) {
            messageTextView.setText(R.string.chords_found);
            messageSecondaryView.setBackgroundResource(R.drawable.focused_shape);

        } else {
            messageTextView.setText(R.string.find_chords_second_message);
        }
        messageSecondaryView.setEnabled(true);
    }

    private NoteNaming getNoteNaming() {
        return PreferenceHelper.getNoteNaming(this);
    }

    private void analyzeHtml() {
//removed section for known webpage as html structure may change -> chordie pattern doesn't work anymore
/*		if (chordWebpage != null) {
			// known webpage

			log.d("known web page: %s", chordWebpage);

			chordText = WebPageExtractionHelper.extractChordChart(
					chordWebpage, html, getNoteNaming());
		} else {

 */
        // unknown webpage

        log.d("unknown webpage");

        chordText = WebPageExtractionHelper.extractLikelyChordChart(html, getNoteNaming());


        if (chordText == null) { // didn't find a good extraction, so use the entire html

            log.d("didn't find a good chord chart using the <pre> tag");

            chordText = WebPageExtractionHelper.convertHtmlToText(html);
        }
//		}

        showConfirmChordChartDialog();

    }

    private void showConfirmChordChartDialog() {

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final EditText editText = (EditText) inflater.inflate(R.layout.confirm_chords_edit_text, null);
        editText.setText(chordText);
        editText.setTypeface(Typeface.MONOSPACE);
        editText.setBackgroundColor(PreferenceHelper.getColorScheme(this).getBackgroundColor(this));
        editText.setTextColor(PreferenceHelper.getColorScheme(this).getForegroundColor(this));

        //set AlertDialog theme according app theme
        int alertDialogTheme;
        if (PreferenceHelper.getColorScheme(this) == ColorScheme.Dark)
            alertDialogTheme = AlertDialog.THEME_DEVICE_DEFAULT_DARK;
        else
            alertDialogTheme = AlertDialog.THEME_DEVICE_DEFAULT_LIGHT;

        AlertDialog.Builder builder = new AlertDialog.Builder(this, alertDialogTheme);
        builder.setTitle(R.string.confirm_chordchart)
                .setInverseBackgroundForced(true)
                .setView(editText)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    chordText = editText.getText().toString();
                    startChordViewActivity();
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(alertDialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        alertDialog.getWindow().setAttributes(lp);

        //log.d(chordText);

    }

    private boolean checkHtmlOfUnknownWebpage() {

        Pattern searchEngine = Pattern.compile(".*://(.*\\.\\w{2,3})/.*");
        Matcher matcher = searchEngine.matcher(url);

        if (matcher.find()) {
            String match = matcher.group(1);
            if (searchEngineURL.contains(match))
            return false; // skip page - we're on the search results page
        }

        String txt = WebPageExtractionHelper.convertHtmlToText(html);
        return ChordParser.containsLineWithChords(txt, getNoteNaming());

    }

    private boolean checkHtmlOfKnownWebpage() {

        // check to make sure that, if this is a page from a known website, we can
        // be sure that there are chords on this page

        String chordChart = WebPageExtractionHelper.extractChordChart(
                chordWebpage, html, getNoteNaming());

        log.d("chordChart is %s...", chordChart != null ? (chordChart.substring(0, Math.min(chordChart.length(), 30))) : chordChart);

        boolean result = ChordParser.containsLineWithChords(chordChart, getNoteNaming());

        log.d("checkHtmlOfKnownWebpage is: %s", result);

        return result;

    }

    private ChordWebpage findKnownWebpage(String url) {

        if (url.contains("chordie.com")) {
            return ChordWebpage.Chordie;
        }

        return null;
    }

    private void performSearch() {

        // dismiss soft keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);

        String searchText = (searchEditText.getText() == null ? "" : searchEditText.getText().toString().trim());

        if (TextUtils.isEmpty(searchText)) {
            return;
        }

        // save the query, add it to the auto suggest text view
        saveQuery(searchText);


        searchText = searchText + " " + getText(R.string.chords_keyword);

        String urlEncoded = null;
        try {
            urlEncoded = URLEncoder.encode(searchText, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.e(e, "this should never happen");
        }

        loadUrl(searchEngineURL + urlEncoded);

    }

    private void saveQuery(String searchText) {

        log.d("saving: '%s'", searchText);

        ChordReaderDBHelper dbHelper = null;

        try {
            dbHelper = new ChordReaderDBHelper(this);
            boolean newQuerySaved = dbHelper.saveQuery(searchText);

            // don't add duplicates
            if (newQuerySaved) {
                queryAdapter.insert(searchText, 0); // add first so it shows up first
            }
        } finally {
            if (dbHelper != null) {
                dbHelper.close();
            }
        }

    }


    private class CustomWebViewClient extends WebViewClient {

        private final AtomicInteger taskCounter = new AtomicInteger(0);

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, final String url) {
            if (Uri.parse(url).getScheme().equals("market") || url.contains("play.google.com"))  {
                log.d("Playstore request blocked: " + url);
            } else
                handler.post(() -> loadUrl(url));

            return true;
        }

        @Override
        public void onPageFinished(WebView view, final String url) {
            super.onPageFinished(view, url);
            log.d("onPageFinished()　" + url);

            if (url.contains(searchEngineURL)) {
                // trust google to only load once
                urlLoaded(url);
            } else { // don't trust other websites

                // have to do this song and dance because sometimes the pages
                // have a bazillion redirects, so I get multiple onPageFinished()
                // before the damn thing is really done, and then my users get confused
                // because the button says "page finished" before it really is
                // so we just wait a couple seconds for the dust to settle and make
                // sure that the web view is REALLY done loading
                // TODO: find a better way to do this

                HandlerThread handlerThread = new HandlerThread("OnPageFinishedHandlerThread");
                handlerThread.start();

                Handler asyncHandler = new Handler(handlerThread.getLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        int id = (int) msg.obj;

                        if (id == taskCounter.get()) {
                            urlLoaded(url);
                        }
                        handlerThread.quit();
                    }
                };

                Runnable runnable = () -> {
                    // your async code goes here.
                    try {
                        Thread.sleep(PAGE_WAIT_TIME);
                    } catch (InterruptedException ignored) {
                    }

                    Message message = new Message();
                    message.obj = taskCounter.incrementAndGet();;

                    asyncHandler.sendMessage(message);
                };

                asyncHandler.post(runnable);
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            log.d("onPageStarted()");
            taskCounter.incrementAndGet();
            urlLoading(url);
        }
    }
}
