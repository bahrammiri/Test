
package com.github.mikephil.charting.data;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;

import com.github.mikephil.charting.components.YAxis.AxisDependency;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.DefaultValueFormatter;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ValueFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * The DataSet class represents one group or type of entries (Entry) in the
 * Chart that belong together. It is designed to logically separate different
 * groups of values inside the Chart (e.g. the values for a specific line in the
 * LineChart, or the values of a specific group of bars in the BarChart).
 *
 * @author Philipp Jahoda
 */
public abstract class DataSet < T extends Entry > {

  /**
   * List representing all colors that are used for this DataSet
   */
  protected List< Integer > mColors = null;
  protected List< Integer > mAltColors = null;

  /**
   * the entries that this dataset represents / holds together
   */
  protected List< T > mYVals = null;

  /**
   * maximum y-value in the y-value array
   */
  protected float mYMax = 0.0f;

  /**
   * the minimum y-value in the y-value array
   */
  protected float mYMin = 0.0f;

  /**
   * the total sum of all y-values
   */
  private float mYValueSum = 0f;

  /**
   * the last start value used for calcMinMax
   */
  protected int mLastStart = 0;

  /**
   * the last end value used for calcMinMax
   */
  protected int mLastEnd = 0;

  /**
   * label that describes the DataSet or the data the DataSet represents
   */
  private String mLabel = "DataSet";

  /**
   * flag that indicates if the DataSet is visible or not
   */
  private boolean mVisible = true;

  /**
   * if true, y-values are drawn on the chart
   */
  protected boolean mDrawValues = true;

  /**
   * the color used for the value-text
   */
  private int mValueColor = Color.BLACK;

  /**
   * the size of the value-text labels
   */
  private float mValueTextSize = 17f;

  /**
   * the typeface used for the value text
   */
  private Typeface mValueTypeface;

  /**
   * custom formatter that is used instead of the auto-formatter if set
   */
  protected ValueFormatter mValueFormatter;

  /**
   * this specifies which axis this DataSet should be plotted against
   */
  protected AxisDependency mAxisDependency = AxisDependency.LEFT;

  /**
   * if true, value highlightning is enabled
   */
  protected boolean mHighlightEnabled = true;

  /**
   * Creates a new DataSet object with the given values it represents. Also, a
   * label that describes the DataSet can be specified. The label can also be
   * used to retrieve the DataSet from a ChartData object.
   *
   * @param yVals
   * @param label
   */
  public DataSet ( List< T > yVals, String label ) {

    this.mLabel = label;
    this.mYVals = yVals;

    if ( mYVals == null )
      mYVals = new ArrayList< T > ();

    mColors = new ArrayList< Integer > ();
    mAltColors = new ArrayList< Integer > ();

    // default color
    mColors.add ( getDefaultColor () );
    mAltColors.add ( getDefaultColor () );

    calcMinMax ( mLastStart, mLastEnd );
    calcYValueSum ();
  }

  public int getDefaultColor () {
    return Color.rgb ( 140, 234, 255 );
  }

  /**
   * Use this method to tell the data set that the underlying data has changed
   */
  public void notifyDataSetChanged () {
    calcMinMax ( mLastStart, mLastEnd );
    calcYValueSum ();
  }

  /**
   * calc minimum and maximum y value
   */
  protected void calcMinMax ( int start, int end ) {
    final int yValCount = mYVals.size ();

    if ( yValCount == 0 )
      return;

    int endValue;

    if ( end == 0 || end >= yValCount )
      endValue = yValCount - 1;
    else
      endValue = end;

    mLastStart = start;
    mLastEnd = endValue;

    mYMin = Float.MAX_VALUE;
    mYMax = - Float.MAX_VALUE;

    for ( int i = start; i <= endValue; i++ ) {

      Entry e = mYVals.get ( i );

      if ( e != null && ! Float.isNaN ( e.getVal () ) ) {

        if ( e.getVal () < mYMin )
          mYMin = e.getVal ();

        if ( e.getVal () > mYMax )
          mYMax = e.getVal ();
      }
    }

    if ( mYMin == Float.MAX_VALUE ) {
      mYMin = 0.f;
      mYMax = 0.f;
    }
  }

