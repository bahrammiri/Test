
package com.github.mikephil.charting.data;

import android.content.Context;
import android.graphics.Color;
import android.graphics.DashPathEffect;

import com.github.mikephil.charting.listener.ChangeEntryStyleListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.Utils;

import java.util.ArrayList;
import java.util.List;
//
public class LineDataSet extends LineRadarDataSet<Entry> implements ChangeEntryStyleListener  {

    /** List representing all colors that are used for the circles */
    private List<Integer> mCircleColors = null;
    private List<Integer> mAltCircleColors = null;
//    private ChangeEntryStyleListener mChangeEntryStyleListener;

    /** the color of the inner circles */
    private int mCircleColorHole = Color.WHITE;
    private int mAltCircleColorHole = Color.WHITE;

    /** the radius of the circle-shaped value indicators */
    private float mCircleSize = 8f;

    /** % of how much is hole take from circle */
    private int mCircleHoleSize = 50;

    private float mXOffset = 0f;

    private float mYOffset = 0f;

    public void setXOffset ( float offset ) {
      mXOffset = offset;
    }

    public void setYOffset ( float offset ) {
      mYOffset = offset;
    }

    public float getXOffset ( ) {
      return mXOffset;
    }
    public float getYOffset ( ) {
      return mYOffset;
    }


    /** sets the intensity of the cubic lines */
    private float mCubicIntensity = 0.2f;

    /** the path effect of this DataSet that makes dashed lines possible */
    private DashPathEffect mDashPathEffect = null;

    /** if true, drawing circles is enabled */
    private boolean mDrawCircles = true;

    /** if true, cubic lines are drawn instead of linear */
    private boolean mDrawCubic = false;

    private boolean mHoleTransparent = false;
    private boolean mDrawCircleHole = true;

    public LineDataSet(List<Entry> yVals, String label) {
        super(yVals, label);

        // mCircleSize = Utils.convertDpToPixel(4f);
        // mLineWidth = Utils.convertDpToPixel(1f);

        mCircleColors = new ArrayList<Integer>();
        mAltCircleColors = new ArrayList<Integer>();

        // default colors
        // mColors.add(Color.rgb(192, 255, 140));
        // mColors.add(Color.rgb(255, 247, 140));
        mCircleColors.add ( getDefaultColor () );
        mAltCircleColors.add ( getDefaultColor () );
//
//      mChangeEntryStyleListener = new ChangeEntryStyleListener () {
//        @Override
//        public void changeColor ( int color ) {
//          Log.d ("Lib", "")
//
//        }
//      };
    }


//  public ChangeEntryStyleListener getChangeEntryStyleListener () {
//    return mChangeEntryStyleListener;
//  }
//
//  public void setChangeEntryStyleListener ( ChangeEntryStyleListener mChangeEntryStyleListener ) {
//    this.mChangeEntryStyleListener = mChangeEntryStyleListener;
//  }

  public int getDefaultColor () {
      return Color.rgb(140, 234, 255);
    }

    @Override
    public DataSet<Entry> copy() {

        List<Entry> yVals = new ArrayList<Entry>();

        for (int i = 0; i < mYVals.size(); i++) {
            yVals.add(mYVals.get(i).copy());
        }

        LineDataSet copied = new LineDataSet(yVals, getLabel());
        copied.mColors = mColors;
        copied.mCircleSize = mCircleSize;
        copied.mCircleColors = mCircleColors;
        copied.mAltCircleColors= mAltCircleColors;
        copied.mDashPathEffect = mDashPathEffect;
        copied.mDrawCircles = mDrawCircles;
        copied.mDrawCubic = mDrawCubic;
        copied.mHighLightColor = mHighLightColor;

        return copied;
    }

    /**
     * Sets the intensity for cubic lines (if enabled). Max = 1f = very cubic,
     * Min = 0.05f = low cubic effect, Default: 0.2f
     * 
     * @param intensity
     */
    public void setCubicIntensity(float intensity) {

        if (intensity > 1f)
            intensity = 1f;
        if (intensity < 0.05f)
            intensity = 0.05f;

        mCubicIntensity = intensity;
    }

    /**
     * Returns the intensity of the cubic lines (the effect intensity).
     * 
     * @return
     */
    public float getCubicIntensity() {
        return mCubicIntensity;
    }

    /**
     * sets the size (radius) of the circle shpaed value indicators, default
     * size = 4f
     * 
     * @param size
     */
    public void setCircleSize(float size) {
        mCircleSize = Utils.convertDpToPixel(size);
    }

    /**
     * returns the circlesize
     */
    public float getCircleSize() {
        return mCircleSize;
    }

