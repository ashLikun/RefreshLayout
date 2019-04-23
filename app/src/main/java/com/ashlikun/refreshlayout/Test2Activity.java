package com.ashlikun.refreshlayout;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.appcompat.app.AppCompatActivity;
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
