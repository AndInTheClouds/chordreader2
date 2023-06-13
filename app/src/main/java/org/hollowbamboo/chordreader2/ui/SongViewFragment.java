package org.hollowbamboo.chordreader2.ui;

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
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.InputType;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.viewpager.widget.ViewPager;

import org.hollowbamboo.chordreader2.R;
import org.hollowbamboo.chordreader2.adapter.ChordPagerAdapter;
import org.hollowbamboo.chordreader2.chords.Chord;
import org.hollowbamboo.chordreader2.chords.NoteNaming;
import org.hollowbamboo.chordreader2.chords.regex.ChordParser;
import org.hollowbamboo.chordreader2.data.ColorScheme;
import org.hollowbamboo.chordreader2.databinding.FragmentSongViewBinding;
import org.hollowbamboo.chordreader2.db.ChordReaderDBHelper;
import org.hollowbamboo.chordreader2.db.Transposition;
import org.hollowbamboo.chordreader2.helper.ChordDictionary;
import org.hollowbamboo.chordreader2.helper.DialogHelper;
import org.hollowbamboo.chordreader2.helper.PreferenceHelper;
import org.hollowbamboo.chordreader2.helper.SaveFileHelper;
import org.hollowbamboo.chordreader2.model.DataViewModel;
import org.hollowbamboo.chordreader2.model.SongViewFragmentViewModel;
import org.hollowbamboo.chordreader2.views.AutoScrollView;
import org.hollowbamboo.chordreader2.views.ChordVisualisationView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class SongViewFragment extends Fragment implements View.OnClickListener {

    private static final String LOG_TAG = "SongViewFragment";
    private static final int PROGRESS_DIALOG_MIN_TIME = 600;
    private static final int POST_SAVE_PROCEEDING_EXIT = 100;
    private static final int POST_SAVE_PROCEEDING_NEXT_SONG = 200;
    private static final int POST_SAVE_PROCEEDING_PREVIOUS_SONG = 201;

    private int howToProceedAfterSaving = 0;

    private SongViewFragmentViewModel songViewFragmentViewModel;
    private FragmentSongViewBinding binding;
    private DataViewModel dataViewModel;

    private ConstraintLayout chordsViewingMainView;
    private TextView viewingTextView;
    private AutoScrollView viewingScrollView;
    private Toolbar toolbar;

    private ImageButton autoScrollPlayButton, autoScrollPauseButton, autoScrollSlowerButton, autoScrollFasterButton;

    private CountDownTimer releaseWakeLockCountDownTimer;
    private Timer metronomeTimer;
    private final Handler metronomeHandler = new Handler();

    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;

    private float lastXCoordinate, lastYCoordinate;
    private boolean doubleTapExecuted = false;

    private int indexCurrentSong, setlistSongsIndexDiffEnd;
    private String filename;

    public static SongViewFragment newInstance() {
        return new SongViewFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        songViewFragmentViewModel =
                new ViewModelProvider(this).get(SongViewFragmentViewModel.class);
        dataViewModel =
                new ViewModelProvider(requireActivity()).get(DataViewModel.class);

        binding = FragmentSongViewBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setInstanceData();

        determineSetListSongProgression();

        setUpWidgets();

        setUpMenu();

        setObserversForLiveData();

        setTextSize();

        handleBackButton();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        applyColorScheme();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
    }

    private void setUpMenu() {
        MenuHost menuHost = requireActivity();
        MenuProvider menuProvider = new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.song_view_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int itemId = menuItem.getItemId();

                if(itemId == R.id.menu_save_chords) {
                    showSaveChordTextDialog();
                    return true;
                } else if(itemId == R.id.menu_edit_file) {
                    showEditChordTextDialog();
                    return true;
                } else if(itemId == R.id.menu_transpose) {
                    createTransposeDialog();
                    return true;
                } else if(itemId == R.id.menu_add_to_setlist) {
                    createSetListDialog();
                    return true;
                } else if(itemId == android.R.id.home) {
                    howToProceedAfterSaving = POST_SAVE_PROCEEDING_EXIT;
                    checkForSaving();

                    return true;
                }

                return false;
            }
        };

        menuHost.addMenuProvider(menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.autoScrollPlayButton) {
            startAutoscroll();
        } else if(id == R.id.autoScrollPauseButton) {
            stopAutoscroll();
        } else if(id == R.id.autoScrollSlower) {
            animationBlink(autoScrollSlowerButton);
            changeAutoScrollFactor(false);
        } else if(id == R.id.autoScrollFaster) {
            animationBlink(autoScrollFasterButton);
            changeAutoScrollFactor(true);
        } else if(id == R.id.setlist_next) {
            howToProceedAfterSaving = POST_SAVE_PROCEEDING_NEXT_SONG;
            checkForSaving();
        } else if(id == R.id.setlist_previous) {
            howToProceedAfterSaving = POST_SAVE_PROCEEDING_PREVIOUS_SONG;
            checkForSaving();
        }
    }

    private void setUpWidgets() {
        viewingTextView = binding.chordsViewingTextView;
        viewingScrollView = binding.chordsViewingScrollView;
        viewingScrollView.setTargetTextView(viewingTextView);

        chordsViewingMainView = binding.chordsViewingMainView;

        autoScrollPlayButton = binding.autoScrollPlayButton;
        autoScrollPlayButton.setOnClickListener(this);
        autoScrollPauseButton = binding.autoScrollPauseButton;
        autoScrollPauseButton.setOnClickListener(this);
        autoScrollSlowerButton = binding.autoScrollSlower;
        autoScrollSlowerButton.setOnClickListener(this);
        autoScrollFasterButton = binding.autoScrollFaster;
        autoScrollFasterButton.setOnClickListener(this);

        if(dataViewModel.getSetListMLD() != null) {
            if(setlistSongsIndexDiffEnd > 0) {
                ImageView nextSongButton = binding.setlistNext;
                nextSongButton.setVisibility(View.VISIBLE);
                nextSongButton.setOnClickListener(this);
            }

            if(indexCurrentSong > 0) {
                ImageView previousSongButton = binding.setlistPrevious;
                previousSongButton.setVisibility(View.VISIBLE);
                previousSongButton.setOnClickListener(this);
            }
        }

        gestureDetector = new GestureDetector(requireContext(), new MyGestureListener());
        scaleGestureDetector = new ScaleGestureDetector(requireContext(), new MyScaleListener());

        toolbar = requireActivity().findViewById(R.id.toolbar);

        View.OnTouchListener touchListener = (v, event) -> {

            lastXCoordinate = event.getRawX();
            lastYCoordinate = event.getRawY();

            final int action = event.getAction();

            if(event.getPointerCount() == 1) {
                if(action == MotionEvent.ACTION_DOWN) {

                    doubleTapExecuted = false;

                    if(viewingScrollView.isAutoScrollOn()) {
                        viewingScrollView.stopAutoScroll();
                    }
                }

                if(action == MotionEvent.ACTION_UP) {
                    viewingScrollView.setTouched(false);
                    if(viewingScrollView.isAutoScrollOn() && !doubleTapExecuted) {
                        startAutoscroll();
                    }
                }

                return gestureDetector.onTouchEvent(event);
            } else {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Disallow ScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        scaleGestureDetector.onTouchEvent(event);
                        break;

                    case MotionEvent.ACTION_MOVE:
                        // Disallow ScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        scaleGestureDetector.onTouchEvent(event);
                        break;

                    case MotionEvent.ACTION_UP:
                        // Allow ScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }
                return true;
            }
        };

        viewingTextView.setOnTouchListener(touchListener);

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
    }

    private void setObserversForLiveData() {

        songViewFragmentViewModel.getFragmentTitle().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String title) {
                setTitle(title);
            }
        });

        songViewFragmentViewModel.getChordTextMLD().observe(getViewLifecycleOwner(), new Observer<Spannable>() {
            @Override
            public void onChanged(@Nullable Spannable finalChordText) {
                applyLinkifiedChordsTextToTextView(finalChordText);
                viewingTextView.post(() -> viewingScrollView.calculateAutoScrollVelocity());
            }
        });

        songViewFragmentViewModel.getTextSize().observe(getViewLifecycleOwner(), new Observer<Float>() {
            @Override
            public void onChanged(Float textSize) {
                if(textSize == 0) {
                    return;
                }

                viewingTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX,textSize);
            }
        });

        songViewFragmentViewModel.getBpmMLD().observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer bpm) {
                viewingScrollView.setBpm(bpm);
            }
        });

       songViewFragmentViewModel.getScrollVelocityCorrFactorMLD().observe(getViewLifecycleOwner(), new Observer<Float>() {
           @Override
           public void onChanged(Float scrollVelocityCorrectionFactor) {
               if(scrollVelocityCorrectionFactor > 0)
                   viewingScrollView.setScrollVelocityCorrectionFactor(scrollVelocityCorrectionFactor);
               else {
                   viewingScrollView.setScrollVelocityCorrectionFactor(1);
               }
           }
       });

        songViewFragmentViewModel.getShowChordPopupMLD().observe(getViewLifecycleOwner(), new Observer<Chord>() {
            @Override
            public void onChanged(Chord chord) {
                showChordPopup(chord);
            }
        });

        songViewFragmentViewModel.getSaveResultMLD().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean successfullySavedLog) {

                if(successfullySavedLog) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.file_saved), Toast.LENGTH_SHORT).show();

                    songViewFragmentViewModel.isEditedTextToSave = false;
                    songViewFragmentViewModel.filename = filename;

                } else {
                    Toast.makeText(getActivity(), getResources().getString(R.string.unable_to_save_file), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setInstanceData() {

        if(songViewFragmentViewModel.getFragmentTitle().getValue() == null ||
            songViewFragmentViewModel.chordText == null) {

            String songTitle = SongViewFragmentArgs.fromBundle(getArguments()).getSongTitle();
            filename = SongViewFragmentArgs.fromBundle(getArguments()).getFilename();
            String chordText = SongViewFragmentArgs.fromBundle(getArguments()).getChordText();
            Transposition transposition = null;

            if(filename == null) {
                songTitle = filename = getResources().getString(R.string.new_file);
                songViewFragmentViewModel.isEditedTextToSave = true;
            } else if(filename.isEmpty() && !(songTitle != null && songTitle.isEmpty())) { // from web search
                filename = songTitle;
                songViewFragmentViewModel.isEditedTextToSave = true;
            } else {
                songTitle = filename;
                chordText = SaveFileHelper.openFile(requireContext(),filename.concat(".txt"));
                transposition = getTransposition(filename);
                songViewFragmentViewModel.autoSave = true;
            }
            songTitle = Character.toUpperCase(songTitle.charAt(0)) + songTitle.substring(1);

            songViewFragmentViewModel.setSongTitle(songTitle);
            songViewFragmentViewModel.filename = filename;
            songViewFragmentViewModel.setNoteNaming(getNoteNaming());
            songViewFragmentViewModel.setTransposition(transposition);
            songViewFragmentViewModel.setLinkColor(PreferenceHelper.getColorScheme(getActivity().getBaseContext()).getLinkColor(getActivity().getBaseContext()));
            songViewFragmentViewModel.setChordText(chordText);
        }


        getParentFragmentManager().setFragmentResultListener("EditChordTextDialog", this, songViewFragmentViewModel.getFragmentResultListener());
    }

    private void determineSetListSongProgression() {
        if(dataViewModel.setListSongs == null)
            return;

        indexCurrentSong = dataViewModel.setListSongs.indexOf(filename);
        int indexLastSong = dataViewModel.setListSongs.size() - 1;

        setlistSongsIndexDiffEnd = indexLastSong - indexCurrentSong;
    }

    private void handleBackButton() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                howToProceedAfterSaving = POST_SAVE_PROCEEDING_EXIT;

                checkForSaving();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);
    }

    private void checkForSaving() {

        if(songViewFragmentViewModel.isEditedTextToSave) {
            if (songViewFragmentViewModel.autoSave)
                saveFile(songViewFragmentViewModel.filename, songViewFragmentViewModel.chordText);
            else {
                showSavePromptDialog();
            }
        } else
            proceedAfterSaving();
    }

    private void saveFile(final String filename, final String fileText) {

        // Save text size

        HandlerThread handlerThread = new HandlerThread("SaveTextSizeHandlerThread");
        handlerThread.start();

        Handler asyncHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Boolean successfullySavedLog = (Boolean) msg.obj;

                handlerThread.quit();
            }
        };

        Runnable runnable = () -> {
            // your async code goes here.
            ChordReaderDBHelper dbHelper = new ChordReaderDBHelper(requireContext());
            dbHelper.saveTransposition(filename, songViewFragmentViewModel.transposeHalfSteps,
                    songViewFragmentViewModel.capoFret);
            dbHelper.close();

            Message message = new Message();
            message.obj = true;

            asyncHandler.sendMessage(message);
        };

        asyncHandler.post(runnable);


        // Save to file
        boolean successfullySavedLog = SaveFileHelper.saveFile(requireContext(), fileText, filename.concat(".txt"));

        songViewFragmentViewModel.getSaveResultMLD().setValue(successfullySavedLog);

        proceedAfterSaving();
    }

    private void proceedAfterSaving() {
        if (howToProceedAfterSaving == POST_SAVE_PROCEEDING_EXIT) {
            if (getParentFragment() != null) {
                Navigation.findNavController(getParentFragment().requireView()).popBackStack();
            }
        } else if (howToProceedAfterSaving == POST_SAVE_PROCEEDING_NEXT_SONG) {
            openNextSong(true);
        } else if (howToProceedAfterSaving == POST_SAVE_PROCEEDING_PREVIOUS_SONG) {
            openNextSong(false);
        } else
            setTitle(songViewFragmentViewModel.filename);
    }

    private boolean checkSdCard() {

        boolean result = SaveFileHelper.checkIfSdCardExists();

        if(!result) {
            Toast.makeText(getActivity(), getResources().getString(R.string.sd_card_not_found), Toast.LENGTH_SHORT).show();
        }
        return result;
    }

    private void setTitle(String titleText) {

        // have to use original thread, else exception
        Handler handlerInMainThread = new Handler(toolbar.getContext().getMainLooper());

        Runnable yourRunnable = new Runnable() {
            @Override
            public void run() {
                toolbar.setTitle(titleText);
            }
        };

        handlerInMainThread.post(yourRunnable);
    }

    private void applyColorScheme() {

        ColorScheme colorScheme = PreferenceHelper.getColorScheme(requireContext());

        viewingTextView.setTextColor(colorScheme.getForegroundColor(requireContext()));
        chordsViewingMainView.setBackgroundColor(colorScheme.getBackgroundColor(requireContext()));
    }

    private void acquireWakeLock() {

        Log.d(LOG_TAG,"Acquiring wakelock");

        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if(releaseWakeLockCountDownTimer != null) {
            releaseWakeLockCountDownTimer.cancel();
        }

    }

    private void releaseWakeLock() {

        //Log.d(LOG_TAG,"Releasing wakelock in 5min");

        releaseWakeLockCountDownTimer = new CountDownTimer(300000, 1000) {

            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                //Log.d(LOG_TAG,"Wakelock released");
                try {
                    requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } catch (IllegalStateException e) {
                    Log.d("SongViewFragment","Fragment not attached to activity");
                }
            }
        }.start();

    }

    private void applyLinkifiedChordsTextToTextView(Spannable newText) {

        SpannableString spannableString = (SpannableString) newText;

        viewingTextView.setMovementMethod(MyLinkMovementMethod.getInstance());
        viewingTextView.setText(spannableString);

    }

    private void setTextSize() {

        ChordReaderDBHelper dbHelper = new ChordReaderDBHelper(requireContext());

        float textSize = dbHelper.findTextSizeByFilename(songViewFragmentViewModel.filename);
        dbHelper.close();

        if(textSize == 0) {
            return;
        }

        viewingTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX,textSize);
    }

    private void saveTextSize(float textSize) {
        if(!(songViewFragmentViewModel.filename == null)) {
            ChordReaderDBHelper dbHelper = new ChordReaderDBHelper(requireContext());
            dbHelper.saveTextSize(songViewFragmentViewModel.filename, textSize);
            dbHelper.close();

            Log.d("SongView-SaveTextSize", " - " + textSize);
        }
    }

    private NoteNaming getNoteNaming() {
        return PreferenceHelper.getNoteNaming(requireContext());
    }

    private Transposition getTransposition(String filename) {
        ChordReaderDBHelper dbHelper = new ChordReaderDBHelper(requireContext());
        Transposition transposition = dbHelper.findTranspositionByFilename(filename);
        dbHelper.close();
        return transposition;
    }

    protected void changeTransposeOrCapo(final int newTransposeHalfSteps, final int newCapoFret) {

        String filename = songViewFragmentViewModel.filename;

        // persist
        if(filename != null) {
            ChordReaderDBHelper dbHelper = new ChordReaderDBHelper(requireContext());
            dbHelper.saveTransposition(filename, newTransposeHalfSteps, newCapoFret);
            dbHelper.close();
        }

        final ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setTitle(R.string.transposing);
        progressDialog.setMessage(getText(R.string.please_wait));
        progressDialog.setIndeterminate(true);

        // transpose in background to avoid jankiness
        progressDialog.show();

        HandlerThread handlerThread = new HandlerThread("ChangeTransposeOrCapoHandlerThread");
        handlerThread.start();

        Handler uiThread = new Handler(requireActivity().getMainLooper());

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

            int capoDiff = songViewFragmentViewModel.capoFret - newCapoFret;
            int transposeDiff = songViewFragmentViewModel.transposeHalfSteps - newTransposeHalfSteps;
            songViewFragmentViewModel.capoFret = newCapoFret;
            songViewFragmentViewModel.transposeHalfSteps = newTransposeHalfSteps;

            songViewFragmentViewModel.updateChordsInTextForTransposition(transposeDiff, capoDiff);

            long elapsed = System.currentTimeMillis() - start;

            if(elapsed < PROGRESS_DIALOG_MIN_TIME) {
                // show progressdialog for at least 1 second, or else it goes by too fast
                // XXX: this is a weird UI hack, but I don't know what else to do
                try {
                    Thread.sleep(PROGRESS_DIALOG_MIN_TIME - elapsed);
                } catch (InterruptedException e) {
                    Log.e(LOG_TAG, e + "unexpected exception");
                }
            }

            Message message = new Message();
            message.obj = songViewFragmentViewModel.buildUpChordTextToDisplay();

            asyncHandler.sendMessage(message);

        };

        asyncHandler.post(runnable);
    }

    private void openNextSong(boolean forward) {
        String subsequentSong;
        if(forward)
            subsequentSong = dataViewModel.setListSongs.get(indexCurrentSong + 1);
        else
            subsequentSong = dataViewModel.setListSongs.get(indexCurrentSong - 1);

        SongViewFragmentDirections.ActionNavSongViewSelf action =
                SongViewFragmentDirections.actionNavSongViewSelf(null, subsequentSong, null);

        Navigation.findNavController(getParentFragment().getView()).navigate(action);
    }

    private void changeAutoScrollFactor(boolean acceleration) {
        String toReplaceText = viewingTextView.getText().toString();
        String oldValue = viewingScrollView.getScrollVelocityCorrectionFactor();
        viewingScrollView.changeScrollVelocity(acceleration);
        String newValue = viewingScrollView.getScrollVelocityCorrectionFactor();
        songViewFragmentViewModel.chordText = toReplaceText.replace("AutoScrollFactor: " + oldValue + " ***", "AutoScrollFactor: " + newValue + " ***");
        songViewFragmentViewModel.chordsInText = ChordParser.findChordsInText(songViewFragmentViewModel.chordText, getNoteNaming());
        Spannable newText = songViewFragmentViewModel.buildUpChordTextToDisplay();
        applyLinkifiedChordsTextToTextView(newText);
        songViewFragmentViewModel.isEditedTextToSave = true;
    }

    // Animations

    protected void animationBlink(ImageButton imageButton) {
        Animation animButtonBlink = AnimationUtils.loadAnimation(requireContext(), R.anim.blink_anim);
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
        if(metronomeTimer != null) {
            metronomeTimer.cancel();
            metronomeTimer.purge();
        }
    }

    private void stopAutoscroll() {
        viewingScrollView.setAutoScrollOn(false);
        viewingScrollView.stopAutoScroll();
        stopAnimationMetronom();
        animationSwitch(autoScrollPauseButton, autoScrollPlayButton);
    }

    private void startAutoscroll() {

        if(!viewingScrollView.isAutoScrollOn()) {
            animationSwitch(autoScrollPlayButton, autoScrollPauseButton);
            viewingScrollView.setAutoScrollOn(true);

            startAnimationMetronome();
            Handler handler = new Handler();
            handler.postDelayed(() -> {
                if(!viewingScrollView.isAutoScrollOn())
                    return;
                viewingScrollView.startAutoScroll();
            }, (long) (60d / viewingScrollView.getBpm() * 1000 * 4)); // delay of autoScroll start, to watch metronome firstly

        } else {
            if(!viewingScrollView.isFlingActive() && !viewingScrollView.isAutoScrollActive()) {
                viewingScrollView.startAutoScroll();
            }
        }
    }

    private void appendSongToSetlist(String setlist) {


        if (!setlist.endsWith(".pl"))
            setlist = setlist.concat(".pl");

        ArrayList<String> filesList = (ArrayList<String>) SaveFileHelper.openSetlist(requireContext(), setlist);

        filesList.add(songViewFragmentViewModel.filename);

        StringBuilder resultText = new StringBuilder();
        for (String line : filesList) {
            if (!line.endsWith(".txt"))
                resultText.append(line).append(".txt\n");
            else
                resultText.append(line).append("\n");
        }

        SaveFileHelper.saveFile(requireContext(), resultText.toString(),setlist);
    }

    // Dialogs

    private void showSavePromptDialog() {
        new AlertDialog.Builder(requireContext())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.unsaved_changes)
                .setMessage(R.string.unsaved_changes_message)
                .setNegativeButton(R.string.no, (dialog, which) -> {
                    songViewFragmentViewModel.isEditedTextToSave = false;
                    proceedAfterSaving();
                })
                .setPositiveButton(R.string.yes, (dialog, which) -> showSaveChordTextDialog())
                .show();
    }

    private void showEditChordTextDialog() {
        EditChordTextDialog editChordTextDialog = new EditChordTextDialog();

        Bundle args = new Bundle();
        args.putString("chordText", Objects.requireNonNull(songViewFragmentViewModel.chordText));

        editChordTextDialog.setArguments(args);

        getParentFragmentManager().setFragmentResultListener("EditChordTextDialog", this, songViewFragmentViewModel.getFragmentResultListener());

        editChordTextDialog.show(getParentFragmentManager(),"EditChordTextDialog");
    }

    protected void showSaveChordTextDialog() {

        if(!checkSdCard()) {
            return;
        }

        final EditText editText = new EditText(requireContext());
        editText.setSingleLine();
        editText.setSingleLine(true);
        editText.setInputType(InputType.TYPE_TEXT_VARIATION_FILTER);
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                InputMethodManager imm = (InputMethodManager)
                        getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

                if (v.requestFocus())
                    editText.post(new Runnable() {
                        @Override
                        public void run() {
                            imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
                        }
                    });
                else
                    imm.showSoftInput(v, InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        });

        editText.setText(songViewFragmentViewModel.filename);

        editText.setSelection(0, songViewFragmentViewModel.filename.length());

        DialogInterface.OnClickListener onClickListener = (dialog, which) -> {


            if(SaveFileHelper.isInvalidFilename(editText.getText())) {
                Toast.makeText(getActivity(), getResources().getString(R.string.enter_good_filename), Toast.LENGTH_SHORT).show();
            } else {

                if(SaveFileHelper.fileExists(requireContext(),editText.getText().toString().concat(".txt"))) {

                    new AlertDialog.Builder(requireContext())
                            .setCancelable(true)
                            .setTitle(R.string.overwrite_file_title)
                            .setMessage(R.string.overwrite_file)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(android.R.string.ok, (dialog1, which1) -> {

                                saveFile(editText.getText().toString(), songViewFragmentViewModel.chordText);
                                songViewFragmentViewModel.filename = editText.getText().toString();
                                proceedAfterSaving();
                            })
                            .show();

                } else {
                    saveFile(editText.getText().toString(), songViewFragmentViewModel.chordText);
                    songViewFragmentViewModel.filename = editText.getText().toString();
                    proceedAfterSaving();
                }

            }
            dialog.dismiss();
        };

        showFilenameSuggestingDialog(editText, onClickListener);
    }

    private void showFilenameSuggestingDialog(EditText editText,
                                              DialogInterface.OnClickListener onClickListener) {

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        builder.setTitle(R.string.save_file)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, onClickListener)
                .setMessage(R.string.enter_filename)
                .setView(editText);

        builder.show();
        editText.requestFocus();
    }

    private void showChordPopup(final Chord chord) {

        if(!ChordDictionary.isInitialized()) {
            // it could take a second or two to initialize, so just wait until then...
            return;
        }

        final LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.popup_chord, null);

        ImageButton nextChordButton = (ImageButton) view.findViewById(R.id.next_chord_button);
        ImageButton previousChordButton = (ImageButton) view.findViewById(R.id.previous_chord_button);

        List<String> guitarChords = ChordDictionary.getGuitarChordsForChord(chord);

        if(guitarChords.size() > 0) {

            ChordPagerAdapter chordPagerAdapter = new ChordPagerAdapter();
            WrapContentViewPager viewPager = view.findViewById(R.id.chord_visualisation_view_pager);
            viewPager.setAdapter(chordPagerAdapter);


            for (String chordVariation : guitarChords) {

                ChordVisualisationView chordVisualisation = new ChordVisualisationView(chordVariation, requireContext());
                chordPagerAdapter.addView(chordVisualisation);
            }

            chordPagerAdapter.notifyDataSetChanged();

            if(guitarChords.size() != 1) {
                previousChordButton.setOnClickListener(view1 -> {
                    View currentView = chordPagerAdapter.getView(viewPager.getCurrentItem());
                    int currentViewPos = chordPagerAdapter.getItemPosition(currentView);
                    if(currentViewPos > 0) {
                        viewPager.arrowScroll(View.FOCUS_LEFT);
                    }
                });

                nextChordButton.setVisibility(View.VISIBLE);
                nextChordButton.setOnClickListener(view1 -> {
                    View currentView = chordPagerAdapter.getView(viewPager.getCurrentItem());
                    int currentViewPos = chordPagerAdapter.getItemPosition(currentView);
                    if(currentViewPos < chordPagerAdapter.getCount() - 1)
                        viewPager.arrowScroll(View.FOCUS_RIGHT);
                });
            }

            TextView chordVisuTextView = (TextView) view.findViewById(R.id.chord_visualisation_text_view);
            String currentVarOfTotal = "Var: " + 1 + getString(R.string.of) + chordPagerAdapter.getCount();
            chordVisuTextView.setText(currentVarOfTotal);

            viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {

                    String currentVarOfTotal = "Var: " + (position + 1) + getString(R.string.of) + chordPagerAdapter.getCount();
                    chordVisuTextView.setText(currentVarOfTotal);

                    if(position == 0)
                        previousChordButton.setVisibility(View.INVISIBLE);
                    else
                        previousChordButton.setVisibility(View.VISIBLE);

                    if(position == chordPagerAdapter.getCount()-1)
                        nextChordButton.setVisibility(View.INVISIBLE);
                    else
                        nextChordButton.setVisibility(View.VISIBLE);
                }

                @Override
                public void onPageScrollStateChanged(int state) {

                }
            });
        } else {
            TextView chordVisuTextView = (TextView) view.findViewById(R.id.chord_visualisation_text_view);
            chordVisuTextView.setText(getString(R.string.no_guitar_chord_available));
        }


        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());//new ContextThemeWrapper(this, android.R.style.Theme_Dialog));

        builder.setTitle(chord.toPrintableString(getNoteNaming()))
                .setView(view)
                .setNeutralButton("Edit",(dialog, which) -> {
                    SongViewFragmentDirections.ActionNavSongViewToNavChordDictEdit action =
                            SongViewFragmentDirections.actionNavSongViewToNavChordDictEdit(chord);
                    Navigation.findNavController(getParentFragment().getView()).navigate(action);
                })
                .setPositiveButton(android.R.string.ok, null);

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        //TODO: didn't found a solution to let alert dialog wrap content, so set fixed pixel density dependent value
        alertDialog.getWindow().setLayout((int) (220 * Resources.getSystem().getDisplayMetrics().density), (int) (270 * Resources.getSystem().getDisplayMetrics().density));
    }

    private void createTransposeDialog() {

        final View view = DialogHelper.createTransposeDialogView(requireContext(),
                songViewFragmentViewModel.capoFret,
                songViewFragmentViewModel.transposeHalfSteps);
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.transpose)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {

                    // grab the user's chosen values for the capo and the transposition

                    View transposeView = view.findViewById(R.id.transpose_include);
                    View capoView = view.findViewById(R.id.capo_include);

                    int newTransposeHalfSteps = DialogHelper.getSeekBarValue(transposeView) + DialogHelper.TRANSPOSE_MIN;
                    int newCapoFret = DialogHelper.getSeekBarValue(capoView) + DialogHelper.CAPO_MIN;

                    Log.d(LOG_TAG, "transposeHalfSteps is now " + newTransposeHalfSteps);
                    Log.d(LOG_TAG, "capoFret is now " + newCapoFret);

                    changeTransposeOrCapo(newTransposeHalfSteps, newCapoFret);

                    dialog.dismiss();

                })
                .setView(view)
                .show();

    }


    private void createSetListDialog() {

        if (!checkSdCard()) {
            return;
        }

        List<String> setListNames = Arrays.asList(SaveFileHelper.getSavedFileNames(requireContext(),".pl"));

        if (setListNames.isEmpty()) {
            Toast.makeText(requireContext(), R.string.no_setlists, Toast.LENGTH_SHORT).show();
            return;
        }

        Collections.sort(setListNames);

        final String[] listItems = new String[setListNames.size()];

        int i = 0;
        for (String setList : setListNames) {
            listItems[i] = setList.replace(".pl","");
            i++;
        }

        final int[] checkedItem = {-1};

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.add_to_setlist)
                .setSingleChoiceItems(listItems, checkedItem[0], (dialog, which) -> {
                    checkedItem[0] = which;
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String setList = listItems[checkedItem[0]];

                        appendSongToSetlist(setList);

                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {})
                .create()
                .show();
    }

    //Classes

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

        public MyGestureListener() {
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {

            if(viewingScrollView.isAutoScrollOn())
                stopAutoscroll();
            else
                startAutoscroll();

            doubleTapExecuted = true;
            return true;
        }

    }

    private class MyScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {

            float textSize = viewingTextView.getTextSize();
            float scaleFactor = scaleGestureDetector.getScaleFactor();

            float textSizeDelta = 0;
            if(scaleFactor > 1.0f)
                textSizeDelta = 0.5f;
            else if(scaleFactor < 1.0f)
                textSizeDelta = -0.5f;

            textSize += textSizeDelta;

            float textSizeMin = getResources().getDimension(R.dimen.text_size_min);
            float textSizeMax = getResources().getDimension(R.dimen.text_size_max);

            if(textSize < textSizeMin) {
                textSize = textSizeMin;
            }
            if(textSize > textSizeMax) {
                textSize = textSizeMax;
            }

            viewingTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

            saveTextSize(textSize);

            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {

            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
        }
    }

    // LinkMovementMethod/onTouchEvent needs to be adapted as otherwise linkified chord links will
    // be triggered till end of line/text view, when linkified chord is last on line
    public static class MyLinkMovementMethod extends LinkMovementMethod {
        private static MyLinkMovementMethod sInstance;

        public static MyLinkMovementMethod getInstance() {
            if(sInstance == null)
                sInstance = new MyLinkMovementMethod();
            return sInstance;
        }


        @Override
        public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
            int action = event.getAction();

            if(action == MotionEvent.ACTION_UP) {
                int x = (int) event.getX();
                int y = (int) event.getY();

                y -= widget.getTotalPaddingTop();
                y += widget.getScrollY();

                Layout layout = widget.getLayout();
                int line = layout.getLineForVertical(y);
                float lineLeft = layout.getLineLeft(line);
                float lineRight = layout.getLineRight(line);

                if(x > lineRight || (x >= 0 && x < lineLeft)) {
                    return true;
                }
            }

            return super.onTouchEvent(widget, buffer, event);
        }
    }

    public static class WrapContentViewPager extends ViewPager {

        private int mCurrentPagePosition = 0;

        public WrapContentViewPager(Context context) {
            super(context);
        }

        public WrapContentViewPager(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            try {
                boolean wrapHeight = MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST;
                if(wrapHeight) {
                    View child = getChildAt(mCurrentPagePosition);
                    if(child != null) {
                        child.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                        int h = child.getMeasuredHeight();

                        heightMeasureSpec = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                boolean wrapWidth = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST;
                if(wrapWidth) {
                    View child = getChildAt(mCurrentPagePosition);
                    if(child != null) {
                        child.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                        int h = child.getMeasuredWidth();

                        widthMeasureSpec = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        public void reMeasureCurrentPage(int position) {
            mCurrentPagePosition = position;
            requestLayout();
        }
    }

    public static class EditChordTextDialog extends DialogFragment {

        EditText editText;
        EditTextDialogViewModel editTextDialogViewModel;

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

            editTextDialogViewModel =
                    new ViewModelProvider(this).get(EditTextDialogViewModel.class);

            String chordText = getArguments() != null ? getArguments().getString("chordText") : "Error";

            if(editTextDialogViewModel.chordText != null)
                chordText = editTextDialogViewModel.chordText;

            // from showConfirmChordChartDialog()
            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            editText = (EditText) inflater.inflate(R.layout.confirm_chords_edit_text, null);
            editText.setText(chordText);
            editText.setBackgroundColor(PreferenceHelper.getColorScheme(requireContext()).getBackgroundColor(requireContext()));
            editText.setTextColor(PreferenceHelper.getColorScheme(requireContext()).getForegroundColor(requireContext()));

            //set AlertDialog theme according app theme
            int alertDialogTheme;
            if(PreferenceHelper.getColorScheme(requireContext()) == ColorScheme.Dark)
                alertDialogTheme = AlertDialog.THEME_DEVICE_DEFAULT_DARK;
            else
                alertDialogTheme = AlertDialog.THEME_DEVICE_DEFAULT_LIGHT;

            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), alertDialogTheme);
            builder.setTitle(R.string.edit_chords)
                    .setInverseBackgroundForced(true)
                    .setView(editText)
                    .setCancelable(true)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        String newChordText = editText.getText().toString();

                        Bundle args = new Bundle();
                        args.putString("NewChordText", newChordText);
                        getParentFragmentManager().setFragmentResult("EditChordTextDialog",args);
                    });

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(alertDialog.getWindow().getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.MATCH_PARENT;
            alertDialog.getWindow().setAttributes(lp);


            return alertDialog;
        }

        @Override
        public void onStop() {
            super.onStop();

            editTextDialogViewModel.chordText = editText.getText().toString();
        }

        public static class EditTextDialogViewModel extends ViewModel {
            protected String chordText;
        }
    }

}