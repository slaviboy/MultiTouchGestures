package com.slaviboy.gestures;

import android.graphics.PointF;
import android.os.SystemClock;
import android.view.MotionEvent;

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
 * Class that determines current finger state and contains properties
 * like gesture -distance, gesture -duration, finger position and many more.
 * And is used with GestureDetector class, to detect state changes for
 * both fingers, separately from one another. That way it is easy to detect
 * one finger gesture or combine gestures from both fingers and create
 * complex two finger gestures that can be used in 2D or 3D games.
 */
public class Finger {

    // public default constants
    public static final int MIN_DISTANCE_SWIPE = 10;
    public static final int MAX_DURATION_SWIPE = 400;
    public static final int MIN_DISTANCE_MOVE = 30;
    public static final int MAX_DURATION_DOUBLE_TAP = 250;
    public static final int MAX_DOWN_DOUBLE_TAP = 100;
    public static final int SLOPE_INTOLERANCE = 1;

    private int stateCurrent;                   // current finger state - down, up, swipe_left...
    private int stateLast;                      // previous finger state

    private double slopeIntolerance;            // slope intolerance for swipe and move

    private int minDistanceSwipe;               // min distance the finger must travel, before swipe event can be detected
    private int maxDurationSwipe;               // max time after which the swipe event WILL NOT be detected (ms)

    private int minDistanceMove;                // min distance the finger must travel, before move event can be detected

    private int maxDurationDoubleTap;           // max delay time between the two -down events for the double tap (ms)
    private int maxDownDoubleTap;               // max time the finger can be hold down for the two -down events (ms)

    // gesture distance
    private double distanceInitial;              // distance between -positionCurrent and -positionInitial
    private double distanceLast;                 // distance between -positionCurrent and -positionLast

    // gesture duration
    private double durationInitial;              // duration between -ACTION_DOWN and current event
    private double durationLast;                 // duration between the previous and current event

    // difference (decrease) between two values
    private PointF positionDeltaInitial;         // difference between -positionCurrent and -positionInitial
    private PointF positionDeltaLast;            // difference between -positionCurrent and -positionLast

    // finger position
    private PointF positionInitial;               // initial finger position when it is pressed down for -ACTION_DOWN event
    private PointF positionLast;                  // previous finger position from -ACTION_UP, ACTION_MOVE events
    private PointF positionCurrent;               // current finger position from  -ACTION_UP, ACTION_MOVE events

    // detected system time
    private long timeInitial;                     // initial time when the finger is pressed down from -ACTION_DOWN event
    private long timeLast;                        // previously detected time from -ACTION_UP, ACTION_MOVE events
    private long timeCurrent;                     // current detected time from    -ACTION_UP, ACTION_MOVE events

    private boolean tracking;                     // if finger is tracked set from -ACTION_UP(false) and -ACTION_DOWN(true) events
    private boolean updateLast;                   // if -positionLast should be updated from current event

    private Finger lastFinger;                    // last finger object from previous event, used to detect double tap

    public Finger() {

        // set default
        this(MIN_DISTANCE_SWIPE, MAX_DURATION_SWIPE, MIN_DISTANCE_MOVE,
                MAX_DURATION_DOUBLE_TAP, MAX_DOWN_DOUBLE_TAP, SLOPE_INTOLERANCE);
    }

    public Finger(int minDistanceSwipe, int maxDurationSwipe, int minDistanceMove,
                  int maxDurationDoubleTap, int maxDownDoubleTap, int slopeIntolerance) {

        this(minDistanceSwipe, maxDurationSwipe, minDistanceMove,
                maxDurationDoubleTap, maxDownDoubleTap, slopeIntolerance, GestureDetector.NONE,
                GestureDetector.NONE, 0, 0, 0, 0,
                0, 0, 0, false, true, new PointF(), new PointF(),
                new PointF(), new PointF(), new PointF());
    }

