package com.ashlikun.swiperefreshlayout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;

/**
 * 作者　　: 李坤
 * 创建时间: 2017/5/9 13:27
 * <p>
 * 方法功能：下拉刷新,改之官方的SwipeRefreshLayout
 */

public class SwipeRefreshLayout extends ViewGroup implements NestedScrollingParent,
        NestedScrollingChild {
    public static final int NORMAL = 1;//正常模式，类似于QQ
    public static final int PINNED = 2;//一直在tager的下面
    public static final int FLOAT = 3;//google官方的
    private static final int[] LAYOUT_ATTRS = new int[]{
            android.R.attr.enabled
    };
    @VisibleForTesting
    private static final String LOG_TAG = SwipeRefreshLayout.class.getSimpleName();

    //默认刷新大小
    private static final int DEFAULT_REFRESH_SIZE_DP = 30;
    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final int INVALID_POINTER = -1;
    private static final float DRAG_RATE = .5f;


    private static final int ANIMATE_TO_TRIGGER_DURATION = 200;

    private static final int DEFAULT_ANIMATE_DURATION = 300;
    // Default background for the progress spinner
    // Default offset in dips from the top of the view to where the progress spinner should stop
    private static final int DEFAULT_REFRESH_TARGET_OFFSET_DP = 64;
    //动画时间
    private int mAnimateToStartDuration = DEFAULT_ANIMATE_DURATION;
    private int mAnimateToRefreshDuration = DEFAULT_ANIMATE_DURATION;
    private View mTarget; // 滑动的内容
    OnRefreshListener mListener;//刷新监听
    boolean mRefreshing = false;//是否真正刷新
    boolean mIsPullToRefresh = false;//保证单次回调下拉到刷新
    boolean mIsPullToMaxBottom = false;//保证单次回调下拉到底部
    private int mTouchSlop;//触摸溢出的系数   8

    private float mTotalUnconsumed;
    private final NestedScrollingParentHelper mNestedScrollingParentHelper;
    private final NestedScrollingChildHelper mNestedScrollingChildHelper;
    private final int[] mParentScrollConsumed = new int[2];
    private final int[] mParentOffsetInWindow = new int[2];
    private boolean mNestedScrollInProgress;
    private float mInitialMotionY;//开始拖拽的Y坐标
    private float mInitialDownY;//按下的Y坐标
    private boolean mIsBeingDragged;//是否开始拖拽
    private int mActivePointerId = INVALID_POINTER;//激活的触摸点的id
    private final DecelerateInterpolator mDecelerateInterpolator;
    private boolean mIsUseringTotalDragDistance = false;//使用者是否动态设置了mTotalDragDistance

    private int mRefreshViewIndex = -1;
    protected int mFrom;//从什么位置移动
    boolean mNotify;//是否通知刷新回调
    private int mOriginalOffsetTop;//原始偏移量 处了pinend是0 其他的就是下拉组件的高度的负数
    private int mCurrentTargetOffsetTop;//当前target或者刷新组件距离顶部偏移量 == .getTop()
    /********************************************************************************************
     *                                           可设置的属性
     ********************************************************************************************/
    //刷新的风格
    private int mRefreshStyle = NORMAL;
    //刷新的状态
    private IRefreshStatus mIRefreshStatus;
    //下拉组件
    private View mRefreshView;
    private int mRefreshViewSize;//默认的刷新view的大小
    private float mTotalDragDistance = -1;//最大拖拽距离,达到这个值就会处于可刷新状态,是宁界点
    private boolean mIsLayoutOk = false;

    private OnChildScrollUpCallback mChildScrollUpCallback;

    public SwipeRefreshLayout(Context context) {
        this(context, null);
    }


    public SwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);
        setWillNotDraw(false);
        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        mRefreshViewSize = (int) (DEFAULT_REFRESH_SIZE_DP * metrics.density);
        mTotalDragDistance = (int) (DEFAULT_REFRESH_TARGET_OFFSET_DP * metrics.density);
        mOriginalOffsetTop = mCurrentTargetOffsetTop = 0;
        //设置绘制子VIew的时候按照给定的顺序，getChildDrawingOrder
        setChildrenDrawingOrderEnabled(true);

        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);

        final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
        setEnabled(a.getBoolean(0, true));
        a.recycle();
        initRefreshView();
    }

    private void initRefreshView() {
        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        setRefreshView(new MaterialRefreshView(getContext()), layoutParams);
    }

    //检查刷新view是否实现了IRefreshStatus接口
    private void refreshViewIsIRefreshStatus() {
        if (mRefreshView instanceof IRefreshStatus) {
            mIRefreshStatus = (IRefreshStatus) mRefreshView;
        } else {
            throw new ClassCastException("刷新的View必须实现IRefreshStatus接口");
        }
    }

    //复位
    void reset() {
        mRefreshView.setVisibility(View.GONE);

        switch (mRefreshStyle) {
            case FLOAT:
                setTargetOffsetTopAndBottom(mOriginalOffsetTop - mCurrentTargetOffsetTop);
                break;
            default:
                setTargetOffsetTopAndBottom(-mCurrentTargetOffsetTop);
                break;
        }
        mIRefreshStatus.onReset();
        mIsBeingDragged = false;


        mCurrentTargetOffsetTop = mRefreshView.getTop();
    }

    /**
     * 子view绘制的时候按照这个顺序
     *
     * @param childCount 子类个数
     * @param i          当前迭代顺序
     * @return
     */
    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        switch (mRefreshStyle) {
            case FLOAT://保证mRefreshView最后一个绘制
                if (mRefreshViewIndex < 0) {
                    return i;
                } else if (i == childCount - 1) {
                    // 当迭代最后一个的时候，就绘制mRefreshView
                    return mRefreshViewIndex;
                } else if (i >= mRefreshViewIndex) {
                    //当迭代到mRefreshView之后的就绘制i+1(后一个)
                    return i + 1;
                } else {
                    // Keep the children before the selected child the same
                    return i;
                }
            default://保证mRefreshView第一个绘制
                if (mRefreshViewIndex < 0) {
                    return i;
                } else if (i == 0) {
                    // 当迭代第一个时候就绘制mRefreshView
                    return mRefreshViewIndex;
                } else if (i <= mRefreshViewIndex) {
                    //当迭代到mRefreshView之前的就绘制i-1(前一个)
                    return i - 1;
                } else {
                    return i;
                }
        }
    }


    //确保target不为null
    private void ensureTarget() {
        if (mTarget == null) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (!child.equals(mRefreshView)) {
                    mTarget = child;
                    break;
                }
            }
        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //确保target不为null
        ensureTarget();
        if (mTarget == null) {
            return;
        }
        //测量Target,大小为本控件大小,去除外边距padding
        mTarget.measure(MeasureSpec.makeMeasureSpec(
                getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
                getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY));
        //测量RefreshView大小
        measureRefreshView(widthMeasureSpec, heightMeasureSpec);

        switch (mRefreshStyle) {
            case PINNED:
                mCurrentTargetOffsetTop = mOriginalOffsetTop = 0;
                if (!mIsUseringTotalDragDistance && mTotalDragDistance != mRefreshView.getMeasuredHeight()) {
                    mTotalDragDistance = mRefreshView.getMeasuredHeight();
                }
                break;
            case FLOAT:
                mCurrentTargetOffsetTop = mOriginalOffsetTop = -mRefreshView.getMeasuredHeight();
                if (!mIsUseringTotalDragDistance && mTotalDragDistance < mRefreshView.getMeasuredHeight()) {
                    mTotalDragDistance = mRefreshView.getMeasuredHeight();
                }
                break;
            default:
                mCurrentTargetOffsetTop = 0;
                mOriginalOffsetTop = -mRefreshView.getMeasuredHeight();
                if (!mIsUseringTotalDragDistance && mTotalDragDistance != mRefreshView.getMeasuredHeight()) {
                    mTotalDragDistance = mRefreshView.getMeasuredHeight();
                }
                break;
        }
        mRefreshViewIndex = -1;
        //获取RefreshView 的下标
        for (int index = 0; index < getChildCount(); index++) {
            if (getChildAt(index) == mRefreshView) {
                mRefreshViewIndex = index;
                break;
            }
        }
    }

    private void measureRefreshView(int widthMeasureSpec, int heightMeasureSpec) {
        final MarginLayoutParams lp = (MarginLayoutParams) mRefreshView.getLayoutParams();

        final int childWidthMeasureSpec;
        if (lp.width == LayoutParams.MATCH_PARENT) {
            final int width = Math.max(0, getMeasuredWidth() - getPaddingLeft() - getPaddingRight()
                    - lp.leftMargin - lp.rightMargin);
            childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        } else {
            childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                    getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin,
                    lp.width);
        }

        final int childHeightMeasureSpec;
        if (lp.height == LayoutParams.MATCH_PARENT) {
            final int height = Math.max(0, getMeasuredHeight()
                    - getPaddingTop() - getPaddingBottom()
                    - lp.topMargin - lp.bottomMargin);
            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                    height, MeasureSpec.EXACTLY);
        } else {
            childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                    getPaddingTop() + getPaddingBottom() +
                            lp.topMargin + lp.bottomMargin,
                    lp.height);
        }

        mRefreshView.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        if (getChildCount() == 0) {
            return;
        }
        //确保mTarget不为null
        ensureTarget();
        if (mTarget == null) {
            return;
        }
        //为mTarget计算位置
        final int childLeft = getPaddingLeft();
        final int childTop = getPaddingTop();
        final int childRight = width - getPaddingRight();
        final int childchildBotton = height - getPaddingBottom();
        //为Mtarget布局
        mTarget.layout(childLeft, childTop, childRight, childchildBotton);
        mRefreshView.layout((width / 2 - mRefreshView.getMeasuredWidth() / 2), mOriginalOffsetTop,
                (width / 2 + mRefreshView.getMeasuredWidth() / 2), (mOriginalOffsetTop + mRefreshView.getMeasuredHeight()));
        mIsLayoutOk = true;
    }


    /**
     * 作者　　: 李坤
     * 创建时间: 2017/5/3 0003 15:32
     * <p>
     * 方法功能：mTarget在垂直方向（-1）是否可以滚动,可以滚动说明没到顶部呢
     */

    public boolean canChildScrollUp() {
        if (mChildScrollUpCallback != null) {
            return mChildScrollUpCallback.canChildScrollUp(this, mTarget);
        }
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(mTarget, -1) || mTarget.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mTarget, -1);
        }
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        ensureTarget();

        final int action = MotionEventCompat.getActionMasked(ev);
        int pointerIndex;//触摸点下标

        //如果不可用，或者mTarget没有达到顶部,或者真正刷新，
        // 就不拦截事件,处理权交给子view


        if (!isEnabled() || canChildScrollUp()
                || mRefreshing || mNestedScrollInProgress) {
            return false;
        }
        switch (mRefreshStyle) {
            case FLOAT:
                if (!isEnabled() || canChildScrollUp()
                        || mRefreshing || mNestedScrollInProgress) {
                    // Fail fast if we're not in a state where a swipe is possible
                    return false;
                }
                break;
            default:
                if ((!isEnabled() || (canChildScrollUp() && !mIsBeingDragged))) {
                    return false;
                }
                break;
        }

        switch (action) {
            //按下时，记录按下的坐标Y与，pointerIndex
            case MotionEvent.ACTION_DOWN:
                setTargetOffsetTopAndBottom(mOriginalOffsetTop - mRefreshView.getTop());
                //激活的触摸点的id,只获取第一个触摸点按下的id
                mActivePointerId = ev.getPointerId(0);
                //开始拖拽复位
                mIsBeingDragged = false;
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                //记录按下Y坐标
                mInitialDownY = ev.getY(pointerIndex);
                break;

            case MotionEvent.ACTION_MOVE:
                //触摸点id没有
                if (mActivePointerId == INVALID_POINTER) {
                    return false;
                }

                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                //开始拖拽
                startDragging(ev.getY(pointerIndex));
                break;
            //当屏幕上有多个点被按住，松开其中一个点时触发（即非最后一个点被放开时）。
            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                //复位mIsBeingDragged
                mIsBeingDragged = false;
                //复位mActivePointerId
                mActivePointerId = INVALID_POINTER;
                break;
        }
        //拖拽就拦截事件
        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);
        int pointerIndex = -1;//触摸点下标
        //如果不可用，或者mTarget没有达到顶部,或者真正刷新，
        // 就不拦截事件,处理权交给子view
        if (!isEnabled() || canChildScrollUp()
                || mRefreshing || mNestedScrollInProgress) {
            return false;
        }
        switch (mRefreshStyle) {
            case FLOAT:
                if (!isEnabled() || canChildScrollUp()
                        || mRefreshing || mNestedScrollInProgress) {
                    return false;
                }
                break;
            default:
                if ((!isEnabled() || (canChildScrollUp() && !mIsBeingDragged))) {
                    return false;
                }
                break;
        }

        switch (action) {
            //按下时 记录触摸点id，复位mIsBeingDragged
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = ev.getPointerId(0);
                mIsBeingDragged = false;
                break;

            case MotionEvent.ACTION_MOVE: {
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                //开始拖拽
                final float y = ev.getY(pointerIndex);
                startDragging(y);
                //如果是开始拖拽
                if (mIsBeingDragged) {
                    //下拉总高度，有摩擦力DRAG_RATE
                    final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;
                    if (overscrollTop > 0) {
                        //移动下拉组件
                        moveSpinner(y - mInitialMotionY);
                    } else {
                        return false;
                    }
                }
                break;
            }
            //多点按下
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                pointerIndex = MotionEventCompat.getActionIndex(ev);
                if (pointerIndex < 0) {
                    return false;
                }
                mIsBeingDragged = false;
                mInitialDownY = ev.getY(pointerIndex);
                mActivePointerId = ev.getPointerId(pointerIndex);
                break;
            }
            //多点抬起
            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
            //  抬起时复位
            case MotionEvent.ACTION_UP: {
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }

                if (mIsBeingDragged) {
                    final float y = ev.getY(pointerIndex);
                    //计算下拉总高度
                    final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;
                    mIsBeingDragged = false;
                    //释放下拉组件
                    finishSpinner(overscrollTop);
                }
                mActivePointerId = INVALID_POINTER;
                return false;
            }
            case MotionEvent.ACTION_CANCEL:
                return false;
        }

        return true;
    }


    /**
     * 作者　　: 李坤
     * 创建时间: 2017/5/3 0003 16:47
     * <p>
     * 方法功能：移动下拉组件，往下拖拽的时候
     *
     * @param overscrollTop : 下拉总高度 可能大于mTotalDragDistance
     */
    private void moveSpinner(float overscrollTop) {
        //下拉总高度，有摩擦力DRAG_RATE
        overscrollTop = overscrollTop * DRAG_RATE;
        if (mRefreshView.getVisibility() != View.VISIBLE) {
            mRefreshView.setVisibility(View.VISIBLE);
        }
        float convertScrollOffset;
        if (!mRefreshing) {
            convertScrollOffset = distanceConverter(overscrollTop, mTotalDragDistance);
            if (mRefreshStyle == FLOAT) {//只有FLOAT,NORMAL模式才会加上mOriginalOffsetTop
                convertScrollOffset += mOriginalOffsetTop;
            }
        } else {
            //The Float style will never come here
            if (overscrollTop > mTotalDragDistance) {
                convertScrollOffset = mTotalDragDistance;
            } else {
                convertScrollOffset = overscrollTop;
            }

            if (convertScrollOffset < 0.0f) {
                convertScrollOffset = 0.0f;
            }

        }
        float moveDistance = convertScrollOffset - (mRefreshStyle == FLOAT ? mOriginalOffsetTop : 0);
        if (!mRefreshing) {
            //已经拉到最大拖拽距离
            if (moveDistance > mTotalDragDistance && !mIsPullToRefresh) {
                mIsPullToRefresh = true;
                mIRefreshStatus.onPullToRefreshStart();
            } else if (moveDistance <= mTotalDragDistance && mIsPullToRefresh) {
                mIsPullToRefresh = false;
                mIRefreshStatus.onPullToRefreshFinish();
            }
        }
        if (!mRefreshing) {
            //是否拉到最底部
            boolean isToBBottom = Math.abs(distanceConverter(Integer.MAX_VALUE, mTotalDragDistance) - moveDistance) < 20;
            if (isToBBottom && !mIsPullToMaxBottom) {//已经拉到最大拖拽距离
                mIsPullToMaxBottom = true;
                mIRefreshStatus.onPullToMaxBottomStart();
            } else if (!isToBBottom && mIsPullToMaxBottom) {
                mIsPullToMaxBottom = false;
                mIRefreshStatus.onPullToMaxBottomFinish();
            }
        }
        setTargetOffsetTopAndBottom((int) (convertScrollOffset - mCurrentTargetOffsetTop));
    }

    //释放下拉组件
    private void finishSpinner(float overscrollTop) {
        if (mIRefreshStatus != null) {
            mIRefreshStatus.onFinishSpinner(overscrollTop > mTotalDragDistance);
        }
        if (overscrollTop > mTotalDragDistance) {//超过最大限制
            //直接刷新
            setRefreshing(true, true /* notify */);
        } else {
            //没有超过，取消刷新，回弹
            // cancel refresh
            mRefreshing = false;
            animateOffsetToStartPosition(mCurrentTargetOffsetTop, mResetListener);
        }
    }

    /**
     * 作者　　: 李坤
     * 创建时间: 2017/5/4 0004 16:42
     * <p>
     * 方法功能：距离转化器
     * moveSpinner方法调用
     */
    private float distanceConverter(float overscrollTop, float refreshDistance) {
        //原始拖拽百分比 拖拽中高低/最大拖拽距离
        float originalDragPercent = overscrollTop / refreshDistance;
        //拖拽百分比 （0,1），确保不会大于1
        float dragPercent = Math.min(1f, Math.abs(originalDragPercent));
        float extraOS = Math.abs(overscrollTop) - refreshDistance;
        float slingshotDist = refreshDistance;
        float tensionSlingshotPercent = Math.max(0, Math.min(extraOS, slingshotDist * 2)
                / slingshotDist);
        float tensionPercent = (float) ((tensionSlingshotPercent / 4) - Math.pow(
                (tensionSlingshotPercent / 4), 2)) * 2f;
        float extraMove = (slingshotDist) * tensionPercent * 2;

        int targetY = (int) ((slingshotDist * dragPercent) + extraMove);
        return targetY;
    }


    //开始拖拽
    @SuppressLint("NewApi")
    private void startDragging(float y) {
        final float yDiff = y - mInitialDownY;
        //如果滑动的Y偏移量大于mTouchSlop（8） 并且此时没有开始拖拽
        if (yDiff > mTouchSlop && !mIsBeingDragged) {
            //计算开始拖拽的Y坐标
            mInitialMotionY = mInitialDownY + mTouchSlop;
            mIsBeingDragged = true;//开始拖拽
        }
    }

    //  根据改变位置动画  移动位置
    void moveCorrectPosition(float interpolatedTime) {
        int targetTop = 0;
        int offset = 0;
        switch (mRefreshStyle) {
            case FLOAT:
                targetTop = mFrom + (int) ((mOriginalOffsetTop + mTotalDragDistance - mFrom) * interpolatedTime);
                offset = targetTop - mRefreshView.getTop();
                break;
            default:
                targetTop = mFrom + (int) ((mTotalDragDistance - mFrom) * interpolatedTime);
                offset = targetTop - mTarget.getTop();
                break;
        }
        setTargetOffsetTopAndBottom(offset);
    }

    //根据还原到开始动画   移动到初始位置
    void moveToStart(float interpolatedTime) {
        int targetTop = 0;
        int offset = 0;
        switch (mRefreshStyle) {
            case FLOAT:
                targetTop = mFrom + (int) ((mOriginalOffsetTop - mFrom) * interpolatedTime);
                offset = targetTop - mRefreshView.getTop();
                break;
            default:
                targetTop = mFrom + (int) (-mFrom * interpolatedTime);
                offset = targetTop - mTarget.getTop();
                break;
        }
        setTargetOffsetTopAndBottom(offset);
    }


    //设置下拉组件的偏移量
    void setTargetOffsetTopAndBottom(int offsetY) {
        switch (mRefreshStyle) {
            case FLOAT:
                ViewCompat.offsetTopAndBottom(mRefreshView, offsetY);
                mCurrentTargetOffsetTop = mRefreshView.getTop();
                break;
            case PINNED:
                ViewCompat.offsetTopAndBottom(mTarget, offsetY);
                mCurrentTargetOffsetTop = mTarget.getTop();
                break;
            default:
                ViewCompat.offsetTopAndBottom(mTarget, offsetY);
                ViewCompat.offsetTopAndBottom(mRefreshView, offsetY);
                mCurrentTargetOffsetTop = mTarget.getTop();
                break;
        }
        //回调刷新进度
        if (!mRefreshing) {
            switch (mRefreshStyle) {
                case FLOAT:
                    mIRefreshStatus.onPullProgress(mCurrentTargetOffsetTop - mOriginalOffsetTop,
                            mTotalDragDistance,
                            (mCurrentTargetOffsetTop - mOriginalOffsetTop) / mTotalDragDistance, mIsPullToRefresh);
                    break;
                default:
                    mIRefreshStatus.onPullProgress(mCurrentTargetOffsetTop,
                            mTotalDragDistance,
                            mCurrentTargetOffsetTop / mTotalDragDistance, mIsPullToRefresh);
                    break;
            }
        }
    }

    //当屏幕上有多个点被按住，松开其中一个点时触发（即非最后一个点被放开时）
    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = ev.getPointerId(newPointerIndex);
        }
    }

    /********************************************************************************************
     *                                           接口与内不类
     ********************************************************************************************/
    /**
     * Per-child layout information for layouts that support margins.
     */
    public static class LayoutParams extends MarginLayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    public static abstract class XAnimationListener implements AnimationListener {
        @Override
        public void onAnimationRepeat(Animation animation) {

        }

        @Override
        public void onAnimationStart(Animation animation) {

        }
    }

    public interface OnRefreshListener {
        void onRefresh();
    }


    public interface OnChildScrollUpCallback {
        boolean canChildScrollUp(SwipeRefreshLayout parent, @Nullable View child);
    }


    /********************************************************************************************
     *                                           公共方法
     ********************************************************************************************/


    public float getTotalDragDistance() {
        return mTotalDragDistance;
    }

    /**
     * 作者　　: 李坤
     * 创建时间: 2017/5/6 0006 22:12
     * <p>
     * 方法功能：设置最大下拉距离
     */
    public void setTotalDragDistance(float mTotalDragDistance) {
        mIsUseringTotalDragDistance = true;
        this.mTotalDragDistance = mTotalDragDistance;
    }

    /**
     * @return Whether the SwipeRefreshWidget is actively showing refresh
     * progress.
     */
    public boolean isRefreshing() {
        return mRefreshing;
    }


    /**
     * Notify the widget that refresh state has changed. Do not call this when
     * refresh is triggered by a swipe gesture.
     *
     * @param refreshing Whether or not the view should show refresh progress.
     */
    public void setRefreshing(final boolean refreshing) {
        if (mIsLayoutOk) {
            setRefreshing(refreshing, true /* notify */);
        } else {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    setRefreshing(refreshing);
                }
            }, 50);
        }
    }

    //设置刷新状态
    private void setRefreshing(boolean refreshing, final boolean notify) {
        if (mRefreshing != refreshing) {
            mNotify = notify;
            ensureTarget();
            mRefreshing = refreshing;
            if (mRefreshing) {
                animateOffsetToCorrectPosition(mCurrentTargetOffsetTop, mRefreshListener);
            } else {
                animateOffsetToStartPosition(mCurrentTargetOffsetTop, mResetListener);
            }
        }
    }

    //计算回弹动画时长
    private int computeAnimateToStartDuration(int from) {
        if (from < mOriginalOffsetTop) {
            return 0;
        }
        switch (mRefreshStyle) {
            case FLOAT:
                return (int) (Math.max(0.0f, Math.min(1.0f, Math.abs(from - mOriginalOffsetTop) / mTotalDragDistance))
                        * mAnimateToStartDuration);
            default:
                return (int) (Math.max(0.0f, Math.min(1.0f, Math.abs(from) / mTotalDragDistance))
                        * mAnimateToStartDuration);
        }
    }

    //计算下拉动画时长
    private int computeAnimateToCorrectDuration(float from) {
        if (from < mOriginalOffsetTop) {
            return 0;
        }
        switch (mRefreshStyle) {
            case FLOAT:
                return (int) (Math.max(0.0f, Math.min(1.0f, Math.abs(from - mOriginalOffsetTop - mTotalDragDistance) / mTotalDragDistance))
                        * mAnimateToRefreshDuration);
            default:
                return (int) (Math.max(0.0f, Math.min(1.0f, Math.abs(from - mTotalDragDistance) / mTotalDragDistance))
                        * mAnimateToRefreshDuration);
        }
    }

    /**
     * Set the listener to be notified when a refresh is triggered via the swipe
     * gesture.
     *
     * @param listener
     */
    public void setOnRefreshListener(OnRefreshListener listener) {
        mListener = listener;
    }

    /**
     * Set a callback to override {@link SwipeRefreshLayout#canChildScrollUp()} method. Non-null
     * callback will return the value provided by the callback and ignore all internal logic.
     *
     * @param callback Callback that should be called when canChildScrollUp() is called.
     */
    public void setOnChildScrollUpCallback(@Nullable OnChildScrollUpCallback callback) {
        mChildScrollUpCallback = callback;
    }

    /**
     * 作者　　: 李坤
     * 创建时间: 2017/5/4 0004 14:14
     * <p>
     * 方法功能：刷新的风格
     *
     * @param refreshStyle {@link #NORMAL}
     *                     , {@link #PINNED}
     *                     {@link #FLOAT}
     */
    public void setRefreshStyle(int refreshStyle) {
        if (refreshStyle != NORMAL && refreshStyle != PINNED && refreshStyle != FLOAT) {
            return;
        }
        mRefreshStyle = refreshStyle;
    }

    public void setRefreshView(View refreshView, LayoutParams layoutParams) {
        if (mRefreshView == refreshView) {
            return;
        }
        //去掉之前的刷新view
        if (mRefreshView != null) {
            removeView(mRefreshView);
        }
        if (layoutParams == null || !(layoutParams instanceof LayoutParams)) {
            layoutParams = new LayoutParams(mRefreshViewSize, mRefreshViewSize);
        }
        mRefreshView = refreshView;
        refreshViewIsIRefreshStatus();
        refreshView.setVisibility(View.GONE);
        addView(refreshView, layoutParams);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            reset();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        reset();
        clearAnimation();
    }


    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {

        if ((android.os.Build.VERSION.SDK_INT < 21 && mTarget instanceof AbsListView)
                || (mTarget != null && !ViewCompat.isNestedScrollingEnabled(mTarget))) {
        } else {
            super.requestDisallowInterceptTouchEvent(b);
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    /********************************************************************************************
     *                                           动画
     ********************************************************************************************/
    //调整刷新位置,到刷新的地方
    private void animateOffsetToCorrectPosition(int from, AnimationListener listener) {
        if (computeAnimateToCorrectDuration(from) <= 0) {
            listener.onAnimationStart(null);
            listener.onAnimationEnd(null);
            return;
        }
        if (mRefreshView.getVisibility() != VISIBLE) {
            mRefreshView.setVisibility(VISIBLE);
        }

        mFrom = from;
        mAnimateToCorrectPosition.reset();
        mAnimateToCorrectPosition.setDuration(ANIMATE_TO_TRIGGER_DURATION);
        mAnimateToCorrectPosition.setInterpolator(mDecelerateInterpolator);
        if (listener != null) {
            mAnimateToCorrectPosition.setAnimationListener(listener);
        }
        clearAnimation();
        startAnimation(mAnimateToCorrectPosition);
    }

    //回滚动画
    private void animateOffsetToStartPosition(int from, AnimationListener listener) {
        if (computeAnimateToStartDuration(from) <= 0) {
            listener.onAnimationStart(null);
            listener.onAnimationEnd(null);
            return;
        }
        mFrom = from;
        mAnimateToStartPosition.reset();
        mAnimateToStartPosition.setDuration(computeAnimateToStartDuration(from));
        mAnimateToStartPosition.setInterpolator(mDecelerateInterpolator);
        if (listener != null) {
            mAnimateToStartPosition.setAnimationListener(listener);
        }
        clearAnimation();
        startAnimation(mAnimateToStartPosition);
    }


    //回滚动画
    private final Animation mAnimateToStartPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            moveToStart(interpolatedTime);

        }
    };
    //复位动画监听
    private final AnimationListener mResetListener = new XAnimationListener() {
        @Override
        public void onAnimationEnd(Animation animation) {
            if (mRefreshing) {
                // Make sure the progress view is fully visible
                if (mNotify) {
                    if (mListener != null) {
                        mListener.onRefresh();
                    }
                }
            } else {
                reset();
            }
        }
    };
    //刷新动画监听
    private AnimationListener mRefreshListener = new XAnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
            mIRefreshStatus.onRefreshing();
        }

        @SuppressLint("NewApi")
        @Override
        public void onAnimationEnd(Animation animation) {
            if (mRefreshing) {
                if (mNotify) {
                    if (mListener != null) {
                        mListener.onRefresh();
                    }
                }
                mCurrentTargetOffsetTop = mRefreshView.getTop();
            } else {
                reset();
            }
        }
    };

    private final Animation mAnimateToCorrectPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            moveCorrectPosition(interpolatedTime);
        }
    };

    private boolean isAnimationRunning(Animation animation) {
        return animation != null && animation.hasStarted() && !animation.hasEnded();
    }

    /********************************************************************************************
     *                                            嵌套滑动 {@link NestedScrollingParent}
     ********************************************************************************************/
    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        boolean res = isEnabled() && !mRefreshing
                && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
        if (res) {
        }
        return res;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        // Dispatch up to the nested parent
        startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
        mTotalUnconsumed = 0;
        mNestedScrollInProgress = true;
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        // If we are in the middle of consuming, a scroll, then we want to move the spinner back up
        // before allowing the list to scroll
        if (dy > 0 && mTotalUnconsumed > 0) {
            if (dy > mTotalUnconsumed) {
                consumed[1] = dy - (int) mTotalUnconsumed;
                mTotalUnconsumed = 0;
            } else {
                mTotalUnconsumed -= dy;
                consumed[1] = dy;
            }
            moveSpinner(mTotalUnconsumed);
        }

        // If a client layout is using a custom start position for the circle
        // view, they mean to hide it again before scrolling the child view
        // If we get back to mTotalUnconsumed == 0 and there is more to go, hide
        // the circle so it isn't exposed if its blocking content is moved
        if (dy > 0 && mTotalUnconsumed == 0
                && Math.abs(dy - consumed[1]) > 0) {
            mRefreshView.setVisibility(View.GONE);
        }

        // Now let our nested parent consume the leftovers
        final int[] parentConsumed = mParentScrollConsumed;
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0];
            consumed[1] += parentConsumed[1];
        }
    }

    @Override
    public int getNestedScrollAxes() {
        return mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    @Override
    public void onStopNestedScroll(View target) {
        mNestedScrollingParentHelper.onStopNestedScroll(target);
        mNestedScrollInProgress = false;
        // Finish the spinner for nested scrolling if we ever consumed any
        // unconsumed nested scroll
        mTotalUnconsumed = mTotalUnconsumed * DRAG_RATE;
        if (mTotalUnconsumed > 0) {
            finishSpinner(mTotalUnconsumed);
            mTotalUnconsumed = 0;
        }
        // Dispatch up our nested parent
        stopNestedScroll();
    }

    @Override
    public void onNestedScroll(final View target, final int dxConsumed, final int dyConsumed,
                               final int dxUnconsumed, final int dyUnconsumed) {
        // Dispatch up to the nested parent first
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                mParentOffsetInWindow);

        // This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
        // sometimes between two nested scrolling views, we need a way to be able to know when any
        // nested scrolling parent has stopped handling events. We do that by using the
        // 'offset in window 'functionality to see if we have been moved from the event.
        // This is a decent indication of whether we should take over the event stream or not.
        final int dy = dyUnconsumed + mParentOffsetInWindow[1];
        if (dy < 0 && !canChildScrollUp()) {
            mTotalUnconsumed += Math.abs(dy);
            moveSpinner(mTotalUnconsumed);
        }
    }

    // NestedScrollingChild

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mNestedScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mNestedScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(
                dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX,
                                    float velocityY) {
        return dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY,
                                 boolean consumed) {
        return dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }
}