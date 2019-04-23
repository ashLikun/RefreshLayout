package com.ashlikun.refreshlayout;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

/**
 * 作者　　: 李坤
 * 创建时间: 2018/5/18 0018　下午 3:01
 * 邮箱　　：496546144@qq.com
 * <p>
 * 功能介绍：
 */
public class MyScoolView extends NestedScrollView {
    public MyScoolView(@NonNull Context context) {
        super(context);
    }

    public MyScoolView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MyScoolView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        Log.e("aaa", "action = " + ev.getAction() + "   x=" + ev.getX() + "     y=" + ev.getY());
        return super.onTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Log.e("aaaaaa", "action = " + ev.getAction() + "   x=" + ev.getX() + "     y=" + ev.getY());
        return super.onInterceptTouchEvent(ev);
    }
}
