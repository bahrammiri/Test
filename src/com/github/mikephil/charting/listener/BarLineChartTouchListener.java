
package com.github.mikephil.charting.listener;

import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.AnimationUtils;

import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarLineScatterCandleData;
import com.github.mikephil.charting.data.BarLineScatterCandleDataSet;
import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.Highlight;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * TouchListener for Bar-, Line-, Scatter- and CandleStickChart with handles all
 * touch interaction. Longpress == Zoom out. Double-Tap == Zoom in.
 * 
 * @author Philipp Jahoda
 */
public class BarLineChartTouchListener extends ChartTouchListener<BarLineChartBase<? extends BarLineScatterCandleData<? extends BarLineScatterCandleDataSet<? extends Entry>>>> {

    /** the original touch-matrix from the chart */
    private Matrix mMatrix = new Matrix();

    /** matrix for saving the original matrix state */
    private Matrix mSavedMatrix = new Matrix();

    /** point where the touch action started */
    private PointF mTouchStartPoint = new PointF();

    /** center between two pointers (fingers on the display) */
    private PointF mTouchPointCenter = new PointF();

    private float mSavedXDist = 1f;
    private float mSavedYDist = 1f;
    private float mSavedDist = 1f;

    private DataSet<?> mClosestDataSetToTouch;

    /** used for tracking velocity of dragging */
    private VelocityTracker mVelocityTracker;

    private long mDecelerationLastTime = 0;
    private PointF mDecelerationCurrentPoint = new PointF();
    private PointF mDecelerationVelocity = new PointF();

    private BarLineScatterCandleDataSet mSelectedDataSet;

    public BarLineChartTouchListener(BarLineChartBase<? extends BarLineScatterCandleData<? extends BarLineScatterCandleDataSet<? extends Entry>>> chart, Matrix touchMatrix) {
        super(chart);
        this.mMatrix = touchMatrix;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {


        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            if (mVelocityTracker != null) {
                mVelocityTracker.recycle();
                mVelocityTracker = null;
            }
        }

        if (mTouchMode == NONE) {
            mGestureDetector.onTouchEvent(event);
        }

      if ( mChart instanceof LineChart ) {
        LineChart chart = ( LineChart ) mChart;
        if ( chart.getEntryDragListener () != null ) {
          chart.getEntryDragListener ().OnClick ();
        }
      }

      switch (event.getAction() & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_MOVE:
//          Log.d ( "Lib", "onTouch performDrag" );
          performDragForData(event);
          break;
        case MotionEvent.ACTION_UP:
//          Log.d ( "Lib", "onTouch ACTION_UP" );
          mSelectedDataSet = null;
          break;

        case MotionEvent.ACTION_DOWN:
          performSelect (event);
          saveTouchStart(event);

          break;

        case MotionEvent.ACTION_POINTER_DOWN:
          saveTouchStart(event);
          break;
      }

//      Log.d ( "Lib", "onTouch action: " + (event.getAction() & MotionEvent.ACTION_MASK) );
        if (!mChart.isDragEnabled() && (!mChart.isScaleXEnabled() && !mChart.isScaleYEnabled())) {
//          Log.d ( "Lib", "onTouch return true" );
          return true;
        }


        // Handle touch events here...
        switch (event.getAction() & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_DOWN:

                stopDeceleration();

                saveTouchStart(event);

                break;
            case MotionEvent.ACTION_POINTER_DOWN:

                if (event.getPointerCount() >= 2) {

                    mChart.disableScroll();

                    saveTouchStart(event);

                    // get the distance between the pointers on the x-axis
                    mSavedXDist = getXDist(event);

                    // get the distance between the pointers on the y-axis
                    mSavedYDist = getYDist(event);

                    // get the total distance between the pointers
                    mSavedDist = spacing(event);

                    if (mSavedDist > 10f) {

                        if (mChart.isPinchZoomEnabled()) {
                            mTouchMode = PINCH_ZOOM;
                        } else {
                            if (mSavedXDist > mSavedYDist)
                                mTouchMode = X_ZOOM;
                            else
                                mTouchMode = Y_ZOOM;
                        }
                    }

                    // determine the touch-pointer center
                    midPoint(mTouchPointCenter, event);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mTouchMode == DRAG) {

                    mChart.disableScroll();
                    performDrag(event);

                    performDragForData ( event );

                } else if (mTouchMode == X_ZOOM || mTouchMode == Y_ZOOM || mTouchMode == PINCH_ZOOM) {

                    mChart.disableScroll();

                    if (mChart.isScaleXEnabled() || mChart.isScaleYEnabled())
                        performZoom(event);

                } else if (mTouchMode == NONE
                        && Math.abs(distance(event.getX(), mTouchStartPoint.x, event.getY(),
                                mTouchStartPoint.y)) > 5f) {

                    if (mChart.hasNoDragOffset()) {

                        if (!mChart.isFullyZoomedOut() && mChart.isDragEnabled())
                            mTouchMode = DRAG;
                        else {
                            if (mChart.isHighlightPerDragEnabled())
                                performHighlightDrag(event);
                        }

                    } else if (mChart.isDragEnabled()) {
                        mTouchMode = DRAG;
                    }
                }
                break;

            case MotionEvent.ACTION_UP:

                final VelocityTracker velocityTracker = mVelocityTracker;
                final int pointerId = event.getPointerId(0);
                velocityTracker.computeCurrentVelocity(1000, Utils.getMaximumFlingVelocity());
                final float velocityY = velocityTracker.getYVelocity(pointerId);
                final float velocityX = velocityTracker.getXVelocity(pointerId);

                if (Math.abs(velocityX) > Utils.getMinimumFlingVelocity() ||
                        Math.abs(velocityY) > Utils.getMinimumFlingVelocity()) {

                    if (mTouchMode == DRAG && mChart.isDragDecelerationEnabled()) {

                        stopDeceleration();

                        mDecelerationLastTime = AnimationUtils.currentAnimationTimeMillis();
                        mDecelerationCurrentPoint = new PointF(event.getX(), event.getY());
                        mDecelerationVelocity = new PointF(velocityX, velocityY);

                        Utils.postInvalidateOnAnimation(mChart); // This causes computeScroll to fire, recommended for this by Google
                    }
                }

                if (mTouchMode == X_ZOOM ||
                        mTouchMode == Y_ZOOM ||
                        mTouchMode == PINCH_ZOOM ||
                        mTouchMode == POST_ZOOM) {

                    // Range might have changed, which means that Y-axis labels
                    // could have changed in size, affecting Y-axis size.
                    // So we need to recalculate offsets.
                    mChart.calculateOffsets();
                    mChart.postInvalidate();
                }

                mTouchMode = NONE;
                mChart.enableScroll();

                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }

                break;
            case MotionEvent.ACTION_POINTER_UP:
                Utils.velocityTrackerPointerUpCleanUpIfNecessary(event, mVelocityTracker);

                mTouchMode = POST_ZOOM;
                break;

            case MotionEvent.ACTION_CANCEL:

                mTouchMode = NONE;

                break;
        }