  /**
   * calculates the sum of all y-values
   */
  private void calcYValueSum () {

    mYValueSum = 0;

    for ( int i = 0; i < mYVals.size (); i++ ) {
      Entry e = mYVals.get ( i );
      if ( e != null )
        mYValueSum += Math.abs ( e.getVal () );
    }
  }

  /**
   * returns the number of y-values this DataSet represents
   *
   * @return
   */
  public int getEntryCount () {
    return mYVals.size ();
  }

  /**
   * Returns the value of the Entry object at the given xIndex. Returns
   * Float.NaN if no value is at the given x-index. INFORMATION: This method
   * does calculations at runtime. Do not over-use in performance critical
   * situations.
   *
   * @param xIndex
   * @return
   */
  public float getYValForXIndex ( int xIndex ) {

    Entry e = getEntryForXIndex ( xIndex );

    if ( e != null && e.getXIndex () == xIndex )
      return e.getVal ();
    else
      return Float.NaN;
  }

  /**
   * Returns the first Entry object found at the given xIndex with binary
   * search. If the no Entry at the specified x-index is found, this method
   * returns the index at the closest x-index. Returns null if no Entry object
   * at that index. INFORMATION: This method does calculations at runtime. Do
   * not over-use in performance critical situations.
   *
   * @param x
   * @return
   */
  public T getEntryForXIndex ( int x ) {

    int index = getEntryIndex ( x );
    if ( index > - 1 )
      return mYVals.get ( index );
    return null;
  }

  /**
   * Returns the first Entry index found at the given xIndex with binary
   * search. If the no Entry at the specified x-index is found, this method
   * returns the index at the closest x-index. Returns -1 if no Entry object
   * at that index. INFORMATION: This method does calculations at runtime. Do
   * not over-use in performance critical situations.
   *
   * @param x
   * @return
   */
  public int getEntryIndex ( int x ) {

    int low = 0;
    int high = mYVals.size () - 1;
    int closest = - 1;

    while ( low <= high ) {
      int m = ( high + low ) / 2;

      if ( x == mYVals.get ( m ).getXIndex () ) {
        while ( m > 0 && mYVals.get ( m - 1 ).getXIndex () == x ) { m--; }

        return m;
      }

      if ( x > mYVals.get ( m ).getXIndex () )
        low = m + 1;
      else
        high = m - 1;

      closest = m;
    }

    return closest;
  }

  /**
   * Returns all Entry objects at the given xIndex. INFORMATION: This method
   * does calculations at runtime. Do not over-use in performance critical
   * situations.
   *
   * @param x
   * @return
   */
  public List< T > getEntriesForXIndex ( int x ) {

    List< T > entries = new ArrayList< T > ();

    int low = 0;
    int high = mYVals.size () - 1;

    while ( low <= high ) {
      int m = ( high + low ) / 2;
      T entry = mYVals.get ( m );

      if ( x == entry.getXIndex () ) {
        while ( m > 0 && mYVals.get ( m - 1 ).getXIndex () == x ) { m--; }

        high = mYVals.size ();
        for (; m < high; m++ ) {
          entry = mYVals.get ( m );
          if ( entry.getXIndex () == x ) {
            entries.add ( entry );
          } else {
            break;
          }
        }
      }

      if ( x > entry.getXIndex () )
        low = m + 1;
      else
        high = m - 1;
    }

    return entries;
  }

  /**
   * returns the DataSets Entry array
   *
   * @return
   */
  public List< T > getYVals () {
    return mYVals;
  }

  /**
   * gets the sum of all y-values
   *
   * @return
   */
  public float getYValueSum () {
    return mYValueSum;
  }

  /**
   * returns the minimum y-value this DataSet holds
   *
   * @return
   */
  public float getYMin () {
    return mYMin;
  }

  /**
   * returns the maximum y-value this DataSet holds
   *
   * @return
   */
  public float getYMax () {
    return mYMax;
  }

  /**
   * Returns the number of entries this DataSet holds.
   *
   * @return
   */
  public int getValueCount () {
    return mYVals.size ();
  }

  /**
   * The xIndex of an Entry object is provided. This method returns the actual
   * index in the Entry array of the DataSet. IMPORTANT: This method does
   * calculations at runtime, do not over-use in performance critical
   * situations.
   *
   * @param xIndex
   * @return
   */
  public int getIndexInEntries ( int xIndex ) {

    for ( int i = 0; i < mYVals.size (); i++ ) {
      if ( xIndex == mYVals.get ( i ).getXIndex () )
        return i;
    }

    return - 1;
  }

