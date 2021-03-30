package org.handmadeideas.chordreader.helper;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;
import android.widget.TextView;

import org.handmadeideas.chordreader.util.StringUtil;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoScrollView extends ScrollView {

    private OnFlingListener mFlingListener;
    private Runnable mScrollChecker;
    private int mPreviousPosition;
    private static ObjectAnimator animator = null;
    private TextView targetView;

    private boolean isAutoScrollOn = false;
    private boolean isAutoScrollActive = false;
    private boolean isFlingActive = false;
    private float scrollVelocityCorrectionFactor = 1;
    private double autoScrollVelocity;

    private static int bpm = 100;


    public AutoScrollView(Context context) {
        this(context, null, 0);
    }

    public AutoScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mScrollChecker = new Runnable() {
            @Override
            public void run() {
                int position = getScrollY();
                if (mPreviousPosition - position == 0) { //has stopped
                    mFlingListener.onFlingStopped();
                    removeCallbacks(mScrollChecker);
                } else { // check until stopped
                    mPreviousPosition = getScrollY();
                    postDelayed(mScrollChecker, 100);
                }
            }
        };
    }

    public interface OnFlingListener {
        void onFlingStarted();
        void onFlingStopped();
    }

    @Override
    public void fling(int velocityY) {
        super.fling(velocityY); // Pass through fling to parent

        if (mFlingListener != null) {
            mFlingListener.onFlingStarted();
            post(mScrollChecker);
        }
    }

    public void setOnFlingListener(OnFlingListener mOnFlingListener) {
        this.mFlingListener = mOnFlingListener;
    }

    public void setTargetTextView(TextView textView) {
        targetView = textView;
    }

    public boolean isAutoScrollOn() {
        return isAutoScrollOn;
    }

    public boolean isAutoScrollActive() {
        return isAutoScrollActive;
    }

    public boolean isFlingActive() {
        return isFlingActive;
    }

    public String getScrollVelocityCorrectionFactor() {
        return String.format(Locale.US,"%.1f", scrollVelocityCorrectionFactor);
    }

    public static int getBpm() {
        return bpm;
    }

    public void setAutoScrollOn(boolean autoScrollOn) {
        isAutoScrollOn = autoScrollOn;
    }

    public void setAutoScrollActive(boolean autoScrollActive) {
        isAutoScrollActive = autoScrollActive;
    }

    public void setFlingActive(boolean flingActive) {
        isFlingActive = flingActive;
    }

    public void setScrollVelocityCorrectionFactor(float scrollVelocityCorrectionFactor) {
        this.scrollVelocityCorrectionFactor = scrollVelocityCorrectionFactor;
    }

    public static void setBpm(int bpm) {
        AutoScrollView.bpm = bpm;
    }

    public void startAutoScroll() {
        int duration = (int) calculateAnimDurationFrom(this.getScrollY());
        if (animator == null) {
            animator = ObjectAnimator.ofInt(this, "scrollY", targetView.getBottom());
            animator.setInterpolator(null);
            animator.setDuration(duration);
        } else
            animator.setDuration(duration);

        animator.start();
        isAutoScrollActive = true;
    }

    public void stopAutoScroll() {
        if (animator != null) {
            animator.cancel();
            animator = null;
            isAutoScrollActive = false;
        }
    }

    public void resetAutoScrollView() {
        this.isAutoScrollOn = false;
        this.stopAutoScroll();
        this.scrollTo(0,0);
    }

    public void calculateAutoScrollVelocity() {
        int textViewLineCount = targetView.getLineCount();
        int totalBeatsPerSong = textViewLineCount / 2 * 16; // one text line (equals to 2 lines: with corresponding chord line) is assumed (in most cases) to have 4 bars = 16 bpm otherwise adjust factor is necessary
        long totalDurationPerSong = (totalBeatsPerSong * 60 * 1000) / bpm; //in milliseconds
        int totalScrollDistance = targetView.getHeight(); // height of view, in pixels
        autoScrollVelocity = (double) totalScrollDistance / totalDurationPerSong; // in pixels / millisecond
    }

    long calculateAnimDurationFrom(float startY) {
        double scrollDeltaY = targetView.getHeight() - (int)startY;
        return (long) ((scrollDeltaY / autoScrollVelocity)/ (scrollVelocityCorrectionFactor * 2)); // (scrollVelocityCorrectionFactor * 2) seems to result in better velocity changes
    }

    public void changeScrollVelocity(boolean acceleration) {
        if (!acceleration)
            if (scrollVelocityCorrectionFactor >= 0.2f)
                scrollVelocityCorrectionFactor -= 0.1f;
            else
                scrollVelocityCorrectionFactor = 0.1f;
        else
            scrollVelocityCorrectionFactor += 0.1f;

        calculateAutoScrollVelocity();

        if (isAutoScrollOn) {
            stopAutoScroll();
            startAutoScroll();
        }
    }


}