package com.github.mikephil.charting.listener;

import com.github.mikephil.charting.data.Entry;

/**
 * Created by vova on 8/4/15.
 */
public interface OnEntryDragListener {
  // need for get new values from entry; listening every change for entry;
  void OnDrag ( Entry entry, ChangeEntryStyleListener listener );
  // first drag for entry -> change style for non draggable entry;
  void OnCreate ( Entry entry );
  // draggable entry has ~same values that non draggable -> return default style
  void OnDestroy( Entry entry );
  // click on any space on plot
  void OnClick ();

}
