
package com.github.mikephil.charting.charts;

import android.content.Context;
import android.util.AttributeSet;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.interfaces.LineDataProvider;
import com.github.mikephil.charting.listener.OnHintDrawListener;
import com.github.mikephil.charting.renderer.LineChartRenderer;
import com.github.mikephil.charting.utils.FillFormatter;

/**
 * Chart that draws lines, surfaces, circles, ...
 * 
 * @author Philipp Jahoda
 */
public class LineChart extends BarLineChartBase<LineData> implements LineDataProvider {

    private FillFormatter mFillFormatter;

    public LineChart(Context context) {
        super(context);
    }

    public LineChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LineChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * OnHintDrawListener
    * */
    OnHintDrawListener mHintDrawListener;

    /**
     * Contains min/max X and Y values for draggable entry;
    */
    Entry mMaxDragEntry;
    Entry mMinDragEntry;

    @Override
    protected void init() {
        super.init();

        mRenderer = new LineChartRenderer(this, mAnimator, mViewPortHandler, mHintDrawListener );
        
        mFillFormatter = new DefaultFillFormatter();
    }

    public void setOnHintDrawListener ( OnHintDrawListener listener ) {
      mHintDrawListener = listener;
      mRenderer = new LineChartRenderer(this, mAnimator, mViewPortHandler, mHintDrawListener );
    }

    public OnHintDrawListener getOnHintDrawListener () {
      return mHintDrawListener;
    }

    @Override
    protected void calcMinMax() {
        super.calcMinMax();

        // // if there is only one value in the chart
        // if (mOriginalData.getYValCount() == 1
        // || mOriginalData.getYValCount() <= mOriginalData.getDataSetCount()) {
        // mDeltaX = 1;
        // }

        if (mDeltaX == 0 && mData.getYValCount() > 0)
            mDeltaX = 1;
    }

    @Override
    public void setFillFormatter(FillFormatter formatter) {

        if (formatter == null)
            formatter = new DefaultFillFormatter();
        else
            mFillFormatter = formatter;
    }

    @Override
    public FillFormatter getFillFormatter() {
        return mFillFormatter;
    }
    
    @Override
    public LineData getLineData() {
        return mData;
    }

  public void setLimitDragEntries ( Entry min, Entry max ) {
    this.mMaxDragEntry = max;
    this.mMinDragEntry = min;
  }
  public Entry getMaxDragEntry ( ) {
    return mMaxDragEntry;
  }

  public Entry getMinDragEntry ( ) {
    return mMinDragEntry;
  }
}
