package com.ashlikun.swiperefreshlayout;

import android.content.Context;
import android.content.res.TypedArray;
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

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.NestedScrollingChild;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.NestedScrollingParent;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;

import com.google.android.material.appbar.AppBarLayout;

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

    /**
     * 默认刷新大小
     */
    private static final int DEFAULT_REFRESH_SIZE_DP = 30;
    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final int INVALID_POINTER = -1;
    private static final float DRAG_RATE = .5f;


    private static final int DEFAULT_ANIMATE_DURATION = 300;
    private static final int DEFAULT_REFRESH_TARGET_OFFSET_DP = 64;
    /**
     * 动画时间
     */
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
    /**
     * 刚开始时候刷新view距离顶部距离
     */
    private int mOriginalRefreshViewOffsetTop;
    /**
     * 当前target组件距离顶部偏移量 == .getTop()
     * 如果是google风格的就是刷新view的偏移量
     */
    private int mCurrentTargetOffsetTop;
    /**
     * 记录非google风格的target底部padding
     */
    private int mTargetPaddingBottom = Integer.MAX_VALUE;
    /********************************************************************************************
     *                                           可设置的属性
     ********************************************************************************************/
    /**
     * 刷新的风格
     */
    private int mRefreshStyle = FLOAT;
    /**
     * 刷新的状态
     */
    private IRefreshStatus mIRefreshStatus;
    /**
     * 下拉组件
     */
    private View mRefreshView;
    /**
     * 默认的刷新view的大小
     */
    private int mRefreshViewSize;
    /**
     * 最大拖拽距离,达到这个值就会处于可刷新状态,是宁界点
     */
    private float mTotalDragDistance = -1;
    /**
     * 极限最大距离
     */
    private float mMaxDragDistance = -1;
    private boolean mIsLayoutOk = false;
    /**
     * 是否是外部调用刷新完成
     */
    private boolean mIsExteRefreshComplete = false;

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
        mOriginalRefreshViewOffsetTop = mCurrentTargetOffsetTop = 0;
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

    protected void initRefreshView() {
        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        setRefreshView(new MaterialRefreshView(getContext()), layoutParams);
    }

    /**
     * 检查刷新view是否实现了IRefreshStatus接口
     */
    private void refreshViewIsIRefreshStatus() {
        if (mRefreshView instanceof IRefreshStatus) {
            mIRefreshStatus = (IRefreshStatus) mRefreshView;
        } else {
            throw new ClassCastException("刷新的View必须实现IRefreshStatus接口");
        }
    }

    /**
     * 复位
     */
    void reset() {
        mRefreshView.setVisibility(View.GONE);
        ensureTarget();
        if (mTarget == null) {
            return;
        }
        switch (mRefreshStyle) {
            case FLOAT:
                setTargetOffsetTopAndBottom(mOriginalRefreshViewOffsetTop - mCurrentTargetOffsetTop);
                break;
            default:
                setTargetOffsetTopAndBottom(-mCurrentTargetOffsetTop);
                break;
        }
        mIRefreshStatus.onReset();
        mIsBeingDragged = false;
        switch (mRefreshStyle) {
            case FLOAT:
                mCurrentTargetOffsetTop = mRefreshView.getTop();
                break;
            default:
                mCurrentTargetOffsetTop = mTarget.getTop();
                break;
        }
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
            case FLOAT:
                //保证mRefreshView最后一个绘制
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


    /**
     * 确保target不为null
     */
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
                mOriginalRefreshViewOffsetTop = 0;
                mCurrentTargetOffsetTop = mTarget.getTop();
                if (!mIsUseringTotalDragDistance && mTotalDragDistance != mRefreshView.getMeasuredHeight()) {
                    mTotalDragDistance = mRefreshView.getMeasuredHeight();
                }
                break;
            case FLOAT:
                mOriginalRefreshViewOffsetTop = -mRefreshView.getMeasuredHeight();
                mCurrentTargetOffsetTop = mRefreshView.getTop();
                if (!mIsUseringTotalDragDistance && mTotalDragDistance < mRefreshView.getMeasuredHeight()) {
                    mTotalDragDistance = mRefreshView.getMeasuredHeight();
                }
                break;
            default:
                mOriginalRefreshViewOffsetTop = -mRefreshView.getMeasuredHeight();
                mCurrentTargetOffsetTop = mTarget.getTop();
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
        switch (mRefreshStyle) {
            case FLOAT:
                mTarget.layout(childLeft, childTop, childRight, childchildBotton);
                //因为这种模式mCurrentTargetOffsetTop 就是mRefreshView的顶部
                mRefreshView.layout((width / 2 - mRefreshView.getMeasuredWidth() / 2), mCurrentTargetOffsetTop,
                        (width / 2 + mRefreshView.getMeasuredWidth() / 2), (mCurrentTargetOffsetTop + mRefreshView.getMeasuredHeight()));
                break;
            case PINNED:
                mTarget.layout(childLeft, childTop + mCurrentTargetOffsetTop, childRight, childchildBotton + mCurrentTargetOffsetTop);
                mRefreshView.layout((width / 2 - mRefreshView.getMeasuredWidth() / 2), mOriginalRefreshViewOffsetTop,
                        (width / 2 + mRefreshView.getMeasuredWidth() / 2), (mOriginalRefreshViewOffsetTop + mRefreshView.getMeasuredHeight()));
                break;
            default:
                mTarget.layout(childLeft, childTop + mCurrentTargetOffsetTop, childRight, childchildBotton + mCurrentTargetOffsetTop);
                mRefreshView.layout((width / 2 - mRefreshView.getMeasuredWidth() / 2), mOriginalRefreshViewOffsetTop + mCurrentTargetOffsetTop,
                        (width / 2 + mRefreshView.getMeasuredWidth() / 2), (mOriginalRefreshViewOffsetTop + mCurrentTargetOffsetTop + mRefreshView.getMeasuredHeight()));
                break;
        }
        mIsLayoutOk = true;
    }


    /**
     * 作者　　: 李坤
     * 创建时间: 2017/5/3 0003 15:32
     * <p>
     * 方法功能：mTarget在垂直方向（-1）是否可以滚动,可以滚动说明没到顶部呢
     *
     * @return true:不能下拉了，false：可以下拉
     */

    public boolean canChildScrollUp() {
        if (mChildScrollUpCallback != null) {
            return mChildScrollUpCallback.canChildScrollUp(this, mTarget);
        }
        try {
            if (mTarget instanceof CoordinatorLayout) {
                //兼容CoordinatorLayout
                if (((CoordinatorLayout) mTarget).getChildCount() > 0) {
                    if (((CoordinatorLayout) mTarget).getChildAt(0) instanceof AppBarLayout) {
                        return ((CoordinatorLayout) mTarget).getChildAt(0).getTop() != 0;
                    } else {
                        for (int i = 0; i < getChildCount(); i++) {
                            View view = ((CoordinatorLayout) mTarget).getChildAt(i);
                            if (view instanceof AppBarLayout) {
                                return view.getTop() != 0;
                            } else if (view instanceof ViewGroup) {
                                view.canScrollVertically(-1);
                            }
                        }
                    }

                }
            }
        } catch (Exception e) {

        }
        return mTarget.canScrollVertically(-1);
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        ensureTarget();

        final int action = ev.getAction();
        //触摸点下标
        int pointerIndex;
        //如果不可用，或者mTarget没有达到顶部,或者正在刷新，就不拦截事件,处理权交给子view
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
                setTargetOffsetTopAndBottom(mOriginalRefreshViewOffsetTop - mRefreshView.getTop());
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
            case MotionEvent.ACTION_POINTER_UP:
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
        final int action = ev.getAction();
        //触摸点下标
        int pointerIndex = -1;
        //如果不可用，或者mTarget没有达到顶部,或者正在刷新，就不拦截事件,处理权交给子view
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
            case MotionEvent.ACTION_POINTER_DOWN: {
                pointerIndex = ev.getActionIndex();
                if (pointerIndex < 0) {
                    return false;
                }
                mIsBeingDragged = false;
                mInitialDownY = ev.getY(pointerIndex);
                mActivePointerId = ev.getPointerId(pointerIndex);
                break;
            }
            //多点抬起
            case MotionEvent.ACTION_POINTER_UP:
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
            convertScrollOffset = distanceConverter(overscrollTop);
            //只有FLOAT,NORMAL模式才会加上mOriginalOffsetTop
            if (mRefreshStyle == FLOAT) {
                convertScrollOffset += mOriginalRefreshViewOffsetTop;
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
        float moveDistance = convertScrollOffset - (mRefreshStyle == FLOAT ? mOriginalRefreshViewOffsetTop : 0);
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
            boolean isToBBottom = Math.abs(distanceConverter(Integer.MAX_VALUE) - moveDistance) < 20;
            //已经拉到最大拖拽距离
            if (isToBBottom && !mIsPullToMaxBottom) {
                mIsPullToMaxBottom = true;
                mIRefreshStatus.onPullToMaxBottomStart();
            } else if (!isToBBottom && mIsPullToMaxBottom) {
                mIsPullToMaxBottom = false;
                mIRefreshStatus.onPullToMaxBottomFinish();
            }
        }
        setTargetOffsetTopAndBottom((int) (convertScrollOffset - mCurrentTargetOffsetTop));
    }

    /**
     * 释放下拉组件
     */
    private void finishSpinner(float overscrollTop) {
        if (mIRefreshStatus != null) {
            mIRefreshStatus.onFinishSpinner(overscrollTop > mTotalDragDistance);
        }
        //超过最大限制
        if (overscrollTop > mTotalDragDistance) {
            //直接刷新
            setRefreshing(true, true);
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
     *
     * @param overscrollTop 下拉总高度
     */
    private float distanceConverter(float overscrollTop) {
        //原始拖拽百分比 可能大于1，到达零界点就是1
        float originalDragPercent = overscrollTop / mTotalDragDistance;
        //拖拽百分比 （0,1），确保不会大于1
        float dragPercent = Math.min(1f, Math.abs(originalDragPercent));
        //距离零界点的值，可能为负数（没有达到零界点）
        float extraOS = Math.abs(overscrollTop) - mTotalDragDistance;
        float maxDragDistance = mMaxDragDistance > mTotalDragDistance ? mMaxDragDistance : mTotalDragDistance;
        //距离零界点的百分比
        float tensionSlingshotPercent = Math.max(0, Math.min(extraOS, maxDragDistance * 2)
                / maxDragDistance);
        //这里模拟越靠近上面越慢
        float tensionPercent = (float) ((tensionSlingshotPercent / 4) - Math.pow(
                (tensionSlingshotPercent / 4), 2)) * 2f;
        //多余的下拉距离
        float extraMove = maxDragDistance * tensionPercent * 2;
        int targetY = (int) ((mTotalDragDistance * dragPercent) + extraMove);
        return targetY;
    }


    /**
     * 开始拖拽
     */
    private void startDragging(float y) {
        final float yDiff = y - mInitialDownY;
        //如果滑动的Y偏移量大于mTouchSlop（8） 并且此时没有开始拖拽
        if (yDiff > mTouchSlop && !mIsBeingDragged) {
            //计算开始拖拽的Y坐标
            mInitialMotionY = mInitialDownY + mTouchSlop;
            mIsBeingDragged = true;//开始拖拽
        }
    }

    /**
     * 根据改变位置动画  移动位置
     */
    void moveCorrectPosition(float interpolatedTime) {
        int targetTop = 0;
        int offset = 0;
        switch (mRefreshStyle) {
            case FLOAT:
                targetTop = mFrom + (int) ((mOriginalRefreshViewOffsetTop + mTotalDragDistance - mFrom) * interpolatedTime);
                offset = targetTop - mRefreshView.getTop();
                break;
            default:
                if (mTargetPaddingBottom == Integer.MAX_VALUE) {
                    mTargetPaddingBottom = mTarget.getPaddingBottom();
                }
                targetTop = mFrom + (int) ((mTotalDragDistance - mFrom) * interpolatedTime);
                offset = targetTop - mTarget.getTop();
                //padding直接一次性到刷新位置,防止包皮出现
                if (mTarget.getPaddingBottom() != (int) (mTargetPaddingBottom + mTotalDragDistance)) {
                    mTarget.setPadding(mTarget.getPaddingLeft(), mTarget.getPaddingTop(),
                            mTarget.getPaddingRight(), (int) (mTargetPaddingBottom + mTotalDragDistance));
                }
                break;
        }
        setTargetOffsetTopAndBottom(offset);

    }

    /**
     * 根据还原到开始动画   移动到初始位置
     */
    void moveToStart(float interpolatedTime) {
        int targetTop = 0;
        int offset = 0;
        switch (mRefreshStyle) {
            case FLOAT:
                targetTop = mFrom + (int) ((mOriginalRefreshViewOffsetTop - mFrom) * interpolatedTime);
                offset = targetTop - mRefreshView.getTop();
                break;
            default:
                if (mTargetPaddingBottom == Integer.MAX_VALUE) {
                    mTargetPaddingBottom = mTarget.getPaddingBottom();
                }
                targetTop = mFrom + (int) (-mFrom * interpolatedTime);
                offset = targetTop - mTarget.getTop();
                //padding一次性全部还原原始位置,防止包皮出现
                if (mTargetPaddingBottom != Integer.MAX_VALUE) {
                    mTarget.setPadding(mTarget.getPaddingLeft(), mTarget.getPaddingTop(),
                            mTarget.getPaddingRight(), mTargetPaddingBottom);
                    mTargetPaddingBottom = Integer.MAX_VALUE;
                }
                break;
        }
        setTargetOffsetTopAndBottom(offset);
    }


    /**
     * 设置下拉组件的偏移量
     */
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
        if (!mRefreshing && !mIsExteRefreshComplete) {
            switch (mRefreshStyle) {
                case FLOAT:
                    mIRefreshStatus.onPullProgress(mCurrentTargetOffsetTop - mOriginalRefreshViewOffsetTop,
                            mTotalDragDistance,
                            (mCurrentTargetOffsetTop - mOriginalRefreshViewOffsetTop) / mTotalDragDistance, mIsPullToRefresh);
                    break;
                default:
                    mIRefreshStatus.onPullProgress(mCurrentTargetOffsetTop,
                            mTotalDragDistance,
                            mCurrentTargetOffsetTop / mTotalDragDistance, mIsPullToRefresh);
                    break;
            }
        }
    }

    /**
     * 当屏幕上有多个点被按住，松开其中一个点时触发（即非最后一个点被放开时）
     */
    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = ev.getActionIndex();
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

    /**
     * 刷新回调
     */
    public interface OnRefreshListener {
        /**
         * 刷新回调
         */
        void onRefresh();
    }

    /**
     * @author　　: 李坤
     * 创建时间: 2018/9/3 17:18
     * 邮箱　　：496546144@qq.com
     * <p>
     * 功能介绍：mTarget在垂直方向（-1）是否可以滚动
     */

    public interface OnChildScrollUpCallback {
        /**
         * mTarget在垂直方向（-1）是否可以滚动,可以滚动说明没到顶部呢
         *
         * @param parent
         * @param child
         * @return
         */
        boolean canChildScrollUp(SwipeRefreshLayout parent, @Nullable View child);
    }


    /********************************************************************************************
     *                                           公共方法
     ********************************************************************************************/


    public float getTotalDragDistance() {
        return mTotalDragDistance;
    }

    /**
     * @author　　: 李坤
     * 创建时间: 2018/5/29 0029 下午 2:06
     * 邮箱　　：496546144@qq.com
     * <p>
     * 方法功能：设置极限最大距离
     */

    public void setMaxDragDistance(float mMaxDragDistance) {
        this.mMaxDragDistance = mMaxDragDistance - mTotalDragDistance;
    }

    /**
     * 作者　　: 李坤
     * 创建时间: 2017/5/6 0006 22:12
     * <p>
     * 方法功能：设置最大下拉距离,达到这个值就会处于可刷新状态,是宁界点
     */
    public void setTotalDragDistance(float mTotalDragDistance) {
        mIsUseringTotalDragDistance = true;
        this.mTotalDragDistance = mTotalDragDistance;
    }

    /**
     * 是否正在积极显示刷新
     */
    public boolean isRefreshing() {
        return mRefreshing;
    }


    /**
     * 通知小部件刷新状态已经更改。不要在
     * 刷新是由一个滑动手势触发的。
     */
    public void setRefreshing(final boolean refreshing) {
        setRefreshing(refreshing, true);
    }

    /**
     * 设置刷新状态
     *
     * @param notify 是否通知回调
     */
    public void setRefreshing(final boolean refreshing, final boolean notify) {
        if (mIsLayoutOk) {
            if (mRefreshing != refreshing) {
                mNotify = notify;
                ensureTarget();
                mRefreshing = refreshing;
                if (mRefreshing) {
                    mIsExteRefreshComplete = false;
                    animateOffsetToCorrectPosition(mCurrentTargetOffsetTop, mRefreshListener);
                } else {
                    mIsExteRefreshComplete = true;
                    animateOffsetToStartPosition(mCurrentTargetOffsetTop, mResetListener);
                }
            }
        } else {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    setRefreshing(refreshing, notify);
                }
            }, 50);
        }

    }

    /**
     * 计算回弹动画时长
     */
    private int computeAnimateToStartDuration(int from) {
        if (from < mOriginalRefreshViewOffsetTop) {
            return 0;
        }
        switch (mRefreshStyle) {
            case FLOAT:
                return (int) (Math.max(0.0f, Math.min(1.0f, Math.abs(from - mOriginalRefreshViewOffsetTop) / mTotalDragDistance))
                        * mAnimateToStartDuration);
            default:
                return (int) (Math.max(0.0f, Math.min(1.0f, Math.abs(from) / mTotalDragDistance))
                        * mAnimateToStartDuration);
        }
    }

    /**
     * 计算下拉动画时长
     */
    private int computeAnimateToCorrectDuration(float from) {
        if (from < mOriginalRefreshViewOffsetTop) {
            return 0;
        }
        switch (mRefreshStyle) {
            case FLOAT:
                return (int) (Math.max(0.0f, Math.min(1.0f, Math.abs(from - mOriginalRefreshViewOffsetTop - mTotalDragDistance) / mTotalDragDistance))
                        * mAnimateToRefreshDuration);
            default:
                return (int) (Math.max(0.0f, Math.min(1.0f, Math.abs(from - mTotalDragDistance) / mTotalDragDistance))
                        * mAnimateToRefreshDuration);
        }
    }

    /**
     * 设置刷新监听
     *
     * @param listener
     */
    public void setOnRefreshListener(OnRefreshListener listener) {
        mListener = listener;
    }

    /**
     * {@link SwipeRefreshLayout#canChildScrollUp()}
     * mTarget在垂直方向（-1）是否可以滚动,可以滚动说明没到顶部呢
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
        if (enabled == isEnabled()) {
            return;
        }
        super.setEnabled(enabled);
        if (!enabled) {
            reset();
        }
    }

    /**
     * 当是Google风格的时候设置圆圈颜色
     *
     * @param colors
     */
    public void setColorSchemeColors(@ColorInt int... colors) {
        if (mRefreshView instanceof MaterialRefreshView) {
            ((MaterialRefreshView) mRefreshView).setColorSchemeColors(colors);
        }
    }

    /**
     * 当是Google风格的时候设置圆圈背景
     *
     * @param color
     */
    public void setProgressBackgroundColorSchemeColor(@ColorInt int color) {
        if (mRefreshView instanceof MaterialRefreshView) {
            ((MaterialRefreshView) mRefreshView).setProgressBackgroundColorSchemeColor(color);
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

    public View getRefreshView() {
        return mRefreshView;
    }

    /********************************************************************************************
     *                                           动画
     ********************************************************************************************/
    /**
     * 刷新动画创建,到刷新的地方
     */
    private void animateOffsetToCorrectPosition(int from, AnimationListener listener) {
        if (mRefreshView.getVisibility() != VISIBLE) {
            mRefreshView.setVisibility(VISIBLE);
        }
        if (getAnimation() != null) {
            getAnimation().cancel();
        }
        //防止动画没有执行完，padding设置错误
        if (mRefreshStyle != FLOAT && mTargetPaddingBottom != Integer.MAX_VALUE) {
            mTarget.setPadding(mTarget.getPaddingLeft(), mTarget.getPaddingTop(),
                    mTarget.getPaddingRight(), mTargetPaddingBottom);
            mTargetPaddingBottom = Integer.MAX_VALUE;
        }
        clearAnimation();
        if (computeAnimateToCorrectDuration(from) <= 0) {
            listener.onAnimationStart(null);
            listener.onAnimationEnd(null);
            return;
        }
        mFrom = from;
        mAnimateToCorrectPosition.reset();
        mAnimateToCorrectPosition.setDuration(computeAnimateToCorrectDuration(from));
        mAnimateToCorrectPosition.setInterpolator(mDecelerateInterpolator);
        if (listener != null) {
            mAnimateToCorrectPosition.setAnimationListener(listener);
        }
        startAnimation(mAnimateToCorrectPosition);
    }

    /**
     * 刷新动画执行
     */
    private final Animation mAnimateToCorrectPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            moveCorrectPosition(interpolatedTime);
        }
    };
    /**
     * 刷新动画监听
     */
    private AnimationListener mRefreshListener = new XAnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
            //防止下拉过快onPullProgress这个方法没有掉全
            switch (mRefreshStyle) {
                case FLOAT:
                    mIRefreshStatus.onPullProgress(mTotalDragDistance,
                            mTotalDragDistance,
                            1, mIsPullToRefresh);
                    break;
                default:
                    mIRefreshStatus.onPullProgress(mTotalDragDistance,
                            mTotalDragDistance,
                            1, mIsPullToRefresh);
                    break;
            }
            mIRefreshStatus.onRefreshing();
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (mRefreshing) {
                if (mNotify) {
                    if (mListener != null) {
                        mListener.onRefresh();
                    }
                }
                switch (mRefreshStyle) {
                    case FLOAT:
                        mCurrentTargetOffsetTop = mRefreshView.getTop();
                        break;
                    default:
                        mCurrentTargetOffsetTop = mTarget.getTop();
                        break;
                }
            } else {
                reset();
                if (mRefreshStyle != FLOAT && mTargetPaddingBottom != Integer.MAX_VALUE) {
                    mTarget.setPadding(mTarget.getPaddingLeft(), mTarget.getPaddingTop(),
                            mTarget.getPaddingRight(), mTargetPaddingBottom);
                    mTargetPaddingBottom = Integer.MAX_VALUE;
                }
            }
        }
    };

    /**
     * 回滚动画创建
     */
    private void animateOffsetToStartPosition(int from, AnimationListener listener) {
        if (getAnimation() != null) {
            getAnimation().cancel();
        }
        //防止动画没有执行完，padding设置错误
        if (mRefreshStyle != FLOAT && mTargetPaddingBottom != Integer.MAX_VALUE) {
            mTarget.setPadding(mTarget.getPaddingLeft(), mTarget.getPaddingTop(),
                    mTarget.getPaddingRight(), mTargetPaddingBottom);
            mTargetPaddingBottom = Integer.MAX_VALUE;
        }
        clearAnimation();
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
        startAnimation(mAnimateToStartPosition);
    }


    /**
     * 回滚动画执行
     */
    private final Animation mAnimateToStartPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            moveToStart(interpolatedTime);
        }
    };
    /**
     * 回滚动画监听
     */
    private final AnimationListener mResetListener = new XAnimationListener() {
        @Override
        public void onAnimationEnd(Animation animation) {
            mIsExteRefreshComplete = false;
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


    /********************************************************************************************
     *                      嵌套滑动 {@link NestedScrollingParent}
     ********************************************************************************************/
    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        if (isEnabled() && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0) {
            // Dispatch up to the nested parent
            startNestedScroll(nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL);
            return true;
        }
        return false;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        if (!mRefreshing) {
            // Dispatch up to the nested parent
            startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
            mTotalUnconsumed = 0;
            mNestedScrollInProgress = true;
        }
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        if (!mRefreshing) {
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
            if (dy > 0 && mTotalUnconsumed == 0
                    && Math.abs(dy - consumed[1]) > 0) {
                mRefreshView.setVisibility(View.GONE);
            }
        }
        // 父元素消耗
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
        if (!mRefreshing) {
            mNestedScrollInProgress = false;
            mTotalUnconsumed = mTotalUnconsumed * DRAG_RATE;
            if (mTotalUnconsumed > 0) {
                finishSpinner(mTotalUnconsumed);
                mTotalUnconsumed = 0;
            }
        }
        // 派发给父元素
        stopNestedScroll();
    }

    @Override
    public void onNestedScroll(final View target, final int dxConsumed, final int dyConsumed,
                               final int dxUnconsumed, final int dyUnconsumed) {
        // 派发给父元素
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                mParentOffsetInWindow);
        if (mRefreshing) {
            return;
        }
        final int dy = dyUnconsumed + mParentOffsetInWindow[1];
        if (dy < 0 && !canChildScrollUp()) {
            mTotalUnconsumed += Math.abs(dy);
            moveSpinner(mTotalUnconsumed);
        }
    }


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
