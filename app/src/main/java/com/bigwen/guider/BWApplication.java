package com.bigwen.guider;

import android.app.Application;

/**
 * Created by bigwen on 6/13/21.
 */
public class BWApplication extends android.app.Application {

    private static Application application;

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
    }

    public static Application getApplication() {
        return application;
    }
}
