package com.ashlikun.refreshlayout;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

/**
 * 作者　　: 李坤
 * 创建时间: 2018/5/18 0018　下午 1:31
 * 邮箱　　：496546144@qq.com
 * <p>
 * 功能介绍：
 */
public class Test2Activity extends AppCompatActivity {
    NestedScrollView scrollView;
    int offset = 10;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        scrollView = findViewById(R.id.scrollView);
    }

    public void onClick(View view) {
        offset += 20;
        ViewCompat.offsetTopAndBottom(scrollView, offset);
    }
}
