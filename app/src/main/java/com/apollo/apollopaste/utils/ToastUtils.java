package com.apollo.apollopaste.utils;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.apollo.apollopaste.R;

/****
 * Toast 工具类
 *
 * @author zayh_yf01
 */
public class ToastUtils {

    private static ToastUtils mToastUtils;
    private static Toast mToast;

    private ToastUtils() {
    }

    public static ToastUtils shareInstance() {
        if (mToastUtils == null) {
            synchronized (ToastUtils.class) {
                if (mToastUtils == null) {
                    mToastUtils = new ToastUtils();
                }
            }
        }
        return mToastUtils;
    }

    public static void show(Context context, int resId) {
        show(context, context.getResources().getText(resId), Toast.LENGTH_SHORT);
    }

    public static void show(Context context, int resId, int duration) {
        show(context, context.getResources().getText(resId), duration);
    }

    public static void show(Context context, CharSequence text) {
        show(context, text, Toast.LENGTH_SHORT);
    }

    public static void show(Context context, CharSequence text, int duration) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
//        if (mToast == null) {
//            mToast = Toast.makeText(context, text, duration);
//        } else {
//            mToast.cancel();//关闭吐司显示
//            mToast = Toast.makeText(context, text, duration);
//        }

        if (mToast == null) {
            mToast = new Toast(context);
        } else {
            mToast.cancel();//关闭吐司显示
            mToast = new Toast(context);
        }
        View toastView = LayoutInflater.from(context).inflate(R.layout.toast_layout, null);
        TextView tvContent = (TextView) toastView.findViewById(R.id.tv_toast_content);
        tvContent.setText(text);
        mToast.setView(toastView);

        mToast.show();
    }

    public static void show(Context context, int resId, Object... args) {
        show(context,
                String.format(context.getResources().getString(resId), args),
                Toast.LENGTH_SHORT);
    }

    public static void show(Context context, String format, Object... args) {
        show(context, String.format(format, args), Toast.LENGTH_SHORT);
    }

    public static void show(Context context, int resId, int duration,
                            Object... args) {
        show(context,
                String.format(context.getResources().getString(resId), args),
                duration);
    }

    public static void show(Context context, String format, int duration,
                            Object... args) {
        show(context, String.format(format, args), duration);
    }
}
