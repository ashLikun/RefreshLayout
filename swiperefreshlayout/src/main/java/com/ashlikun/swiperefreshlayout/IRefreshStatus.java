package com.ashlikun.swiperefreshlayout;


public interface IRefreshStatus {

    //复位状态
    void onReset();

    //正在刷新
    void onRefreshing();

    //下拉到刷新位置
    void onPullToRefreshStart();

    //从刷新位置还原到没有刷新的时候
    void onPullToRefreshFinish();

    /**
     * 当释放刷新控件的时候
     *
     * @param isRefresh 是否可以刷新
     */
    void onFinishSpinner(boolean isRefresh);

    /**
     * 作者　　: 李坤
     * 创建时间: 2017/5/5 0005 11:52
     * <p>
     * 方法功能：下拉距离改变
     *
     * @param pullDistance   下拉距离
     * @param totalDistance  总距离
     * @param pullProgress   下拉进度
     * @param isRefreshStart 是否可以刷新了
     */
    void onPullProgress(float pullDistance, float totalDistance, float pullProgress, boolean isRefreshStart);

    /**
     * 作者　　: 李坤
     * 创建时间: 2017/6/2 17:25
     * <p>
     * 方法功能：下拉到最大值开始
     */
    void onPullToMaxBottomStart();

    /**
     * 作者　　: 李坤
     * 创建时间: 2017/6/2 17:25
     * <p>
     * 方法功能：下拉到最大值结束
     */

    void onPullToMaxBottomFinish();
}