    public Finger(int minDistanceSwipe, int maxDurationSwipe, int minDistanceMove, int maxDurationDoubleTap,
                  int maxDownDoubleTap, double slopeIntolerance, int stateCurrent, int stateLast, double distanceInitial,
                  double distanceLast, double durationInitial, double durationLast, long timeInitial, long timeLast,
                  long timeCurrent, boolean tracking, boolean updateLast, PointF positionDeltaInitial,
                  PointF positionDeltaLast, PointF positionInitial, PointF positionLast, PointF positionCurrent) {

        this.stateCurrent = stateCurrent;
        this.stateLast = stateLast;
        this.slopeIntolerance = slopeIntolerance;
        this.minDistanceSwipe = minDistanceSwipe;
        this.maxDurationSwipe = maxDurationSwipe;
        this.minDistanceMove = minDistanceMove;
        this.maxDurationDoubleTap = maxDurationDoubleTap;
        this.maxDownDoubleTap = maxDownDoubleTap;
        this.distanceInitial = distanceInitial;
        this.distanceLast = distanceLast;
        this.durationInitial = durationInitial;
        this.durationLast = durationLast;
        this.timeInitial = timeInitial;
        this.timeLast = timeLast;
        this.timeCurrent = timeCurrent;
        this.tracking = tracking;
        this.updateLast = updateLast;

        // for point objects
        this.positionDeltaInitial = copy(positionDeltaInitial);
        this.positionDeltaLast = copy(positionDeltaLast);
        this.positionInitial = copy(positionInitial);
        this.positionLast = copy(positionLast);
        this.positionCurrent = copy(positionCurrent);
    }

    public Finger(Finger f) {

        // copy values from another finger object
        this(f.minDistanceSwipe, f.maxDurationSwipe, f.minDistanceMove, f.maxDurationDoubleTap,
                f.maxDownDoubleTap, f.slopeIntolerance, f.stateCurrent, f.stateLast, f.distanceInitial,
                f.distanceLast, f.durationInitial, f.durationLast, f.timeInitial, f.timeLast,
                f.timeCurrent, f.tracking, f.updateLast, f.positionDeltaInitial,
                f.positionDeltaLast, f.positionInitial, f.positionLast, f.positionCurrent);
    }

    private PointF copy(PointF pointF) {
        return new PointF(pointF.x, pointF.y);
    }

    /**
     * Reset current values to default ones
     */
    private void reset(){

        // init default
        stateCurrent = GestureDetector.NONE;
        stateLast = GestureDetector.NONE;
        distanceInitial = 0;
        distanceLast = 0;
        durationInitial = 0;
        durationLast = 0;
        timeInitial = 0;
        timeLast = 0;
        timeCurrent = 0;
        tracking = false;
        updateLast = true;

        positionDeltaInitial = new PointF();
        positionDeltaLast = new PointF();
        positionInitial = new PointF();
        positionLast = new PointF();
        positionCurrent = new PointF();
    }


    /**
     * Determine if double tap is made using initial time from both finger
     * object -current and -last to determine gesture duration and use initial
     * duration from both finger objects to determine the finger down delay.
     * And check if those values are in allowed ranges.
     *
     * @return
     */
    public boolean isDoubleTap() {

        if (lastFinger == null) {
            return false;
        }

        if (this.timeInitial - lastFinger.timeInitial < maxDurationDoubleTap &&
                this.durationInitial < maxDownDoubleTap &&
                lastFinger.durationInitial < maxDownDoubleTap) {
            return true;
        }

        return false;
    }

