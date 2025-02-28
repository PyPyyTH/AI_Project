package com.example.backup;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.vision.classifier.ImageClassifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 101;
    private static final int PICK_IMAGE_REQUEST = 1;

    private PreviewView previewView;
    private ImageCapture imageCapture;
    private LinearLayout resultContainer;
    private TextView resultTextView;
    private final List<String> analysisHistory = new ArrayList<>();
    private ProcessCameraProvider cameraProvider;
    private boolean isToastShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("MainActivity", "onCreate called");

        previewView = findViewById(R.id.previewView);
        resultContainer = findViewById(R.id.resultContainer);
        resultTextView = findViewById(R.id.resultTextView);
        ImageButton captureImageBtn = findViewById(R.id.captureImageBtn);
        Button selectImageBtn = findViewById(R.id.selectImageBtn);
        Button viewHistoryBtn = findViewById(R.id.viewHistoryBtn);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            startCamera();
        }

        captureImageBtn.setOnClickListener(v -> takePhoto());
        selectImageBtn.setOnClickListener(v -> chooseImageFromGallery());
        viewHistoryBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            intent.putStringArrayListExtra("historyList", new ArrayList<>(analysisHistory));
            startActivity(intent);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                if (!isToastShown) {
                    Toast.makeText(this, "Quyền camera bị từ chối.", Toast.LENGTH_SHORT).show();
                    isToastShown = true;
                }
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                chooseImageFromGallery();
            } else {
                if (!isToastShown) {
                    Toast.makeText(this, "Quyền truy cập bộ nhớ bị từ chối.", Toast.LENGTH_SHORT).show();
                    isToastShown = true;
                }
            }
        }
    }

    private void startCamera() {
        Log.d("MainActivity", "Starting camera");
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                Log.d("MainActivity", "Unbinding all existing use cases");
                cameraProvider.unbindAll();

                CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                String cameraId = cameraManager.getCameraIdList()[0];
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Size previewSize = new Size(640, 480);
                Size captureSize = new Size(640, 480);
                if (map != null) {
                    Size[] previewSizes = map.getOutputSizes(SurfaceTexture.class);
                    Size[] captureSizes = map.getOutputSizes(ImageFormat.JPEG);
                    Log.d("MainActivity", "Supported Preview sizes: " + Arrays.toString(previewSizes));
                    Log.d("MainActivity", "Supported Capture sizes: " + Arrays.toString(captureSizes));

                    for (Size size : previewSizes) {
                        if (size.getWidth() == 1280 && size.getHeight() == 720) {
                            previewSize = new Size(1280, 720);
                            break;
                        }
                    }
                    for (Size size : captureSizes) {
                        if (size.getWidth() == 1280 && size.getHeight() == 720) {
                            captureSize = new Size(1280, 720);
                            break;
                        }
                    }
                    Log.d("MainActivity", "Selected Preview size: " + previewSize);
                    Log.d("MainActivity", "Selected Capture size: " + captureSize);
                }

                Preview preview = new Preview.Builder()
                        .setTargetResolution(previewSize)
                        .build();
                imageCapture = new ImageCapture.Builder()
                        .setTargetResolution(captureSize)
                        .build();

                CameraSelector cameraSelector;
                try {
                    cameraSelector = new CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build();
                } catch (IllegalArgumentException e) {
                    Log.w("MainActivity", "No back camera found, trying front camera");
                    cameraSelector = new CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                            .build();
                }

                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
                Log.d("MainActivity", "Camera started successfully");
                isToastShown = false;
            } catch (Exception e) {
                Log.e("MainActivity", "Error starting camera", e);
                runOnUiThread(() -> {
                    if (!isToastShown) {
                        if (e instanceof IllegalArgumentException) {
                            Toast.makeText(this, "Không thể khởi động camera: Thiết bị không hỗ trợ cấu hình này", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Lỗi khởi động camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                        isToastShown = true;
                    }
                });
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void stopCamera() {
        Log.d("MainActivity", "Stopping camera");
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MainActivity", "onResume called");
        isToastShown = false;
        startCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("MainActivity", "onPause called");
        stopCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("MainActivity", "onDestroy called");
        stopCamera();
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, "photo_" + System.currentTimeMillis() + ".jpg");
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
        ).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Uri savedUri = outputFileResults.getSavedUri();
                        if (savedUri != null) {
                            try {
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), savedUri);
                                if (bitmap != null) {
                                    analyzeImage(bitmap);
                                } else {
                                    if (!isToastShown) {
                                        Toast.makeText(MainActivity.this, "Không thể chuyển đổi ảnh thành Bitmap.", Toast.LENGTH_SHORT).show();
                                        isToastShown = true;
                                    }
                                }
                            } catch (Exception e) {
                                Log.e("MainActivity", "Error loading captured image", e);
                                if (!isToastShown) {
                                    Toast.makeText(MainActivity.this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    isToastShown = true;
                                }
                            }
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        if (!isToastShown) {
                            Toast.makeText(MainActivity.this, "Lỗi chụp ảnh: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                            isToastShown = true;
                        }
                    }
                });
    }

    private void chooseImageFromGallery() {
        String permission = android.os.Build.VERSION.SDK_INT >= 33 ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_STORAGE_PERMISSION);
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            try {
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    if (bitmap != null) {
                        analyzeImage(bitmap);
                    } else {
                        if (!isToastShown) {
                            Toast.makeText(this, "Không thể chuyển đổi ảnh được chọn thành Bitmap.", Toast.LENGTH_SHORT).show();
                            isToastShown = true;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Error loading gallery image", e);
                if (!isToastShown) {
                    Toast.makeText(this, "Lỗi khi chọn ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    isToastShown = true;
                }
            }
        }
    }

    private void analyzeImage(Bitmap bitmap) {
        if (!isToastShown) {
            Toast.makeText(this, "Đang phân tích ảnh...", Toast.LENGTH_SHORT).show();
            isToastShown = true;
        }

        new Thread(() -> {
            try {
                Log.d("MainActivity", "Starting image analysis");
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
                TensorImage tensorImage = TensorImage.fromBitmap(resizedBitmap);
                Log.d("MainActivity", "Image resized and converted to TensorImage");

                ImageClassifier.ImageClassifierOptions options = ImageClassifier.ImageClassifierOptions.builder()
                        .setMaxResults(5)
                        .setScoreThreshold(0.2f)
                        .build();

                ImageClassifier imageClassifier = ImageClassifier.createFromFileAndOptions(
                        this,
                        "hoa_van_resnet50_with_metadata.tflite",
                        options
                );
                Log.d("MainActivity", "ImageClassifier initialized");

                List<org.tensorflow.lite.task.vision.classifier.Classifications> classificationResults = imageClassifier.classify(tensorImage);
                Log.d("MainActivity", "Classification completed, results size: " + classificationResults.size());

                StringBuilder resultText = new StringBuilder();
                if (!classificationResults.isEmpty()) {
                    List<Category> results = classificationResults.get(0).getCategories();
                    Log.d("MainActivity", "Categories found: " + results.size());
                    if (!results.isEmpty()) {
                        for (Category category : results) {
                            resultText.append("Đối tượng: ").append(category.getLabel())
                                    .append(", Độ chính xác: ").append(category.getScore()).append("\n");
                        }
                    } else {
                        resultText.append("Không tìm thấy đối tượng nào với độ chính xác trên 0.5.");
                        Log.w("MainActivity", "No categories found in classification results");
                    }
                } else {
                    resultText.append("Không tìm thấy kết quả phân loại.");
                    Log.w("MainActivity", "Classification results are empty");
                }

                runOnUiThread(() -> {
                    resultTextView.setText(resultText.toString());
                    resultContainer.setVisibility(View.VISIBLE);

                    AlertDialog dialog = new AlertDialog.Builder(this)
                            .setTitle("Lưu kết quả phân tích")
                            .setMessage("Bạn có muốn lưu kết quả phân tích này vào lịch sử không?\n" + resultText)
                            .setPositiveButton("Có", (d, which) -> {
                                analysisHistory.add(resultText.toString());
                                Log.d("MainActivity", "Result saved to history: " + resultText);
                                resultTextView.setText("");
                                resultContainer.setVisibility(View.GONE);
                                d.dismiss();
                            })
                            .setNegativeButton("Không", (d, which) -> {
                                resultTextView.setText("");
                                resultContainer.setVisibility(View.GONE);
                                d.dismiss();
                            })
                            .create();
                    dialog.show();
                    isToastShown = false;
                    Log.d("MainActivity", "Dialog shown with result: " + resultText);
                });

            } catch (Exception e) {
                Log.e("MainActivity", "Error during image analysis", e);
                runOnUiThread(() -> {
                    if (!isToastShown) {
                        String errorMessage = "Lỗi khi phân tích ảnh: " + e.getMessage();
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                        resultTextView.setText("");
                        resultContainer.setVisibility(View.GONE);
                        isToastShown = true;
                    }
                });
            }
        }).start();
    }
}