        // Perform the transformation, update the chart
        // if (needsRefresh())
        mMatrix = mChart.getViewPortHandler().refresh(mMatrix, mChart, true);

        return true; // indicate event was handled
    }

    /**
     * ################ ################ ################ ################
     */
    /** BELOW CODE PERFORMS THE ACTUAL TOUCH ACTIONS */

    /**
     * Saves the current Matrix state and the touch-start point.
     * 
     * @param event
     */
    private void saveTouchStart(MotionEvent event) {

        mSavedMatrix.set(mMatrix);
        mTouchStartPoint.set(event.getX(), event.getY());

        mClosestDataSetToTouch = mChart.getDataSetByTouchPoint(event.getX(), event.getY());
    }


    /**
     * Perform operation for dragging selected data;
     *
     * @param event
     */
    private void performDragForData ( MotionEvent event ) {
      if ( mSelectedDataSet != null ) {
        if ( mSelectedDataSet instanceof LineDataSet ) {
          LineDataSet dataSet = ( LineDataSet ) mSelectedDataSet;
          List<Entry> entries = dataSet.getYVals();

          if ( mChart instanceof LineChart ) {
            LineChart chart = ( LineChart ) mChart;

            Entry new_entry = mChart.createEntryByTouch ( event.getX (), event.getY () );

            for ( Entry entry : entries ) {
//              && ( boolean ) entry.getData ()

              LineDataSet lineDataSet = null;
              for ( LineDataSet data : chart.getLineData ().getDataSets () ) {
                if ( data.contains ( entry ) ) lineDataSet = data;
              }

              if ( entry.getData () != null && ( boolean ) entry.getData () ) {

                // perform drag for data;
                if ( new_entry != null ) {
                  Entry entry_max = chart.getMaxDragEntry ();
                  Entry entry_min = chart.getMinDragEntry ();

                  // check X values;
                  if ( new_entry.getXIndex () > entry_min.getXIndex () && new_entry.getXIndex () < entry_max.getXIndex () ) {
                    entry.setXIndex ( new_entry.getXIndex () );
                  } else {
                    if ( new_entry.getXIndex () < entry_min.getXIndex () ) {
                      entry.setXIndex ( entry_min.getXIndex () );
                    } else
                    if ( new_entry.getXIndex () > entry_max.getXIndex () ) {
                      entry.setXIndex ( entry_max.getXIndex () );
                    }
                  }

                  // check Y values;
                  if ( new_entry.getVal () > entry_min.getVal () && new_entry.getVal () < entry_max.getVal () ) {
                    entry.setVal ( new_entry.getVal () );
                  } else {
                    if ( new_entry.getVal () < entry_min.getVal () ) {
                      entry.setVal ( entry_min.getVal () );
                    } else
                    if ( new_entry.getVal () > entry_max.getVal () ) {
                      entry.setVal ( entry_max.getVal () );
                    }
                  }
//
//                  if ( new_entry.getVal () < entry_max.getVal () ) {
//                    entry.setVal ( new_entry.getVal () );
//                  } else {
//                    entry.setVal ( entry_max.getVal () );
//                  }

                  if ( chart.getEntryDragListener () != null ) {
                    entry.setUseAlt ( true );
                    // todo: mb it's a different dataSet 'cos it's was created on the original base
                    if ( lineDataSet != null ) {
                      chart.getEntryDragListener ().OnDrag ( entry, lineDataSet );
                    } else {
                      chart.getEntryDragListener ().OnDrag ( entry, dataSet );
                    }
                  }

                  mChart.invalidate ();
                }
                //set new values;
              }
            }
          }
        }
      }
    }
    /**
     * Performs all necessary operations needed for dragging.
     * 
     * @param event
     */
    private void performDrag(MotionEvent event) {

        mMatrix.set(mSavedMatrix);

        OnChartGestureListener l = mChart.getOnChartGestureListener();

        float dX, dY;

        // check if axis is inverted
        if (mChart.isAnyAxisInverted() && mClosestDataSetToTouch != null
                && mChart.getAxis(mClosestDataSetToTouch.getAxisDependency()).isInverted()) {

            // if there is an inverted horizontalbarchart
            if (mChart instanceof HorizontalBarChart) {
                dX = -(event.getX() - mTouchStartPoint.x);
                dY = event.getY() - mTouchStartPoint.y;
            } else {
                dX = event.getX() - mTouchStartPoint.x;
                dY = -(event.getY() - mTouchStartPoint.y);
            }
        }
        else {
            dX = event.getX() - mTouchStartPoint.x;
            dY = event.getY() - mTouchStartPoint.y;
        }

        mMatrix.postTranslate(dX, dY);

        if (l != null)
            l.onChartTranslate(event, dX, dY);
    }

    /**
     * Performs the all operations necessary for pinch and axis zoom.
     * 
     * @param event
     */
    private void performZoom(MotionEvent event) {

        if (event.getPointerCount() >= 2) {

            OnChartGestureListener l = mChart.getOnChartGestureListener();

            // get the distance between the pointers of the touch
            // event
            float totalDist = spacing(event);

            if (totalDist > 10f) {

                // get the translation
                PointF t = getTrans(mTouchPointCenter.x, mTouchPointCenter.y);

                // take actions depending on the activated touch
                // mode
                if (mTouchMode == PINCH_ZOOM) {

                    float scale = totalDist / mSavedDist; // total scale

                    boolean isZoomingOut = (scale < 1);
                    boolean canZoomMoreX = isZoomingOut ?
                            mChart.getViewPortHandler().canZoomOutMoreX() :
                            mChart.getViewPortHandler().canZoomInMoreX();

                    float scaleX = (mChart.isScaleXEnabled()) ? scale : 1f;
                    float scaleY = (mChart.isScaleYEnabled()) ? scale : 1f;

                    if (mChart.isScaleYEnabled() || canZoomMoreX) {

                        mMatrix.set(mSavedMatrix);
                        mMatrix.postScale(scaleX, scaleY, t.x, t.y);

                        if (l != null)
                            l.onChartScale(event, scaleX, scaleY);
                    }

                } else if (mTouchMode == X_ZOOM && mChart.isScaleXEnabled()) {

                    float xDist = getXDist(event);
                    float scaleX = xDist / mSavedXDist; // x-axis scale

                    boolean isZoomingOut = (scaleX < 1);
                    boolean canZoomMoreX = isZoomingOut ?
                            mChart.getViewPortHandler().canZoomOutMoreX() :
                            mChart.getViewPortHandler().canZoomInMoreX();

                    if (canZoomMoreX) {

                        mMatrix.set(mSavedMatrix);
                        mMatrix.postScale(scaleX, 1f, t.x, t.y);

                        if (l != null)
                            l.onChartScale(event, scaleX, 1f);
                    }

                } else if (mTouchMode == Y_ZOOM && mChart.isScaleYEnabled()) {

                    float yDist = getYDist(event);
                    float scaleY = yDist / mSavedYDist; // y-axis scale

                    mMatrix.set(mSavedMatrix);

                    // y-axis comes from top to bottom, revert y
                    mMatrix.postScale(1f, scaleY, t.x, t.y);

                    if (l != null)
                        l.onChartScale(event, 1f, scaleY);
                }
            }
        }
    }

    /**
     * Perform a highlight operation.
     * 
     * @param e
     */
    private void performHighlight(MotionEvent e) {

        Highlight h = mChart.getHighlightByTouchPoint(e.getX(), e.getY());

        if (h == null || h.equalTo(mLastHighlighted ) || !mChart.isHighlightEnabled () ) {
            mChart.highlightTouch(null);
            mLastHighlighted = null;
        } else {
            mLastHighlighted = h;
            mChart.highlightTouch(h);
        }
    }

    /**
     * Highlights upon dragging, generates callbacks for the selection-listener.
     * 
     * @param e
     */
    private void performHighlightDrag(MotionEvent e) {

        Highlight h = mChart.getHighlightByTouchPoint(e.getX(), e.getY());

        if (h != null && !h.equalTo(mLastHighlighted)) {
            mLastHighlighted = h;
            mChart.highlightTouch(h);
        }
    }

    /**
     * ################ ################ ################ ################
     */
    /** DOING THE MATH BELOW ;-) */


    /**
     * Determines the center point between two pointer touch points.
     * 
     * @param point
     * @param event
     */
    private static void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2f, y / 2f);
    }

    /**
     * returns the distance between two pointer touch points
     * 
     * @param event
     * @return
     */
    private static float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * calculates the distance on the x-axis between two pointers (fingers on
     * the display)
     * 
     * @param e
     * @return
     */
    private static float getXDist(MotionEvent e) {
        float x = Math.abs(e.getX(0) - e.getX(1));
        return x;
    }

    /**
     * calculates the distance on the y-axis between two pointers (fingers on
     * the display)
     * 
     * @param e
     * @return
     */
    private static float getYDist(MotionEvent e) {
        float y = Math.abs(e.getY(0) - e.getY(1));
        return y;
    }

    /**
     * returns the correct translation depending on the provided x and y touch
     * points
     * 
     * @param x
     * @param y
     * @return
     */
    public PointF getTrans(float x, float y) {

        ViewPortHandler vph = mChart.getViewPortHandler();

        float xTrans = x - vph.offsetLeft();
        float yTrans = 0f;

        // check if axis is inverted
        if (mChart.isAnyAxisInverted() && mClosestDataSetToTouch != null
                && mChart.isInverted(mClosestDataSetToTouch.getAxisDependency())) {
            yTrans = -(y - vph.offsetTop());
        } else {
            yTrans = -(mChart.getMeasuredHeight() - y - vph.offsetBottom());
        }

        return new PointF(xTrans, yTrans);
    }

    /**
     * ################ ################ ################ ################
     */
    /** GETTERS AND GESTURE RECOGNITION BELOW */

    /**
     * returns the matrix object the listener holds
     * 
     * @return
     */
    public Matrix getMatrix() {
        return mMatrix;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {

        OnChartGestureListener l = mChart.getOnChartGestureListener();

        if (l != null) {
            l.onChartDoubleTapped(e);
            return super.onDoubleTap(e);
        }

        // check if double-tap zooming is enabled
        if (mChart.isDoubleTapToZoomEnabled()) {

            PointF trans = getTrans(e.getX(), e.getY());

            mChart.zoom(mChart.isScaleXEnabled() ? 1.4f : 1f, mChart.isScaleYEnabled() ? 1.4f : 1f, trans.x, trans.y);

            if (mChart.isLogEnabled())
                Log.i("BarlineChartTouch", "Double-Tap, Zooming In, x: " + trans.x + ", y: "
                        + trans.y);
        }

        return super.onDoubleTap(e);
    }

    @Override
    public void onLongPress(MotionEvent e) {

        OnChartGestureListener l = mChart.getOnChartGestureListener();

        if (l != null) {

            l.onChartLongPressed(e);
        }

      // note: long press;
//      performSelect (e);
    }



    /**
     * Select data that need to move;
     * @param e
     */
    private void performSelect ( MotionEvent e ) {
      mSelectedDataSet = mChart.getDataSetByTouchPoint ( e.getX (), e.getY () );


//      /***/
//      if ( mChart instanceof LineChart ) {
//        LineChart lineChart = ( LineChart ) mChart;
//        for ( int i = 0; i < lineChart.getLineData ().getDataSets ().size (); ++ i ) {
//          for ( Entry entry : lineChart.getLineData ().getDataSets ().get ( i ).getYVals (  ) ) {
//            Log.d ( "Lib", "entry.getData: " + entry.getData () + "; y:x: " + entry.getVal () + entry.getXIndex () );
//          }
//
//        }
//      }

      // find all enties with same x and y. add only those who has data == true;
      // works only for LineDataSet and LineChart;
      if ( mSelectedDataSet instanceof  LineDataSet && mChart instanceof LineChart ) {
        LineDataSet dataSet = ( LineDataSet ) mSelectedDataSet;
        List < Entry > selectable_entries = new ArrayList<> (  );
        if ( dataSet != null ) {
          List< Entry > entries = dataSet.getYVals ();

          for ( Entry touch_entry : entries ) {
            // find all enries with same x and y;
            LineChart lineChart = ( LineChart ) mChart;
            for ( int i = 0; i < lineChart.getLineData ().getDataSets ().size (); ++ i ) {
              for ( Entry all_entry : lineChart.getLineData ().getDataSets ().get ( i ).getYVals () ) {
                if ( all_entry.getVal () == touch_entry.getVal () && all_entry.getXIndex () == touch_entry.getXIndex () ) {
                  if ( all_entry.getData () != null ) {
                    selectable_entries.add ( all_entry );
                  }
                }
              }
            }
          }
          mSelectedDataSet = new LineDataSet ( selectable_entries , "" );
        }
      }
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {

        performHighlight(e);

        return super.onSingleTapUp(e);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        //remove selection;
        mSelectedDataSet = null;
        OnChartGestureListener l = mChart.getOnChartGestureListener();

        if (l != null) {
            l.onChartSingleTapped(e);
        }

        return super.onSingleTapConfirmed(e);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

        OnChartGestureListener l = mChart.getOnChartGestureListener();

        if (l != null)
            l.onChartFling(e1, e2, velocityX, velocityY);

        return super.onFling(e1, e2, velocityX, velocityY);
    }

    public void stopDeceleration() {
        mDecelerationVelocity = new PointF(0.f, 0.f);
    }

    public void computeScroll() {

        if (mDecelerationVelocity.x == 0.f && mDecelerationVelocity.y == 0.f)
            return; // There's no deceleration in progress

        final long currentTime = AnimationUtils.currentAnimationTimeMillis();

        mDecelerationVelocity.x *= mChart.getDragDecelerationFrictionCoef();
        mDecelerationVelocity.y *= mChart.getDragDecelerationFrictionCoef();

        final float timeInterval = (float)(currentTime - mDecelerationLastTime) / 1000.f;

        float distanceX = mDecelerationVelocity.x * timeInterval;
        float distanceY = mDecelerationVelocity.y * timeInterval;

        mDecelerationCurrentPoint.x += distanceX;
        mDecelerationCurrentPoint.y += distanceY;

        MotionEvent event = MotionEvent.obtain(currentTime, currentTime, MotionEvent.ACTION_MOVE, mDecelerationCurrentPoint.x, mDecelerationCurrentPoint.y, 0);
        performDrag(event);
        event.recycle();
        mMatrix = mChart.getViewPortHandler().refresh(mMatrix, mChart, false);

        mDecelerationLastTime = currentTime;

        if (Math.abs(mDecelerationVelocity.x) >= 0.01 || Math.abs(mDecelerationVelocity.y) >= 0.01)
            Utils.postInvalidateOnAnimation(mChart); // This causes computeScroll to fire, recommended for this by Google
        else {
            // Range might have changed, which means that Y-axis labels
            // could have changed in size, affecting Y-axis size.
            // So we need to recalculate offsets.
            mChart.calculateOffsets();
            mChart.postInvalidate();

            stopDeceleration();
        }
    }

}
