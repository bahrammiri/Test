
package com.github.mikephil.charting.renderer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.buffer.CircleBuffer;
import com.github.mikephil.charting.buffer.LineBuffer;
import com.github.mikephil.charting.data.BitmapSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.LineDataProvider;
import com.github.mikephil.charting.listener.OnHintDrawListener;
import com.github.mikephil.charting.utils.Highlight;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.Iterator;
import java.util.List;

public class LineChartRenderer extends LineScatterCandleRadarRenderer {

  protected LineDataProvider mChart;

  /**
   * paint for the inner circle of the value indicators
   */
  protected Paint mCirclePaintInner;

  /**
   * Bitmap object used for drawing the paths (otherwise they are too long if
   * rendered directly on the canvas)
   */
  protected Bitmap mDrawBitmap;

  private boolean mUseAlt = false;
  /**
   * on this canvas, the paths are rendered, it is initialized with the
   * pathBitmap
   */
  protected Canvas mBitmapCanvas;

  protected Path cubicPath = new Path ();
  protected Path cubicFillPath = new Path ();

  protected LineBuffer[] mLineBuffers;

  protected CircleBuffer[] mCircleBuffers;
  protected CircleBuffer[] mBitmapBuffers;

  protected OnHintDrawListener mHintDrawListener;


  public LineChartRenderer ( LineDataProvider chart, ChartAnimator animator,
                             ViewPortHandler viewPortHandler ) {
    super ( animator, viewPortHandler );


  }

  public LineChartRenderer ( LineDataProvider chart, ChartAnimator animator,
                             ViewPortHandler viewPortHandler, OnHintDrawListener listener ) {
    super ( animator, viewPortHandler );
    mChart = chart;

    mCirclePaintInner = new Paint ( Paint.ANTI_ALIAS_FLAG );
    mCirclePaintInner.setStyle ( Paint.Style.FILL );
    mCirclePaintInner.setColor ( Color.WHITE );

    mHintDrawListener = listener;
  }

  @Override
  public void initBuffers () {

    LineData lineData = mChart.getLineData ();
    mLineBuffers = new LineBuffer[ lineData.getDataSetCount () ];
    mCircleBuffers = new CircleBuffer[ lineData.getDataSetCount () ];
    mBitmapBuffers = new CircleBuffer[ lineData.getBitmapSets ().size () ];

    for ( int i = 0; i < mLineBuffers.length; i++ ) {
      LineDataSet set = lineData.getDataSetByIndex ( i );


      mLineBuffers[ i ] = new LineBuffer ( set.getEntryCount () * 4 - 4 );
      mCircleBuffers[ i ] = new CircleBuffer ( set.getEntryCount () * 2 );

    }

    for ( int i = 0; i < mBitmapBuffers.length; ++ i ) {
//          mBitmapBuffers[ i ] = new CircleBuffer ( (lineData.getBitmapSets () == null ? 0 : lineData.getBitmapSets ().size () ) * 2 );
      BitmapSet set = lineData.getBitmapSets ().get ( i );
      mBitmapBuffers[ i ] = new CircleBuffer ( set.getEntries ().size () * 2 );
    }

  }

  @Override
  public void drawData ( Canvas c ) {

    int width = ( int ) mViewPortHandler.getChartWidth ();
    int height = ( int ) mViewPortHandler.getChartHeight ();

    if ( mDrawBitmap == null
            || ( mDrawBitmap.getWidth () != width )
            || ( mDrawBitmap.getHeight () != height ) ) {

      if ( width > 0 && height > 0 ) {

        mDrawBitmap = Bitmap.createBitmap ( width, height, Bitmap.Config.ARGB_4444 );
        mBitmapCanvas = new Canvas ( mDrawBitmap );
      } else
        return;
    }

    mDrawBitmap.eraseColor ( Color.TRANSPARENT );

    LineData lineData = mChart.getLineData ();

    for ( LineDataSet set : lineData.getDataSets () ) {

      if ( set.isVisible () )
        drawDataSet ( c, set );
    }

    c.drawBitmap ( mDrawBitmap, 0, 0, mRenderPaint );
  }

