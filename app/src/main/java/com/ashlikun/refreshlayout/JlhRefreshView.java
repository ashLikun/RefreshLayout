package com.ashlikun.refreshlayout;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ashlikun.swiperefreshlayout.IRefreshStatus;
import com.ashlikun.swiperefreshlayout.SwipeRefreshLayout;

/**
 * 作者　　: 李坤
 * 创建时间:2017/6/2　14:19
 * 邮箱　　：496546144@qq.com
 * <p>
 * 功能介绍：
 */

public class JlhRefreshView extends RelativeLayout implements IRefreshStatus {
    private TextView refreshTextView;
    private ImageView refreshImage;
    AnimationDrawable mAnimDrawable;
    int frameSize = 11;
    String refreshPullMax;
    String refreshPullRefresh;
    String refreshPullStart;
    String refreshPullInit;
    private boolean pullToMaxBottomStart = false;

    public JlhRefreshView(Context context) {
        this(context, null);
    }

    public JlhRefreshView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public JlhRefreshView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
        setLayoutParams(new SwipeRefreshLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void initView(Context context, AttributeSet attrs) {
        int padding = dip2px(10);
        setPadding(0, padding, 0, padding);
        refreshPullMax = context.getString(R.string.refresh_pull_max);
        refreshPullRefresh = context.getString(R.string.refresh_pull_refresh);
        refreshPullStart = context.getString(R.string.refresh_pull_start);
        refreshPullInit = context.getString(R.string.refresh_pull_init);
        LayoutInflater.from(context).inflate(R.layout.refresh_jlh, this);
        refreshImage = (ImageView) findViewById(R.id.refreshImage);
        refreshTextView = (TextView) findViewById(R.id.refreshTextView);
        mAnimDrawable = new AnimationDrawable();
        for (int i = 1; i <= frameSize; i++) {
            int pngId = getContext().getResources().getIdentifier("refresh_loading_" + String.format("%02d", i), "mipmap", getContext().getPackageName());
            Drawable pngDraw = getContext().getResources().getDrawable(pngId);
            mAnimDrawable.addFrame(pngDraw, 80);
        }
        mAnimDrawable.setOneShot(false);
        refreshImage.setImageDrawable(mAnimDrawable);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        refreshImage.setPivotX(refreshImage.getWidth() / 2);
        refreshImage.setPivotY(refreshImage.getHeight() / 2);
    }

    @Override
    public void onReset() {
        refreshImage.setRotationX(0);
        refreshImage.setImageDrawable(getResources().getDrawable(R.mipmap.ic_refresh_init));
        refreshTextView.setText(refreshPullInit);
        mAnimDrawable.stop();
    }

    @Override
    public void onRefreshing() {
        refreshTextView.setText(refreshPullRefresh);
        refreshImage.setImageDrawable(mAnimDrawable);
        mAnimDrawable.stop();
        mAnimDrawable.start();
    }

    @Override
    public void onPullToRefreshStart() {
        refreshTextView.setText(refreshPullStart);
        Log.e("onPullToRefreshStart", "onPullToRefreshStart");
    }

    @Override
    public void onPullToRefreshFinish() {
        refreshTextView.setText(refreshPullInit);
        Log.e("onPullToRefreshFinish", "onPullToRefreshFinish");
    }

    @Override
    public void onFinishSpinner(boolean isRefresh) {

    }

    @Override
    public void onPullProgress(float pullDistance, float totalDistance, float pullProgress, boolean isRefreshStart) {
        if (pullProgress >= 1) {
//            Log.e("onPullProgress", "pullDistance = " + pullDistance + "    totalDistance = " + totalDistance + "     pullProgress = " + pullProgress);
        }
//        Log.e("onPullProgress", "pullDistance = " + pullDistance + "    totalDistance = " + totalDistance + "     pullProgress = " + pullProgress);
        float rotationX = 270;
        if (pullProgress >= 0 && pullProgress <= 1) {
            rotationX += 90 * pullProgress;
            refreshImage.setRotationX(rotationX);
        }
        if (!pullToMaxBottomStart) {
            Drawable drawable = getProgressDrawable(pullProgress);
            if (drawable != null) {
                refreshImage.setImageDrawable(drawable);
            }
        }
    }
    /**
     * 作者　　: 李坤
     * 创建时间: 2017/7/6 17:50
     * 邮箱　　：496546144@qq.com
     * <p>
     * 方法功能：根据
     */

    private Drawable getProgressDrawable(float progress) {
        int index = (int) ((frameSize * progress * 2) % frameSize);
        if (index >= 0 && index < mAnimDrawable.getNumberOfFrames()) {
            return mAnimDrawable.getFrame(index);
        } else {
            return null;
        }
    }

    /**
     * 作者　　: 李坤
     * 创建时间: 2017/6/2 17:25
     * <p>
     * 方法功能：下拉到最大值开始
     */
    @Override
    public void onPullToMaxBottomStart() {
        pullToMaxBottomStart = true;
        refreshImage.setImageDrawable(getResources().getDrawable(R.mipmap.ic_refresh_to_bottom));
        refreshTextView.setText(refreshPullMax);
        Log.e("onPullToMaxBottomStart", "onPullToMaxBottomStart");
    }

    /**
     * 作者　　: 李坤
     * 创建时间: 2017/6/2 17:25
     * <p>
     * 方法功能：下拉到最大值结束
     */
    @Override
    public void onPullToMaxBottomFinish() {
        pullToMaxBottomStart = false;
        refreshImage.setImageDrawable(getResources().getDrawable(R.mipmap.ic_refresh_init));
        refreshTextView.setText(refreshPullStart);
        Log.e("onPullToMaxBottomFinish", "onPullToMaxBottomFinish");
    }

    private int dip2px(float dipValue) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }
}
