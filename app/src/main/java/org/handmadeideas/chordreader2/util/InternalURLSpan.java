package org.handmadeideas.chordreader2.util;

import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.View.OnClickListener;

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

