package com.example.wifiscanner;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

public class QRCodeActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 201;
    private BarcodeDetector barcodeDetector;
    SurfaceView surfaceView;
    private CameraSource cameraSource;
    String intentSSID = "";
    String intentPassword = "";
    boolean isWifi = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_qr_code);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (getSupportActionBar() != null) this.getSupportActionBar().hide();

        surfaceView = findViewById(R.id.sv_qr_code);

        barcodeDetector = new BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.ALL_FORMATS).build();

        cameraSource = new CameraSource.Builder(this, barcodeDetector).setRequestedPreviewSize(1920, 1080).setAutoFocusEnabled(true).build();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(QRCodeActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraSource.start(surfaceView.getHolder());
                    } else {
                        ActivityCompat.requestPermissions(QRCodeActivity.this, new
                                String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);

                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
                Toast.makeText(getApplicationContext(), "To prevent memory leaks barcode scanner has been stopped", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.valueAt(0).wifi != null) {
                    intentSSID = barcodes.valueAt(0).wifi.ssid;
                    intentPassword = barcodes.valueAt(0).wifi.password;
                    isWifi = true;
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("QR_SSID", intentSSID);
                    returnIntent.putExtra("QR_PWD", intentPassword);
                    setResult(Activity.RESULT_OK, returnIntent);
                    finish();
                } else
                    isWifi = false;
            }
        });
    }
}