  protected void drawDataSet ( Canvas c, LineDataSet dataSet ) {

    List< Entry > entries = dataSet.getYVals ();

    if ( entries.size () < 1 )
      return;

    mRenderPaint.setStrokeWidth ( dataSet.getLineWidth () );
    mRenderPaint.setPathEffect ( dataSet.getDashPathEffect () );

    /***/
    // critical;
    mRenderPaint.setStrokeCap(Paint.Cap.ROUND);
    /**/

    // if drawing cubic lines is enabled
    if ( dataSet.isDrawCubicEnabled () ) {

      drawCubic ( c, dataSet, entries );

      // draw normal (straight) lines
    } else {
      drawLinear ( c, dataSet, entries );
    }

    mRenderPaint.setPathEffect ( null );
  }

  /**
   * Draws a cubic line.
   *
   * @param c
   * @param dataSet
   * @param entries
   */
  protected void drawCubic ( Canvas c, LineDataSet dataSet, List< Entry > entries ) {

    Transformer trans = mChart.getTransformer ( dataSet.getAxisDependency () );

    Entry entryFrom = dataSet.getEntryForXIndex ( mMinX );
    Entry entryTo = dataSet.getEntryForXIndex ( mMaxX );

    int minx = Math.max ( dataSet.getEntryPosition ( entryFrom ), 0 );
    int maxx = Math.min ( dataSet.getEntryPosition ( entryTo ) + 1, entries.size () );

    float phaseX = mAnimator.getPhaseX ();
    float phaseY = mAnimator.getPhaseY ();

    float intensity = dataSet.getCubicIntensity ();

    cubicPath.reset ();

    int size = ( int ) Math.ceil ( ( maxx - minx ) * phaseX + minx );

    if ( size - minx >= 2 ) {

      float prevDx = 0f;
      float prevDy = 0f;
      float curDx = 0f;
      float curDy = 0f;

      Entry prevPrev = entries.get ( minx );
      Entry prev = entries.get ( minx );
      Entry cur = entries.get ( minx );
      Entry next = entries.get ( minx + 1 );

      // let the spline start
      cubicPath.moveTo ( cur.getXIndex (), cur.getVal () * phaseY );

      prevDx = ( cur.getXIndex () - prev.getXIndex () ) * intensity;
      prevDy = ( cur.getVal () - prev.getVal () ) * intensity;

      curDx = ( next.getXIndex () - cur.getXIndex () ) * intensity;
      curDy = ( next.getVal () - cur.getVal () ) * intensity;

      // the first cubic
      cubicPath.cubicTo ( prev.getXIndex () + prevDx, ( prev.getVal () + prevDy ) * phaseY,
              cur.getXIndex () - curDx,
              ( cur.getVal () - curDy ) * phaseY, cur.getXIndex (), cur.getVal () * phaseY );

      for ( int j = minx + 1, count = Math.min ( size, entries.size () - 1 ); j < count; j++ ) {

        prevPrev = entries.get ( j == 1 ? 0 : j - 2 );
        prev = entries.get ( j - 1 );
        cur = entries.get ( j );
        next = entries.get ( j + 1 );

        prevDx = ( cur.getXIndex () - prevPrev.getXIndex () ) * intensity;
        prevDy = ( cur.getVal () - prevPrev.getVal () ) * intensity;
        curDx = ( next.getXIndex () - prev.getXIndex () ) * intensity;
        curDy = ( next.getVal () - prev.getVal () ) * intensity;

        cubicPath.cubicTo ( prev.getXIndex () + prevDx, ( prev.getVal () + prevDy ) * phaseY,
                cur.getXIndex () - curDx,
                ( cur.getVal () - curDy ) * phaseY, cur.getXIndex (), cur.getVal () * phaseY );
      }

      if ( size > entries.size () - 1 ) {

        prevPrev = entries.get ( ( entries.size () >= 3 ) ? entries.size () - 3
                : entries.size () - 2 );
        prev = entries.get ( entries.size () - 2 );
        cur = entries.get ( entries.size () - 1 );
        next = cur;

        prevDx = ( cur.getXIndex () - prevPrev.getXIndex () ) * intensity;
        prevDy = ( cur.getVal () - prevPrev.getVal () ) * intensity;
        curDx = ( next.getXIndex () - prev.getXIndex () ) * intensity;
        curDy = ( next.getVal () - prev.getVal () ) * intensity;

        // the last cubic
        cubicPath.cubicTo ( prev.getXIndex () + prevDx, ( prev.getVal () + prevDy ) * phaseY,
                cur.getXIndex () - curDx,
                ( cur.getVal () - curDy ) * phaseY, cur.getXIndex (), cur.getVal () * phaseY );
      }
    }

    // if filled is enabled, close the path
    if ( dataSet.isDrawFilledEnabled () ) {

      cubicFillPath.reset ();
      cubicFillPath.addPath ( cubicPath );
      // create a new path, this is bad for performance
      drawCubicFill ( dataSet, cubicFillPath, trans,
              entryFrom.getXIndex (), entryFrom.getXIndex () + size );
    }

    mRenderPaint.setColor ( dataSet.getColor () );

    mRenderPaint.setStyle ( Paint.Style.STROKE );

    trans.pathValueToPixel ( cubicPath );

    mBitmapCanvas.drawPath ( cubicPath, mRenderPaint );

    mRenderPaint.setPathEffect ( null );
  }

