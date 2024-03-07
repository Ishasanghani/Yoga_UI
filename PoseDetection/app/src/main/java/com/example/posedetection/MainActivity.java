package com.example.posedetection;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

// Other necessary imports...

// Add this import for OptIn annotation

// Add this import for the InputImage class
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;


public class MainActivity extends AppCompatActivity {
    int PERMISSION_REQUESTS = 1;
    PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    PoseDetectorOptions options =
            new PoseDetectorOptions.Builder()
                    .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                    .build();
    PoseDetector poseDetector = PoseDetection.getClient(options);

    Canvas canvas;

    Paint mPaint = new Paint();

    Display display;

    Bitmap bitmap4Save;

    ArrayList<Bitmap> bitmapArrayList = new ArrayList<>();
    ArrayList<Bitmap> bitmap4DisplayArrayList = new ArrayList<>();
    ArrayList<Pose> poseArrayList = new ArrayList<>();

    boolean isRunning = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        previewView=findViewById(R.id.previewView);

        display = findViewById(R.id.displayOverlay);

        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint.setStrokeWidth(10);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (InterruptedException | ExecutionException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));

        if (!allPermissionsGranted()) {
            getRuntimePermissions();
        }
    }

    Runnable RunMLkit = new Runnable() {
                @Override
                public void run() {
                     poseDetector.process(InputImage.fromBitmap(bitmapArrayList.get(0),0)).addOnSuccessListener(new OnSuccessListener<Pose>() {
                         @Override
                         public void onSuccess(Pose pose) {
                             poseArrayList.add(pose);
                         }

                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                        }
                    });
                }
    };

    void bindPreview(@NonNull  ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        // enable the following line if RGBA output is needed.
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
//                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
        imageAnalysis.setAnalyzer(ActivityCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            @NonNull
            @OptIn(markerClass = ExperimentalGetImage.class)
            public void analyze(@NonNull ImageProxy imageProxy) {
                int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                @ExperimentalGetImage
                // Retrieve the Image from ImageProxy
                ByteBuffer byteBuffer = imageProxy.getImage().getPlanes()[0].getBuffer();
                byteBuffer.rewind();
                Bitmap bitmap = Bitmap.createBitmap(imageProxy.getWidth(), imageProxy.getHeight(), Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(byteBuffer);

                Matrix matrix  = new Matrix();
                matrix.postRotate(270);
                matrix.postScale(-1,1);
                Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap,0,0,imageProxy.getWidth(), imageProxy.getHeight(),matrix,false);
//                Image mediaImage = imageProxy.getImage();
//                if (rotatedBitmap != null) {
//                InputImage image =
////                        InputImage.fromBitmap(bitmap, imageProxy.getImageInfo().getRotationDegrees());
//                        InputImage.fromBitmap(rotatedBitmap, 0);
//
//                canvas = new Canvas(bitmap);
//
//                Task<Pose> result =
//                        poseDetector.process(image)
//                                .addOnSuccessListener(
//                                        new OnSuccessListener<Pose>() {
//                                            @Override
//                                            public void onSuccess(Pose pose) {
//                                                canvas = new Canvas(rotatedBitmap);
//                                                for(PoseLandmark poselandmark : pose.getAllPoseLandmarks()) {
//                                                    Log.d("PoseData","pose x: "+String.valueOf(poselandmark.getPosition().x));
//                                                    Log.d("PoseData","pose y: "+String.valueOf(poselandmark.getPosition().y));
//                                                        canvas.drawCircle(poselandmark.getPosition().x,poselandmark.getPosition().y,5, mPaint );
//                                                }
//                                                display.getBitmap(rotatedBitmap);
//                                                // Task completed successfully
//                                                // ...
//                                            }
//                                        })
//                                .addOnFailureListener(
//                                        new OnFailureListener() {
//                                            @Override
//                                            public void onFailure(@NonNull Exception e) {
//                                                // Task failed with an exception
//                                                // ...
//                                            }
//                                        });
//                    // Continue processing the image...
//                }
                // Pass image to an ML Kit Vision API
//                else {
//                    Log.e("Error", "mediaImage is null");
//                }


                // Release the ImageProxy
                bitmapArrayList.add(rotatedBitmap);

                if(poseArrayList.size() >= 1) {
                    canvas = new Canvas(bitmapArrayList.get(0));

                    for (PoseLandmark poseLandmark : poseArrayList.get(0).getAllPoseLandmarks()) {
                        canvas.drawCircle(poseLandmark.getPosition().x, poseLandmark.getPosition().y,5,mPaint);
                    }

                    bitmap4DisplayArrayList.clear();
                    bitmap4DisplayArrayList.add(bitmapArrayList.get(0));
                    bitmap4Save = bitmapArrayList.get(bitmapArrayList.size()-1);
                    bitmapArrayList.clear();
                    bitmapArrayList.add(bitmap4Save);
                    poseArrayList.clear();
                    isRunning = false;
                }

                if(poseArrayList.size() == 0 && bitmapArrayList.size() >=1 && !isRunning) {
                    RunMLkit.run();
                    isRunning = true;
                }

                if(bitmap4DisplayArrayList.size() >= 1) {
                    display.getBitmap(bitmap4DisplayArrayList.get(0));
                }

                imageProxy.close();
            }
        });
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector,imageAnalysis, preview);
    }
    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(getApplicationContext(), permission)) {
                return false;
            }
        }
        return true;
    }
    private String[] getRequiredPermissions() {
        try {
            PackageInfo info =
                    this.getPackageManager()
                            .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }

    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }
}