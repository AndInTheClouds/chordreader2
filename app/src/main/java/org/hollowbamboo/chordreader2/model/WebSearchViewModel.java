package org.hollowbamboo.chordreader2.model;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.hollowbamboo.chordreader2.ChordWebpage;
import org.hollowbamboo.chordreader2.chords.NoteNaming;
import org.hollowbamboo.chordreader2.chords.regex.ChordParser;
import org.hollowbamboo.chordreader2.helper.WebPageExtractionHelper;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebSearchViewModel extends ViewModel {
    // TODO: Implement the

    private static final String LOG_TAG = "WebSearchViewModel";
    private static final long PAGE_WAIT_TIME = 3000;

    private ChordWebpage chordWebpage;
    private String url = null;

    private String searchEngineURL = null;
    private String searchText;

    private volatile String chordText;
    private String html = null;
    boolean isUrlLoading;
    private NoteNaming noteNaming;

    private final CustomWebViewClient client;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<String> urlMLD = new MutableLiveData<>();
    private final MutableLiveData<String> htmlUrlMLD = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isUrlLoadingMLD = new MutableLiveData<>();
    private final MutableLiveData<String> showConfirmChordChartDialogMLD = new MutableLiveData<>();

    public WebSearchViewModel() {
        client = new CustomWebViewClient();
    }

    public MutableLiveData<String> getHTMLurlMLD() {
        return htmlUrlMLD;
    }

    public MutableLiveData<String> getUrlMLD() { return urlMLD; }

    public MutableLiveData<Boolean> getIsUrlLoadingMLD() {return isUrlLoadingMLD;}

    public MutableLiveData<String > getShowConfirmChordChartDialogMLD() {
        return showConfirmChordChartDialogMLD;
    }

    public CustomWebViewClient getClient() {
        return client;
    }

    public ChordWebpage getChordWebpage() {
        return chordWebpage;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public void setSearchEngineURL(String searchEngineURL) {
        this.searchEngineURL = searchEngineURL;
    }

    public String getSearchEngineURL() {
        return searchEngineURL;
    }

    public String getChordText() { return chordText; }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public String getSearchText() { return searchText; }

    public void setNoteNaming(NoteNaming noteNaming) {
        this.noteNaming = noteNaming;
    }

    private void loadUrl(String url) {
        Log.d("url is: %s", url);

        urlMLD.setValue(url);
    }

    private void getHtmlFromWebView() {
        htmlUrlMLD.setValue("" +
                "javascript:window.HTMLOUT.showHTML(" +
                "'<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");

        Log.d(LOG_TAG,"loadURL per JS: " + htmlUrlMLD.getValue());
    }

    public void urlLoaded(String url) {

        this.url = url;
        this.chordWebpage = findKnownWebpage(url);

        handler.post(() -> getHtmlFromWebView());

    }

    public void analyzeHtml() {
//removed section for known webpage as html structure may change -> chordie pattern doesn't work anymore
/*		if(chordWebpage != null) {
			// known webpage

			log.d("known web page: %s", chordWebpage);

			chordText = WebPageExtractionHelper.extractChordChart(
					chordWebpage, html, getNoteNaming());
		} else {

 */
        // unknown webpage

        Log.d(LOG_TAG,"unknown webpage");

        chordText = WebPageExtractionHelper.extractLikelyChordChart(html, noteNaming);


        if(chordText == null) { // didn't find a good extraction, so use the entire html

            Log.d(LOG_TAG,"didn't find a good chord chart using the <pre> tag");

            chordText = WebPageExtractionHelper.convertHtmlToText(html);
        }
//		}

        showConfirmChordChartDialogMLD.setValue(chordText);
    }

    public boolean checkHtmlOfUnknownWebpage() {

        Pattern searchEngine = Pattern.compile(".*://(.*\\.\\w{2,3})/.*");
        Matcher matcher = searchEngine.matcher(url);

        if(matcher.find()) {
            String match = matcher.group(1);
            if(searchEngineURL.contains(match))
                return false; // skip page - we're on the search results page
        }

        String txt = WebPageExtractionHelper.convertHtmlToText(html);
        return ChordParser.containsLineWithChords(txt, noteNaming);

    }

    public boolean checkHtmlOfKnownWebpage() {

        // check to make sure that, if this is a page from a known website, we can
        // be sure that there are chords on this page

        String chordChart = WebPageExtractionHelper.extractChordChart(
                chordWebpage, html, noteNaming);

        Log.d("chordChart is %s...", chordChart != null ? (chordChart.substring(0, Math.min(chordChart.length(), 30))) : chordChart);

        boolean result = ChordParser.containsLineWithChords(chordChart, noteNaming);

        Log.d(LOG_TAG,"checkHtmlOfKnownWebpage is: %s" + result);

        return result;

    }

    private ChordWebpage findKnownWebpage(String url) {

        if(url.contains("chordie.com")) {
            return ChordWebpage.Chordie;
        }

        return null;
    }

    private class CustomWebViewClient extends WebViewClient {

        private final AtomicInteger taskCounter = new AtomicInteger(0);

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, final String url) {
            if(Uri.parse(url).getScheme().equals("market") || url.contains("play.google.com"))  {
                Log.d("WebView", "Playstore request blocked: " + url);
            } else
                handler.post(() -> loadUrl(url));

            return true;
        }

        @Override
        public void onPageFinished(WebView view, final String url) {
            super.onPageFinished(view, url);
            Log.d(LOG_TAG,"onPageFinished()　" + url);

            if(url.contains(searchEngineURL)) {
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

                        if(id == taskCounter.get()) {
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
            Log.d(LOG_TAG,"onPageStarted()");
            taskCounter.incrementAndGet();
            isUrlLoadingMLD.setValue(isUrlLoading = true);
        }
    }
}