  protected void drawCubicFill ( LineDataSet dataSet, Path spline, Transformer trans,
                                 int from, int to ) {

    float fillMin = mChart.getFillFormatter ()
            .getFillLinePosition ( dataSet, mChart.getLineData (), mChart.getYChartMax (),
                    mChart.getYChartMin () );

    spline.lineTo ( to - 1, fillMin );
    spline.lineTo ( from, fillMin );
    spline.close ();

    mRenderPaint.setStyle ( Paint.Style.FILL );

    mRenderPaint.setColor ( dataSet.getFillColor () );
    // filled is drawn with less alpha
    mRenderPaint.setAlpha ( dataSet.getFillAlpha () );

    trans.pathValueToPixel ( spline );
    mBitmapCanvas.drawPath ( spline, mRenderPaint );

    mRenderPaint.setAlpha ( 255 );
  }

  /**
   * Draws a normal line.
   *
   * @param c
   * @param dataSet
   * @param entries
   */
  protected void drawLinear ( Canvas c, LineDataSet dataSet, List< Entry > entries ) {

    int dataSetIndex = mChart.getLineData ().getIndexOfDataSet ( dataSet );

    Transformer trans = mChart.getTransformer ( dataSet.getAxisDependency () );

    float phaseX = mAnimator.getPhaseX ();
    float phaseY = mAnimator.getPhaseY ();

    mRenderPaint.setStyle ( Paint.Style.STROKE );

    Canvas canvas = null;

    // if the data-set is dashed, draw on bitmap-canvas
    if ( dataSet.isDashedLineEnabled () ) {
      canvas = mBitmapCanvas;
    } else {
      canvas = c;
    }

    Entry entryFrom = dataSet.getEntryForXIndex ( mMinX );
    Entry entryTo = dataSet.getEntryForXIndex ( mMaxX );

    int minx = Math.max ( dataSet.getEntryPosition ( entryFrom ), 0 );
    int maxx = Math.min ( dataSet.getEntryPosition ( entryTo ) + 1, entries.size () );

    int range = ( maxx - minx ) * 4 - 4;

    LineBuffer buffer = mLineBuffers[ dataSetIndex ];
    buffer.setPhases ( phaseX, phaseY );
    buffer.limitFrom ( minx );
    buffer.limitTo ( maxx );
    buffer.feed ( entries );

    trans.pointValuesToPixel ( buffer.buffer );

    // more than 1 color
    if ( dataSet.getColors ().size () > 1 ) {

      for ( int j = 0; j < range; j += 4 ) {

        if ( ! mViewPortHandler.isInBoundsRight ( buffer.buffer[ j ] ) )
          break;

        // make sure the lines don't do shitty things outside
        // bounds
        if ( ! mViewPortHandler.isInBoundsLeft ( buffer.buffer[ j + 2 ] )
                || ( ! mViewPortHandler.isInBoundsTop ( buffer.buffer[ j + 1 ] ) && ! mViewPortHandler
                .isInBoundsBottom ( buffer.buffer[ j + 3 ] ) )
                || ( ! mViewPortHandler.isInBoundsTop ( buffer.buffer[ j + 1 ] ) && ! mViewPortHandler
                .isInBoundsBottom ( buffer.buffer[ j + 3 ] ) ) )
          continue;

        // get the color that is set for this line-segment

        if ( ! mUseAlt ) {
          mUseAlt = isUseAlt ( entries );
        }

        mRenderPaint.setColor ( dataSet.getColor ( j / 4 + minx ) );
        if ( mUseAlt ) {
          if ( dataSet.getDefaultColor () != dataSet.getAltColor ( j / 4 + minx ) ) {
            mRenderPaint.setColor ( dataSet.getAltColor ( j / 4 + minx ) );
          }
        }

        canvas.drawLine ( buffer.buffer[ j ], buffer.buffer[ j + 1 ],
                buffer.buffer[ j + 2 ], buffer.buffer[ j + 3 ], mRenderPaint );
      }

    } else { // only one color per dataset

      if ( ! mUseAlt ) {
        mUseAlt = isUseAlt ( entries );
      }
      mRenderPaint.setColor ( dataSet.getColor ( ) );
      if ( mUseAlt ) {
        if ( dataSet.getDefaultColor () != dataSet.getAltColor ( ) ) {
          mRenderPaint.setColor ( dataSet.getAltColor () );
        }
      }

      // c.drawLines(buffer.buffer, mRenderPaint);
      //fixme: add "padding" here; - dataSet.getCircleSize () / 2;

      for ( int j = 0; j < range; j += 4 ) {
        // recalc x0 x1;
        float offset = dataSet.getCircleSize () / 2;
        float x0 = buffer.buffer[ j ],
                x1 = buffer.buffer[ j + 2 ],
                y0 = buffer.buffer[ j + 1 ],
                y1 = buffer.buffer[ j + 3 ];

        // critical: some desc of calc inside PlotHelper (!);
        // todo: recalc, need angle

//            if ( buffer.buffer [ j ] > buffer.buffer [ j + 2 ] ) {
//              x0 = buffer.buffer [ j ] - offset;
//              x1 = buffer.buffer [ j + 2 ] + offset;
//            } else if ( buffer.buffer [ j ] < buffer.buffer [ j + 2 ] ) {
//              x0 = buffer.buffer [ j ] + offset;
//              x1 = buffer.buffer [ j + 2 ] - offset;
//            }
//
//            if ( buffer.buffer [ j + 1 ] > buffer.buffer [ j + 3 ] ) {
//              y0 = buffer.buffer [ j + 1 ] - offset;
//              y1 = buffer.buffer [ j + 3 ] + offset;
//            } else if ( buffer.buffer [ j + 1 ] < buffer.buffer [ j + 3 ] ) {
//              y0 = buffer.buffer [ j + 1 ] + offset;
//              y1 = buffer.buffer [ j + 3 ] - offset;
//            }

        canvas.drawLine ( x0, y0,
                x1, y1, mRenderPaint );
      }
//            canvas.drawLines ( buffer.buffer, 0, range,
//                    mRenderPaint );
    }

    mRenderPaint.setPathEffect ( null );

    // if drawing filled is enabled
    if ( dataSet.isDrawFilledEnabled () && entries.size () > 0 ) {
      drawLinearFill ( c, dataSet, entries, minx, maxx, trans );
    }
  }

