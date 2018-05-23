package com.ashlikun.swiperefreshlayout;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

/**
 * the default implementation class of the interface IRefreshStatus, and the class should always be rewritten
 */
public class MaterialRefreshView extends AppCompatImageView implements IRefreshStatus {
    // Default background for the progress spinner
    private static final int CIRCLE_BG_LIGHT = 0xFFFAFAFA;
    private static final int KEY_SHADOW_COLOR = 0x1E000000;
    private static final int FILL_SHADOW_COLOR = 0x3D000000;
    // PX
    private static final float X_OFFSET = 0f;
    private static final float Y_OFFSET = 1.75f;
    private static final float SHADOW_RADIUS = 3.5f;
    private static final int SHADOW_ELEVATION = 4;


    private int mShadowRadius;

    MaterialProgressDrawable drawable;

    public MaterialRefreshView(Context context) {
        this(context, null);
    }

    public MaterialRefreshView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaterialRefreshView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initBackground();
        createProgressView();
        initPaint();
    }

    private void createProgressView() {
        drawable = new MaterialProgressDrawable(getContext(), this);
        drawable.setBackgroundColor(CIRCLE_BG_LIGHT);

        drawable.setStartEndTrim(0, 0.8f);
        drawable.setColorSchemeColors(0xffff0000);
        setImageDrawable(drawable);
    }


    private void initPaint() {
    }

    private void initBackground() {
        final float density = getResources().getDisplayMetrics().density;
        final int color = Color.parseColor("#FFFAFAFA");
        ShapeDrawable circle;
        if (elevationSupported()) {
            circle = new ShapeDrawable(new OvalShape());
            ViewCompat.setElevation(this, SHADOW_ELEVATION);
        } else {
            final int shadowYOffset = (int) (density * Y_OFFSET);
            final int shadowXOffset = (int) (density * X_OFFSET);
            mShadowRadius = (int) (density * SHADOW_RADIUS);
            circle = new ShapeDrawable(new OvalShadow(mShadowRadius));
            ViewCompat.setLayerType(this, ViewCompat.LAYER_TYPE_SOFTWARE, circle.getPaint());
            circle.getPaint().setShadowLayer(mShadowRadius, shadowXOffset, shadowYOffset, KEY_SHADOW_COLOR);
            final int padding = mShadowRadius;
            // set padding so the inner image sits correctly within the shadow.
            circle.setPadding(padding, padding, padding, padding);
            setPadding(50, 50, 50, 50);
        }
        circle.getPaint().setColor(color);
        ViewCompat.setBackground(this, circle);

    }

    private boolean elevationSupported() {
        return android.os.Build.VERSION.SDK_INT >= 21;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int dp40 = (int) (getResources().getDisplayMetrics().density * 40);
        setMeasuredDimension(dp40, dp40);
    }

    @Override
    public void onReset() {
//        drawable.setStartEndTrim(0, 0);
        drawable.showArrow(false);
        drawable.setStartEndTrim(0f, 0.8f);
        drawable.setAlpha(0);
        drawable.stop();
    }

    @Override
    public void onRefreshing() {
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
        // Log.e("aaa", "pullProgress = " + pullProgress + "     pullDistance = " + pullDistance + "       totalDistance" + totalDistance);
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


    private class OvalShadow extends OvalShape {
        private RadialGradient mRadialGradient;
        private Paint mShadowPaint;

        OvalShadow(int shadowRadius) {
            super();
            mShadowPaint = new Paint();
            mShadowRadius = shadowRadius;
            updateRadialGradient((int) rect().width());
        }

        @Override
        protected void onResize(float width, float height) {
            super.onResize(width, height);
            updateRadialGradient((int) width);
        }

        @Override
        public void draw(Canvas canvas, Paint paint) {
            final int viewWidth = (int) this.getWidth();
            final int viewHeight = (int) this.getHeight();
            canvas.drawCircle(viewWidth / 2, viewHeight / 2, viewWidth / 2, mShadowPaint);
            canvas.drawCircle(viewWidth / 2, viewHeight / 2, viewWidth / 2 - mShadowRadius, paint);
        }

        private void updateRadialGradient(int diameter) {
            mRadialGradient = new RadialGradient(diameter / 2, diameter / 2,
                    mShadowRadius, new int[]{FILL_SHADOW_COLOR, Color.TRANSPARENT},
                    null, Shader.TileMode.CLAMP);
            mShadowPaint.setShader(mRadialGradient);
        }
    }
}
