package com.slaviboy.gestures;

import android.graphics.PointF;
import android.os.Handler;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.Arrays;

/**
 * Copyright (c) 2019 Stanislav Georgiev. (MIT License)
 * https://github.com/slaviboy
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * - The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * - The Software is provided "as is", without warranty of any kind, express or
 * implied, including but not limited to the warranties of merchantability,
 * fitness for a particular purpose and noninfringement. In no event shall the
 * authors or copyright holders be liable for any claim, damages or other
 * liability, whether in an action of contract, tort or otherwise, arising from,
 * out of or in connection with the Software or the use or other dealings in the
 * Software.
 * <p>
 * <p>
 * Class that implements the OnTouchListener and can be used to detect finger state
 * changes for one or multiple fingers separately from one another. That way multi
 * fingers gestures can be formed, and used in games or other apps.
 */
public class GestureDetector implements View.OnTouchListener {

    // public finger states
    public static final int NONE = 0;
    public static final int SWIPE_UP = 1;
    public static final int SWIPE_DOWN = 2;
    public static final int SWIPE_LEFT = 3;
    public static final int SWIPE_RIGHT = 4;
    public static final int HOLD_DOWN = 5;
    public static final int DOWN = 6;
    public static final int UP = 7;
    public static final int MOVE_UP = 8;
    public static final int MOVE_DOWN = 9;
    public static final int MOVE_LEFT = 10;
    public static final int MOVE_RIGHT = 11;
    public static final int DOUBLE_TAP = 12;


    private int numberOfFingers;             // number of allowed fingers, that will be detected
    private int holdDownDelay;               // delay time after which if finger is -hold down, state will be changed to HOLD_DOWN
    private int upDelay;                     // delay time after which if finger is -swiped, state will be changed to UP
    private boolean consumeTouchEvents;      // whether to consume the touch event after handling
    private Finger[] fingers;                // array with finger object that detect the -finger state, -gesture time, ...
    private Handler handler;                 // handler to post runnable in queue for changing state for a finger
    private ChangeState[] runnables;         // array that holds runnable for each finger, for changing finger state after delay
    private OnGestureListener
            onGestureListener;              // listener used to call the onStateChange() method

    public GestureDetector() {
        this(2, 100, 50, true);
    }

    public GestureDetector(int numberOfFingers, int holdDownDelay, int upDelay,
                           boolean consumeTouchEvents) {
        handler = new Handler();

        this.numberOfFingers = numberOfFingers;
        this.holdDownDelay = holdDownDelay;
        this.upDelay = upDelay;
        this.consumeTouchEvents = consumeTouchEvents;

        setNumberOfFingers(numberOfFingers);
    }


    /**
     * Called on ACTION_DOWN || ACTION_POINTER_DOWN events, to detect and set
     * new current state, for a certain finger. Remove delay callback for
     * up state and set new one for hold-down state, and finally call listener method.
     *
     * @param event        motion event from the onTouch event
     * @param arrayIndex   index corresponding to consecutive finger on screen
     * @param pointerIndex pointer index used, to get current finger position
     */
    private void down(MotionEvent event, int arrayIndex, int pointerIndex) {

        // set finger state and tracking
        fingers[arrayIndex].setTracking(true);
        fingers[arrayIndex].detectState(event, pointerIndex);

        // remove callback for change current state to -up
        handler.removeCallbacks(runnables[arrayIndex + fingers.length]);

        // post a delay callback for detecting hold-down state
        handler.postDelayed(runnables[arrayIndex], holdDownDelay);

        // call listener method, fot state change
        onGestureListener.onStateChange(fingers, arrayIndex);
    }


    /**
     * Called on ACTION_UP || ACTION_POINTER_UP events, to detect and set
     * new current state, for a certain finger. Remove delay callback for
     * hold-down state and set new one for up state, and finally call listener method.
     *
     * @param event        motion event from the onTouch event
     * @param arrayIndex   index corresponding to consecutive finger on screen
     * @param pointerIndex pointer index used, to get current finger position
     */
    private void up(MotionEvent event, int arrayIndex, int pointerIndex) {

        // set finger state and tracking
        fingers[arrayIndex].setTracking(false);
        fingers[arrayIndex].detectState(event, pointerIndex);

        // set callback for up state, if swipe or double tap event is made!!!
        int state = fingers[arrayIndex].getStateCurrent();
        if (state == SWIPE_DOWN || state == SWIPE_LEFT || state == SWIPE_RIGHT ||
                state == SWIPE_UP || state == DOUBLE_TAP
        ) {
            handler.postDelayed(runnables[arrayIndex + fingers.length], upDelay);
        }

        // remove listener callback for hold-down state
        handler.removeCallbacks(runnables[arrayIndex]);

        // call listener method for state change
        onGestureListener.onStateChange(fingers, arrayIndex);
    }