  protected void drawLinearFill ( Canvas c, LineDataSet dataSet, List< Entry > entries, int minx,
                                  int maxx,
                                  Transformer trans ) {

    mRenderPaint.setStyle ( Paint.Style.FILL );

    mRenderPaint.setColor ( dataSet.getFillColor () );
    // filled is drawn with less alpha
    mRenderPaint.setAlpha ( dataSet.getFillAlpha () );

    Path filled = generateFilledPath (
            entries,
            mChart.getFillFormatter ().getFillLinePosition ( dataSet, mChart.getLineData (),
                    mChart.getYChartMax (), mChart.getYChartMin () ), minx, maxx );

    trans.pathValueToPixel ( filled );

    c.drawPath ( filled, mRenderPaint );

    // restore alpha
    mRenderPaint.setAlpha ( 255 );
  }

  /**
   * Generates the path that is used for filled drawing.
   *
   * @param entries
   * @return
   */
  private Path generateFilledPath ( List< Entry > entries, float fillMin, int from, int to ) {

    float phaseX = mAnimator.getPhaseX ();
    float phaseY = mAnimator.getPhaseY ();

    Path filled = new Path ();
    filled.moveTo ( entries.get ( from ).getXIndex (), fillMin );
    filled.lineTo ( entries.get ( from ).getXIndex (), entries.get ( from ).getVal () * phaseY );

    // create a new path
    for ( int x = from + 1, count = ( int ) Math.ceil ( ( to - from ) * phaseX + from ); x < count; x++ ) {

      Entry e = entries.get ( x );
      filled.lineTo ( e.getXIndex (), e.getVal () * phaseY );
    }

    // close up
    filled.lineTo (
            entries.get (
                    Math.max (
                            Math.min ( ( int ) Math.ceil ( ( to - from ) * phaseX + from ) - 1,
                                    entries.size () - 1 ), 0 ) ).getXIndex (), fillMin );

    filled.close ();

    return filled;
  }

