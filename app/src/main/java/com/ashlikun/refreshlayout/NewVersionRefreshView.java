package com.ashlikun.refreshlayout;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.ashlikun.swiperefreshlayout.IRefreshStatus;
import com.ashlikun.swiperefreshlayout.MaterialProgressDrawable;

/**
 * 作者　　: 李坤
 * 创建时间: 2018/5/23 0023　上午 10:03
 * 邮箱　　：496546144@qq.com
 * <p>
 * 功能介绍：2.8.0开始的刷新view
 */
public class NewVersionRefreshView extends FrameLayout implements IRefreshStatus {
    /**
     * 上面显示图片的View
     */
    ImageView draweeView;
    String imgUrl = "";
    MaterialProgressDrawable drawable;
    ImageView progressImageView;

    public NewVersionRefreshView(Context context) {
        this(context, null);
    }

    public NewVersionRefreshView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context) {
        int dp10 = dip2px(10);
        int imageWidth = dip2px(150);
        int imageHeight = dip2px(35);
        setPadding(0, dp10, 0, 0);
        draweeView = new ImageView(context);
        LayoutParams paramsImg = new LayoutParams(imageWidth, imageHeight);
        paramsImg.gravity = Gravity.CENTER_HORIZONTAL;
        addView(draweeView, paramsImg);
        draweeView.setImageResource(R.mipmap.aaa);

        //转圈的view
        progressImageView = new ImageView(context);
        LayoutParams paramsCircle = new LayoutParams(dip2px(45), dip2px(45));
        paramsCircle.topMargin = imageHeight;
        paramsCircle.gravity = Gravity.CENTER_HORIZONTAL;
        addView(progressImageView, paramsCircle);
        createProgressView();
    }

    private void createProgressView() {
        drawable = new MaterialProgressDrawable(getContext(), this);
        drawable.setBackgroundColor(0x00000000);
        drawable.setStartEndTrim(0, 0.8f);
        drawable.setColorSchemeColors(0xff000000);
        progressImageView.setImageDrawable(drawable);
    }

    @Override
    public void onReset() {
        drawable.setStartEndTrim(0, 0);
        drawable.showArrow(false);
        drawable.setAlpha(0);
        drawable.stop();
    }

    @Override
    public void onRefreshing() {
        drawable.setStartEndTrim(0, 0);
        drawable.showArrow(false);
        drawable.setAlpha(255);
        drawable.start();
    }

    @Override
    public void onPullToRefreshStart() {
        drawable.setAlpha(255);
    }

    @Override
    public void onPullToRefreshFinish() {
    }

    @Override
    public void onFinishSpinner(boolean isRefresh) {
        drawable.showArrow(false);
    }

    @Override
    public void onPullProgress(float pullDistance, float totalDistance, float pullProgress, boolean isRefreshStart) {
        drawable.showArrow(true);
        drawable.setArrowScale(Math.min(1f, pullProgress));
        drawable.setStartEndTrim(0f, Math.min(0.8f, pullProgress * 0.8f));
        if (isRefreshStart) {
            drawable.setAlpha(255);
            drawable.setProgressRotation(pullProgress);
        } else {
            drawable.setAlpha((int) Math.min(150, 150 * pullProgress));
        }

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
