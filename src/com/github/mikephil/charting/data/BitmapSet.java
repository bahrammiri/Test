package com.github.mikephil.charting.data;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.utils.Utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by vova on 7/28/15.
 */
public class BitmapSet {

  /**
   * the actual value
   */
  private Bitmap mBitmap = null;

  // critical rename this variable and methods!
  private boolean mOffset = false;


  /** the radius of the circle-shaped value indicators */
  private float mCircleSize = 8f;

  /** the index on the x-axis */
//  private int mXIndex = -1;

  /**
   * the value on the y-axis
   */
//  private float mYValue = -1;

  private List< Entry > entries;

  private YAxis.AxisDependency mAxisDependency;

  /**
   * A DrawableSet represents one single drawable in the chart.
   *
   * @param bitmap  the drawable in the chart
   * @param entries
   */
  public BitmapSet ( Bitmap bitmap, List< Entry > entries ) {
    mBitmap = bitmap;
//    mXIndex = xIndex;
//    mYValue = yValue;
    this.entries = entries;
  }

  public BitmapSet ( Bitmap bitmap, List< Entry > entries, YAxis.AxisDependency dependency ) {
    mBitmap = bitmap;
//    mXIndex = xIndex;
//    mYValue = yValue;
    this.entries = entries;
    mAxisDependency = dependency;
  }

  public BitmapSet ( Bitmap bitmap, List< Entry > entries, YAxis.AxisDependency dependency, boolean offset ) {
    mBitmap = bitmap;
//    mXIndex = xIndex;
//    mYValue = yValue;
    this.entries = entries;
    mAxisDependency = dependency;
    mOffset = offset;
  }

  public BitmapSet ( Bitmap bitmap, List< Entry > entries, YAxis.AxisDependency dependency, float size, boolean offset ) {
    mBitmap = bitmap;
//    mXIndex = xIndex;
//    mYValue = yValue;
    this.entries = entries;
    mAxisDependency = dependency;
    mOffset = offset;
    mCircleSize = size;
  }

  /**
   * Returns the axis this DataSet should be plotted against.
   *
   * @return
   */
  public YAxis.AxisDependency getAxisDependency () {
    return mAxisDependency;
  }

  /**
   * Set the y-axis this DataSet should be plotted against (either LEFT or
   * RIGHT). Default: LEFT
   *
   * @param dependency
   */
  public void setAxisDependency ( YAxis.AxisDependency dependency ) {
    mAxisDependency = dependency;
  }

  /**
   * returns the bitmap
   *
   * @return
   */
  public Bitmap getBitmap () {
    return mBitmap;
  }

  /**
   * sets the bitmap for the set
   *
   * @param bitmap
   */
  public void setBitmap ( Bitmap bitmap ) {
    this.mBitmap = bitmap;
  }


  /**
   * returns an exact copy of the entry
   *
   * @return
   */
  public BitmapSet copy () {
    BitmapSet bitmapSet = new BitmapSet ( mBitmap, entries );
    return bitmapSet;
  }

  /**
   * Compares value, xIndex and data of the entries. Returns true if entries
   * are equal in those points, false if not. Does not check by hash-code like
   * it's done by the "equals" method.
   *
   * @param d
   * @return
   */
  public boolean equalTo ( BitmapSet d ) {

    if ( d == null )
      return false;

    if ( d.mBitmap != this.mBitmap )
      return false;
    if ( d.mAxisDependency != this.mAxisDependency )
      return false;

    if ( d.entries.size () != entries.size () ) return false;

    for ( int i = 0; i < entries.size (); ++ i ) {
      if ( ! entries.get ( i ).equalTo ( d.entries.get ( i ) ) ) return false;
    }

    return true;
  }

  /**
   * returns a string representation of the entry containing x-index and value
   */
  @Override
  public String toString () {

    String str_entries = "";
    for ( Entry entry : entries ) {
      str_entries += " " + entry.toString ();
    }


    return "DrawableSet, drawable: " + mBitmap + "; axis dep: " + mAxisDependency + " entries: " + str_entries;
  }


  /**
   * sets the size (radius) of the circle shpaed value indicators, default
   * size = 4f
   *
   * @param size
   */
  public void setCircleSize(float size) {
    mCircleSize = Utils.convertDpToPixel ( size );
  }

  /**
   * returns the circlesize
   */
  public float getCircleSize() {
    return mCircleSize;
  }

//  public List<Entry > createEntries ( List < BitmapSet > list ) {
//    List < Entry > res = new ArrayList <> ();
//
//    Iterator < BitmapSet > iter = list.iterator ();
//
//    while ( iter.hasNext () ) {
//      BitmapSet value = iter.next ();
//
//      res.add ( createEntry ( value ) );
//
//    }
//
//
//    return res;
//  }
//
//
//  private Entry createEntry ( BitmapSet value ) {
//
//    float y = value.getYValue ();
//    int x = value.getXIndex ();
//    Log.d ( "Lib", "createEntry: y : x: " + getYValue () + " : " + value.getXIndex () );
//    return new Entry ( y, x );
//  }

  public Entry getEntryForXIndex ( int xIndex ) {
    int index = getClossest ( xIndex );

    return index > - 1 ? entries.get ( index ) : null;
  }

  private int getClossest ( int x ) {

    int low = 0;
    int high = entries.size() - 1;
    int closest = -1;

    while (low <= high) {
      int m = (high + low) / 2;

      if (x == entries.get(m).getXIndex()) {
        while (m > 0 && entries.get(m - 1).getXIndex() == x)
          m--;

        return m;
      }

      if (x > entries.get(m).getXIndex())
        low = m + 1;
      else
        high = m - 1;

      closest = m;
    }

    return closest;
  }

  public boolean isOffset () {
    return mOffset;
  }

  public void setOffset ( boolean mOffset ) {
    this.mOffset = mOffset;
  }

  public List< Entry > getEntries () {
    return entries;
  }

  public void setEntries ( List< Entry > entries ) {
    this.entries = entries;
  }

  public int getEntryPosition ( Entry e ) {

    for ( int i = 0; i < entries.size (); i++ ) {
      if ( e.equalTo ( entries.get ( i ) ) )
        return i;
    }

    return - 1;
  }

}
