package org.hollowbamboo.chordreader2.helper;

import static java.lang.Integer.max;
import static java.lang.Integer.min;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import org.hollowbamboo.chordreader2.R;
import org.hollowbamboo.chordreader2.chords.NoteNaming;
import org.hollowbamboo.chordreader2.data.ColorScheme;

import java.util.Objects;

public class DialogHelper {
	
	public static final int CAPO_MIN = 0;
	public static final int CAPO_MAX = 6;
	public static final int TRANSPOSE_MIN = -6;
	public static final int TRANSPOSE_MAX = 6;

	public static View createTransposeDialogView(Context context, int capoFret, int transposeHalfSteps, NoteNaming noteNaming) {
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.transpose_dialog, null);

		setUpNoteNamingSpinner(context, view.findViewById(R.id.transpose_note_naming_spinner), noteNaming);

		View transposeView = view.findViewById(R.id.transpose_include);
		View capoView = view.findViewById(R.id.capo_include);

		setUpEnhancedSeekBar(transposeView, TRANSPOSE_MIN, TRANSPOSE_MAX, transposeHalfSteps);
		setUpEnhancedSeekBar(capoView, CAPO_MIN, CAPO_MAX, capoFret);

		return view;
	}

	public static View createConfirmChordsDialogView(Context context, String chordInputText, NoteNaming noteNaming) {

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.confirm_chords_dialog, null);

		int bgColor = PreferenceHelper.getColorScheme(context).getBackgroundColor(context);
		int textColor = PreferenceHelper.getColorScheme(context).getForegroundColor(context);

		final EditText editText = (EditText) view.findViewById(R.id.conf_chord_edit_text);
		editText.setText(chordInputText);
		editText.setTypeface(Typeface.MONOSPACE);
		editText.setBackgroundColor(bgColor);
		editText.setTextColor(textColor);

		Spinner spinner = view.findViewById(R.id.transpose_note_naming_spinner_conf_chords);

		setUpNoteNamingSpinner(context, spinner, noteNaming);

		return view;
	}

	private static void setUpNoteNamingSpinner (Context context, Spinner spinner, NoteNaming noteNaming) {
		ArrayAdapter<CharSequence> spinnerArrayAdapter = ArrayAdapter.createFromResource(context, R.array.note_namings, R.layout.spinner_chord_edit);
		spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(spinnerArrayAdapter);

		// set note naming
		int count = spinner.getAdapter().getCount();

		for (int i = 0; i < count; i++) {
			String nn = (String) spinner.getAdapter().getItem(i);
			if (Objects.equals(nn, context.getResources().getString(noteNaming.getPrintableNameResource()))) {
				spinner.setSelection(i);
			}
		}
	}

	public static int getSpinnerIndex(Spinner spinner) {
		return spinner.getSelectedItemPosition();
	}

	public static int getSeekBarValue(View enhancedSeekBarView) {

		SeekBar seekBar = (SeekBar) enhancedSeekBarView.findViewById(R.id.seek_bar_main);

		return seekBar.getProgress();
	}

	private static void setUpEnhancedSeekBar(View view, final int min, int max, int defaultValue) {
		SeekBar seekBar = (SeekBar) view.findViewById(R.id.seek_bar_main);
		TextView minTextView = (TextView) view.findViewById(R.id.seek_bar_min_text_view);
		TextView maxTextView = (TextView) view.findViewById(R.id.seek_bar_max_text_view);

		setupOnTouchListenerForTextViewToChangeSeekBarProgress(minTextView, seekBar, -1, min, max);
		setupOnTouchListenerForTextViewToChangeSeekBarProgress(maxTextView, seekBar, +1, min, max);

		final TextView progressTextView = (TextView) view.findViewById(R.id.seek_bar_main_text_view);

		minTextView.setText(Integer.toString(min));
		// check if we need to distinguish between negative and positive
		maxTextView.setText((min < 0 && max > 0 ? "+" : "") + max);
		progressTextView.setText((min < 0 && defaultValue > 0 ? "+" : "") + defaultValue);
		seekBar.setMax(max - min);
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// do nothing

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// do nothing

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				String progressAsString = Integer.toString(progress + min);
				if (min < 0 && (progress + min) > 0) {
					// need to distinguish positive from negative
					progressAsString = "+" + progressAsString;
				}
				progressTextView.setText(progressAsString);

			}
		});
		seekBar.setProgress(defaultValue - min); // initialize to default value

	}

	private static void setupOnTouchListenerForTextViewToChangeSeekBarProgress(
			final TextView textView,
			SeekBar seekBar,
			final int change,
			final int minimumValue,
			final int maximumValue
	) {
		textView.setOnTouchListener((view, motionEvent) -> {
			if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
				updateSeekBarProgress(seekBar, change, minimumValue, maximumValue);
			}
			return false;
		});
	}

	private static void updateSeekBarProgress(
			SeekBar seekBar,
			final int change,
			final int minimumValue,
			final int maximumValue
	) {
		int currentValue = seekBar.getProgress() + minimumValue;

		// Make sure the new value does not exceed the bounds
		int newValue = min(maximumValue, max(minimumValue, currentValue + change));

		seekBar.setProgress(newValue - minimumValue);
	}
	
}