  @Override
  public void drawValues ( Canvas c ) {

    if ( mChart.getLineData ().getYValCount () < mChart.getMaxVisibleCount ()
            * mViewPortHandler.getScaleX () ) {

      List< LineDataSet > dataSets = mChart.getLineData ().getDataSets ();

      for ( int i = 0; i < dataSets.size (); i++ ) {

        LineDataSet dataSet = dataSets.get ( i );

        if ( ! dataSet.isDrawValuesEnabled () )
          continue;

        // apply the text-styling defined by the DataSet
        applyValueTextStyle ( dataSet );

        Transformer trans = mChart.getTransformer ( dataSet.getAxisDependency () );

        // make sure the values do not interfear with the circles
        int valOffset = ( int ) ( dataSet.getCircleSize () * 1.75f );

        if ( ! dataSet.isDrawCirclesEnabled () )
          valOffset = valOffset / 2;

        List< Entry > entries = dataSet.getYVals ();

        Entry entryFrom = dataSet.getEntryForXIndex ( mMinX );
        Entry entryTo = dataSet.getEntryForXIndex ( mMaxX );

        int minx = Math.max ( dataSet.getEntryPosition ( entryFrom ), 0 );
        int maxx = Math.min ( dataSet.getEntryPosition ( entryTo ) + 1, entries.size () );

        float[] positions = trans.generateTransformedValuesLine (
                entries, mAnimator.getPhaseX (), mAnimator.getPhaseY (), minx, maxx );

        for ( int j = 0; j < positions.length; j += 2 ) {

          float x = positions[ j ];
          float y = positions[ j + 1 ];

          if ( ! mViewPortHandler.isInBoundsRight ( x ) )
            break;

          if ( ! mViewPortHandler.isInBoundsLeft ( x ) || ! mViewPortHandler.isInBoundsY ( y ) )
            continue;

          float val = entries.get ( j / 2 + minx ).getVal ();

          c.drawText ( dataSet.getValueFormatter ().getFormattedValue ( val ), x,
                  y - valOffset,
                  mValuePaint );
        }
      }
    }
  }

  @Override
  public void drawExtras ( Canvas c ) {
    drawCircles ( c );
    drawDrawables ( c );
  }


