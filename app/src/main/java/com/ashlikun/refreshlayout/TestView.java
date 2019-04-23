package com.ashlikun.refreshlayout;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import androidx.core.view.ViewCompat;
import android.view.View;

import com.ashlikun.swiperefreshlayout.IRefreshStatus;

/**
 * 作者　　: 李坤
 * 创建时间: 2017/7/6　16:46
 * 邮箱　　：496546144@qq.com
 * <p>
 * 功能介绍：
 */

public class TestView extends View implements IRefreshStatus {
    Paint mPaint;

    public TestView(Context context) {
        super(context);
        mPaint = new Paint();
        mPaint.setColor(Color.RED);
        setBackgroundColor(Color.BLACK);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(60, 60, 60, mPaint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        ViewCompat.setLayerType(this, ViewCompat.LAYER_TYPE_SOFTWARE, mPaint);
    }

    @Override
    public void onReset() {

    }

    @Override
    public void onRefreshing() {

    }

    @Override
    public void onPullToRefreshStart() {

    }

    @Override
    public void onPullToRefreshFinish() {

    }

    @Override
    public void onFinishSpinner(boolean isRefresh) {

    }

    @Override
    public void onPullProgress(float pullDistance, float totalDistance, float pullProgress, boolean isRefreshStart) {

    }

    @Override
    public void onPullToMaxBottomStart() {

    }

    @Override
    public void onPullToMaxBottomFinish() {

    }
}
