package com.bigwen.guider.util;


import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by bigwen on 6/13/21.
 */
public class ThreadUtil {

    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final ExecutorService pool = Executors.newFixedThreadPool(1);

    public static void post(Runnable runnable) {
        handler.post(runnable);
    }

    public static void postDelay(Runnable runnable, long time) {
        handler.postDelayed(runnable, time);
    }

    public static void runChild(Runnable runnable) {
        pool.submit(runnable);
    }
}