  protected void drawDrawables ( Canvas c ) {
    mRenderPaint.setStyle ( Paint.Style.FILL );

    float phaseX = mAnimator.getPhaseX ();
    float phaseY = mAnimator.getPhaseY ();

//      Log.d ( "Lib", "drawDrawables; phaseX : phaseY " + phaseX + " : " + phaseY );

//      List<LineDataSet> dataSets = mChart.getLineData().getDataSets();

    List< BitmapSet > dataSets = mChart.getLineData ().getBitmapSets ();
    for ( int i = 0; i < dataSets.size (); i++ ) {


      BitmapSet dataSet = dataSets.get ( i );
//        if ( dataSet.getYValue () != 0 ) continue;

      Transformer trans = mChart.getTransformer ( dataSet.getAxisDependency () );

      List< Entry > entries = dataSet.getEntries ();

      Entry entryFrom = dataSet.getEntryForXIndex ( ( mMinX < 0 ) ? 0 : mMinX );
      Entry entryTo = dataSet.getEntryForXIndex ( mMaxX );

      int minx = Math.max ( dataSet.getEntryPosition ( entryFrom ), 0 );
      int maxx = Math.min ( dataSet.getEntryPosition ( entryTo ) + 1, entries.size () );

      CircleBuffer buffer = mBitmapBuffers[ i ];
      buffer.setPhases ( phaseX, phaseY );
      buffer.limitFrom ( minx );
      buffer.limitTo ( maxx );
      buffer.feed ( entries );

      trans.pointValuesToPixel ( buffer.buffer );

      for ( int j = 0, count = ( int ) Math.ceil ( ( maxx - minx ) * phaseX + minx ) * 2; j < count; j += 2 ) {

        float x = buffer.buffer[ j ];
        float y = buffer.buffer[ j + 1 ];

//          if ( dataSet.getXIndex () == 0 ) {
//
//            // move y
//          } else {
//            // move x
//            x -= dataSet.getBitmap ().getHeight () / 2;
//            y -= dataSet.getBitmap ().getWidth ();
//          }

//          if (!mViewPortHandler.isInBoundsRight(x)) {
//            break;
//          }

        // make sure the circles don't do shitty things outside
        // bounds
//          if (!mViewPortHandler.isInBoundsLeft(x) || !mViewPortHandler.isInBoundsY(y,dataSet.getBitmap ().getHeight ())) {
//            continue;
//          }

        // j / 2 -> get entry -> check if its y - > move different - > x, y + height/2
        if ( !dataSet.isOffset () ) {
          if ( entries.get ( j / 2 ).getXIndex () == 0 ) {
            c.drawBitmap ( dataSet.getBitmap (), x, y - dataSet.getBitmap ().getHeight () / 2, mRenderPaint );
          } else {
            c.drawBitmap ( dataSet.getBitmap (), x - dataSet.getBitmap ().getWidth () / 2, y - dataSet.getBitmap ().getHeight (), mRenderPaint );
          }
        } else {
          // drawable inside graf;
          // draw with image size offset
          float x_offset = 0f;
          float y_offset = 0f;
          x_offset = dataSet.getBitmap ().getWidth () - dataSet.getCircleSize ();
          y_offset = dataSet.getBitmap ().getHeight () / 2;

          c.drawBitmap ( dataSet.getBitmap (), x - x_offset, y - y_offset, mRenderPaint );

          mHintDrawListener.onDrawHint ( c, x - x_offset, y - y_offset );
        }
//          mRenderPaint.setColor ( Color.BLACK );
//          c.drawCircle ( x, y, 10, mRenderPaint );
      }
    }
  }

//    private Bitmap findBitmap ( int x, float y ) {
//
//      Iterator < BitmapSet > iter = mChart.getLineData ().getBitmapSets ().iterator ();
//
//      while ( iter.hasNext () ) {
//        BitmapSet bitmapSet = iter.next ();
//        if ( bitmapSet.getXIndex ( ) != -1 && bitmapSet.getXIndex ( )  == x ) {
//          return bitmapSet.getBitmap ();
//        } else
//        if ( bitmapSet.getYValue () != -1 && bitmapSet.getYValue ()  == y ) {
//          return bitmapSet.getBitmap ();
//        }
//
//
//      }
//
//      /** cannot find bitmap */
//      return null;
//
//    }
//
//  private void drawDrawablesY ( Canvas c, List < Entry > entries, float [] coordinates, int minx, int maxx, float phaseX ) {
//    mRenderPaint.setStyle(Paint.Style.FILL);
//
//    int size = (int)Math.ceil((maxx-minx) * phaseX + minx);
//
//    List < BitmapSet > dataSets = mChart.getLineData ().getBitmapSets ();
//
//    for (int i = minx < 0 ? 0 : minx; i < size; i++) {
//      Entry e = entries.get ( i );
//      Iterator < BitmapSet > iterator = dataSets.iterator ();
//      while ( iterator.hasNext () ) {
//        BitmapSet bitmapSet = iterator.next ( );
//
//        if ( bitmapSet.getYValue () == e.getVal () ) {
//          c.drawBitmap ( bitmapSet.getBitmap (), 0, coordinates [i*2+1], mRenderPaint );
//        }
//      }
//    }
//
//  }


