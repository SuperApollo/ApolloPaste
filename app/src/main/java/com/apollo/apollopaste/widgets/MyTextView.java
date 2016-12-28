package com.apollo.apollopaste.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewParent;
import android.widget.TextView;

/**
 * Created by zayh_yf20160909 on 2016/12/28.
 */

public class MyTextView extends TextView {
    private OnTextViewListner onTextViewListner;
    private final String TAG = MyTextView.class.getSimpleName();

    public OnTextViewListner getOnTextViewListner() {
        return onTextViewListner;
    }

    public void setOnTextViewListner(OnTextViewListner onTextViewListner) {
        this.onTextViewListner = onTextViewListner;
    }

    public MyTextView(Context context) {
        super(context);
    }

    public MyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (onTextViewListner != null) {
                    onTextViewListner.onPositionChanged(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                Log.i(TAG, event.getY() + "=========");
                ViewParent parent = getParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (onTextViewListner != null) {
                    onTextViewListner.onPositionChanged(false);
                }
                break;
        }
        return super.onTouchEvent(event);
    }


    /**
     * 监听是否点击在textview上
     */
    public interface OnTextViewListner {
        void onPositionChanged(boolean isOnTextView);
    }
}
