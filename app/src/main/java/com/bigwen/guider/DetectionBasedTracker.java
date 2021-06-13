package com.bigwen.guider;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;

public class DetectionBasedTracker {

    public DetectionBasedTracker(String cascadeName, int minFaceSize) {
        mNativeObj = nativeCreateObject(cascadeName, minFaceSize);
    }

    public void start() {
        nativeStart(mNativeObj);
    }

    public void stop() {
        nativeStop(mNativeObj);
    }

    public void setMinFaceSize(int size) {
        nativeSetFaceSize(mNativeObj, size);
    }

    public void detect(Mat imageGray, MatOfRect faces) {
        nativeDetect(mNativeObj, imageGray.getNativeObjAddr(), faces.getNativeObjAddr());
    }

    public void release() {
        nativeDestroyObject(mNativeObj);
        mNativeObj = 0;
    }

    private long mNativeObj = 0;

    public static native long nativeCreateObject(String cascadeName, int minFaceSize);

    public static native void nativeDestroyObject(long thiz);

    public static native void nativeStart(long thiz);

    public static native void nativeStop(long thiz);

    public static native void nativeSetFaceSize(long thiz, int size);

    public static native void nativeDetect(long thiz, long inputImage, long faces);
}