  protected void drawCircles ( Canvas c ) {
//        mRenderPaint.setStyle ( Paint.Style.FILL );

    float phaseX = mAnimator.getPhaseX ();
    float phaseY = mAnimator.getPhaseY ();

    List< LineDataSet > dataSets = mChart.getLineData ().getDataSets ();

    for ( int i = 0; i < dataSets.size (); i++ ) {

      LineDataSet dataSet = dataSets.get ( i );

      if ( ! dataSet.isVisible () || ! dataSet.isDrawCirclesEnabled () )
        continue;

      mCirclePaintInner.setColor ( dataSet.getCircleHoleColor () );

      Transformer trans = mChart.getTransformer ( dataSet.getAxisDependency () );


      List< Entry > entries = dataSet.getYVals ();



      Entry entryFrom = dataSet.getEntryForXIndex ( ( mMinX < 0 ) ? 0 : mMinX );
      Entry entryTo = dataSet.getEntryForXIndex ( mMaxX );


      int minx = Math.max ( dataSet.getEntryPosition ( entryFrom ), 0 );
      int maxx = Math.min ( dataSet.getEntryPosition ( entryTo ) + 1, entries.size () );


      CircleBuffer buffer = mCircleBuffers[ i ];
      buffer.setPhases ( phaseX, phaseY );
      buffer.limitFrom ( minx );
      buffer.limitTo ( maxx );
      buffer.feed ( entries );

      trans.pointValuesToPixel ( buffer.buffer );

      float hole_size = dataSet.getCircleSize () * dataSet.getCircleHoleSize () / 100;

      // entry, coordinates;
//            drawDrawablesY ( c, entries, buffer.buffer, minx, maxx, phaseX );
      for ( int j = 0, count = ( int ) Math.ceil ( ( maxx - minx ) * phaseX + minx ) * 2; j < count; j += 2 ) {

        float x = buffer.buffer[ j ];
        float y = buffer.buffer[ j + 1 ];

//                if (!mViewPortHandler.isInBoundsRight(x))
//                    break;

        // make sure the circles don't do shitty things outside
        // bounds
//                if (!mViewPortHandler.isInBoundsLeft(x) || !mViewPortHandler.isInBoundsY(y))
//                    continue;

        int circleColor = dataSet.getCircleColor ( j / 2 + minx );

        mRenderPaint.setColor ( circleColor );

//        Log.d ( "Lib", "drawCircles: " + x + ":" + y );

        /** already set to true -> use always alt colors */
        if ( !mUseAlt ) {
          mUseAlt = isUseAlt ( entries );
        }

        // todo: need refactoring
        // if alt == default color -> use non alt color for drawing;
        if ( mUseAlt && dataSet.getAltCircleColor ( j / 2 + minx ) != dataSet.getDefaultColor () ) {
          mCirclePaintInner.setColor ( dataSet.getAltCircleHoleColor () );
          if ( isDraggable ( entries )  ) {
            mRenderPaint.setColor ( dataSet.getAltCircleColor ( j / 2 + minx ) );
            mRenderPaint.setStyle ( Paint.Style.FILL );

            c.drawCircle ( x, y, dataSet.getCircleSize (),
                    mRenderPaint );
            if ( dataSet.isDrawCircleHoleEnabled ()
                    && circleColor != mCirclePaintInner.getColor () ) {
              c.drawCircle ( x, y,
                      hole_size,
                      mCirclePaintInner );
            }
          } else {
            mRenderPaint.setColor ( dataSet.getAltCircleColor ( j / 2 + minx ) );
            if ( ! dataSet.isHoleTransparent () ) {
              mRenderPaint.setStyle ( Paint.Style.FILL );
//              Log.d ( "Lib", "transparent: false " );
              c.drawCircle ( x, y, dataSet.getCircleSize (),
                      mRenderPaint );

              if ( dataSet.isDrawCircleHoleEnabled ()
                      && circleColor != mCirclePaintInner.getColor () ) {
                c.drawCircle ( x, y,
                        hole_size,
                        mCirclePaintInner );
              }
            } else if ( dataSet.isDrawCircleHoleEnabled () ) {
//              Log.d ( "Lib", "transparent: true " );
              mRenderPaint.setStyle ( Paint.Style.STROKE );
              mRenderPaint.setStrokeWidth ( dataSet.getCircleSize () - dataSet.getCircleSize () * dataSet.getCircleHoleSize () / 100 );
              c.drawCircle ( x, y, dataSet.getCircleSize (),
                      mRenderPaint );
            }
          }

        } else {
          if ( ! dataSet.isHoleTransparent () ) {
            mRenderPaint.setStyle ( Paint.Style.FILL );
//            Log.d ( "Lib", "transparent: false " );
            c.drawCircle ( x, y, dataSet.getCircleSize (),
                    mRenderPaint );

            if ( dataSet.isDrawCircleHoleEnabled ()
                    && circleColor != mCirclePaintInner.getColor () ) {
              c.drawCircle ( x, y,
                      hole_size,
                      mCirclePaintInner );
            }
          } else if ( dataSet.isDrawCircleHoleEnabled () ) {
//            Log.d ( "Lib", "transparent: true " );
            mRenderPaint.setStyle ( Paint.Style.STROKE );
            mRenderPaint.setStrokeWidth ( dataSet.getCircleSize () - dataSet.getCircleSize () * dataSet.getCircleHoleSize () / 100 );
            c.drawCircle ( x, y, dataSet.getCircleSize (),
                    mRenderPaint );
          }
        }
      }
    }
  }

