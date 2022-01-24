package com.harris.inxcamerax;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.harris.inxcamerax.utils.CameraConstant;
import com.harris.inxcamerax.utils.CameraParam;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ImageView img_view;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textView = findViewById(R.id.txt_add_view);
        img_view=findViewById(R.id.img_view);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onOpenView();
            }
        });

    }

    private void onOpenView() {
        CameraParam mCameraParam = new CameraParam.Builder()
                .setRequestCode(1010)
                .setActivity(this)
                .build();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1010 && resultCode == RESULT_OK) {
            //获取图片路径
            String picturePath = data.getStringExtra(CameraConstant.PICTURE_PATH_KEY);
            Glide.with(this).load(picturePath).into(img_view);
            //显示出来
        }
    }
}