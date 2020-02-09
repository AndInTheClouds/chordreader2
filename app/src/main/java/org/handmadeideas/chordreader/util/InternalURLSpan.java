package org.handmadeideas.chordreader.util;

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
}

