package com.apollo.apollopaste.base;

import android.app.Application;
import android.content.Context;

/**
 * Created by zayh_yf20160909 on 2016/12/27.
 */

public class BaseApplication extends Application {

    private static Context mContext;
    private static BaseApplication mBaseApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        init();

    }

    private void init() {
        mContext = getApplicationContext();
        mBaseApplication = this;
    }

    public static BaseApplication getBaseApplication() {
        return mBaseApplication;
    }

    public static Context getContext() {
        return mContext;
    }
}