    /**
     * Enables the line to be drawn in dashed mode, e.g. like this
     * "- - - - - -". THIS ONLY WORKS IF HARDWARE-ACCELERATION IS TURNED OFF.
     * Keep in mind that hardware acceleration boosts performance.
     * 
     * @param lineLength the length of the line pieces
     * @param spaceLength the length of space in between the pieces
     * @param phase offset, in degrees (normally, use 0)
     */
    public void enableDashedLine(float lineLength, float spaceLength, float phase) {
        mDashPathEffect = new DashPathEffect(new float[] {
                lineLength, spaceLength
        }, phase);
    }

    /**
     * Disables the line to be drawn in dashed mode.
     */
    public void disableDashedLine() {
        mDashPathEffect = null;
    }

    /**
     * Returns true if the dashed-line effect is enabled, false if not.
     * 
     * @return
     */
    public boolean isDashedLineEnabled() {
        return mDashPathEffect == null ? false : true;
    }

    /**
     * returns the DashPathEffect that is set for this DataSet
     * 
     * @return
     */
    public DashPathEffect getDashPathEffect() {
        return mDashPathEffect;
    }

    /**
     * set this to true to enable the drawing of circle indicators for this
     * DataSet, default true
     * 
     * @param enabled
     */
    public void setDrawCircles(boolean enabled) {
        this.mDrawCircles = enabled;
    }

    /**
     * returns true if drawing circles for this DataSet is enabled, false if not
     * 
     * @return
     */
    public boolean isDrawCirclesEnabled() {
        return mDrawCircles;
    }

    /**
     * If set to true, the linechart lines are drawn in cubic-style instead of
     * linear. This affects performance! Default: false
     * 
     * @param enabled
     */
    public void setDrawCubic(boolean enabled) {
        mDrawCubic = enabled;
    }

    /**
     * returns true if drawing cubic lines is enabled, false if not.
     * 
     * @return
     */
    public boolean isDrawCubicEnabled() {
        return mDrawCubic;
    }

    /** ALL CODE BELOW RELATED TO CIRCLE-COLORS */

    /**
     * returns all colors specified for the circles
     * 
     * @return
     */
    public List<Integer> getCircleColors() {
        return mCircleColors;
    }
    public List<Integer> getAltCircleColors() {
        return mAltCircleColors;
    }

    /**
     * Returns the color at the given index of the DataSet's circle-color array.
     * Performs a IndexOutOfBounds check by modulus.
     * 
     * @param index
     * @return
     */
    public int getCircleColor(int index) {
        return mCircleColors.get(index % mCircleColors.size());
    }
    public int getAltCircleColor(int index) {
        return mAltCircleColors.get(index % mAltCircleColors.size());
    }

    /**
     * Sets the colors that should be used for the circles of this DataSet.
     * Colors are reused as soon as the number of Entries the DataSet represents
     * is higher than the size of the colors array. Make sure that the colors
     * are already prepared (by calling getResources().getColor(...)) before
     * adding them to the DataSet.
     * 
     * @param colors
     */
    public void setCircleColors(List<Integer> colors) {
        mCircleColors = colors;
    }

    public void setAltCircleColors(List<Integer> colors) {
        mAltCircleColors = colors;
    }

    /**
     * Sets the colors that should be used for the circles of this DataSet.
     * Colors are reused as soon as the number of Entries the DataSet represents
     * is higher than the size of the colors array. Make sure that the colors
     * are already prepared (by calling getResources().getColor(...)) before
     * adding them to the DataSet.
     * 
     * @param colors
     */
    public void setCircleColors(int[] colors) {
        this.mCircleColors = ColorTemplate.createColors(colors);
    }

    public void setAltCircleColors(int[] colors) {
        this.mAltCircleColors = ColorTemplate.createColors(colors);
    }

    /**
     * ets the colors that should be used for the circles of this DataSet.
     * Colors are reused as soon as the number of Entries the DataSet represents
     * is higher than the size of the colors array. You can use
     * "new String[] { R.color.red, R.color.green, ... }" to provide colors for
     * this method. Internally, the colors are resolved using
     * getResources().getColor(...)
     * 
     * @param colors
     */
    public void setCircleColors(int[] colors, Context c) {

        List<Integer> clrs = new ArrayList<Integer>();

        for (int color : colors) {
            clrs.add(c.getResources().getColor(color));
        }

        mCircleColors = clrs;
    }

    public void setAltCircleColors(int[] colors, Context c) {

        List<Integer> clrs = new ArrayList<Integer>();

        for (int color : colors) {
            clrs.add(c.getResources().getColor(color));
        }

        mAltCircleColors = clrs;
    }

    /**
     * Sets the one and ONLY color that should be used for this DataSet.
     * Internally, this recreates the colors array and adds the specified color.
     * 
     * @param color
     */
    public void setCircleColor(int color) {
        resetCircleColors();
        mCircleColors.add(color);
    }

