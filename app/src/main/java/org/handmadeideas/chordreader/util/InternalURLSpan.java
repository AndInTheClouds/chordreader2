package org.handmadeideas.chordreader.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.View.OnClickListener;

import org.handmadeideas.chordreader.data.ColorScheme;
import org.handmadeideas.chordreader.helper.PreferenceHelper;

public class InternalURLSpan extends ClickableSpan {
	OnClickListener mListener;

	public InternalURLSpan(OnClickListener listener) {
		mListener = listener;
	}

	@Override
	public void onClick(View widget) {
		mListener.onClick(widget);
	}

	@Override
	public void updateDrawState(TextPaint ds) {
		this.updateDrawState(ds);
	}

}

