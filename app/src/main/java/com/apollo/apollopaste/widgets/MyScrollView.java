package com.apollo.apollopaste.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

/**
 * Created by zayh_yf20160909 on 2016/12/28.
 */

public class MyScrollView extends ScrollView {
    private final String TAG = MyScrollView.class.getSimpleName();
    private View childView;

    public void setChildView(View childView) {
        this.childView = childView;
    }

    public MyScrollView(Context context) {
        super(context);
    }

    public MyScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if ((childView != null) && checkArea(childView, ev)) {
            return false;
        } else {
            return super.onInterceptTouchEvent(ev);
        }
    }

    /**
     * 测试view是否在点击范围内
     *
     * @param v
     * @return
     */
    private boolean checkArea(View v, MotionEvent event) {
        float x = event.getRawX();
        float y = event.getRawY();
        int[] locate = new int[2];
        v.getLocationOnScreen(locate);
        int l = locate[0];
        int r = l + v.getWidth();
        int t = locate[1];
        int b = t + v.getHeight();
        boolean tag = false;
        if ((l < x) && (x < r) && (t < y) && (y < b)) {
            tag = true;
        }
        return tag;
    }

}