    /**
     * Method that determine the new state and sets current finger object
     * state value.
     *
     * @param event        onTouch motion event
     * @param pointerIndex pointer index used, to get current finger position
     */
    public void detectState(MotionEvent event, int pointerIndex) {
        stateLast = stateCurrent;

        int action = event.getAction() & MotionEvent.ACTION_MASK;

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {

            // when finger is pressed down
            setInitial(event, pointerIndex);
            stateCurrent = GestureDetector.DOWN;

        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {

            updateLast = true;

            // when finger is lift up
            update(event, pointerIndex);

            if (isDoubleTap()) {
                stateCurrent = GestureDetector.DOUBLE_TAP;
                return;
            }

            if ((Math.abs(positionDeltaInitial.x) < minDistanceSwipe &&
                    Math.abs(positionDeltaInitial.y) < minDistanceSwipe) ||
                    durationInitial > maxDurationSwipe) {
                // if minimum distance is not reached or the maximum time is passed, swipe is NOT detected
                stateCurrent = GestureDetector.UP;
            } else {

                // determine the swipe direction
                float x = positionDeltaInitial.x;
                float y = positionDeltaInitial.y;
                if (-y > slopeIntolerance * Math.abs(x)) {
                    stateCurrent = GestureDetector.SWIPE_UP;
                } else if (y > slopeIntolerance * Math.abs(x)) {
                    stateCurrent = GestureDetector.SWIPE_DOWN;
                } else if (-x > slopeIntolerance * Math.abs(y)) {
                    stateCurrent = GestureDetector.SWIPE_LEFT;
                } else if (x > slopeIntolerance * Math.abs(y)) {
                    stateCurrent = GestureDetector.SWIPE_RIGHT;
                }
            }

        } else if (action == MotionEvent.ACTION_MOVE) {

            // when finger is moved
            update(event, pointerIndex);

            // check if finger moved to minimum distance before, detecting the move state
            if ((Math.abs(positionDeltaLast.x) < minDistanceMove &&
                    Math.abs(positionDeltaLast.y) < minDistanceMove)) {
                updateLast = false;
                return;
            } else {
                updateLast = true;
            }

            // determine the direction
            float x = positionDeltaLast.x;
            float y = positionDeltaLast.y;
            if (-y > slopeIntolerance * Math.abs(x)) {
                stateCurrent = GestureDetector.MOVE_UP;
            } else if (y > slopeIntolerance * Math.abs(x)) {
                stateCurrent = GestureDetector.MOVE_DOWN;
            } else if (-x > slopeIntolerance * Math.abs(y)) {
                stateCurrent = GestureDetector.MOVE_LEFT;
            } else if (x > slopeIntolerance * Math.abs(y)) {
                stateCurrent = GestureDetector.MOVE_RIGHT;
            }
        }
    }


    /**
     * Event that is called when finger is pressed down. Method saves
     * finger object values to -lastFinger object before resetting current
     * finger values to default. Than set initial time and finger position.
     *
     * @param event        - onTouch motion event
     * @param pointerIndex - finger index whose state will be changed
     */
    public void setInitial(MotionEvent event, int pointerIndex) {

        lastFinger = new Finger(this);  // save current finger object
        reset();                           // reset current

        // set initial time and position
        positionInitial = new PointF(event.getX(pointerIndex), event.getY(pointerIndex));
        timeInitial = SystemClock.uptimeMillis();
        tracking = true; // since it is called from down event and tracking has began
    }

    /**
     * Update previous and current time, finger position,
     * delta, distance and duration for the gesture
     *
     * @param event        - onTouch motion event
     * @param pointerIndex - finger index whose state will be changed
     */
    public void update(MotionEvent event, int pointerIndex) {

        if (updateLast) {
            positionLast = new PointF(positionCurrent.x, positionCurrent.y);
            timeLast = timeCurrent;
        }
        positionCurrent = new PointF(event.getX(pointerIndex), event.getY(pointerIndex));
        timeCurrent = SystemClock.uptimeMillis();

        // delta and distance between -positionCurrent and -positionLast
        positionDeltaLast = new PointF
                (positionCurrent.x - positionLast.x, positionCurrent.y - positionLast.y);
        distanceLast = Math.sqrt(Math.pow(positionDeltaLast.x, 2) + Math.pow(positionDeltaLast.y, 2));

        // delta and distance between -positionCurrent and -positionInitial
        positionDeltaInitial = new PointF
                (positionCurrent.x - positionInitial.x, positionCurrent.y - positionInitial.y);
        distanceInitial = Math.sqrt(Math.pow(positionDeltaInitial.x, 2) + Math.pow(positionDeltaInitial.y, 2));

        // duration
        durationLast = timeCurrent - timeLast;
        durationInitial = timeCurrent - timeInitial;
    }

    public String getCurrentStateAsString() {
        return getStateAsString(stateCurrent);
    }

    public String getLastStateAsString() {
        return getStateAsString(stateLast);
    }

    /**
     * Method that returns given finger state as a string value
     *
     * @param state - given state
     * @return
     */
    public static String getStateAsString(int state) {

        switch (state) {
            case GestureDetector.SWIPE_UP:
                return "SWIPE UP";
            case GestureDetector.SWIPE_DOWN:
                return "SWIPE DOWN";
            case GestureDetector.SWIPE_LEFT:
                return "SWIPE LEFT";
            case GestureDetector.SWIPE_RIGHT:
                return "SWIPE RIGHT";
            case GestureDetector.HOLD_DOWN:
                return "HOLD DOWN";
            case GestureDetector.DOWN:
                return "DOWN";
            case GestureDetector.UP:
                return "UP";
            case GestureDetector.MOVE_UP:
                return "MOVE UP";
            case GestureDetector.MOVE_DOWN:
                return "MOVE DOWN";
            case GestureDetector.MOVE_LEFT:
                return "MOVE LEFT";
            case GestureDetector.MOVE_RIGHT:
                return "MOVE RIGHT";
            case GestureDetector.DOUBLE_TAP:
                return "DOUBLE TAP";
        }

        return "NONE";
    }

    public int getStateCurrent() {
        return stateCurrent;
    }

    public int getStateLast() {
        return stateLast;
    }

    public double getSlopeIntolerance() {
        return slopeIntolerance;
    }

    public int getMinDistanceSwipe() {
        return minDistanceSwipe;
    }

    public int getMaxDurationSwipe() {
        return maxDurationSwipe;
    }

    public int getMinDistanceMove() {
        return minDistanceMove;
    }

    public int getMaxDurationDoubleTap() {
        return maxDurationDoubleTap;
    }

    public int getMaxDownDoubleTap() {
        return maxDownDoubleTap;
    }

    public double getDistanceInitial() {
        return distanceInitial;
    }

    public double getDistanceLast() {
        return distanceLast;
    }

    public double getDurationInitial() {
        return durationInitial;
    }

    public double getDurationLast() {
        return durationLast;
    }

    public PointF getPositionDeltaInitial() {
        return positionDeltaInitial;
    }

    public PointF getPositionDeltaLast() {
        return positionDeltaLast;
    }

    public PointF getPositionInitial() {
        return positionInitial;
    }

    public PointF getPositionLast() {
        return positionLast;
    }

    public PointF getPositionCurrent() {
        return positionCurrent;
    }

    public long getTimeInitial() {
        return timeInitial;
    }

    public long getTimeLast() {
        return timeLast;
    }

    public long getTimeCurrent() {
        return timeCurrent;
    }

    public boolean isUpdateLast() {
        return updateLast;
    }

    public Finger getLastFinger() {
        return lastFinger;
    }

    public void setSlopeIntolerance(double slopeIntolerance) {
        this.slopeIntolerance = slopeIntolerance;
    }

    public void setMinDistanceSwipe(int minDistanceSwipe) {
        this.minDistanceSwipe = minDistanceSwipe;
    }

    public void setMaxDurationSwipe(int maxDurationSwipe) {
        this.maxDurationSwipe = maxDurationSwipe;
    }

    public void setMinDistanceMove(int minDistanceMove) {
        this.minDistanceMove = minDistanceMove;
    }

    public void setMaxDurationDoubleTap(int maxDurationDoubleTap) {
        this.maxDurationDoubleTap = maxDurationDoubleTap;
    }

    public void setMaxDownDoubleTap(int maxDownDoubleTap) {
        this.maxDownDoubleTap = maxDownDoubleTap;
    }

    public boolean isTracking() {
        return tracking;
    }

    public void setTracking(boolean tracking) {
        this.tracking = tracking;
    }

    public void setStateCurrent(int stateCurrent) {
        this.stateCurrent = stateCurrent;
    }

    public void setStateLast(int stateLast) {
        this.stateLast = stateLast;
    }
}