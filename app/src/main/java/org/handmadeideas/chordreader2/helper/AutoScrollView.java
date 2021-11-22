package org.handmadeideas.chordreader2.helper;

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

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

public class AutoScrollView extends ScrollView {

    private OnFlingListener mFlingListener;
    private final Runnable mScrollChecker;
    private OnActiveAutoScrollListener mActiveAutoScrollListener;
    private final Runnable mActiveAutoScrollChecker;

    private int mPreviousFlingPosition;
    private int mPreviousAutoScrollPosition;
    private static ObjectAnimator animator = null;
    private TextView targetView;

    private boolean isAutoScrollOn = false;
    private boolean isAutoScrollActive = false;

    private boolean isTouched = false;
    private boolean isFlingActive = false;
    private float scrollVelocityCorrectionFactor = 1;
    private double autoScrollVelocity;

    private  int bpm = 100;


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
                if (mPreviousFlingPosition - position == 0) { //has stopped
                    mFlingListener.onFlingStopped();
                    removeCallbacks(mScrollChecker);
                } else { // check until stopped
                    mPreviousFlingPosition = getScrollY();
                    postDelayed(mScrollChecker, 100);
                }
            }
        };

        mActiveAutoScrollChecker = new Runnable() {
            @Override
            public void run() {
                int position = getScrollY();
                if (mPreviousAutoScrollPosition - position == 0) { //has stopped
                    mActiveAutoScrollListener.onAutoScrollInactive();
                    removeCallbacks(mActiveAutoScrollChecker);
                } else { // check until stopped
                    mPreviousAutoScrollPosition = getScrollY();
                    postDelayed(mActiveAutoScrollChecker, 1000);
                }
            }
        };

        this.setOnFlingListener(new AutoScrollView.OnFlingListener() {

            @Override
            public void onFlingStarted() {
                AutoScrollView.this.setFlingActive(true);
            }

            @Override
            public void onFlingStopped() {
                AutoScrollView.this.setFlingActive(false);
                AutoScrollView.this.postDelayed(new Runnable() { //wait a moment to check if a fling was caused
                    @Override
                    public void run() {
                        if (!isTouched && AutoScrollView.this.isAutoScrollOn() && !AutoScrollView.this.isAutoScrollActive()) {
                            AutoScrollView.this.startAutoScroll();
                        }
                    }
                }, 100);
            }
        });
    }

    public interface OnFlingListener {
        void onFlingStarted();
        void onFlingStopped();
    }

    public interface OnActiveAutoScrollListener {
        void onAutoScrollActive();
        void onAutoScrollInactive();
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
        mFlingListener = mOnFlingListener;
    }

    public void setOnActiveAutoScrollListener(OnActiveAutoScrollListener mActiveAutoScrollListener) {
        this.mActiveAutoScrollListener = mActiveAutoScrollListener;
    }

    public void setTargetTextView(TextView textView) {
        targetView = textView;
    }

    public void setTouched(boolean touched) {
        this.isTouched = touched;
    }

    public void setAutoScrollOn(boolean autoScrollOn) {
        this.isAutoScrollOn = autoScrollOn;
    }

    public boolean isAutoScrollOn() {
        return this.isAutoScrollOn;
    }

    public boolean isAutoScrollActive() {
        return this.isAutoScrollActive;
    }

    public void setFlingActive(boolean flingActive) {
        this.isFlingActive = flingActive;
    }

    public boolean isFlingActive() {
        return this.isFlingActive;
    }

    public String getScrollVelocityCorrectionFactor() {
        return String.format(Locale.US,"%.1f", this.scrollVelocityCorrectionFactor);
    }

    public void setBpm(int bpm) {
        this.bpm = bpm;
    }

    public int getBpm() {
        return this.bpm;
    }

    public void setScrollVelocityCorrectionFactor(float scrollVelocityCorrectionFactor) {
        this.scrollVelocityCorrectionFactor = scrollVelocityCorrectionFactor;
    }

    public void startAutoScroll() {
        if (animator != null) {
            animator.cancel();
        }

        animator = ObjectAnimator.ofInt(this, "scrollY", targetView.getBottom());
        animator.setInterpolator(null);

        int duration = (int) calculateAnimDurationFrom(this.getScrollY());
        animator.setDuration(duration);
        animator.start();

        this.isAutoScrollActive = true;
        mPreviousAutoScrollPosition = 99999;

        if (mActiveAutoScrollListener != null) {
            mActiveAutoScrollListener.onAutoScrollActive();
            post(mActiveAutoScrollChecker);
        }
    }

    public void stopAutoScroll() {
        if (animator != null) {
            animator.cancel();
            animator = null;

            this.isAutoScrollActive = false;
        }
    }

    public void calculateAutoScrollVelocity() {
        int textViewLineCount = targetView.getLineCount();
        int totalBeatsPerSong = textViewLineCount / 2 * 16; // one text line (equals to 2 lines: with corresponding chord line) is assumed (in most cases) to have 4 bars = 16 bpm otherwise adjust factor is necessary
        long totalDurationPerSong = ((long) totalBeatsPerSong * 60 * 1000) / this.bpm; //in milliseconds
        int totalScrollDistance = targetView.getHeight(); // height of view, in pixels
        this.autoScrollVelocity = (double) totalScrollDistance / totalDurationPerSong; // in pixels / millisecond
    }

    long calculateAnimDurationFrom(float startY) {
        double scrollDeltaY = targetView.getHeight() - (int)startY;
        return (long) ((scrollDeltaY / this.autoScrollVelocity)/ (this.scrollVelocityCorrectionFactor * 2)); // (scrollVelocityCorrectionFactor * 2) seems to result in better velocity changes
    }

    public void changeScrollVelocity(boolean acceleration) {
        if (!acceleration)
            if (this.scrollVelocityCorrectionFactor >= 0.2f)
                this.scrollVelocityCorrectionFactor -= 0.1f;
            else
                this.scrollVelocityCorrectionFactor = 0.1f;
        else
            this.scrollVelocityCorrectionFactor += 0.1f;

        calculateAutoScrollVelocity();

        if (this.isAutoScrollOn) {
            stopAutoScroll();
            startAutoScroll();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {

        final int action = event.getAction();

        if (action == MotionEvent.ACTION_DOWN) {

            this.isTouched = true;
            if (this.isAutoScrollOn()) {
                this.stopAutoScroll();
            }
//            return true;
        }
        super.onTouchEvent(event);
        return false;
    }

    @Override
    public boolean onTouchEvent (MotionEvent event) {

        final int action = event.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            this.isTouched = true;
            if (this.isAutoScrollOn()) {
                this.stopAutoScroll();
            }
            super.onTouchEvent(event);
           return true;
        }

        if (action == MotionEvent.ACTION_MOVE) {
            super.onTouchEvent(event);
        }

        if (action == MotionEvent.ACTION_UP) {
            this.isTouched = false;
            if (this.isAutoScrollOn()) {
                this.postDelayed(new Runnable() { //wait a moment to check if a fling was caused
                    @Override
                    public void run() {
                        if (!AutoScrollView.this.isFlingActive() && AutoScrollView.this.isAutoScrollOn() && !AutoScrollView.this.isAutoScrollActive()) {
                            AutoScrollView.this.startAutoScroll();
                        }
                    }
                }, 100);
            }
            super.onTouchEvent(event);
        }
        return false;
    }




}