    /**
     * Called on ACTION_MOVE event, to detect and set new current state,
     * for all finger that are being tracked. Remove delay callback for
     * hold-down state and finally call listener method.
     *
     * @param event motion event from the onTouch event
     */
    public void move(MotionEvent event) {

        int num = event.getPointerCount();
        for (int pointerIndex = 0; pointerIndex < num; pointerIndex++) {
            int arrayIndex = event.getPointerId(pointerIndex);  // id corresponding to array index

            // if it is being tracked
            if (arrayIndex < fingers.length && fingers[arrayIndex].isTracking()) {

                // get last and current state
                fingers[arrayIndex].detectState(event, pointerIndex);

                // call only if state is changed
                if (fingers[arrayIndex].getStateLast() != fingers[arrayIndex].getStateCurrent()) {
                    onGestureListener.onStateChange(fingers, arrayIndex);
                }

                // if actual move is made reset the callback for the hold-down
                if (fingers[arrayIndex].isUpdateLast()) {
                    handler.removeCallbacks(runnables[arrayIndex]);
                    handler.postDelayed(runnables[arrayIndex], holdDownDelay);
                }
            }
        }

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        int pointerIndex = event.getActionIndex();
        int arrayIndex = event.getPointerId(pointerIndex); // corresponds to array index, since it is const.

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                if (arrayIndex < fingers.length) {
                    down(event, arrayIndex, pointerIndex);
                }
                return consumeTouchEvents;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                if (arrayIndex < fingers.length) {
                    up(event, arrayIndex, pointerIndex);
                }
                return consumeTouchEvents;
            }
            case MotionEvent.ACTION_MOVE: {
                move(event);
                return consumeTouchEvents;
            }
        }
        return consumeTouchEvents;
    }

    /**
     * Set allowed number of fingers, create finger objects and runnable
     * for delay callbacks for state changes.
     *
     * @param numberOfFingers
     */
    public void setNumberOfFingers(int numberOfFingers) {
        this.numberOfFingers = numberOfFingers;

        // remove all callbacks
        handler.removeCallbacks(null);

        fingers = new Finger[numberOfFingers];
        runnables = new ChangeState[numberOfFingers * 2]; // set twice as many runnables, as the number of allowed fingers, for 4-Runnable => 2-HoldDown + 2-Up

        // init finger objects and runnable
        for (int i = 0; i < numberOfFingers; i++) {

            fingers[i] = new Finger();

            // runnable for changing finger state to -HOLD_DOWN or -UP
            runnables[i] = new ChangeState(i, HOLD_DOWN);            // first half for hold down
            runnables[i + numberOfFingers] = new ChangeState(i, UP); // second half for up
        }
    }

    public int getNumberOfFingers() {
        return numberOfFingers;
    }

    public void setOnGestureListener(OnGestureListener onGestureListener) {
        this.onGestureListener = onGestureListener;
    }

    public int getHoldDownDelay() {
        return holdDownDelay;
    }

    public void setHoldDownDelay(int holdDownDelay) {
        this.holdDownDelay = holdDownDelay;

        // remove all callbacks
        handler.removeCallbacks(null);
    }

    public int getUpDelay() {
        return upDelay;
    }

    public void setUpDelay(int upDelay) {
        this.upDelay = upDelay;

        // remove all callbacks
        handler.removeCallbacks(null);
    }

    public boolean isConsumeTouchEvents() {
        return consumeTouchEvents;
    }

    public void setConsumeTouchEvents(boolean consumeTouchEvents) {
        this.consumeTouchEvents = consumeTouchEvents;
    }

    /**
     * Runnable implementation class that hold the array index, and the new state
     * for the corresponding finger. Runnable is set for delay callback, for changing
     * finger state, whenever a state should be changed after a delay timeout.
     */
    class ChangeState implements Runnable {

        private int arrayIndex;
        private int newState;

        ChangeState(int arrayIndex, int newState) {
            this.arrayIndex = arrayIndex;
            this.newState = newState;
        }

        public void run() {

            // set last state, before changing current
            fingers[arrayIndex].setStateLast(fingers[arrayIndex].getStateCurrent());
            fingers[arrayIndex].setStateCurrent(newState);
            onGestureListener.onStateChange(fingers, arrayIndex);
        }
    }

    /**
     * Public interface with one method, that can be implemented and listen for
     * fingers state changes.
     */
    public interface OnGestureListener {

        /**
         * Called when finger state is changed, first arguments hold the array with
         * all finger objects, second argument is the finger index showing which
         * finger has a state change.
         *
         * @param fingers     - array with all finger objects
         * @param fingerIndex - finger index whose state is changed
         */
        void onStateChange(Finger[] fingers, int fingerIndex);
    }
}
