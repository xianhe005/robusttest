package com.hxh.robusttest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.meituan.robust.patch.annotaion.Add;
import com.meituan.robust.patch.annotaion.Modify;

/**
 * Created by HXH at 2019/3/7
 * second
 */
public class SecondActivity extends AppCompatActivity {
    TextView t;

    @Override
    @Modify
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        t = findViewById(R.id.text);
        //t.setText("未修改");
        t.setText("已经修改");
        addMethod();
    }

    @Add
    private void addMethod() {
        Toast.makeText(this, "add", Toast.LENGTH_SHORT).show();
    }
}
