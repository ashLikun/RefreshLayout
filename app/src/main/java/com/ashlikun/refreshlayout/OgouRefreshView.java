package com.ashlikun.refreshlayout;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.ashlikun.swiperefreshlayout.IRefreshStatus;

import java.util.List;

/**
 * 作者　　: 李坤
 * 创建时间: 2018/5/16 0016　下午 4:44
 * 邮箱　　：496546144@qq.com
 * <p>
 * 功能介绍：相对于新版刷新控件的刷新view,只适用于
 */
public class OgouRefreshView extends View implements IRefreshStatus {
    //下拉图片
    private Bitmap logBitmap;
    //下拉进度
    private float pullProgress = 1;
    //水波纹的path
    private Path wavePath;
    private Paint wavePaint;
    private Paint paint;
    //水波纹颜色
    private int waveColor = 0xffff9343;
    //水波纹的点
    private List<Point> mPointsList;
    int iconWidth;
    int iconHeight;
    //水波纹上涨的幅度
    private float mWaveSpeed = 0.4f;
    /**
     * 水平移动幅度
     */
    private float mWaveXSpeed = 4;
    /**
     * 水位线
     */
    private float mLevelLine;
    /**
     * x偏移量
     */
    private float mLevelXOffset;
    /**
     * 波浪起伏幅度
     */
    private float mWaveHeight = 8;
    /**
     * 波长
     */
    private float mWaveWidth = 20;

    private ValueAnimator mRunAnimator;

    public OgouRefreshView(Context context) {
        this(context, null);
    }

    public OgouRefreshView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OgouRefreshView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        setPadding(0, dip2px(14), 0, dip2px(10));
        logBitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_loading);
        iconWidth = logBitmap.getWidth();
        iconHeight = logBitmap.getHeight();
        initAnim();

        wavePath = new Path();
        wavePaint = new Paint();
        wavePaint.setAntiAlias(true);
        wavePaint.setStyle(Paint.Style.FILL);
        wavePaint.setColor(waveColor);

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));
        resetWave();

    }

    private void initAnim() {
        mRunAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        mRunAnimator.setInterpolator(new LinearInterpolator());
        mRunAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                animWave();
            }
        });
        mRunAnimator.setRepeatMode(ValueAnimator.RESTART);
        mRunAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mRunAnimator.setDuration(1000);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int height = logBitmap.getHeight() + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));


    }

    private void resetWave() {
        // 波浪起伏幅度,高度
        mWaveHeight = iconWidth / 5.0f;
        // 波长等于2倍View宽度也就是View中只能看到四分之一个波形，这样可以使起伏更明显
        mWaveWidth = iconWidth * 2;
        mLevelXOffset = 0;
        // 水位线从最底下开始上升
        mLevelLine = iconHeight + getPaddingTop() - mWaveHeight;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float offsetX = getWidth() / 2 - logBitmap.getWidth() / 2;
        float offsetY = getPaddingBottom() - getHeight() * (1.0f - pullProgress * 0.78f) + (pullProgress == 0.0f ? 0 : getHeight() * 0.3f);

        if (offsetY >= getPaddingTop()) {
            offsetY = getPaddingTop();
        }
        canvas.drawBitmap(createImage(), offsetX, offsetY, null);
    }

    private Bitmap createImage() {
        Bitmap finalBmp = Bitmap.createBitmap(iconWidth, iconHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(finalBmp);
        drawWave(canvas);
        //绘制背景icon
        canvas.drawBitmap(logBitmap, 0, 0, paint);
        return finalBmp;
    }

    /**
     * 绘制水波纹
     *
     * @param canvas
     */
    private void drawWave(Canvas canvas) {
        wavePath.reset();
        float startX = -mWaveWidth + mLevelXOffset;
        float endX = 0;
        //移到屏幕外最左边
        wavePath.moveTo(startX, mLevelLine);
        // 这里计算在可见的View宽度中能容纳几个波形，注意n上取整
        int mWaveCount = (int) Math.round(iconWidth / mWaveWidth + 0.5);
        //这里要多一个波形，应为左边多一个波形
        for (int i = 0; i <= mWaveCount; i++) {
            float skipWaveWidth = i * mWaveWidth;
            endX = skipWaveWidth + mLevelXOffset;
            //正弦曲线
            wavePath.quadTo((-mWaveWidth * 3 / 4) + skipWaveWidth + mLevelXOffset, mLevelLine + mWaveHeight,
                    (-mWaveWidth / 2) + skipWaveWidth + mLevelXOffset, mLevelLine);
            wavePath.quadTo((-mWaveWidth / 4) + skipWaveWidth + mLevelXOffset, mLevelLine - mWaveHeight,
                    endX, mLevelLine);
        }
        //填充矩形
        wavePath.lineTo(endX, getHeight());
        wavePath.lineTo(startX, getHeight());
        wavePath.close();

        canvas.drawPath(wavePath, wavePaint);
    }


    private void animWave() {
        // 水位上升
        mLevelLine -= mWaveSpeed;
        if (mLevelLine < -mWaveHeight / 2) {
            // 水位线从最底下开始上升
            mLevelLine = iconHeight + getPaddingTop() - mWaveHeight;
        }
        //水平移动
        mLevelXOffset += mWaveXSpeed;
        if (mLevelXOffset >= mWaveWidth) {
            // 波形平移超过一个完整波形后复位
            mLevelXOffset = 0;
        }
        invalidate();
    }

    /**
     * 根据下拉进度改变动画
     */
    private void animWaveToPosition() {
        float minLevelLine = -mWaveHeight / 2;
        float maxLevelLine = iconHeight + getPaddingTop() - mWaveHeight;
        float diff = maxLevelLine - minLevelLine;
        float progressDiff = diff * Math.min(pullProgress, 1);
        // 水位上升
        mLevelLine = maxLevelLine - progressDiff;
        progressDiff = mWaveWidth * Math.min(pullProgress, 1);

        //水平移动
        mLevelXOffset = progressDiff;
        invalidate();
    }


    @Override
    public void onReset() {
        resetWave();
        mRunAnimator.cancel();
    }

    @Override
    public void onRefreshing() {
        mRunAnimator.start();
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
        this.pullProgress = pullProgress;
        animWaveToPosition();
    }

    @Override
    public void onPullToMaxBottomStart() {

    }

    @Override
    public void onPullToMaxBottomFinish() {

    }

    private int dip2px(float dipValue) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }
}
