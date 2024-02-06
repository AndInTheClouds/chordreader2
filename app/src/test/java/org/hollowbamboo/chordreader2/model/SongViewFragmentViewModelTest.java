package org.hollowbamboo.chordreader2.model;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hollowbamboo.chordreader2.chords.NoteNaming;
import org.hollowbamboo.chordreader2.db.Transposition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SongViewFragmentViewModelTest {


    private SongViewFragmentViewModel viewModel;
    @Before
    public void setUp() throws Exception {

        viewModel = new SongViewFragmentViewModel();
        viewModel.setNoteNaming(NoteNaming.valueOf("English"));
        viewModel.filename = "TestSong.txt";
        viewModel.setBpm(100);
    }

    @After
    public void tearDown() throws Exception {
    }


    @Test
    public void testTransposeFunction_ChangeTranspositionPlusOne() {

        String expectedResult = "*** 100 BPM - AutoScrollFactor: 1.0 ***\n" + "Bb C Db Eb F Gb Ab" + "\n";
        String initialChordText = "A B C D E F G";

        Transposition transposition = new Transposition();
        transposition.setCapo(0);
        transposition.setTranspose(1);

        viewModel.setChordText(initialChordText,transposition);

        String actualResult = viewModel.chordText;
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testTransposeFunction_ChangeTranspositionPlusTwo() {
        String expectedResult =  "*** 100 BPM - AutoScrollFactor: 1.0 ***\n" + "B Db D E Gb G A" + "\n";

        String initialChordText = "A B C D E F G";

        Transposition transposition = new Transposition();
        transposition.setCapo(0);
        transposition.setTranspose(2);

        viewModel.setChordText(initialChordText,transposition);

        String actualResult = viewModel.chordText;
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testTransposeFunction_ChangeTranspositionPlusThree() {
        String expectedResult = "*** 100 BPM - AutoScrollFactor: 1.0 ***\n" + "C D Eb F G Ab Bb" + "\n";

        String initialChordText = "A B C D E F G";

        Transposition transposition = new Transposition();
        transposition.setCapo(0);
        transposition.setTranspose(3);

        viewModel.setChordText(initialChordText,transposition);

        String actualResult = viewModel.chordText;
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testTransposeFunction_ChangeTranspositionPlusFour() {
        String expectedResult = "*** 100 BPM - AutoScrollFactor: 1.0 ***\n" + "Db Eb E Gb Ab A B" + "\n";

        String initialChordText = "A B C D E F G";

        Transposition transposition = new Transposition();
        transposition.setCapo(0);
        transposition.setTranspose(4);

        viewModel.setChordText(initialChordText,transposition);


        String actualResult = viewModel.chordText;
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testTransposeFunction_ChangeTranspositionPlusFive() {
        String expectedResult = "*** 100 BPM - AutoScrollFactor: 1.0 ***\n" + "D E F G A Bb C" + "\n";

        String initialChordText = "A B C D E F G";

        Transposition transposition = new Transposition();
        transposition.setCapo(0);
        transposition.setTranspose(5);

        viewModel.setChordText(initialChordText,transposition);

        String actualResult = viewModel.chordText;
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testTransposeFunction_ChangeTranspositionPlusSix() {
        String expectedResult = "*** 100 BPM - AutoScrollFactor: 1.0 ***\n" + "Eb F Gb Ab Bb B Db" + "\n";

        String initialChordText = "A B C D E F G";

        Transposition transposition = new Transposition();
        transposition.setCapo(0);
        transposition.setTranspose(6);

        viewModel.setChordText(initialChordText,transposition);

        String actualResult = viewModel.chordText;
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testTransposeFunction_ChangeTranspositionMinusOne() {
        String expectedResult = "*** 100 BPM - AutoScrollFactor: 1.0 ***\n" + "Ab Bb B Db Eb E Gb" + "\n";

        String initialChordText = "A B C D E F G";

        Transposition transposition = new Transposition();
        transposition.setCapo(0);
        transposition.setTranspose(-1);

        viewModel.setChordText(initialChordText,transposition);

        String actualResult = viewModel.chordText;
        assertEquals(expectedResult, actualResult);
    }

}