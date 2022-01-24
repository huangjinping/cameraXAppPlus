package com.harris.inxcamerax;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;


import com.google.common.util.concurrent.ListenableFuture;
import com.harris.inxcamerax.utils.CameraConstant;
import com.harris.inxcamerax.utils.CameraParam;
import com.harris.inxcamerax.utils.CameraXPreviewViewTouchListener;
import com.harris.inxcamerax.utils.FocusImageView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ViewPort;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;

/**
 * author Created by harrishuang on 6/28/21.
 * email : huangjinping1000@163.com
 */
public class CameraXActivity extends AppCompatActivity {
    private ImageCapture imageCapture;
    private CameraControl mCameraControl;
    private CameraInfo mCameraInfo;
    private boolean isInfer = true;
    private int imageRotationDegrees = 0;
    private int flashMode = ImageCapture.FLASH_MODE_OFF;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
    private ExecutorService cameraExecutor;
    private Bitmap bitmapBuffer;

    private final String TAG = "CameraXActivity";
    private final int REQUEST_CODE_PERMISSIONS = 10;
    private String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private PreviewView viewFinder;
    private FocusImageView focus_view;
    private ImageButton camera_switch_button;
    private ImageButton camera_capture_button;
    private ImageButton photo_view_button;
    private View box_prediction;
    private ImageButton flash_switch_button;

