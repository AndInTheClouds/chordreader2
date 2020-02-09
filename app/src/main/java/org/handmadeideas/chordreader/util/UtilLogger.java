package org.handmadeideas.chordreader.util;

import android.util.Log;

import java.util.Arrays;

/**
 * Easier way to interact with logcat.
 * @author nolan
 */
public class UtilLogger {

	public static final boolean DEBUG_MODE = false;
	
	private String tag;
	
	public UtilLogger(String tag) {
		this.tag = tag;
	}
	
	public UtilLogger(Class<?> clazz) {
		this.tag = clazz.getSimpleName();
	}
	
	public void i(String format, Object... more) {
		if (DEBUG_MODE) {
			Log.i(tag, smartFormat(format,more));
		}
	}
	
	public void i(Exception e, String format, Object... more) {
		if (DEBUG_MODE) {
			Log.i(tag, smartFormat(format,more), e);
		}
	}
	
	public void w(Exception e, String format, Object... more) {
		if (DEBUG_MODE) {
			Log.w(tag, smartFormat(format,more), e);
		}
	}
	
	public void w(String format, Object... more) {
		if (DEBUG_MODE) {
			Log.w(tag, smartFormat(format,more));
		}
	}	
	
	public void e(String format, Object... more) {
		Log.e(tag, smartFormat(format,more));
	}	
	
	public void e(Exception e, String format, Object... more) {
		Log.e(tag, smartFormat(format,more), e);
	}
	
	public void d(String format, Object... more) {	
		if (DEBUG_MODE) {
			for (int i = 0; i < more.length; i++) {
				if (more[i] instanceof int[]) {
					more[i] = Arrays.toString((int[])more[i]);
				} else if (more[i] instanceof String[]) {
					more[i] = Arrays.toString((String[])more[i]);
				}
			}
			Log.d(tag, smartFormat(format,more));
		}
	}	
	
	public void d(Exception e, String format, Object... more) {
		if (DEBUG_MODE) {
			for (int i = 0; i < more.length; i++) {
				if (more[i] instanceof int[]) {
					more[i] = Arrays.toString((int[])more[i]);
				} else if (more[i] instanceof String[]) {
					more[i] = Arrays.toString((String[])more[i]);
				}
			}
			Log.d(tag, smartFormat(format,more), e);
		}
	}	
	
	private static String smartFormat(String format, Object... more) {
		return more.length > 0 ? String.format(format, more) : format;
	}
}
