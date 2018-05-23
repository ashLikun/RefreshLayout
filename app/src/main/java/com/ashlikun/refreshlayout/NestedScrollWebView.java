package com.ashlikun.refreshlayout;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.webkit.WebView;

/**
 * 作者　　: 李坤
 * 创建时间: 2018/5/21 0021　下午 3:54
 * 邮箱　　：496546144@qq.com
 * <p>
 * 功能介绍：
 */
public class NestedScrollWebView extends WebView implements NestedScrollingChild {
    private static final int INVALID_POINTER = -1;
    public static final String TAG = NestedScrollWebView.class.getSimpleName();

    private int mLastMotionY;
    private boolean mIsBeingDragged = false;
    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];

    private int mNestedYOffset;
    private int mActivePointerId = INVALID_POINTER;
    private int mTouchSlop;
    private NestedScrollingChildHelper mChildHelper;

    public NestedScrollWebView(Context context) {
        super(context);
        init();

    }


    public NestedScrollWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NestedScrollWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mChildHelper = new NestedScrollingChildHelper(this);
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        setNestedScrollingEnabled(true);
    }

//    @Override
//    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        final int action = ev.getAction();
//        if ((action == MotionEvent.ACTION_MOVE) && (mIsBeingDragged)) {
//            return true;
//        }
//        switch (action & MotionEvent.ACTION_MASK) {
//            case MotionEvent.ACTION_MOVE: {
//                final int activePointerId = mActivePointerId;
//                if (activePointerId == INVALID_POINTER) {
//                    break;
//                }
//
//                final int pointerIndex = ev.findPointerIndex(activePointerId);
//                if (pointerIndex == -1) {
//                    Log.e(TAG, "Invalid pointerId=" + activePointerId
//                            + " in onInterceptTouchEvent");
//                    break;
//                }
//
//                final int y = (int) ev.getY(pointerIndex);
//                final int yDiff = Math.abs(y - mLastMotionY);
//                if (yDiff > mTouchSlop
//                        && (getNestedScrollAxes() & ViewCompat.SCROLL_AXIS_VERTICAL) == 0) {
//                    mIsBeingDragged = true;
//                    mLastMotionY = y;
//                    initVelocityTrackerIfNotExists();
//                    mVelocityTracker.addMovement(ev);
//                    mNestedYOffset = 0;
//                    final ViewParent parent = getParent();
//                    if (parent != null) {
//                        parent.requestDisallowInterceptTouchEvent(true);
//                    }
//                }
//                break;
//            }
//
//            case MotionEvent.ACTION_DOWN: {
//                final int y = (int) ev.getY();
//                if (!inChild((int) ev.getX(), y)) {
//                    mIsBeingDragged = false;
//                    recycleVelocityTracker();
//                    break;
//                }
//
//                /*
//                 * Remember location of down touch.
//                 * ACTION_DOWN always refers to pointer index 0.
//                 */
//                mLastMotionY = y;
//                mActivePointerId = ev.getPointerId(0);
//
//                initOrResetVelocityTracker();
//                mVelocityTracker.addMovement(ev);
//                /*
//                 * If being flinged and user touches the screen, initiate drag;
//                 * otherwise don't. mScroller.isFinished should be false when
//                 * being flinged. We need to call computeScrollOffset() first so that
//                 * isFinished() is correct.
//                 */
//                mScroller.computeScrollOffset();
//                mIsBeingDragged = !mScroller.isFinished();
//                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
//                break;
//            }
//
//            case MotionEvent.ACTION_CANCEL:
//            case MotionEvent.ACTION_UP:
//                /* Release the drag */
//                mIsBeingDragged = false;
//                mActivePointerId = INVALID_POINTER;
//                recycleVelocityTracker();
//                if (mScroller.springBack(getScrollX(), getScrollY(), 0, 0, 0, getScrollRange())) {
//                    ViewCompat.postInvalidateOnAnimation(this);
//                }
//                stopNestedScroll(ViewCompat.TYPE_TOUCH);
//                break;
//            case MotionEvent.ACTION_POINTER_UP:
//                onSecondaryPointerUp(ev);
//                break;
//        }
//
//        /*
//         * The only time we want to intercept motion events is if we are in the
//         * drag mode.
//         */
//        return mIsBeingDragged;
//    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = false;

        MotionEvent trackedEvent = MotionEvent.obtain(event);

        final int action = MotionEventCompat.getActionMasked(event);

        if (action == MotionEvent.ACTION_DOWN) {
            mNestedYOffset = 0;
        }

        int y = (int) event.getY();

        event.offsetLocation(0, mNestedYOffset);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = y;
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                result = super.onTouchEvent(event);
                break;
            case MotionEvent.ACTION_MOVE:
                int deltaY = mLastMotionY - y;

                if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset)) {
                    deltaY -= mScrollConsumed[1];
                    trackedEvent.offsetLocation(0, mScrollOffset[1]);
                    mNestedYOffset += mScrollOffset[1];
                }

                mLastMotionY = y - mScrollOffset[1];

                int oldY = getScrollY();
                int newScrollY = Math.max(0, oldY + deltaY);
                int dyConsumed = newScrollY - oldY;
                int dyUnconsumed = deltaY - dyConsumed;

                if (dispatchNestedScroll(0, dyConsumed, 0, dyUnconsumed, mScrollOffset)) {
                    mLastMotionY -= mScrollOffset[1];
                    trackedEvent.offsetLocation(0, mScrollOffset[1]);
                    mNestedYOffset += mScrollOffset[1];
                }

                result = super.onTouchEvent(trackedEvent);
                trackedEvent.recycle();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                stopNestedScroll();
                result = super.onTouchEvent(event);
                break;
        }
        return result;
    }

    // NestedScrollingChild

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

}