    private Context context;
    private File outputDirectory;
    private final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private CameraParam mCameraParam;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int flag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        Window window = this.getWindow();
        window.setFlags(flag, flag);
        setContentView(R.layout.activity_camerax);
        Intent intent = getIntent();
        if (intent.getParcelableExtra(CameraConstant.CAMERA_PARAM_KEY) != null) {
            mCameraParam = (CameraParam) intent.getParcelableExtra(CameraConstant.CAMERA_PARAM_KEY);
        }
        initView();
        outputDirectory = getOutputDirectory();
        context = this;
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
        camera_capture_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhoto();
            }
        });
        cameraExecutor = Executors.newSingleThreadExecutor();
        camera_switch_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CameraSelector.DEFAULT_FRONT_CAMERA == cameraSelector) {
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                } else {
                    cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                }
                startCamera();
            }
        });


        flash_switch_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (flashMode) {
                    case ImageCapture.FLASH_MODE_OFF:
                        flashMode = ImageCapture.FLASH_MODE_ON;
                        flash_switch_button.setImageResource(R.drawable.open_flash);
                        break;
                    case ImageCapture.FLASH_MODE_ON:
                        flashMode = ImageCapture.FLASH_MODE_AUTO;
                        flash_switch_button.setImageResource(R.drawable.auto_flash);
                        break;
                    case ImageCapture.FLASH_MODE_AUTO:
                        flashMode = ImageCapture.FLASH_MODE_OFF;
                        flash_switch_button.setImageResource(R.drawable.stop_flash);
                        break;
                }
                startCamera();
            }
        });
    }


    public File getOutputDirectory() {
        File file = new File(getExternalCacheDir(), getString(R.string.app_name));
        if (!file.exists()) {
            file.mkdir();
        }
        return file;
    }

    private void takePhoto() {
        ImageCapture tImageCapture = imageCapture;
        if (tImageCapture == null) {
            return;
        }
        File photoFile = new File(outputDirectory, new SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg");
        Log.d(TAG, "==photoFile===" + photoFile.getAbsolutePath());
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

//        imageCapture.takePicture(ContextCompat.getMainExecutor(this),new ImageCapture.OnImageCapturedCallback(){
//            @Override
//            public void onCaptureSuccess(@NonNull ImageProxy image) {
//                super.onCaptureSuccess(image);
//            }
//
//            @Override
//            public void onError(@NonNull ImageCaptureException exception) {
//                super.onError(exception);
//            }
//        });
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {

            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Uri savedUri = Uri.fromFile(photoFile);
//                String msg = "Photo capture succeeded: $savedUri";
//                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
//                Bitmap bitmap = Tools.bitmapClip(CameraXActivity.this, photoFile.getAbsolutePath(), false);
//                val savedUri = Uri.fromFile(photoFile);
//                Log.d(TAG, "拍照成功，保存路径: $savedUri");
//                Glide.with(CameraXActivity.this)
//                        .load(savedUri)
//                        .apply(RequestOptions.circleCropTransform())
//                        .into(photo_view_button);

//                photo_view_button.setImageBitmap(bitmap);
//                Log.d(TAG, msg);

                Intent intent = new Intent();
                intent.putExtra(CameraConstant.PICTURE_PATH_KEY, photoFile.getAbsolutePath());
                setResult(RESULT_OK, intent);
                finish();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Photo capture failed: " + exception.getMessage() + "");

            }
        });

    }

    public boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS
        ) {

            Log.d(TAG, ContextCompat.checkSelfPermission(getBaseContext(), permission) + "======permission===" + permission);
            if (!(ContextCompat.checkSelfPermission(getBaseContext(), permission) == PackageManager.PERMISSION_GRANTED)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "没有授权，无法使用！", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void startCamera() {

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    int rotation = getWindowManager().getDefaultDisplay().getRotation();
                    Preview preview = new Preview.Builder().build();
                    preview.setSurfaceProvider(viewFinder.getSurfaceProvider());


                    //设置相机支持拍照
                    imageCapture = new ImageCapture
                            .Builder()
                            .setFlashMode(flashMode)
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build();


                    OrientationEventListener orientationEventListener = new OrientationEventListener(context) {
                        @Override
                        public void onOrientationChanged(int orientation) {
                            int rotation;

                            // Monitors orientation values to determine the target rotation value
                            if (orientation >= 45 && orientation < 135) {
                                rotation = Surface.ROTATION_270;
                            } else if (orientation >= 135 && orientation < 225) {
                                rotation = Surface.ROTATION_180;
                            } else if (orientation >= 225 && orientation < 315) {
                                rotation = Surface.ROTATION_90;
                            } else {
                                rotation = Surface.ROTATION_0;
                            }

                            imageCapture.setTargetRotation(rotation);
                        }
                    };

                    orientationEventListener.enable();


                    //设置相机支持图像分析
                    ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build();

                    //实时获取图像进行分析
                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), new ImageAnalysis.Analyzer() {
                        @Override
                        public void analyze(@NonNull ImageProxy image) {
                            if (isInfer) {

                            }
                        }
                    });

                    try {
                        cameraProvider.unbindAll();

//                        ViewPort viewPort = new ViewPort.Builder(
//                                new Rational(108, 55),
//                                rotation).setScaleType(ViewPort.FILL_END).build();

                        ViewPort viewPort = viewFinder.getViewPort();
//                        Rational aspectRatio = viewPort1.getAspectRatio();
//                        Log.d(TAG, aspectRatio.getNumerator() + "====1==" + aspectRatio.getDenominator());

                        UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
                                .addUseCase(preview)
                                .addUseCase(imageAnalysis)
                                .addUseCase(imageCapture)
                                .setViewPort(viewPort)
                                .build();


                        Camera camera = cameraProvider.bindToLifecycle(CameraXActivity.this, cameraSelector, useCaseGroup);


//                        Camera camera = cameraProvider.bindToLifecycle(
//                                CameraX1Activity.this,
//                                cameraSelector,
//                                preview,
//                                imageCapture,
//                                imageAnalysis
//                        );
                        // 相机控制，如点击
                        mCameraControl = camera.getCameraControl();
                        mCameraInfo = camera.getCameraInfo();

                        initCameraListener();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }, ContextCompat.getMainExecutor(this));

    }

    private void initCameraListener() {
        LiveData<ZoomState> zoomState = mCameraInfo.getZoomState();
        CameraXPreviewViewTouchListener cameraXPreviewViewTouchListener = new CameraXPreviewViewTouchListener(this);
        cameraXPreviewViewTouchListener.setmCustomTouchListener(new CameraXPreviewViewTouchListener.CustomTouchListener() {
            @Override
            public void zoom(Float delta) {
                Log.d(TAG, "缩放");
                ZoomState value = zoomState.getValue();
                float currentZoomRatio = value.getZoomRatio();
                mCameraControl.setZoomRatio(currentZoomRatio * delta);
            }

            @Override
            public void click(Float x, Float y) {
                Log.d(TAG, "单击");
                MeteringPointFactory factory = viewFinder.getMeteringPointFactory();
                MeteringPoint point = factory.createPoint(x, y);
                FocusMeteringAction action = new FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                        .setAutoCancelDuration(3, TimeUnit.SECONDS).build();
                focus_view.startFocus(new Point(x.intValue(), y.intValue()));
                ListenableFuture<FocusMeteringResult> future = mCameraControl.startFocusAndMetering(action);
                future.addListener(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final FocusMeteringResult result = future.get();
                            if (result.isFocusSuccessful()) {
                                focus_view.onFocusSuccess();
                            } else {
                                focus_view.onFocusFailed();
                            }

                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                            e.printStackTrace();
                        }
                    }
                }, ContextCompat.getMainExecutor(context));
            }

            @Override
            public void doubleClick(Float x, Float y) {
                Log.d(TAG, "双击");
                float currentZoomRatio = zoomState.getValue().getZoomRatio();
                if (currentZoomRatio > zoomState.getValue().getMinZoomRatio()) {
                    mCameraControl.setLinearZoom(0f);
                } else {
                    mCameraControl.setLinearZoom(0.5f);
                }
            }

            @Override
            public void longPress(Float x, Float y) {
                Log.d(TAG, "长按");

            }
        });
        viewFinder.setOnTouchListener(cameraXPreviewViewTouchListener);

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    private void initView() {
        viewFinder = (PreviewView) findViewById(R.id.viewFinder);
        focus_view = (FocusImageView) findViewById(R.id.focus_view);
        camera_switch_button = (ImageButton) findViewById(R.id.camera_switch_button);
        camera_capture_button = (ImageButton) findViewById(R.id.camera_capture_button);
        photo_view_button = (ImageButton) findViewById(R.id.photo_view_button);
        box_prediction = (View) findViewById(R.id.box_prediction);
        flash_switch_button = (ImageButton) findViewById(R.id.flash_switch_button);

//        camera_switch_button.setOnClickListener(this);
//        camera_capture_button.setOnClickListener(this);
//        photo_view_button.setOnClickListener(this);
//        flash_switch_button.setOnClickListener(this);
    }


}
