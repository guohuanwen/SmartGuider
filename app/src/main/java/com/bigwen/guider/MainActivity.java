package com.bigwen.guider;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

import java.util.Collections;
import java.util.List;

public class MainActivity extends CameraActivity {

    private static final String TAG = "MainActivity";
    private OpenCVService openCVService;
    private SerialService serialService;
    private CameraBridgeViewBase cameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cameraView = findViewById(R.id.fd_activity_surface_view);
        findViewById(R.id.java_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCVService.setDetectorType(OpenCVService.JAVA_DETECTOR);
            }
        });

        findViewById(R.id.native_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCVService.setDetectorType(OpenCVService.NATIVE_DETECTOR);
            }
        });

        openCVService = new OpenCVService(this);
        serialService = new SerialService(this, msg -> {

        });
        serialService.connect();

        cameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        cameraView.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {
                openCVService.initMat();
            }

            @Override
            public void onCameraViewStopped() {
                openCVService.releaseMat();
            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                return openCVService.recognize(inputFrame);
            }
        });
        cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        cameraView.post(() -> openCVService.setViewSize(cameraView.getWidth(), cameraView.getHeight()));
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(cameraView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        openCVService.connected(() -> {
            Log.i(TAG, "cameraView enableView");
            cameraView.enableView();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraView != null) {
            cameraView.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraView != null) {
            cameraView.disableView();
        }
    }
}