  private boolean isDraggable ( List < Entry > entries ) {
    Iterator < Entry > iter = entries.iterator ();

    while ( iter.hasNext () ) {
      Entry entry = iter.next ();
      if ( entry.getData () != null && ( boolean ) entry.getData () ) return true;
    }
    return false;
  }

  private boolean isUseAlt ( List < Entry > entries ) {
    Iterator < Entry > iter = entries.iterator ();

    while ( iter.hasNext () ) {
      if ( iter.next ().isUseAlt () ) return true;
    }
    return false;
  }

  @Override
  public void drawHighlighted ( Canvas c, Highlight[] indices ) {

    for ( int i = 0; i < indices.length; i++ ) {

      LineDataSet set = mChart.getLineData ().getDataSetByIndex ( indices[ i ]
              .getDataSetIndex () );

      if ( set == null || ! set.isHighlightEnabled () )
        continue;

      mHighlightPaint.setColor ( set.getHighLightColor () );
      mHighlightPaint.setStrokeWidth ( set.getHighlightLineWidth () );

      int xIndex = indices[ i ].getXIndex (); // get the
      // x-position

      if ( xIndex > mChart.getXChartMax () * mAnimator.getPhaseX () )
        continue;

      final float yVal = set.getYValForXIndex ( xIndex );
      if ( yVal == Float.NaN )
        continue;

      float y = yVal * mAnimator.getPhaseY (); // get
      // the
      // y-position

      float[] pts = new float[] {
              xIndex, mChart.getYChartMax (), xIndex, mChart.getYChartMin (), mChart.getXChartMin (), y,
              mChart.getXChartMax (), y
      };

      mChart.getTransformer ( set.getAxisDependency () ).pointValuesToPixel ( pts );

      // draw the lines
      drawHighlightLines ( c, pts, set.isHorizontalHighlightIndicatorEnabled (), set.isVerticalHighlightIndicatorEnabled () );
    }
  }
}
