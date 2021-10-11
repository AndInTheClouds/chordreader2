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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;

import org.handmadeideas.chordreader2.helper.PackageHelper;
import org.handmadeideas.chordreader2.util.UtilLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

public class AboutActivity extends Activity implements OnClickListener {
	
	private static final UtilLogger log = new UtilLogger(AboutActivity.class);
	
	private Button okButton;
	private WebView aboutWebView;
	private ProgressBar progressBar;
	private final Handler handler = new Handler();
	private View topPanel;
	
	
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
	
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.about);

		topPanel = findViewById(R.id.topPanel);
		topPanel.setVisibility(View.GONE);
		okButton = findViewById(R.id.okButton);
		okButton.setOnClickListener(this);
		okButton.setVisibility(View.GONE);
				
		aboutWebView = findViewById(R.id.aboutTextWebView);

		aboutWebView.setVisibility(View.GONE);
		
		progressBar = findViewById(R.id.aboutProgressBar);

		aboutWebView.setBackgroundColor(0);
		
		aboutWebView.setWebViewClient(new AboutWebClient());
		
		initializeWebView();				
	}
	
	
	public void initializeWebView() {
		String newhtml_code = Base64.encodeToString(loadTextFile().getBytes(), Base64.NO_PADDING);
		aboutWebView.loadData(newhtml_code,"text/html", "base64");
	}




	private String loadTextFile() {

		InputStream inputStream;
		Locale locale = Locale.getDefault();

		if (locale.equals(Locale.GERMAN))
			inputStream = getResources().openRawResource(R.raw.about_body_de);
		else if (locale.equals(Locale.FRENCH))
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
		return String.format(result, PackageHelper.getVersionName(this));
	}
	
	private void loadExternalUrl(String url) {
		Intent intent = new Intent();
		intent.setAction("android.intent.action.VIEW"); 
		intent.setData(Uri.parse(url));
		
		startActivity(intent);
	}

	@Override
	public void onClick(View v) {
		finish();
	}
	
	private class AboutWebClient extends WebViewClient {

		

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, final String url) {
			log.d("shouldOverrideUrlLoading");
			
			// XXX hack to make the webview go to an external url if the hyperlink is 
			// in my own HTML file - otherwise it says "Page not available" because I'm not calling
			// loadDataWithBaseURL.  But if I call loadDataWithBaseUrl using a fake URL, then
			// the links within the page itself don't work!!  Arggggh!!!
			
			if (url.startsWith("http") || url.startsWith("mailto") || url.startsWith("market")) {
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
