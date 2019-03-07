package com.hxh.robusttest;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.meituan.robust.PatchExecutor;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init() {
        Button btn = findViewById(R.id.btn);
        Button second = findViewById(R.id.btn_seconde);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isGrantSDCardReadPermission()) {
                    runRobust();
                } else {
                    requestPermission();
                }
            }
        });
        second.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SecondActivity.class));
            }
        });
    }

    private boolean isGrantSDCardReadPermission() {
        return PermissionUtils.isGrantSDCardReadPermission(this);
    }

    private void requestPermission() {
        PermissionUtils.requestSDCardReadPermission(this, REQUEST_CODE_SDCARD_READ);
    }

    private static final int REQUEST_CODE_SDCARD_READ = 1;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_SDCARD_READ:
                handlePermissionResult();
                break;

            default:
                break;
        }
    }

    private void handlePermissionResult() {
        if (isGrantSDCardReadPermission()) {
            runRobust();
        } else {
            Toast.makeText(this, "failure because without sd card read permission", Toast.LENGTH_SHORT).show();
        }

    }

    private void runRobust() {
        new PatchExecutor(getApplicationContext(), new PatchManipulateImp(), new RobustCallBackSample()).start();
    }
}