  /**
   * Provides an exact copy of the DataSet this method is used on.
   *
   * @return
   */
  public abstract DataSet< T > copy ();

  @Override
  public String toString () {
    StringBuffer buffer = new StringBuffer ();
    buffer.append ( toSimpleString () );
    for ( int i = 0; i < mYVals.size (); i++ ) {
      buffer.append ( mYVals.get ( i ).toString () + " " );
    }
    return buffer.toString ();
  }

  /**
   * Returns a simple string representation of the DataSet with the type and
   * the number of Entries.
   *
   * @return
   */
  public String toSimpleString () {
    StringBuffer buffer = new StringBuffer ();
    buffer.append ( "DataSet, label: " + ( mLabel == null ? "" : mLabel ) + ", entries: " + mYVals.size () + "\n" );
    return buffer.toString ();
  }

  /**
   * Sets the label string that describes the DataSet.
   *
   * @return
   */
  public void setLabel ( String label ) {
    mLabel = label;
  }

  /**
   * Returns the label string that describes the DataSet.
   *
   * @return
   */
  public String getLabel () {
    return mLabel;
  }

  /**
   * Set the visibility of this DataSet. If not visible, the DataSet will not
   * be drawn to the chart upon refreshing it.
   *
   * @param visible
   */
  public void setVisible ( boolean visible ) {
    mVisible = visible;
  }

  /**
   * Returns true if this DataSet is visible inside the chart, or false if it
   * is currently hidden.
   *
   * @return
   */
  public boolean isVisible () {
    return mVisible;
  }

  /**
   * Returns the axis this DataSet should be plotted against.
   *
   * @return
   */
  public AxisDependency getAxisDependency () {
    return mAxisDependency;
  }

  /**
   * Set the y-axis this DataSet should be plotted against (either LEFT or
   * RIGHT). Default: LEFT
   *
   * @param dependency
   */
  public void setAxisDependency ( AxisDependency dependency ) {
    mAxisDependency = dependency;
  }

  /**
   * set this to true to draw y-values on the chart NOTE (for bar and
   * linechart): if "maxvisiblecount" is reached, no values will be drawn even
   * if this is enabled
   *
   * @param enabled
   */
  public void setDrawValues ( boolean enabled ) {
    this.mDrawValues = enabled;
  }

  /**
   * returns true if y-value drawing is enabled, false if not
   *
   * @return
   */
  public boolean isDrawValuesEnabled () {
    return mDrawValues;
  }

  /**
   * Adds an Entry to the DataSet dynamically.
   * Entries are added to the end of the list.
   * This will also recalculate the current minimum and maximum
   * values of the DataSet and the value-sum.
   *
   * @param e
   */
  @SuppressWarnings ( "unchecked" )
  public void addEntry ( Entry e ) {

    if ( e == null )
      return;

    float val = e.getVal ();

    if ( mYVals == null ) {
      mYVals = new ArrayList< T > ();
    }

    if ( mYVals.size () == 0 ) {
      mYMax = val;
      mYMin = val;
    } else {
      if ( mYMax < val )
        mYMax = val;
      if ( mYMin > val )
        mYMin = val;
    }

    mYValueSum += val;

    // add the entry
    mYVals.add ( ( T ) e );
  }

  /**
   * Adds an Entry to the DataSet dynamically.
   * Entries are added to their appropriate index respective to it's x-index.
   * This will also recalculate the current minimum and maximum
   * values of the DataSet and the value-sum.
   *
   * @param e
   */
  @SuppressWarnings ( "unchecked" )
  public void addEntryOrdered ( Entry e ) {

    if ( e == null )
      return;

    float val = e.getVal ();

    if ( mYVals == null ) {
      mYVals = new ArrayList< T > ();
    }

    if ( mYVals.size () == 0 ) {
      mYMax = val;
      mYMin = val;
    } else {
      if ( mYMax < val )
        mYMax = val;
      if ( mYMin > val )
        mYMin = val;
    }

    mYValueSum += val;

    if ( mYVals.size () > 0 && mYVals.get ( mYVals.size () - 1 ).getXIndex () > e.getXIndex () ) {
      int closestIndex = getEntryIndex ( e.getXIndex () );
      if ( mYVals.get ( closestIndex ).getXIndex () < e.getXIndex () )
        closestIndex++;
      mYVals.add ( closestIndex, ( T ) e );
      return;
    }

    mYVals.add ( ( T ) e );
  }

