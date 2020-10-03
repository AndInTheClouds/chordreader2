package org.handmadeideas.chordreader.data;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import org.handmadeideas.chordreader.R;


public enum ColorScheme {
	
	Dark (R.string.pref_scheme_dark, R.color.scheme_dark_background, 
			R.color.scheme_dark_foreground, R.color.scheme_dark_bubble, R.color.scheme_dark_link,
			R.drawable.app_selector),
	Light (R.string.pref_scheme_light, R.color.scheme_light_background, 
			R.color.scheme_light_foreground, R.color.scheme_light_bubble, R.color.scheme_light_link,
			R.drawable.app_selector_light),
	Android (R.string.pref_scheme_android, R.color.scheme_android_background, 
			R.color.scheme_android_foreground, R.color.scheme_android_bubble, R.color.scheme_android_link,
			R.drawable.app_selector_android),
	;
	
	private int nameResource;
	private int backgroundColorResource;
	private int foregroundColorResource;
	private int bubbleColorResource;
	private int linkColorResource;
	private int selectorResource;
	
	private int backgroundColor = -1;
	private int foregroundColor = -1;
	private int bubbleColor = -1;
	private int linkColor = -1;
	
	private static Map<String, ColorScheme> preferenceNameToColorScheme = new HashMap<String, ColorScheme>();
	
	private ColorScheme(int nameResource, int backgroundColorResource, int foregroundColorResource,
			int bubbleColorResource, int linkColorResource, int selectorResource) {
		this.nameResource = nameResource;
		this.backgroundColorResource = backgroundColorResource;
		this.foregroundColorResource = foregroundColorResource;
		this.bubbleColorResource = bubbleColorResource;
		this.linkColorResource = linkColorResource;
		this.selectorResource = selectorResource;

	}

	public int getNameResource() {
		return nameResource;
	}	
	
	public int getBackgroundColor(Context context) {
		if (backgroundColor == -1) {
			backgroundColor = context.getResources().getColor(backgroundColorResource);
		}
		return backgroundColor;
	}
	
	public int getForegroundColor(Context context) {
		if (foregroundColor == -1) {
			foregroundColor = context.getResources().getColor(foregroundColorResource);
		}
		return foregroundColor;
	}
	
	
	public int getLinkColor(Context context) {
		if (linkColor == -1) {
			linkColor = context.getResources().getColor(linkColorResource);
		}
		return linkColor;
	}
	
	public int getBubbleColor(Context context) {
		if (bubbleColor == -1) {
			bubbleColor = context.getResources().getColor(bubbleColorResource);
		}
		return bubbleColor;
	}
	
	public int getSelectorResource() {
		return selectorResource;
	}

	public static ColorScheme findByPreferenceName(String name, Context context) {
		if (preferenceNameToColorScheme.isEmpty()) {
			// initialize map
			for (ColorScheme colorScheme : values()) {
				preferenceNameToColorScheme.put(context.getText(colorScheme.getNameResource()).toString(), colorScheme);
			}
		}
		return preferenceNameToColorScheme.get(name);
	}
}
