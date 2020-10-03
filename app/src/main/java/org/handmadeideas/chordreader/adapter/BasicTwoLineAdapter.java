package org.handmadeideas.chordreader.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;
import org.handmadeideas.chordreader.R;

import java.util.List;

/**
 * Simple adapter that displays two lines for each item
 * @author nolan
 *
 */
public class BasicTwoLineAdapter extends ArrayAdapter<String> {

	private static final int RES_ID = R.layout.simple_dropdown_item_2line;
	
	private List<String> firstLines;
	private List<String> secondLines;
	private int checked;
	
	
	public BasicTwoLineAdapter(Context context, List<String> firstLines, List<String> secondLines, int checked) {
		super(context, RES_ID, firstLines);
		
		this.firstLines = firstLines;
		this.secondLines = secondLines;
		this.checked = checked;
		
	}
	
	@Override
	public View getView(int position, View view, ViewGroup parent) {
		
		Context context = parent.getContext();
		
		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(RES_ID, parent, false);
		}
		
		CheckedTextView text1 = (CheckedTextView) view.findViewById(android.R.id.text1);
		TextView text2 = (TextView) view.findViewById(android.R.id.text2);
		
		text1.setText(firstLines.get(position));
		text2.setText(secondLines.get(position));
		
		text1.setChecked(position == checked);
		
		return view;
	}
	

}