  /**
   * Removes an Entry from the DataSets entries array. This will also
   * recalculate the current minimum and maximum values of the DataSet and the
   * value-sum. Returns true if an Entry was removed, false if no Entry could
   * be removed.
   *
   * @param e
   */
  public boolean removeEntry ( T e ) {

    if ( e == null )
      return false;

    // remove the entry
    boolean removed = mYVals.remove ( e );

    if ( removed ) {

      float val = e.getVal ();
      mYValueSum -= val;

      calcMinMax ( mLastStart, mLastEnd );
    }

    return removed;
  }

  /**
   * Removes the Entry object that has the given xIndex from the DataSet.
   * Returns true if an Entry was removed, false if no Entry could be removed.
   *
   * @param xIndex
   */
  public boolean removeEntry ( int xIndex ) {

    T e = getEntryForXIndex ( xIndex );
    return removeEntry ( e );
  }

  /** BELOW THIS COLOR HANDLING */

  /**
   * Sets the colors that should be used fore this DataSet. Colors are reused
   * as soon as the number of Entries the DataSet represents is higher than
   * the size of the colors array. If you are using colors from the resources,
   * make sure that the colors are already prepared (by calling
   * getResources().getColor(...)) before adding them to the DataSet.
   *
   * @param colors
   */
  public void setColors ( List< Integer > colors ) {
    this.mColors = colors;
  }

  public void setAltColors ( List< Integer > colors ) {
    this.mAltColors = colors;
  }

  /**
   * Sets the colors that should be used fore this DataSet. Colors are reused
   * as soon as the number of Entries the DataSet represents is higher than
   * the size of the colors array. If you are using colors from the resources,
   * make sure that the colors are already prepared (by calling
   * getResources().getColor(...)) before adding them to the DataSet.
   *
   * @param colors
   */
  public void setColors ( int[] colors ) {
    this.mColors = ColorTemplate.createColors ( colors );
  }

  public void setAltColors ( int[] colors ) {
    this.mAltColors = ColorTemplate.createColors ( colors );
  }

  /**
   * Sets the colors that should be used fore this DataSet. Colors are reused
   * as soon as the number of Entries the DataSet represents is higher than
   * the size of the colors array. You can use
   * "new int[] { R.color.red, R.color.green, ... }" to provide colors for
   * this method. Internally, the colors are resolved using
   * getResources().getColor(...)
   *
   * @param colors
   */
  public void setColors ( int[] colors, Context c ) {

    List< Integer > clrs = new ArrayList< Integer > ();

    for ( int color : colors ) {
      clrs.add ( c.getResources ().getColor ( color ) );
    }

    mColors = clrs;
  }

  public void setAltColors ( int[] colors, Context c ) {

    List< Integer > clrs = new ArrayList< Integer > ();

    for ( int color : colors ) {
      clrs.add ( c.getResources ().getColor ( color ) );
    }

    mAltColors = clrs;
  }

  /**
   * Adds a new color to the colors array of the DataSet.
   *
   * @param color
   */
  public void addColor ( int color ) {
    if ( mColors == null )
      mColors = new ArrayList< Integer > ();
    mColors.add ( color );
  }

  public void addAltColor ( int color ) {
    if ( mAltColors == null )
      mAltColors = new ArrayList< Integer > ();
    mAltColors.add ( color );
  }

  /**
   * Sets the one and ONLY color that should be used for this DataSet.
   * Internally, this recreates the colors array and adds the specified color.
   *
   * @param color
   */
  public void setColor ( int color ) {
    resetColors ();
    mColors.add ( color );
  }

  public void setAltColor ( int color ) {
    resetAltColors ();
    mAltColors.add ( color );
  }

  /**
   * returns all the colors that are set for this DataSet
   *
   * @return
   */
  public List< Integer > getColors () {
    return mColors;
  }

  public List< Integer > getAltColors () {
    return mAltColors;
  }