    public void setAltCircleColor(int color) {
        resetAltCircleColors();
        mAltCircleColors.add(color);
    }

    /**
     * resets the circle-colors array and creates a new one
     */
    public void resetCircleColors() {
        mCircleColors = new ArrayList<Integer>();
    }
    public void resetAltCircleColors() {
        mAltCircleColors = new ArrayList<Integer>();
    }

    /**
     * Sets the color of the inner circle of the line-circles.
     * 
     * @param color
     */
    public void setCircleColorHole(int color) {
        mCircleColorHole = color;
    }

    public void setAltCircleColorHole(int color) {
        mAltCircleColorHole = color;
    }

    /**
     * Returns the color of the inner circle.
     * 
     * @return
     */
    public int getCircleHoleColor() {
        return mCircleColorHole;
    }
    public int getAltCircleHoleColor() {
        return mAltCircleColorHole;
    }

    /**
     * Set this to true to allow drawing a hole in each data circle.
     * 
     * @param enabled
     */
    public void setDrawCircleHole(boolean enabled) {
        mDrawCircleHole = enabled;
    }

    public boolean isDrawCircleHoleEnabled() {
        return mDrawCircleHole;
    }

  public void setDrawHoleTranspatrent ( boolean enabled ) {
    this.mHoleTransparent = enabled;
  }

  public boolean isHoleTransparent () {
        return mHoleTransparent;
    }


  public void setCircleHoleSize ( int value ) {
    mCircleHoleSize = value;
  }

  public int getCircleHoleSize () {
    return mCircleHoleSize;
  }

  @Override
  public void changeColor ( int color ) {
    mAltCircleColorHole = color;
  }


  @Override
  public boolean equals ( Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass () != o.getClass () ) return false;

    LineDataSet that = ( LineDataSet ) o;

    if ( mAltCircleColorHole != that.mAltCircleColorHole ) return false;
    if ( mCircleColorHole != that.mCircleColorHole ) return false;
    if ( mCircleHoleSize != that.mCircleHoleSize ) return false;
    if ( Float.compare ( that.mCircleSize, mCircleSize ) != 0 ) return false;
    if ( Float.compare ( that.mCubicIntensity, mCubicIntensity ) != 0 ) return false;
    if ( mDrawCircleHole != that.mDrawCircleHole ) return false;
    if ( mDrawCircles != that.mDrawCircles ) return false;
    if ( mDrawCubic != that.mDrawCubic ) return false;
    if ( mHoleTransparent != that.mHoleTransparent ) return false;
    if ( Float.compare ( that.mXOffset, mXOffset ) != 0 ) return false;
    if ( Float.compare ( that.mYOffset, mYOffset ) != 0 ) return false;
    if ( mAltCircleColors != null ? ! mAltCircleColors.equals ( that.mAltCircleColors ) : that.mAltCircleColors != null )
      return false;
    if ( mCircleColors != null ? ! mCircleColors.equals ( that.mCircleColors ) : that.mCircleColors != null )
      return false;
    if ( mDashPathEffect != null ? ! mDashPathEffect.equals ( that.mDashPathEffect ) : that.mDashPathEffect != null )
      return false;

    return true;
  }

  @Override
  public int hashCode () {
    int result = mCircleColors != null ? mCircleColors.hashCode () : 0;
    result = 31 * result + ( mAltCircleColors != null ? mAltCircleColors.hashCode () : 0 );
    result = 31 * result + mCircleColorHole;
    result = 31 * result + mAltCircleColorHole;
    result = 31 * result + ( mCircleSize != + 0.0f ? Float.floatToIntBits ( mCircleSize ) : 0 );
    result = 31 * result + mCircleHoleSize;
    result = 31 * result + ( mXOffset != + 0.0f ? Float.floatToIntBits ( mXOffset ) : 0 );
    result = 31 * result + ( mYOffset != + 0.0f ? Float.floatToIntBits ( mYOffset ) : 0 );
    result = 31 * result + ( mCubicIntensity != + 0.0f ? Float.floatToIntBits ( mCubicIntensity ) : 0 );
    result = 31 * result + ( mDashPathEffect != null ? mDashPathEffect.hashCode () : 0 );
    result = 31 * result + ( mDrawCircles ? 1 : 0 );
    result = 31 * result + ( mDrawCubic ? 1 : 0 );
    result = 31 * result + ( mHoleTransparent ? 1 : 0 );
    result = 31 * result + ( mDrawCircleHole ? 1 : 0 );
    return result;
  }

  @Override
  public boolean contains ( Entry e ) {
    for ( Entry entry : getYVals () ) {
      if ( entry.equalTo ( e ) ) return true;
    }

    return super.contains ( e );
  }
}
