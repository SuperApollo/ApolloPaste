package com.apollo.apollopaste.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.ScrollView;

/**
 * Created by zayh_yf20160909 on 2016/12/28.
 */

public class MyListview extends ListView {
    private ScrollView parentView;

    public View getParentView() {
        return parentView;
    }

    public void setParentView(View parentView) {
        this.parentView = (ScrollView) parentView;
    }

    public MyListview(Context context) {
        super(context);
    }

    public MyListview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (parentView == null) {
            return super.onInterceptTouchEvent(ev);
        } else {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    parentView.requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_UP:
                    parentView.requestDisallowInterceptTouchEvent(false);
                    break;
            }
            return super.onInterceptTouchEvent(ev);
        }

    }
}
