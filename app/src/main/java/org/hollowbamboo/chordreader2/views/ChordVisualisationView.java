package org.hollowbamboo.chordreader2.views;

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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class ChordVisualisationView extends View {

    private String chord;
    private String[] fretPositions;
    private int minFretPos = 100;
    private int minSecondFretPos = 100;

    private String chordType = "";

    private Paint paint;
    private Rect rectNut;
    private RectF rectFStringHighE;
    private RectF rectFStringH;
    private RectF rectFStringG;
    private RectF rectFStringD;
    private RectF rectFStringA;
    private RectF rectFStringLowE;
    private Rect rectBarre;

    private final int desiredViewWidth = dpToPx(150);
    private final int desiredViewHeight = dpToPx(100);

    private final int lineStartX = dpToPx(30);
    private final int lineStopX = dpToPx(175);
    private final int lineSpacingX = dpToPx(24);

    private final int lineStartY = dpToPx(8);
    private final int lineStopY = dpToPx(83);
    private final int lineSpacingY = dpToPx(15);

    private int XPosHighE, XPosH, XPosG, XPosD, XPosA, XPosLowE;
    private int YPosHighE, YPosH, YPosG, YPosD, YPosA, YPosLowE;

    private final int fingerPosOvalXRadius = lineSpacingX / 4;
    private final int fingerPosOvalYRadius = lineSpacingY / 3;

    private final int textShiftX = dpToPx(-7);
    private final int textShiftY = dpToPx(5);
    private final int textSize = dpToPx(15);

    private int fretPosHighE;
    private int fretPosH;
    private int fretPosG;
    private int fretPosD;
    private int fretPosA;
    private int fretPosLowE;


    public ChordVisualisationView(Context context) {
        super(context);
        init(null);
    }

    public ChordVisualisationView(String chord, Context context) {
        super(context);

        init(chord);
    }

    public ChordVisualisationView(String chord, Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init(chord);
    }

    public ChordVisualisationView(String chord, Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(chord);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ChordVisualisationView(String chord, Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        init(chord);
    }

    private void init(String chord) {

        this.chord = chord;
        parseChord();
        checkChordForBarre();
        calculatePixelPosOfFingerPos();

        rectNut = new Rect();
        rectBarre = new Rect();
        rectFStringHighE = new RectF();
        rectFStringH = new RectF();
        rectFStringG = new RectF();
        rectFStringD = new RectF();
        rectFStringA = new RectF();
        rectFStringLowE = new RectF();

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.RIGHT);
    }

    private void parseChord() {
        fretPositions = chord.split("-");

        int[] fretPositionsInt = new int[6];

        for (int i = 0; i < fretPositions.length; i++) {
            fretPositionsInt[i] = parseFretPosNumber(fretPositions[i]);
            if(fretPositionsInt[i] < minFretPos && fretPositionsInt[i] >= 0)
                minFretPos = fretPositionsInt[i];

            if(fretPositionsInt[i] > minFretPos &&
                    fretPositionsInt[i] < minSecondFretPos )
                minSecondFretPos = fretPositionsInt[i];
        }

        for (int j : fretPositionsInt) {
            if(j > minFretPos && j < minSecondFretPos)
                minSecondFretPos = j;
        }

        fretPosLowE = fretPositionsInt[0];
        fretPosA = fretPositionsInt[1];
        fretPosD = fretPositionsInt[2];
        fretPosG = fretPositionsInt[3];
        fretPosH = fretPositionsInt[4];
        fretPosHighE = fretPositionsInt[5];

    }

    private int parseFretPosNumber(String fretPosNumber) {
        try {
            return Integer.parseInt(fretPosNumber);
        } catch (NumberFormatException e){
            return -1;
        }
    }

    private void checkChordForBarre(){

        if(fretPosLowE == fretPosHighE &&
                fretPosLowE == minFretPos &&
                fretPosLowE <= fretPosA &&
                fretPosLowE <= fretPosD &&
                fretPosLowE <= fretPosG &&
                fretPosLowE <= fretPosH &&
                minFretPos > 0)
        {
            chordType = "ChordStandardBarre";

            if(minFretPos > 2) {
                fretPosLowE = 2;
                fretPosA = fretPosA - minFretPos + 2;
                fretPosD = fretPosD - minFretPos + 2;
                fretPosG = fretPosG - minFretPos + 2;
                fretPosH = fretPosH - minFretPos + 2;
                fretPosHighE = 2;
            }
            Log.d("ChordVisuView","Chord: "+ chord + " FretPos EADGHE: "+fretPosLowE+"-"+fretPosA+"-"+fretPosD+"-"+fretPosG+"-"+fretPosH+"-"+fretPosHighE+" Barre: ChordBarre" + " MinFretPos: "+minFretPos+" MinSecond: "+minSecondFretPos);
        }

        else if(fretPosA == fretPosHighE &&
                fretPosLowE <= 0 &&
                fretPosD >= fretPosA &&
                fretPosG >= fretPosA &&
                fretPosH >= fretPosA &&
                fretPosA > 0)
        {
            chordType = "ChordBarreWithoutLowE";

            if(minFretPos > 2) {
                fretPosA = fretPosA - minFretPos + 2;
                fretPosD = fretPosD - minFretPos + 2;
                fretPosG = fretPosG - minFretPos + 2;
                fretPosH = fretPosH - minFretPos + 2;
                fretPosHighE = 2;
            }
            Log.d("ChordVisuView","Chord: "+ chord + " FretPos EADGHE: "+fretPosLowE+"-"+fretPosA+"-"+fretPosD+"-"+fretPosG+"-"+fretPosH+"-"+fretPosHighE+" Barre: ChordBarreWithoutLowE" + " MinFretPos: "+minFretPos+" MinSecond: "+minSecondFretPos);
        }

        else if(fretPosG == fretPosH &&
                fretPosG == fretPosHighE &&
                fretPosG == fretPosA &&
                fretPosG == fretPosLowE &&
                fretPosD == fretPosG - 1)
        {
            chordType = "ChordPinkyBarre";

            if(minFretPos > 2) {
                fretPosLowE = fretPosLowE - minFretPos + 2;
                fretPosA = fretPosA - minFretPos + 2;
                fretPosD = fretPosD - minFretPos + 2;
                fretPosG = fretPosG - minFretPos + 2;
                fretPosH = fretPosH - minFretPos + 2;
                fretPosHighE = fretPosHighE - minFretPos + 2;
            }
            Log.d("ChordVisuView","Chord: "+ chord + " FretPos EADGHE: "+fretPosLowE+"-"+fretPosA+"-"+fretPosD+"-"+fretPosG+"-"+fretPosH+"-"+fretPosHighE+" Barre: ChordPinkyBarre" + " MinFretPos: "+minFretPos+" MinSecond: "+minSecondFretPos);
        }

        else if(minFretPos <= 0 && minSecondFretPos >= 4) {
            chordType = "ChordWidespread";

            fretPosLowE = fretPosLowE >= 4 ? fretPosLowE - minSecondFretPos + 2 : fretPosLowE;
            fretPosA = fretPosA >= 4 ? fretPosA - minSecondFretPos + 2 : fretPosA;
            fretPosD = fretPosD >= 4 ? fretPosD - minSecondFretPos + 2 : fretPosD;
            fretPosG = fretPosG >= 4 ? fretPosG - minSecondFretPos + 2 : fretPosG;
            fretPosH = fretPosH >= 4 ? fretPosH - minSecondFretPos + 2 : fretPosH;
            fretPosHighE = fretPosHighE >= 4 ? fretPosHighE - minSecondFretPos + 2 : fretPosHighE;

            Log.d("ChordVisuView","Chord: "+ chord + " FretPos EADGHE: "+fretPosLowE+"-"+fretPosA+"-"+fretPosD+"-"+fretPosG+"-"+fretPosH+"-"+fretPosHighE+" Barre: ChordWidespread" + " MinFretPos: "+minFretPos+" MinSecond: "+minSecondFretPos);

        } else
            chordType = "NormalChord";


    }

    private void calculatePixelPosOfFingerPos() {

        XPosHighE = lineStartX + convertFretToPixelPos(fretPosHighE);
        XPosH = lineStartX + convertFretToPixelPos(fretPosH);
        XPosG = lineStartX + convertFretToPixelPos(fretPosG);
        XPosD = lineStartX + convertFretToPixelPos(fretPosD);
        XPosA = lineStartX + convertFretToPixelPos(fretPosA);
        XPosLowE = lineStartX + convertFretToPixelPos(fretPosLowE);

        YPosHighE = lineStartY;
        YPosH = lineStartY + lineSpacingY;
        YPosG = lineStartY + 2 * lineSpacingY;
        YPosD = lineStartY + 3 * lineSpacingY;
        YPosA = lineStartY + 4 * lineSpacingY;
        YPosLowE = lineStartY + 5 * lineSpacingY;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {


        int requestedWidth = MeasureSpec.getSize(widthMeasureSpec);
        int requestedWidthMode = MeasureSpec.getMode(widthMeasureSpec);

        int requestedHeight = MeasureSpec.getSize(heightMeasureSpec);
        int requestedHeightMode = MeasureSpec.getMode(heightMeasureSpec);

        int width;
        switch (requestedWidthMode) {

            case MeasureSpec.EXACTLY:
                width = requestedWidth;
                break;
            case MeasureSpec.UNSPECIFIED:
                width = desiredViewWidth;
                break;
            case MeasureSpec.AT_MOST:
                width = Math.min(requestedWidth, desiredViewWidth);
                break;

            default:
                throw new IllegalStateException("Unexpected value requestedWidthMode: " + requestedWidthMode);
        }

        int height;

        switch (requestedHeightMode) {

            case MeasureSpec.EXACTLY:
                height = requestedHeight;
                break;
            case MeasureSpec.UNSPECIFIED:
                height = desiredViewHeight;
                break;
            case MeasureSpec.AT_MOST:
                height = Math.min(requestedHeight, desiredViewHeight);
                break;
            default:
                throw new IllegalStateException("Unexpected value requestedHeightMode: " + requestedHeightMode);
        }

        setMeasuredDimension(width,height);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawLines(canvas);

        drawFingerPositions(canvas);

        drawLeadingFretNumbers(canvas);

        drawBarreBar(canvas);

        drawNut(canvas);

        }

    private void drawLines(Canvas canvas) {
        //horizontal lines = strings
        canvas.drawLine(lineStartX, lineStartY, lineStopX, lineStartY, paint);
        canvas.drawLine(lineStartX, lineStartY + lineSpacingY, lineStopX, lineStartY + lineSpacingY, paint);
        canvas.drawLine(lineStartX, lineStartY + 2 * lineSpacingY, lineStopX, lineStartY + 2 * lineSpacingY, paint);
        canvas.drawLine(lineStartX, lineStartY + 3 * lineSpacingY, lineStopX, lineStartY + 3 * lineSpacingY, paint);
        canvas.drawLine(lineStartX, lineStartY + 4 * lineSpacingY, lineStopX, lineStartY + 4 * lineSpacingY, paint);
        canvas.drawLine(lineStartX, lineStartY + 5 * lineSpacingY, lineStopX, lineStartY + 5 * lineSpacingY, paint);

        //vertical lines = frets
        canvas.drawLine(lineStartX + lineSpacingX, lineStartY, lineStartX + lineSpacingX, lineStopY, paint);
        canvas.drawLine(lineStartX + 2 * lineSpacingX, lineStartY, lineStartX + 2 * lineSpacingX, lineStopY, paint);
        canvas.drawLine(lineStartX + 3 * lineSpacingX, lineStartY, lineStartX + 3 * lineSpacingX, lineStopY, paint);
        canvas.drawLine(lineStartX + 4 * lineSpacingX, lineStartY, lineStartX + 4 * lineSpacingX, lineStopY, paint);
        canvas.drawLine(lineStartX + 5 * lineSpacingX, lineStartY, lineStartX + 5 * lineSpacingX, lineStopY, paint);
    }

    private void drawFingerPositions(Canvas canvas) {

        if(isFingerPosToDraw(fretPosHighE)) {
            rectFStringHighE.left = -fingerPosOvalXRadius + XPosHighE;
            rectFStringHighE.top = -fingerPosOvalYRadius + YPosHighE;
            rectFStringHighE.right = fingerPosOvalXRadius + XPosHighE;
            rectFStringHighE.bottom = fingerPosOvalYRadius + YPosHighE;

            canvas.drawOval(rectFStringHighE, paint);
        }

        if(isFingerPosToDraw(fretPosH)) {
            rectFStringH.left = -fingerPosOvalXRadius + XPosH;
            rectFStringH.top = -fingerPosOvalYRadius + YPosH;
            rectFStringH.right = fingerPosOvalXRadius + XPosH;
            rectFStringH.bottom = fingerPosOvalYRadius + YPosH;

            canvas.drawOval(rectFStringH, paint);
        }

        if(isFingerPosToDraw(fretPosG)) {
            rectFStringG.left = -fingerPosOvalXRadius + XPosG;
            rectFStringG.top = -fingerPosOvalYRadius + YPosG;
            rectFStringG.right = fingerPosOvalXRadius + XPosG;
            rectFStringG.bottom = fingerPosOvalYRadius + YPosG;

            canvas.drawOval(rectFStringG, paint);
        }

        if(isFingerPosToDraw(fretPosD)) {
            rectFStringD.left = -fingerPosOvalXRadius + XPosD;
            rectFStringD.top = -fingerPosOvalYRadius + YPosD;
            rectFStringD.right = fingerPosOvalXRadius + XPosD;
            rectFStringD.bottom = fingerPosOvalYRadius + YPosD;

            canvas.drawOval(rectFStringD, paint);
        }

        if(isFingerPosToDraw(fretPosA)) {
            rectFStringA.left = -fingerPosOvalXRadius + XPosA;
            rectFStringA.top = -fingerPosOvalYRadius + YPosA;
            rectFStringA.right = fingerPosOvalXRadius + XPosA;
            rectFStringA.bottom = fingerPosOvalYRadius + YPosA;

            canvas.drawOval(rectFStringA, paint);
        }

        if(isFingerPosToDraw(fretPosLowE)) {
            rectFStringLowE.left = -fingerPosOvalXRadius + XPosLowE;
            rectFStringLowE.top = -fingerPosOvalYRadius + YPosLowE;
            rectFStringLowE.right = fingerPosOvalXRadius + XPosLowE;
            rectFStringLowE.bottom = fingerPosOvalYRadius + YPosLowE;

            canvas.drawOval(rectFStringLowE, paint);
        }
    }

    private void drawLeadingFretNumbers(Canvas canvas) {

        canvas.drawText(fretPositions[0], lineStartX + textShiftX, YPosLowE + textShiftY, paint);
        canvas.drawText(fretPositions[1], lineStartX + textShiftX, YPosA + textShiftY, paint);
        canvas.drawText(fretPositions[2], lineStartX + textShiftX, YPosD + textShiftY, paint);
        canvas.drawText(fretPositions[3], lineStartX + textShiftX, YPosG + textShiftY, paint);
        canvas.drawText(fretPositions[4], lineStartX + textShiftX, YPosH + textShiftY, paint);
        canvas.drawText(fretPositions[5], lineStartX + textShiftX, YPosHighE + textShiftY, paint);
    }

    private void drawBarreBar(Canvas canvas) {

        switch (chordType) {
            case "ChordStandardBarre":
                rectBarre.left = -fingerPosOvalXRadius + XPosLowE;
                rectBarre.top = YPosHighE;
                rectBarre.right = fingerPosOvalXRadius + XPosLowE;
                rectBarre.bottom = YPosLowE;

                canvas.drawRect(rectBarre, paint);

                break;
            case "ChordBarreWithoutLowE":
                rectBarre.left = -fingerPosOvalXRadius + XPosHighE;
                rectBarre.top = YPosHighE;
                rectBarre.right = fingerPosOvalXRadius + XPosHighE;
                rectBarre.bottom = YPosA;

                canvas.drawRect(rectBarre, paint);

                break;
            case "ChordPinkyBarre":
                rectBarre.left = -fingerPosOvalXRadius + XPosHighE;
                rectBarre.top = YPosHighE;
                rectBarre.right = fingerPosOvalXRadius + XPosHighE;
                rectBarre.bottom = YPosG;

                canvas.drawRect(rectBarre, paint);
                break;
            default:
                //Log.d("ChordVisuView", "Normal Chord " + chord);
                break;
        }
    }

    private void drawNut(Canvas canvas) {

        if(minFretPos <= 2 || chordType.equals("NormalChord")) {

            rectNut.left = lineStartX - dpToPx(4);
            rectNut.top = lineStartY;
            rectNut.right = lineStartX;
            rectNut.bottom = lineStopY;

            canvas.drawRect(rectNut, paint);
        }
    }

    private static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    private int convertFretToPixelPos(int fretPos) {

        if(fretPos <= 0)
            return  -1;
        else
            return (lineSpacingX / 2) + lineSpacingX * (fretPos - 1);
    }

    private boolean isFingerPosToDraw(int fretPos) {
        return fretPos > 0;
    }

}