  /**
   * Returns the color at the given index of the DataSet's color array.
   * Performs a IndexOutOfBounds check by modulus.
   *
   * @param index
   * @return
   */
  public int getColor ( int index ) {
    return mColors.get ( index % mColors.size () );
  }

  public int getAltColor ( int index ) {
    return mAltColors.get ( index % mAltColors.size () );
  }

  /**
   * Returns the first color (index 0) of the colors-array this DataSet
   * contains.
   *
   * @return
   */
  public int getColor () {
    return mColors.get ( 0 );
  }

  public int getAltColor () {
    return mAltColors.get ( 0 );
  }

  /**
   * Resets all colors of this DataSet and recreates the colors array.
   */
  public void resetColors () {
    mColors = new ArrayList< Integer > ();
  }

  public void resetAltColors () {
    mAltColors = new ArrayList< Integer > ();
  }

  /**
   * If set to true, value highlighting is enabled which means that values can
   * be highlighted programmatically or by touch gesture.
   *
   * @param enabled
   */
  public void setHighlightEnabled ( boolean enabled ) {
    mHighlightEnabled = enabled;
  }

  /**
   * returns true if highlighting of values is enabled, false if not
   *
   * @return
   */
  public boolean isHighlightEnabled () {
    return mHighlightEnabled;
  }

  /**
   * Returns the position of the provided entry in the DataSets Entry array.
   * Returns -1 if doesn't exist.
   *
   * @param e
   * @return
   */
  public int getEntryPosition ( Entry e ) {

    for ( int i = 0; i < mYVals.size (); i++ ) {
      if ( e.equalTo ( mYVals.get ( i ) ) )
        return i;
    }

    return - 1;
  }

  /**
   * Sets the formatter to be used for drawing the values inside the chart. If
   * no formatter is set, the chart will automatically determine a reasonable
   * formatting (concerning decimals) for all the values that are drawn inside
   * the chart. Use chart.getDefaultValueFormatter() to use the formatter
   * calculated by the chart.
   *
   * @param f
   */
  public void setValueFormatter ( ValueFormatter f ) {

    if ( f == null )
      return;
    else
      mValueFormatter = f;
  }

  /**
   * Returns the formatter used for drawing the values inside the chart.
   *
   * @return
   */
  public ValueFormatter getValueFormatter () {
    if ( mValueFormatter == null )
      return new DefaultValueFormatter ( 1 );
    return mValueFormatter;
  }

  /**
   * If this component has no ValueFormatter or is only equipped with the
   * default one (no custom set), return true.
   *
   * @return
   */
  public boolean needsDefaultFormatter () {
    if ( mValueFormatter == null )
      return true;
    if ( mValueFormatter instanceof DefaultValueFormatter )
      return true;

    return false;
  }

  /**
   * Sets the color the value-labels of this DataSet should have.
   *
   * @param color
   */
  public void setValueTextColor ( int color ) {
    mValueColor = color;
  }

  public int getValueTextColor () {
    return mValueColor;
  }

  /**
   * Sets a Typeface for the value-labels of this DataSet.
   *
   * @param tf
   */
  public void setValueTypeface ( Typeface tf ) {
    mValueTypeface = tf;
  }

  public Typeface getValueTypeface () {
    return mValueTypeface;
  }

  /**
   * Sets the text-size of the value-labels of this DataSet in dp.
   *
   * @param size
   */
  public void setValueTextSize ( float size ) {
    mValueTextSize = Utils.convertDpToPixel ( size );
  }

  /**
   * Returns the text-size of the labels that are displayed above the values.
   *
   * @return
   */
  public float getValueTextSize () {
    return mValueTextSize;
  }

  /**
   * Checks if this DataSet contains the specified Entry. Returns true if so,
   * false if not. NOTE: Performance is pretty bad on this one, do not
   * over-use in performance critical situations.
   *
   * @param e
   * @return
   */
  public boolean contains ( Entry e ) {

    for ( Entry entry : mYVals ) {
      if ( entry.equals ( e ) )
        return true;
    }

    return false;
  }

  /**
   * Removes all values from this DataSet and recalculates min and max value.
   */
  public void clear () {
    mYVals.clear ();
    mLastStart = 0;
    mLastEnd = 0;
    notifyDataSetChanged ();
  }
}
