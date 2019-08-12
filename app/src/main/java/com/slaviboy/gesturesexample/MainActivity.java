package com.slaviboy.gesturesexample;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.slaviboy.fingerdraw.Canvas;
import com.slaviboy.fingerdraw.Circle;
import com.slaviboy.fingerdraw.Path;
import com.slaviboy.gestures.Finger;
import com.slaviboy.gestures.GestureDetector;

import static com.slaviboy.gesturesexample.Base.hideSystemUI;

public class MainActivity extends AppCompatActivity implements
        GestureDetector.OnGestureListener, View.OnClickListener,
        View.OnTouchListener {

    private TextView fingerOneValue;
    private TextView fingerTwoValue;
    private GestureDetector detector;
    private Canvas canvas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // set finger draw - paths and circles
        canvas = findViewById(R.id.canvas);
        canvas.setPaths(new Path[]{
                new Path.Builder().withColor(Color.WHITE).build(),
                new Path.Builder().withColor(Color.WHITE).build()
        });
        canvas.setCircles(new Circle[]{
                new Circle.Builder().withFillColor(Color.WHITE).withOpacity(122).build(),
                new Circle.Builder().withFillColor(Color.WHITE).withOpacity(122).build()
        });
        canvas.setOnTouchListener(this);


        // init multi finger gestures detector
        detector = new GestureDetector();
        detector.setOnGestureListener(this);

        setTypeFace();
    }

    /**
     * Set typeface for textViews, different typeface for
     * labels and values.
     */
    private void setTypeFace() {

        Typeface sourceSans = Typeface.createFromAsset(getAssets(), "fonts/source-sans-pro/SourceSansPro-ExtraLight.otf");
        Typeface komica = Typeface.createFromAsset(getAssets(), "fonts/KOMIKAX.ttf");
        Typeface burbank = Typeface.createFromAsset(getAssets(), "fonts/BurbankBigRegular-Bold.otf");


        TextView fingerOneLabel = findViewById(R.id.finger_one_label);
        fingerOneLabel.setTypeface(sourceSans);

        fingerOneValue = findViewById(R.id.finger_one_value);
        fingerOneValue.setTypeface(komica);

        TextView fingerTwoLabel = findViewById(R.id.finger_two_label);
        fingerTwoLabel.setTypeface(sourceSans);

        fingerTwoValue = findViewById(R.id.finger_two_value);
        fingerTwoValue.setTypeface(komica);


        TextView mainLabel = findViewById(R.id.main_label);
        mainLabel.setTypeface(burbank);
    }


    @Override
    public void onStateChange(Finger[] fingers, int fingerIndex) {

        // set the new finger states to corresponding textView
        String[] statesStr = new String[fingers.length];
        for (int i = 0; i < fingers.length; i++) {

            String stateStr;
            int state = fingers[i].getStateCurrent();
            int prevState = fingers[i].getStateLast();
            if ((state == GestureDetector.UP &&
                    prevState != GestureDetector.HOLD_DOWN &&
                    prevState != GestureDetector.DOWN)) {
                // to be able to show swipe event, since HOLD_DOWN and UP event are shown after that
                stateStr = fingers[i].getLastStateAsString();
            } else {
                stateStr = fingers[i].getCurrentStateAsString();
            }
            statesStr[i] = " " + stateStr + " ";
        }

        fingerOneValue.setText(statesStr[0]);
        fingerTwoValue.setText(statesStr[1]);
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {

        // for canvas finger draw visualization
        canvas.onTouch(v, event);

        return detector.onTouch(v, event);
    }

    @Override
    public void onClick(View v) {
        v.requestFocus();
        hideSystemUI((Activity) v.getContext());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        canvas.stop();
    }
}
