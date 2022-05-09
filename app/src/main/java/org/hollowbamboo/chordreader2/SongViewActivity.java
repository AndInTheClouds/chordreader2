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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.InputType;
import android.text.Layout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.core.view.GravityCompat;

import org.hollowbamboo.chordreader2.chords.Chord;
import org.hollowbamboo.chordreader2.chords.NoteNaming;
import org.hollowbamboo.chordreader2.chords.regex.ChordInText;
import org.hollowbamboo.chordreader2.chords.regex.ChordParser;
import org.hollowbamboo.chordreader2.data.ColorScheme;
import org.hollowbamboo.chordreader2.db.ChordReaderDBHelper;
import org.hollowbamboo.chordreader2.db.Transposition;
import org.hollowbamboo.chordreader2.helper.AutoScrollView;
import org.hollowbamboo.chordreader2.helper.ChordDictionary;
import org.hollowbamboo.chordreader2.helper.DialogHelper;
import org.hollowbamboo.chordreader2.helper.PopupHelper;
import org.hollowbamboo.chordreader2.helper.PreferenceHelper;
import org.hollowbamboo.chordreader2.helper.SaveFileHelper;
import org.hollowbamboo.chordreader2.helper.TransposeHelper;
import org.hollowbamboo.chordreader2.util.InternalURLSpan;
import org.hollowbamboo.chordreader2.util.Pair;
import org.hollowbamboo.chordreader2.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SongViewActivity extends DrawerBaseActivity implements OnClickListener {

    private static final int CHORD_POPUP_Y_OFFSET_IN_SP = 24;
    private static final int PROGRESS_DIALOG_MIN_TIME = 600;
    private static final String LOG_TAG = "SongViewActivity";


    private static float lastXCoordinate, lastYCoordinate;

    private String filename, searchText;
    private volatile String chordText;
    private List<ChordInText> chordsInText;
    public int capoFret = 0;
    protected int transposeHalfSteps = 0;

    private boolean doubleTapExcecuted = false;
    private boolean isEditedTextToSave = false;

    private LinearLayout chordsViewingMainView;
    private TextView viewingTextView;
    private AutoScrollView viewingScrollView;

    private ImageButton autoScrollPlayButton, autoScrollPauseButton, autoScrollSlowerButton, autoScrollFasterButton;

    private GestureDetector mDetector;
    private final Handler metronomeHandler = new Handler();
    private Timer metronomeTimer;
    private CountDownTimer releaseWakeLockCountDownTimer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contentView = inflater.inflate(R.layout.activity_song_view, null, false);
        mDrawerLayout.addView(contentView, 0);

        setUpWidgets();

        viewingScrollView.setOnActiveAutoScrollListener(new AutoScrollView.OnActiveAutoScrollListener() {

            @Override
            public void onAutoScrollActive() {
                acquireWakeLock();
            }

            @Override
            public void onAutoScrollInactive() {
                releaseWakeLock();
            }
        });


        PreferenceHelper.clearCache();
        viewingTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, PreferenceHelper.getTextSizePreference(this));

        setInstanceData();

        applyColorScheme();
    }

    private void setInstanceData() {
        //get data
        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            chordText = bundle.getString("chordText");
            filename = bundle.getString("filename");
            searchText = bundle.getString("searchText");
        }

        if (chordText != null) { //coming from web search
            setTitle(searchText);
            isEditedTextToSave = true;
        } else if (filename != null) { //open chord file
            openFile(filename);
            setTitle(filename);
        } else {
            setTitle(R.string.new_file);
        }
    }

    private void setTitle (String title) {
        super.setTitle(title.replace("_"," ").replace(".txt",""));
    }

    @Override
    protected void onResume() {
        super.onResume();

        // just in case the text size has changed
        viewingTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, PreferenceHelper.getTextSizePreference(this));

        resetDataExceptChordTextAndFilename();
        analyzeChordsEtcAndShowChordView();

    }

    @Override
    protected void onPause() {
        super.onPause();

        releaseWakeLock();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.song_view_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemId = item.getItemId();
        if (itemId == R.id.menu_save_chords) {
            showSaveChordChartDialog("");
            return true;
        } else if (itemId == R.id.menu_edit_file) {
            showConfirmChordChartDialog();
            return true;
        } else if (itemId == R.id.menu_transpose) {
            createTransposeDialog();
            return true;
        } else if (itemId == android.R.id.home && isEditedTextToSave) {
            showSavePromptDialog("onHomePressed");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // came back from the settings activity; need to update the text size
        PreferenceHelper.clearCache();
        viewingTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, PreferenceHelper.getTextSizePreference(this));

        // reapply color scheme
        applyColorScheme();

        // if the note naming changed, then update the currently displayed file
        if (data != null
                && data.hasExtra(SettingsActivity.EXTRA_NOTE_NAMING_CHANGED)
                && data.getBooleanExtra(SettingsActivity.EXTRA_NOTE_NAMING_CHANGED, false)) {

            setInstanceData();
        }
    }

    private void setUpWidgets() {

        viewingTextView = (TextView) findViewById(R.id.chords_viewing_text_view);
        viewingTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, PreferenceHelper.getTextSizePreference(this));
        viewingScrollView = (AutoScrollView) findViewById(R.id.chords_viewing_scroll_view);
        viewingScrollView.setTargetTextView(viewingTextView);

        chordsViewingMainView = (LinearLayout) findViewById(R.id.chords_viewing_layout);

        autoScrollPlayButton = (ImageButton) findViewById(R.id.autoScrollPlayButton);
        autoScrollPlayButton.setOnClickListener(this);
        autoScrollPauseButton = (ImageButton) findViewById(R.id.autoScrollPauseButton);
        autoScrollPauseButton.setOnClickListener(this);
        autoScrollSlowerButton = (ImageButton) findViewById(R.id.autoScrollSlower);
        autoScrollSlowerButton.setOnClickListener(this);
        autoScrollFasterButton = (ImageButton) findViewById(R.id.autoScrollFaster);
        autoScrollFasterButton.setOnClickListener(this);

        mDetector = new GestureDetector(this, new MyGestureListener());

        OnTouchListener touchListener = (v, event) -> {

            lastXCoordinate = event.getRawX();
            lastYCoordinate = event.getRawY();

            final int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {

                doubleTapExcecuted = false;

                if (viewingScrollView.isAutoScrollOn()) {
                    viewingScrollView.stopAutoScroll();
                }
            }

            if (action == MotionEvent.ACTION_UP) {
                viewingScrollView.setTouched(false);
                if (viewingScrollView.isAutoScrollOn() && !doubleTapExcecuted) {
                    startAutoscroll();
                }
            }
            return mDetector.onTouchEvent(event);
        };

        viewingTextView.setOnTouchListener(touchListener);
    }

    @Override
    public void onBackPressed() {
        if (isEditedTextToSave) {
            showSavePromptDialog("onBackPressed");
        } else
            finish();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.autoScrollPlayButton) {
            startAutoscroll();
        } else if (id == R.id.autoScrollPauseButton) {
            stopAutoscroll();
        } else if (id == R.id.autoScrollSlower) {
            animationBlink(autoScrollSlowerButton);
            changeAutoScrollFactor(false);
        } else if (id == R.id.autoScrollFaster) {
            animationBlink(autoScrollFasterButton);
            changeAutoScrollFactor(true);
            //TODO: Add playlist buttons
        }

    }

    private void acquireWakeLock() {

        //Log.d(LOG_TAG,"Acquiring wakelock");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (releaseWakeLockCountDownTimer != null) {
            releaseWakeLockCountDownTimer.cancel();
        }

    }

    private void releaseWakeLock() {

        //Log.d(LOG_TAG,"Releasing wakelock in 3min");

        releaseWakeLockCountDownTimer = new CountDownTimer(180000, 1000) {

            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                //Log.d(LOG_TAG,"Wakelock released");
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }.start();


    }

    private NoteNaming getNoteNaming() {
        return PreferenceHelper.getNoteNaming(this);
    }

    private void showSavePromptDialog(final String callingMethod) {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.unsaved_changes)
                .setMessage(R.string.unsaved_changes_message)
                .setNegativeButton(R.string.no, (dialog, which) -> {
                    isEditedTextToSave = false;
                    proceedAfterSaving(callingMethod);
                })
                .setPositiveButton(R.string.yes, (dialog, which) -> showSaveChordChartDialog(callingMethod))
                .show();
    }

    private void createTransposeDialog() {

        final View view = DialogHelper.createTransposeDialogView(this, capoFret, transposeHalfSteps);
        new AlertDialog.Builder(this)
                .setTitle(R.string.transpose)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {

                    // grab the user's chosen values for the capo and the transposition

                    View transposeView = view.findViewById(R.id.transpose_include);
                    View capoView = view.findViewById(R.id.capo_include);

                    int newTransposeHalfSteps = DialogHelper.getSeekBarValue(transposeView) + DialogHelper.TRANSPOSE_MIN;
                    int newCapoFret = DialogHelper.getSeekBarValue(capoView) + DialogHelper.CAPO_MIN;

                    Log.d(LOG_TAG,"transposeHalfSteps is now " + newTransposeHalfSteps);
                    Log.d(LOG_TAG,"capoFret is now " + newCapoFret);

                    changeTransposeOrCapo(newTransposeHalfSteps, newCapoFret);

                    dialog.dismiss();

                })
                .setView(view)
                .show();

    }

    protected void changeTransposeOrCapo(final int newTransposeHalfSteps, final int newCapoFret) {

        // persist
        if (filename != null) {
            ChordReaderDBHelper dbHelper = new ChordReaderDBHelper(this);
            dbHelper.saveTransposition(filename, newTransposeHalfSteps, newCapoFret);
            dbHelper.close();
        }

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(R.string.transposing);
        progressDialog.setMessage(getText(R.string.please_wait));
        progressDialog.setIndeterminate(true);

        // transpose in background to avoid jankiness
        progressDialog.show();

        HandlerThread handlerThread = new HandlerThread("ChangeTransposeOrCapoHandlerThread");
        handlerThread.start();

        Handler uiThread = new Handler(getApplicationContext().getMainLooper());

        Handler asyncHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                Spannable chordTextSpannable = (Spannable) msg.obj;

                Runnable runnable = () -> {
                    applyLinkifiedChordsTextToTextView(chordTextSpannable);
                    progressDialog.dismiss();
                };

                uiThread.post(runnable);
                handlerThread.quit();
            }
        };

        Runnable runnable = () -> {
            // your async code goes here.
            long start = System.currentTimeMillis();

            int capoDiff = capoFret - newCapoFret;
            int transposeDiff = transposeHalfSteps - newTransposeHalfSteps;
            capoFret = newCapoFret;
            transposeHalfSteps = newTransposeHalfSteps;

            updateChordsInTextForTransposition(transposeDiff, capoDiff);

            long elapsed = System.currentTimeMillis() - start;

            if (elapsed < PROGRESS_DIALOG_MIN_TIME) {
                // show progressdialog for at least 1 second, or else it goes by too fast
                // XXX: this is a weird UI hack, but I don't know what else to do
                try {
                    Thread.sleep(PROGRESS_DIALOG_MIN_TIME - elapsed);
                } catch (InterruptedException e) {
                    Log.e(LOG_TAG, e + "unexpected exception");
                }
            }

            Message message = new Message();
            message.obj = buildUpChordTextToDisplay();

            asyncHandler.sendMessage(message);

        };

        asyncHandler.post(runnable);
    }

    private void updateChordsInTextForTransposition(int transposeDiff, int capoDiff) {

        for (ChordInText chordInText : chordsInText) {

            chordInText.setChord(TransposeHelper.transposeChord(
                    chordInText.getChord(), capoDiff, transposeDiff));
        }

    }


    private void openFile(String filenameToOpen) {
        filename = filenameToOpen;
        chordText = SaveFileHelper.openFile(filename);
        if (chordText.isEmpty())
           finish();
    }

    private void showConfirmChordChartDialog() {

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);


        final EditText editText = (EditText) inflater.inflate(R.layout.confirm_chords_edit_text, null);
        editText.setText(chordText);
        editText.setTypeface(Typeface.MONOSPACE);
        editText.setBackgroundColor(PreferenceHelper.getColorScheme(this).getBackgroundColor(this));
        editText.setTextColor(PreferenceHelper.getColorScheme(this).getForegroundColor(this));

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, android.R.style.Theme_Dialog));

        builder.setTitle(R.string.edit_chords)
                .setView(editText)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    chordText = editText.getText().toString();
                    onResume();
                    isEditedTextToSave = true;
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(alertDialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        alertDialog.getWindow().setAttributes(lp);

        //Log.d(chordText);

    }

    protected void showSaveChordChartDialog(final String callingMethod) {

        if (!checkSdCard()) {
            return;
        }

        final EditText editText = createEditTextForFilenameSuggestingDialog();

        DialogInterface.OnClickListener onClickListener = (dialog, which) -> {


            if (SaveFileHelper.isInvalidFilename(editText.getText())) {
                super.showToastShort(getResources().getString(R.string.enter_good_filename));
            } else {

                if (SaveFileHelper.fileExists(editText.getText().toString())) {

                    new AlertDialog.Builder(SongViewActivity.this)
                            .setCancelable(true)
                            .setTitle(R.string.overwrite_file_title)
                            .setMessage(R.string.overwrite_file)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(android.R.string.ok, (dialog1, which1) -> {

                                saveFile(editText.getText().toString(), chordText);
                                isEditedTextToSave = false;
                                filename = editText.getText().toString();
                                setTitle(filename);

                                proceedAfterSaving(callingMethod);
                            })
                            .show();


                } else {
                    saveFile(editText.getText().toString(), chordText);
                    isEditedTextToSave = false;
                    filename = editText.getText().toString();
                    setTitle(filename);

                    proceedAfterSaving(callingMethod);
                }

            }
            dialog.dismiss();

        };

        showFilenameSuggestingDialog(editText, onClickListener);

    }

    private void proceedAfterSaving(String callingMethod) {
        if (callingMethod.equals("onBackPressed")) {
            onBackPressed();
        } else if (callingMethod.equals("onHomePressed")) {
            super.mDrawerLayout.openDrawer(GravityCompat.START);
        }
    }


    private void saveFile(final String filename, final String filetext) {

        // do in background to avoid jankiness
        HandlerThread handlerThread = new HandlerThread("SaveFileHandlerThread");
        handlerThread.start();

        Handler asyncHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Boolean successfullySavedLog = (Boolean) msg.obj;
                if (successfullySavedLog) {
                    SongViewActivity.super.showToastShort(getResources().getString(R.string.file_saved));
                } else {
                    SongViewActivity.super.showToastLong(getResources().getString(R.string.unable_to_save_file));
                }
                handlerThread.quit();
            }
        };

        Runnable runnable = () -> {
            // your async code goes here.
            ChordReaderDBHelper dbHelper = new ChordReaderDBHelper(SongViewActivity.this);
            dbHelper.saveTransposition(filename, transposeHalfSteps, capoFret);
            dbHelper.close();

            Message message = new Message();
            message.obj = SaveFileHelper.saveFile(filetext, filename);

            asyncHandler.sendMessage(message);
        };
        asyncHandler.post(runnable);
    }

    private EditText createEditTextForFilenameSuggestingDialog() {

        final EditText editText = new EditText(this);
        editText.setSingleLine();
        editText.setSingleLine(true);
        editText.setInputType(InputType.TYPE_TEXT_VARIATION_FILTER);
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editText.setOnEditorActionListener((v, actionId, event) -> {

            if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                // dismiss soft keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                assert imm != null;
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                return true;
            }


            return false;
        });

        String newFilename;

        if (filename != null) {
            //just suggest the same filename as before
            newFilename = filename;
        } else {
            // create an initial filename to suggest to the user
            if (searchText != null) {
                newFilename = searchText; // coming from web search
            } else {
                newFilename = getString(R.string.new_file);
            }
        }

        editText.setText(newFilename);

        editText.setSelection(0, newFilename.length());

        return editText;
    }

    private void showFilenameSuggestingDialog(EditText editText,
                                              DialogInterface.OnClickListener onClickListener) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.save_file)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, onClickListener)
                .setMessage(R.string.enter_filename)
                .setView(editText);

        builder.show();

    }

    private boolean checkSdCard() {

        boolean result = SaveFileHelper.checkIfSdCardExists();

        if (!result) {
            super.showToastLong(getResources().getString(R.string.sd_card_not_found));
        }
        return result;
    }

    private void analyzeChordsEtcAndShowChordView() {

        // <-- find BPMs and AutoScrollSpeed
        int bpmTemp = (int) extractAutoScrollParam(chordText, "bpm");
        if (bpmTemp > 0)
            viewingScrollView.setBpm(bpmTemp);
        else {
            super.showToastLong(getResources().getString(R.string.No_BPM_found));
            viewingScrollView.setBpm(100);
        }

        float scrollVelocityCorrectionFactorTemp = extractAutoScrollParam(chordText, "scrollVelocityCorrectionFactor");
        if (scrollVelocityCorrectionFactorTemp > 0)
            viewingScrollView.setScrollVelocityCorrectionFactor(scrollVelocityCorrectionFactorTemp);
        else {
            viewingScrollView.setScrollVelocityCorrectionFactor(1);
        }

        String returnCheckAndAddAutoScrollParams = checkAndAddAutoScrollParams(chordText, viewingScrollView.getScrollVelocityCorrectionFactor());
        if (!returnCheckAndAddAutoScrollParams.isEmpty()) {
            chordText = returnCheckAndAddAutoScrollParams;
        }
        // -->

        chordsInText = ChordParser.findChordsInText(chordText, getNoteNaming());


        Log.d(LOG_TAG,"found " + chordsInText.size() + " chords");

        showChordView();

    }

    public static float extractAutoScrollParam(String text, String autoScrollParam) {

        Matcher matcher;
        int matchGroup;

        if (text == null || autoScrollParam == null)
            return -1;

        if (autoScrollParam.equals("bpm")) {
            Pattern bpmPattern = Pattern.compile("(\\d{2,3})(\\s*bpm)", Pattern.CASE_INSENSITIVE);
            matcher = bpmPattern.matcher(text);
            matchGroup = 1;
        } else if (autoScrollParam.equals("scrollVelocityCorrectionFactor")) {
            Pattern scrollVelocityCorrectionFactorPattern = Pattern.compile("(autoscrollfactor|asf):?\\s*(\\d+[.|,]\\d+)", Pattern.CASE_INSENSITIVE);
            matcher = scrollVelocityCorrectionFactorPattern.matcher(text);
            matchGroup = 2;
        } else
            return -1;

        if (matcher.find()) {
            String match = matcher.group(matchGroup);
            if (match != null)
                match = match.replace(",", ".");
            return Float.parseFloat(match);
        } else
            return -1;
    }

    public String checkAndAddAutoScrollParams(String text, String scrollVelocityCorrectionFactor) {
        if (text == null )
            text = "";

        ArrayList<String> lines = new ArrayList<>(Arrays.asList(StringUtil.split(text, "\n")));
        String firstLine = lines.get(0).toLowerCase();

        if (!(firstLine.contains("bpm") && firstLine.contains("autoscrollfactor"))) {
            lines.add(0, "*** " + viewingScrollView.getBpm() + " BPM - AutoScrollFactor: " + scrollVelocityCorrectionFactor + " ***");

            StringBuilder resultText = new StringBuilder();
            for (String line : lines) {
                resultText.append(line).append("\n");
            }
            return resultText.toString();
        } else
            return "";
    }

    private void changeAutoScrollFactor(boolean acceleration) {
        String toReplaceText = viewingTextView.getText().toString();
        String oldValue = viewingScrollView.getScrollVelocityCorrectionFactor();
        viewingScrollView.changeScrollVelocity(acceleration);
        String newValue = viewingScrollView.getScrollVelocityCorrectionFactor();
        chordText = toReplaceText.replace("AutoScrollFactor: " + oldValue + " ***", "AutoScrollFactor: " + newValue + " ***");
        chordsInText = ChordParser.findChordsInText(chordText, getNoteNaming());
        Spannable newText = buildUpChordTextToDisplay();
        applyLinkifiedChordsTextToTextView(newText);
        isEditedTextToSave = true;
    }

    private void showChordView() {

        // do in the background to avoid jankiness
        HandlerThread handlerThread = new HandlerThread("ShowChordViewHandlerThread");
        handlerThread.start();

        Handler uiThread = new Handler(Looper.getMainLooper());

        Handler asyncHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Spannable newText = (Spannable) msg.obj;

                Runnable runnable = () -> {
                    applyLinkifiedChordsTextToTextView(newText);
                    viewingTextView.post(() -> viewingScrollView.calculateAutoScrollVelocity());
                };

                uiThread.post(runnable);
                handlerThread.quit();
            }
        };

        Runnable runnable = () -> {
            // your async code goes here.
            if (capoFret != 0 || transposeHalfSteps != 0) {
                updateChordsInTextForTransposition(-transposeHalfSteps, -capoFret);
            }

            Message message = new Message();
            message.obj = buildUpChordTextToDisplay();

            asyncHandler.sendMessage(message);

        };
        asyncHandler.post(runnable);
    }

    private void applyLinkifiedChordsTextToTextView(Spannable newText) {

        viewingTextView.setMovementMethod(MyLinkMovementMethod.getInstance());
        viewingTextView.setText(newText);

    }

    private Spannable buildUpChordTextToDisplay() {

        // have to build up a new string, because some of the chords may have different string lengths
        // than in the original text (e.g. if they are transposed)
        int lastEndIndex = 0;

        StringBuilder sb = new StringBuilder();

        List<Pair<Integer, Integer>> newStartAndEndPositions =
                new ArrayList<>(chordsInText.size());

        for (ChordInText chordInText : chordsInText) {

            //Log.d("chordInText is " + chordInText);

            sb.append(chordText, lastEndIndex, chordInText.getStartIndex());

            String chordAsString = chordInText.getChord().toPrintableString(getNoteNaming());

            sb.append(chordAsString);

            newStartAndEndPositions.add(new Pair<>(
                    sb.length() - chordAsString.length(), sb.length()));

            lastEndIndex = chordInText.getEndIndex();
        }

        // append the last bit of text after the last chord
        sb.append(chordText.substring(lastEndIndex));

        Spannable spannable = new Spannable.Factory().newSpannable(sb.toString());

        //Log.d("new start and end positions are: " + newStartAndEndPositions);

        // add a hyperlink to each chord
        for (int i = 0; i < newStartAndEndPositions.size(); i++) {

            Pair<Integer, Integer> newStartAndEndPosition = newStartAndEndPositions.get(i);

            //Log.d("pair is " + newStartAndEndPosition);
            //Log.d("substr is " + sb.substring(
            //		newStartAndEndPosition.getFirst(), newStartAndEndPosition.getSecond()));

            final Chord chord = chordsInText.get(i).getChord();

            InternalURLSpan urlSpan = new InternalURLSpan(v -> showChordPopup(chord)) {
                @Override
                public void updateDrawState(TextPaint ds) {
                    ds.setUnderlineText(false);
                    ds.setColor(PreferenceHelper.getColorScheme(getBaseContext()).getLinkColor(getBaseContext()));
                }
            };

            spannable.setSpan(urlSpan,
                    newStartAndEndPosition.getFirst(),
                    newStartAndEndPosition.getSecond(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannable;
    }

    private void showChordPopup(final Chord chord) {

        if (!ChordDictionary.isInitialized()) {
            // it could take a second or two to initialize, so just wait until then...
            return;
        }

        final PopupWindow window = PopupHelper.newBasicPopupWindow(this);


        final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.chord_popup, null);
        final TextView textView = (TextView) view.findViewById(android.R.id.text1);
        textView.setText(chord.toPrintableString(getNoteNaming()));

        final TextView textView2 = (TextView) view.findViewById(android.R.id.text2);
        textView2.setText(createGuitarChordText(chord));

        ImageButton chordVarEditButton = (ImageButton) view.findViewById(R.id.chord_var_edit_button);
        chordVarEditButton.setVisibility(View.VISIBLE);
        chordVarEditButton.setOnClickListener(view1 -> {
            startChordEditActivity(chord);
            window.dismiss();
        });

        window.setContentView(view);

        int[] textViewLocation = new int[2];
        viewingTextView.getLocationOnScreen(textViewLocation);

        int chordPopupOffset = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, CHORD_POPUP_Y_OFFSET_IN_SP, getResources().getDisplayMetrics()));

        int offsetX = Math.round(lastXCoordinate - textViewLocation[0]);
        int offsetY = Math.max(0, Math.round(lastYCoordinate - textViewLocation[1]) - chordPopupOffset);

        int heightOverride = getResources().getDimensionPixelSize(R.dimen.popup_height);

        PopupHelper.showLikeQuickAction(window, view, viewingTextView, getWindowManager(), offsetX, offsetY, heightOverride);

    }

    private CharSequence createGuitarChordText(Chord chord) {
        // TODO: have a better interface for switching between alternative ways of playing the same chord.
        // For now, just build up a list and show everything at once.

        List<String> guitarChords = ChordDictionary.getGuitarChordsForChord(chord);

        // Given how the dictionary is read in, these chords should have the simplest ones first
        // Just separate each with a number, if there is more than one

        switch (guitarChords.size()) {
            case 0:
                return getString(R.string.no_guitar_chord_available);
            case 1:
                return guitarChords.get(0);
            default:
                // create a list
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < guitarChords.size(); i++) {
                    stringBuilder
                            .append(getString(R.string.variation))
                            .append(' ')
                            .append(i + 1)
                            .append(": ")
                            .append(guitarChords.get(i))
                            .append('\n');
                }
                return stringBuilder.substring(0, stringBuilder.length() - 1); // cut off final newline
        }
    }

    private void startChordEditActivity(Chord chord) {
        Intent chordVarEditIntent = new Intent(getBaseContext(), ChordDictionaryEditActivity.class);
        //pass chord by intend
        chordVarEditIntent.putExtra("CHORD", chord);
        chordVarEditIntent.putExtra("NOTENAMING", getNoteNaming());
        startActivity(chordVarEditIntent);
    }

    private void resetDataExceptChordTextAndFilename() {

        chordsInText = null;
        if (filename != null) {
            ChordReaderDBHelper dbHelper = new ChordReaderDBHelper(this);
            Transposition transposition = dbHelper.findTranspositionByFilename(filename);
            dbHelper.close();
            if (transposition != null) {
                capoFret = transposition.getCapo();
                transposeHalfSteps = transposition.getTranspose();
            } else {
                capoFret = 0;
                transposeHalfSteps = 0;
            }
        } else {
            capoFret = 0;
            transposeHalfSteps = 0;
        }

    }

    private void applyColorScheme() {

        ColorScheme colorScheme = PreferenceHelper.getColorScheme(this);

        viewingTextView.setTextColor(colorScheme.getForegroundColor(this));
        chordsViewingMainView.setBackgroundColor(colorScheme.getBackgroundColor(this));
    }

    // Animations
    protected void animationBlink(ImageButton imageButton) {
        Animation animButtonBlink = AnimationUtils.loadAnimation(this, R.anim.blink_anim);
        imageButton.startAnimation(animButtonBlink);
    }

    public void animationSwitch(final View view1, final View view2) {
        view1.setVisibility(View.INVISIBLE);
        view2.setVisibility(View.VISIBLE);
    }


    protected void startAnimationMetronome() {
        final long switchDuration = (long) (60d * 1000) / viewingScrollView.getBpm();
        final LightingColorFilter lightningColorFilter = new LightingColorFilter(Color.rgb(50, 160, 186), Color.rgb(0, 60, 86));

        metronomeTimer = new Timer();
        TimerTask metronomTimerTask = new TimerTask() {
            public void run() {
                metronomeHandler.post(() -> {

                    autoScrollPauseButton.setColorFilter(lightningColorFilter);
                    autoScrollPauseButton.postDelayed(() -> autoScrollPauseButton.clearColorFilter(), switchDuration / 5);


                });
            }
        };
        metronomeTimer.schedule(metronomTimerTask, switchDuration, switchDuration);
    }

    protected void stopAnimationMetronom() {
        if (metronomeTimer != null) {
            metronomeTimer.cancel();
            metronomeTimer.purge();
        }
    }

    // Autoscroll

    private void stopAutoscroll() {
        viewingScrollView.setAutoScrollOn(false);
        viewingScrollView.stopAutoScroll();
        stopAnimationMetronom();
        animationSwitch(autoScrollPauseButton, autoScrollPlayButton);
    }

    private void startAutoscroll() {

        if (!viewingScrollView.isAutoScrollOn()) {
            animationSwitch(autoScrollPlayButton, autoScrollPauseButton);
            viewingScrollView.setAutoScrollOn(true);

            startAnimationMetronome();
            Handler handler = new Handler();
            handler.postDelayed(() -> {
                if (!viewingScrollView.isAutoScrollOn())
                    return;
                viewingScrollView.startAutoScroll();
            }, (long) (60d / viewingScrollView.getBpm() * 1000 * 4)); // delay of autoScroll start, to watch metronome firstly

        } else {
            if (!viewingScrollView.isFlingActive() && !viewingScrollView.isAutoScrollActive()) {
                viewingScrollView.startAutoScroll();
            }
        }
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDoubleTap(MotionEvent e) {

            if (viewingScrollView.isAutoScrollOn())
                stopAutoscroll();
            else
                startAutoscroll();

            doubleTapExcecuted = true;
            return true;
        }

    }

    // LinkMovementMethod/onTouchEvent needs to be adapted as otherwise linkified chord links will
    // be triggered till end of line/text view, when linkified chord is last on line
    public static class MyLinkMovementMethod extends LinkMovementMethod {
        private static MyLinkMovementMethod sInstance;

        public static MyLinkMovementMethod getInstance() {
            if (sInstance == null)
                sInstance = new MyLinkMovementMethod();
            return sInstance;
        }


        @Override
        public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
            int action = event.getAction();

            if (action == MotionEvent.ACTION_UP) {
                int x = (int) event.getX();
                int y = (int) event.getY();

                y -= widget.getTotalPaddingTop();
                y += widget.getScrollY();

                Layout layout = widget.getLayout();
                int line = layout.getLineForVertical(y);
                float lineLeft = layout.getLineLeft(line);
                float lineRight = layout.getLineRight(line);

                if (x > lineRight || (x >= 0 && x < lineLeft)) {
                    return true;
                }
            }

            return super.onTouchEvent(widget, buffer, event);
        }
    }
}