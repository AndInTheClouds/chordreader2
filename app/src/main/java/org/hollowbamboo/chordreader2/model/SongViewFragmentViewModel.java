package org.hollowbamboo.chordreader2.model;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.util.Log;

import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.hollowbamboo.chordreader2.chords.Chord;
import org.hollowbamboo.chordreader2.chords.NoteNaming;
import org.hollowbamboo.chordreader2.chords.regex.ChordInText;
import org.hollowbamboo.chordreader2.chords.regex.ChordParser;
import org.hollowbamboo.chordreader2.db.Transposition;
import org.hollowbamboo.chordreader2.helper.TransposeHelper;
import org.hollowbamboo.chordreader2.util.InternalURLSpan;
import org.hollowbamboo.chordreader2.util.Pair;
import org.hollowbamboo.chordreader2.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SongViewFragmentViewModel extends ViewModel {

    private static final String LOG_TAG = "SongViewFragmentVM";

    public String filename, chordText;

    public List<ChordInText> chordsInText;
    public int capoFret, transposeHalfSteps = 0;
    private NoteNaming noteNaming;
    private Integer bpm;
    private int linkColor;
    private float scrollVelocityCorrectionFactor;
    private FragmentResultListener fragmentResultListener;

    public boolean isEditedTextToSave = false;
    public boolean autoSave = false;

    private final MutableLiveData<String> fragmentTitle = new MutableLiveData<>();
    private final MutableLiveData<Spannable> chordTextMLD = new MutableLiveData<>();
    private final MutableLiveData<Float> textSizeMLD = new MutableLiveData<>();
    private final MutableLiveData<Chord> showChordPopupMLD = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showTranspositionProgressMLD = new MutableLiveData<>();
    private final MutableLiveData<Integer> bpmMLD = new MutableLiveData<>();
    private final MutableLiveData<Float> scrollVelocityCorrFactorMLD = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveResultMLD = new MutableLiveData<>();

    public SongViewFragmentViewModel() {
    }

    public MutableLiveData<Float> getTextSize() {
        return textSizeMLD;
    }

    public MutableLiveData<String> getFragmentTitle() {
        return fragmentTitle;
    }

    public MutableLiveData<Spannable> getChordTextMLD() {
        return chordTextMLD;
    }

    public MutableLiveData<Chord> getShowChordPopupMLD() {
        return showChordPopupMLD;
    }

    public MutableLiveData<Boolean> getShowTranspositionProgressMLD() {
        return showTranspositionProgressMLD;
    }

    public MutableLiveData<Integer> getBpmMLD() {
        return bpmMLD;
    }

    public MutableLiveData<Float> getScrollVelocityCorrFactorMLD() {
        return scrollVelocityCorrFactorMLD;
    }

    public MutableLiveData<Boolean> getSaveResultMLD() {
        return saveResultMLD;
    }

    public void setBpm(int bpm) {
        this.bpm = bpm;
    }

    public void setSongTitle(String songTitle) {
        fragmentTitle.postValue(songTitle);
    }

    public void setChordText(String chordText, Transposition transposition) {
        if (chordText != null)
            this.chordText = chordText;

        // BPMs and AutoScrollSpeed
        if (bpm < 0)
            bpm = (int) extractAutoScrollParam(chordText, "bpm");
        bpm = bpm < 0 ? 100 : bpm;

        scrollVelocityCorrectionFactor = extractAutoScrollParam(chordText, "scrollVelocityCorrectionFactor");
        scrollVelocityCorrectionFactor = scrollVelocityCorrectionFactor < 0 ? 1 : scrollVelocityCorrectionFactor;

        bpmMLD.postValue(bpm);
        scrollVelocityCorrFactorMLD.postValue(scrollVelocityCorrectionFactor);

        checkAndAddAutoScrollParams();

        parseChordsInText();

        Log.d(LOG_TAG, "found " + chordsInText.size() + " chords");

        // Transposition
        if (transposition == null) { // try to extract (capo param) from file
            int capoParam = findCapoParamInText(chordText).getSecond();

            Transposition newTransposition = new Transposition();
            newTransposition.setCapo(capoParam);
            newTransposition.setTranspose(0);

            transposition = newTransposition;
        } else {
            // save Capo changes to file, migrate from DB
            isEditedTextToSave = true;
        }

        setTransposition(transposition);

        showChordView();
    }

    public void setNoteNaming(NoteNaming noteNaming) {
        this.noteNaming = noteNaming;
    }

    public NoteNaming getNoteNaming() {
        return this.noteNaming;
    }

    public void setLinkColor(int linkColor) {
        this.linkColor = linkColor;
    }

    public void setTransposition(Transposition transposition) {

        if (transposition != null) {

            //Transposition directly stored in text, therefore transposed chords have to be updated,
            // stored chordText and again parsed to keep their index positions
            if (transposition.getTranspose() != 0) {
                updateChordsInTextForTransposition(-transposition.getTranspose(), 0);

                this.chordText = String.valueOf(buildUpChordTextToDisplay());

                parseChordsInText();
            }

            // Capo initially kept in variable and only stored in text when save to file
            capoFret = transposition.getCapo();
        }
    }

    private void parseChordsInText() {
        chordsInText = null;
        chordsInText = ChordParser.findChordsInText(this.chordText, noteNaming);
    }

    public Spannable buildUpChordTextToDisplay() {

        // have to build up a new string, because some of the chords may have different string lengths
        // than in the original text (e.g. if they are transposed)
        int lastEndIndex = 0;

        StringBuilder sb = new StringBuilder();

        List<Pair<Integer, Integer>> newStartAndEndPositions =
                new ArrayList<>(chordsInText.size());

        for (ChordInText chordInText : chordsInText) {
            sb.append(chordText, lastEndIndex, chordInText.getStartIndex());

            String chordAsString = chordInText.getChord().toPrintableString(noteNaming);

            sb.append(chordAsString);

            newStartAndEndPositions.add(new Pair<>(
                    sb.length() - chordAsString.length(), sb.length()));

            lastEndIndex = chordInText.getEndIndex();
        }

        // append the last bit of text after the last chord
        sb.append(chordText.substring(lastEndIndex));

        Spannable spannable = new Spannable.Factory().newSpannable(sb.toString());

        // add a hyperlink to each chord
        for (int i = 0; i < newStartAndEndPositions.size(); i++) {

            Pair<Integer, Integer> newStartAndEndPosition = newStartAndEndPositions.get(i);

            final Chord chord = chordsInText.get(i).getChord();

            InternalURLSpan urlSpan = new InternalURLSpan(v -> showChordPopupMLD.postValue(chord)) {
                @Override
                public void updateDrawState(TextPaint ds) {
                    ds.setUnderlineText(false);
                    ds.setColor(linkColor);
                }
            };

            spannable.setSpan(urlSpan,
                    newStartAndEndPosition.getFirst(),
                    newStartAndEndPosition.getSecond(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannable;
    }

    public void showChordView() {

        // do in the background to avoid jankiness
        HandlerThread handlerThread = new HandlerThread("ShowChordViewHandlerThread");
        handlerThread.start();

        Handler uiThread = new Handler(Looper.getMainLooper());

        Handler asyncHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Spannable newText = (Spannable) msg.obj;

                chordTextMLD.postValue(newText);
                Runnable runnable = () -> chordTextMLD.postValue(newText);

                uiThread.post(runnable);
                handlerThread.quit();
            }
        };

        Runnable runnable = () -> {

            if (capoFret != 0) {
                updateChordsInTextForTransposition(0, -capoFret);
            }

            Message message = new Message();
            message.obj = buildUpChordTextToDisplay();

            asyncHandler.sendMessage(message);

        };
        asyncHandler.post(runnable);
    }

    public void updateChordsInTextForTransposition(int transposeDiff, int capoDiff) {

        for (ChordInText chordInText : chordsInText) {

            chordInText.setChord(TransposeHelper.transposeChord(
                    chordInText.getChord(), capoDiff, transposeDiff));
        }

    }

    private static float extractAutoScrollParam(String text, String autoScrollParam) {

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
            if (match != null) {
                match = match.replace(",", ".");
                return Float.parseFloat(match);
            }
        }
        return -1;
    }

    public void checkAndAddAutoScrollParams() {
        ArrayList<String> lines = new ArrayList<>();
        String firstLine = "";

        if (!(chordText == null)) {
            lines = new ArrayList<>(Arrays.asList(StringUtil.split(chordText
                    , "\n")));
            firstLine = lines.get(0).toLowerCase();
        }

        if (!(firstLine.contains("bpm") && firstLine.contains("autoscrollfactor"))) {
            lines.add(0, "*** " + bpm + " BPM - AutoScrollFactor: " + scrollVelocityCorrectionFactor + " ***");

            StringBuilder resultText = new StringBuilder();
            for (String line : lines) {
                resultText.append(line).append("\n");
            }
            chordText = resultText.toString();
        }
    }

    public Pair<String, Integer> findCapoParamInText(String chordText) {

        Pattern capoPattern = Pattern.compile("([K,C](apodaster|apo).?\\s?)(\\d{1,2})", Pattern.CASE_INSENSITIVE);

        if (!(chordText == null)) {
            Matcher matcher = capoPattern.matcher(chordText);
            if (matcher.find()) {
                String match = matcher.group(0);
                String capoValString = matcher.group(3);
                int capoVal = 0;

                if (capoValString != null)
                    capoVal = Integer.parseInt(capoValString);

                int startIndex = matcher.start();

                int startIndexFirstChord;

                try {
                    startIndexFirstChord = chordsInText.get(0).getStartIndex();
                } catch (Exception e) {
                    startIndexFirstChord = 9999999;
                }

                if (match != null && startIndex <= startIndexFirstChord) {

                    return new Pair<>(match, capoVal);
                }
            }
        }

        return new Pair<>("", 0);
    }

    public void replaceOrAddCapoParamToText(int newCapoPosition) {

        String newCapoText;

        if (newCapoPosition == 0)
            newCapoText = "";
        else
            newCapoText = "Capo: " + newCapoPosition;

        Pair<String, Integer> capoParam = findCapoParamInText(chordText);
        String oldCapoText = capoParam.getFirst();

        if (oldCapoText.equals("")) {
            ArrayList<String> lines = new ArrayList<>();

            if (!(chordText == null)) {
                lines = new ArrayList<>(Arrays.asList(StringUtil.split(chordText
                        , "\n")));
                if (lines.get(1) != "")
                    lines.add(1, "");

                lines.add(2, newCapoText);

                if (lines.get(3) != "")
                    lines.add(3, "");
            }

            StringBuilder resultText = new StringBuilder();
            for (String line : lines) {
                resultText.append(line).append("\n");
            }
            chordText = resultText.toString();
        } else {
            chordText = chordText.replace(oldCapoText, newCapoText);
        }
    }

    public FragmentResultListener getFragmentResultListener() {
        if (fragmentResultListener == null) {
            fragmentResultListener = (requestKey, result) -> {
                if (requestKey.equals("EditChordTextDialog")) {
                    String newChordText = result.getString("NewChordText");

                    setChordText(newChordText, null);
                    isEditedTextToSave = true;
                }
            };
        }

        return fragmentResultListener;
    }
}