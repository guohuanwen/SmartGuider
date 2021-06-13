package com.bigwen.guider;

import android.content.Context;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by bigwen on 6/13/21.
 */
public class OpenCVService {

    private static final String TAG = "OpenCVService";
    public static final int JAVA_DETECTOR = 0;
    public static final int NATIVE_DETECTOR = 1;
    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);

    private int mDetectorType = JAVA_DETECTOR;
    private Context mContext;
    private DetectionBasedTracker mNativeDetector;
    private CascadeClassifier mJavaDetector;
    private Mat mGray, mRgba;
    private int mAbsoluteFaceSize, mRelativeFaceSize;
    private int viewWidth, viewHeight;
    private ConnectedCallback connectedCallback;

    public OpenCVService(Context context) {
        mContext = context;
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(mContext) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("detection_based_tracker");

                    try {
                        // load cascade file from application resources
                        InputStream is = mContext.getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = mContext.getDir("cascade", Context.MODE_PRIVATE);
                        File mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0);

                        cascadeDir.delete();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }
                    if (connectedCallback != null) connectedCallback.onConnected();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public void connected(ConnectedCallback callback) {
        connectedCallback = callback;
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, mContext, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public Mat recognize(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        Log.i(TAG, "onCameraFrame: " + mRgba.width() + " " + mRgba.height());

        Core.rotate(mRgba, mRgba, Core.ROTATE_90_COUNTERCLOCKWISE);
        Core.rotate(mGray, mGray, Core.ROTATE_90_COUNTERCLOCKWISE);

        Log.i(TAG, "onCameraFrame1: " + mRgba.width() + " " + mRgba.height());

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
            mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        }

        MatOfRect faces = new MatOfRect();

        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        } else if (mDetectorType == NATIVE_DETECTOR) {
            if (mNativeDetector != null)
                mNativeDetector.detect(mGray, faces);
        } else {
            Log.e(TAG, "Detection method is not selected!");
        }

        Rect[] facesArray = faces.toArray();
        for (int i = 0; i < facesArray.length; i++) {
            Log.i(TAG, "onCameraFrame: found ");
            Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);
        }

        Log.i(TAG, "onCameraFrame2: " + mRgba.width() + " " + mRgba.height());
        Core.flip(mRgba, mRgba, 1);
//        Core.rotate(mRgba, mRgba, Core.ROTATE_90_CLOCKWISE);
        Log.i(TAG, "onCameraFrame3: " + mRgba.width() + " " + mRgba.height());

        if (viewWidth > 0 && viewHeight > 0) {
            Mat rotateMat = Imgproc.getRotationMatrix2D(new Point(0, 0),
                    0, Math.max(viewWidth / mRgba.width(), viewHeight / mRgba.height()));
            Imgproc.warpAffine(mRgba, mRgba, rotateMat, new Size(viewWidth, viewHeight));
        }
        return mRgba;
    }

    public void initMat() {
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void releaseMat() {
        mGray.release();
        mRgba.release();
    }

    public void setViewSize(int width, int height) {
        viewWidth = width;
        viewHeight = height;
    }

    public void setDetectorType(int type) {
        if (mDetectorType != type) {
            mDetectorType = type;

            if (type == NATIVE_DETECTOR) {
                Log.i(TAG, "Detection Based Tracker enabled");
                mNativeDetector.start();
            } else {
                Log.i(TAG, "Cascade detector enabled");
                mNativeDetector.stop();
            }
        }
    }

    public interface ConnectedCallback {
        void onConnected();
    }
}
