
package com.fencer.hatomsdk.window;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.RectF;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.LinkedList;
import java.util.List;


public class WindowGroupAdapter {
    private static final String TAG = "WindowGroupAdapter";
    /**
     * 放大动画参数
     */
    private static final float SCALE_SIZE = 1.08f;
    private static final float ALPHA_TRANSLUCENT = 0.8f;
    private static final long SCALE_DURATION = 150;
    /**
     * 父容器
     */
    private final WindowGroup mWindowGroup;
    private int mWindowItemWidth;
    private int mWindowItemHeight;
    private int mWindowGroupWidth;
    private int mWindowGroupHeight;
    /**
     * 当前第几屏
     */
    private int mCurrentPage = 0;
    private int mLastPage = 0;
    /**
     * 判断当前是否是多窗口画面
     */
    @WindowGroup.WindowMode
    private int mWindowMode;
    /**
     * 上一次分屏模式
     */
    @WindowGroup.WindowMode
    private int mLastWindowMode;
    /**
     * 最大屏幕数
     */
    private int mScreenCount = 4;
    /**
     * 窗口最大数
     */
    private int mMaxCount = 16;

    private int DEFAULT_SUPPORT_MAX_WINDOW = 16;
    /**
     * 当前最大显示数
     */
    private int mShowWindowMaxCount = 16;
    /**
     * 页面切换回调监听
     */
    private OnPageChangeListener mOnPageChangeListener = null;
    /**
     * 当前选中窗口的回调监听
     */
    private OnSingleClickListener mCurrentSelectedListener = null;
    /**
     * 窗口长按监听器
     */
    private OnViewGroupTouchEventListener mOnWindowLongClickListener = null;
    /**
     * 当前选中窗口到达边界时候回调监听
     */
    private OnWindowItemScreenEdgeListener mOnWindowItemScreenEdgeListener = null;
    /**
     * 改变当前分屏模式监听
     */
    private OnChangeWindowScreenModeListener mOnChangeWindowScreenModeListener = null;
    /**
     * 改变显示窗口最大数回调
     */
    private OnChangeShowWindowMaxCountListener mOnChangeShowWindowMaxCountListener = null;
    /**
     * 窗口双击使能
     */
    private boolean mIsDoubleClickEnable = true;
    /**
     * 是否需要禁止拖动窗口
     */
    private boolean IS_FORBID_WINDOW_MOVE = false;
    /**
     * 是否接受触碰处理
     */
    private boolean mIsTouchEnable = true;
    /**
     * 是否允许滑动
     */
    private boolean mIsAllowScroller = true;
    /**
     * 是否允许窗口交换
     */
    private boolean mIsAllowWindowSwap = true;
    /**
     * 左右边界宽度
     */
    private int mScaledEdgeSlop;
    /**
     * 上边界宽度
     */
    private int mDeleteEdgeSlop;
    /**
     * 子view的引用
     */
    private final List<WindowItemView> mItemWindowStructList = new LinkedList<>();
    private final List<WindowItemView> mItemWindowStructAllList = new LinkedList<>();
    /**
     * 用户当前操作的子view
     */
    private WindowItemView mCurrentWindowItem;
    /**
     * 需要被替换的子view
     */
    private WindowItemView mReplaceWindowItem;
    /**
     * 用户之前操作的子view
     */
    private WindowItemView mLastContainer;
    private WindowGroup.PagerObserver mObserver;

    /* package */ WindowGroupAdapter(WindowGroup windowGroup) {
        mWindowGroup = windowGroup;
    }

    /**
     * 初始化
     */
    /* package */void init(int defaultWindowMode, int maxCount) {
        mMaxCount = maxCount;
        mShowWindowMaxCount = maxCount;
        setWindowMode(defaultWindowMode);
        mScaledEdgeSlop = ViewConfiguration.get(mWindowGroup.getContext()).getScaledEdgeSlop();
        mDeleteEdgeSlop = ViewConfiguration.get(mWindowGroup.getContext()).getScaledEdgeSlop();
    }

    public int getWindowItemWidth() {
        return mWindowItemWidth;
    }

    public int getWindowItemHeight() {
        return mWindowItemHeight;
    }

    public int setWindowGroupWidth() {
        return mWindowGroupWidth;
    }

    public int setWindowGroupHeight() {
        return mWindowGroupHeight;
    }

    public int getScreenCount() {
        return mScreenCount;
    }

    @WindowGroup.WindowMode
    public int getWindowMode() {
        return mWindowMode;
    }

    @WindowGroup.WindowMode
    public int getLastWindowMode() {
        return mLastWindowMode;
    }

    /* package */ void setWindowGroupWidth(int windowGroupWidth) {
        mWindowGroupWidth = windowGroupWidth;
    }

    /* package */ void setWindowGroupHeight(int windowGroupHeight) {
        mWindowGroupHeight = windowGroupHeight;
    }

    /* package */ void setWindowItemWidth(int windowItemWidth) {
        mWindowItemWidth = windowItemWidth;
    }

    /* package */ void setWindowItemHeight(int windowItemHeight) {
        mWindowItemHeight = windowItemHeight;
    }

    public WindowItemView getLastContainer() {
        return mLastContainer;
    }

    /* package */ void setLastContainer(WindowItemView lastContainer) {
        mLastContainer = lastContainer;
    }

    public WindowItemView getCurrentWindowItem() {
        return mCurrentWindowItem;
    }

    /* package */ void setCurrentWindowItem(WindowItemView currentWindowItem) {
        mCurrentWindowItem = currentWindowItem;
    }

    public WindowItemView getReplaceWindowItem() {
        return mReplaceWindowItem;
    }

    /* package */ void setReplaceWindowItem(WindowItemView replaceWindowItem) {
        mReplaceWindowItem = replaceWindowItem;
    }

    /**
     * 设置当前分屏模式
     *
     * @param windowMode 窗口分屏模式
     */
    public void setWindowMode(@WindowGroup.WindowMode int windowMode) {
        if (windowMode == mWindowMode) {//重复操作
            return;
        }
        mLastWindowMode = mWindowMode;
        mWindowMode = windowMode;
        int temp = getShowWindowMaxCount() / (windowMode * windowMode);
        mScreenCount = (getShowWindowMaxCount() % (windowMode * windowMode) == 0) ? temp : (temp + 1);
        mWindowGroup.setWindowMode(windowMode);
        if (mCurrentWindowItem != null) {
            setCurrentPage(mCurrentWindowItem.getScreenIndex());
        }
        if (mOnChangeWindowScreenModeListener != null) {
            mOnChangeWindowScreenModeListener.onChangeMode(mLastPage, getCurrentPage(), mLastWindowMode, windowMode);
        }
        //设置WindowItemView对用户可见或不可见
        setPageChangeEvent(getCurrentPage(), getWindowMode());
    }

    void refreshWindow() {
        int temp = getShowWindowMaxCount() / (mWindowMode * mWindowMode);
        mScreenCount = (getShowWindowMaxCount() % (mWindowMode * mWindowMode) == 0) ? temp : (temp + 1);
        mWindowGroup.setWindowMode(mWindowMode);
        if (mCurrentWindowItem != null) {
            setCurrentPage(mCurrentWindowItem.getScreenIndex());
        }
//        if (mOnChangeWindowScreenModeListener != null) {
//            mOnChangeWindowScreenModeListener.onChangeMode(mLastPage, getCurrentPage(), mLastWindowMode, mWindowMode);
//        }
        //设置WindowItemView对用户可见或不可见
        setPageChangeEvent(getCurrentPage(), getWindowMode());
    }

    public int getMaxCount() {
        return mMaxCount;
    }

    /**
     * 修改默认支持的最大播放窗口数量，默认是16个
     *
     * @param defaultSupportMaxWindowCount 默认支持的最大播放窗口数量
     */
    public void setDefaultSupportMaxWindowCount(int defaultSupportMaxWindowCount) {
        this.DEFAULT_SUPPORT_MAX_WINDOW = defaultSupportMaxWindowCount;
    }

    /**
     * 设置需要的窗体数量,一般是首次设置播放时或播放窗体数量有所增加时调用
     *
     * @param needWindowCount 需要的窗体数量
     */
    public void setCurrentNeedWindowCount(int needWindowCount) {
        //如果需要的窗口数量小于等于0或大于默认支持的窗口数就不执行后面的处理逻辑
        if (needWindowCount <= 0 || needWindowCount > DEFAULT_SUPPORT_MAX_WINDOW) {
            return;
        }
        int finalNeedWindowCount;
        //当前需要的页数（每页包含(mWindowMode * mWindowMode)个播放窗口）
        int dividedPagerCount = Math.abs(needWindowCount) / (mWindowMode * mWindowMode);
        //无法占满一页时，多余的窗体数量
        int modelWindowCount = Math.abs(needWindowCount) % (mWindowMode * mWindowMode);
        //如果模数不为0那就需要再加一页（4个）播放窗口
        if (modelWindowCount == 0) {
            finalNeedWindowCount = dividedPagerCount * (mWindowMode * mWindowMode);
        } else {
            finalNeedWindowCount = (dividedPagerCount + 1) * (mWindowMode * mWindowMode);
        }
        this.mMaxCount = finalNeedWindowCount;
        this.mShowWindowMaxCount = finalNeedWindowCount;
    }

    public int getShowWindowMaxCount() {
        return mShowWindowMaxCount;
    }

    /**
     * 设置当前最大显示窗口数
     *
     * @param showWindowMaxCount
     */
    public void setShowWindowMaxCount(int showWindowMaxCount) {
        if (showWindowMaxCount >= getMaxCount()) {
            showWindowMaxCount = getMaxCount();
        } else if (showWindowMaxCount < 0) {
            showWindowMaxCount = 0;
        }
        int temp = showWindowMaxCount / (mWindowMode * mWindowMode);
        int maxScreenCount = (showWindowMaxCount % (mWindowMode * mWindowMode) == 0) ? temp : (temp + 1);
        setShowScreenMaxPage(maxScreenCount);
    }

    /**
     * 设置当前最大显示页数
     *
     * @param showScreenMaxPage
     */
    public void setShowScreenMaxPage(int showScreenMaxPage) {
        int temp = getMaxCount() / (mWindowMode * mWindowMode);
        int maxScreenCount = (getMaxCount() % (mWindowMode * mWindowMode) == 0) ? temp : (temp + 1);
        boolean isChanged = false;
        if (showScreenMaxPage != mScreenCount) {
            isChanged = true;
        }
        if (showScreenMaxPage > maxScreenCount) {
            mScreenCount = maxScreenCount;
        } else if (showScreenMaxPage < 0) {
            mScreenCount = 0;
        } else {
            mScreenCount = showScreenMaxPage;
        }
//        if (mCurrentWindowItem != null && isChanged)
//        {
//            setCurrentPage(getCurrentPage());
//        }
        mShowWindowMaxCount = getScreenCount() * (mWindowMode * mWindowMode);
        showOrHideWindowItem(getShowWindowMaxCount());
    }

    /**
     * 显示隐藏窗口
     *
     * @param showWindowMaxCount 数量
     */
    private void showOrHideWindowItem(int showWindowMaxCount) {
        List<WindowItemView> windowItemStructList = getWindowItemStructAllList();
        if (showWindowMaxCount > windowItemStructList.size()) {
            showWindowMaxCount = windowItemStructList.size();
        }
        for (WindowItemView item : windowItemStructList) {
            int windowSerial = item.getWindowSerial();
            if (windowSerial < showWindowMaxCount) {
                item.setVisibility(View.VISIBLE);
            } else {
                item.setVisibility(View.GONE);
            }
        }

        if (mOnChangeShowWindowMaxCountListener != null) {
            mOnChangeShowWindowMaxCountListener.onChangeMaxCount(getCurrentWindowItem(), mLastPage, getCurrentPage(), mLastWindowMode, mWindowMode);
        }
    }

    public int getCurrentPage() {
        return mCurrentPage;
    }

    /* package */ void setCurrentPage(int pageIndex) {
        boolean isChange = false;
        while (pageIndex >= mScreenCount) {
            isChange = true;
            pageIndex--;
        }

        if (mCurrentPage == pageIndex) {
            return;
        }
        mLastPage = mCurrentPage;
        mCurrentPage = pageIndex;
        if (isChange) {
            mWindowGroup.selectedCurrFirstWindow();
        }


        //设置窗口改变的回调事件
        if (getOnPageChangeListener() != null) {
            getOnPageChangeListener().onPageChange(pageIndex, getWindowMode(), getScreenCount());
        }
    }

    /**
     * <p>当窗口页面改变时，需要设置WindowItemView对用户可见或不可见，共有以下两种情况：</p>
     * <p>
     * 1、改变窗口分屏模式时（分为两种情况：双击改变、主动setWindowMode）
     * 2、滑动屏幕，改变页码时
     * </p>
     *
     * @param curPage        当前页码
     * @param currWindowMode 分屏模式
     */
    public void setPageChangeEvent(int curPage, int currWindowMode) {
        int pagerSize = (int) Math.pow(currWindowMode, 2);
        //当前页面中窗口的开始序号和结束序号
        int startIndex = curPage * pagerSize;
        int endIndex = startIndex + pagerSize - 1;
        Log.d(TAG, "当前页码 = 第" + curPage + "页, startIndex = " + startIndex + ", endIndex = " + endIndex + "，共有" + getScreenCount() + "屏");
        //设置WindowItemView对用户可见或不可见
        for (WindowItemView itemView : getWindowItemStructAllList()) {
            int serial = itemView.getWindowSerial();
            if (serial < startIndex || serial > endIndex) {
                if (itemView.getUserVisibleHint()) {
                    itemView.setUserVisibleHint(false);
                }
            } else {
                if (!itemView.getUserVisibleHint()) {
                    itemView.setUserVisibleHint(true);
                }
            }
        }
    }


    /**
     * 刷新当前播放窗口的数量，仅在窗口模式变化时调用，此时用来增补窗口
     *
     * @param windowItemViews 正在使用的窗体队列
     */
    public void refreshPlayWindowNum(List<? extends WindowItemView> windowItemViews) {
        //如果窗口不足，则需要补充
        if (getWindowItemStructList().size() != mWindowMode * mWindowMode) {
            int needNum = Math.max(mWindowMode * mWindowMode, getWindowItemStructList().size());
            setCurrentNeedWindowCount(needNum);
            notifyDataSetChanged();
            return;
        }

        //如果窗体数量最后只剩下mWindowMode*mWindowMode个就不需要删除了，需要保留
        if (getWindowItemStructList().size() == mWindowMode * mWindowMode) {
            return;
        }
        //如果是单画面模式或是没有窗体在使用，不做刷新数量更新操作
        if (mWindowMode == WindowGroup.WINDOW_MODE_ONE || windowItemViews.isEmpty()) {
            return;
        }
        /*
         * 统计出正在播放窗口说在分屏的页码，查看是否存在跳页的现象
         * 如果跳页了就把跳跃的那页窗体移除
         */
        //根据现有的窗口数量和当前模式计算出页码数据。
        int[] screenIndexArray = new int[mMaxCount / (mWindowMode * mWindowMode)];
        for (WindowItemView windowItemView : windowItemViews) {
            //取出当前窗口所处的分屏页码
            int screenIndex = windowItemView.getScreenIndex();
            if (screenIndex >= screenIndexArray.length) {
                continue;
            }
            //计算当前页有多少窗口正在使用，并记录数量
            screenIndexArray[screenIndex] = screenIndexArray[screenIndex] + 1;
        }
        //统计完毕后，如果数组中某一页正在使用的窗体数量为0就把删除该页窗体
        mWindowGroup.deleteNotUseWindowGroupByScreen(screenIndexArray);
    }

    /**
     * 设置是否禁止拖动窗口
     *
     * @param forbidMove 是否禁止拖动窗口
     */
    public void setWindowForbidMove(boolean forbidMove) {
        IS_FORBID_WINDOW_MOVE = forbidMove;
    }

    /**
     * 是否禁止拖动窗口
     *
     * @return 是否禁止拖动窗口
     */
    public boolean isForbidWindowMoved() {
        return IS_FORBID_WINDOW_MOVE;
    }

    public void setAllDoubleClickEnable(boolean isEnabled) {
        mIsDoubleClickEnable = isEnabled;
    }

    public boolean getAllDoubleClickEnable() {
        return mIsDoubleClickEnable;
    }

    public List<WindowItemView> getWindowItemStructList() {
        return mItemWindowStructList;
    }

    public List<WindowItemView> getWindowItemStructAllList() {
        return mItemWindowStructAllList;
    }

    /**
     * 判断是否接受触碰
     *
     * @return
     */
    public boolean isTouchEnable() {
        return mIsTouchEnable;
    }

    /**
     * 设置是否接受触碰
     *
     * @param enable
     */
    public void setIsTouchEnable(boolean enable) {
        mIsTouchEnable = enable;
    }


    /**
     * 获取滑动
     *
     * @return
     */
    public boolean getIsAllowScroller() {
        return mIsAllowScroller;
    }

    /**
     * 是否允许滑动
     *
     * @param isAllowScroller
     */
    public void setIsAllowScroller(boolean isAllowScroller) {
        mIsAllowScroller = isAllowScroller;
    }

    /**
     * 是否允许窗口交换
     */
    public boolean isAllowWindowSwap() {
        return mIsAllowWindowSwap;
    }

    /**
     * 设置是否允许窗口交换
     */
    public void setAllowWindowSwap(boolean mIsAllowWindowSwap) {
        this.mIsAllowWindowSwap = mIsAllowWindowSwap;
    }

    /**
     * 获得左右边界宽度
     *
     * @return
     */
    public int getScaledEdgeSlop() {
        return mScaledEdgeSlop;
    }

    public void setScaledEdgeSlop(int scaledEdgeSlop) {
        mScaledEdgeSlop = scaledEdgeSlop;
    }

    /**
     * 获得上边边界宽度
     *
     * @return
     */
    public int getDeleteEdgeSlop() {
        return mDeleteEdgeSlop;
    }

    public void setDeleteEdgeSlop(int deleteEdgeSlop) {
        mDeleteEdgeSlop = deleteEdgeSlop;
    }


    /**
     * 放大缩小动画(SurfaceView用不了动画)
     *
     * @param windowItem
     * @param isEnlarge
     */
    void animatorScale(WindowItemView windowItem, boolean isEnlarge) {
        if (windowItem == null) {
            return;
        }
        if (!windowItem.isAnimatorScaleEnable()) {
            windowItem.setScaleX(1.0f);
            windowItem.setScaleY(1.0f);
            windowItem.setAlpha(1.0f);
            return;
        }
        synchronized (windowItem) {
            ObjectAnimator scaleXAnimation = ObjectAnimator.ofFloat(windowItem, "scaleX", (isEnlarge ? 1.0f : SCALE_SIZE), (isEnlarge ? SCALE_SIZE : 1.0f));
            ObjectAnimator scaleYAnimation = ObjectAnimator.ofFloat(windowItem, "scaleY", (isEnlarge ? 1.0f : SCALE_SIZE), (isEnlarge ? SCALE_SIZE : 1.0f));
            ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat(windowItem, "alpha", (isEnlarge ? 1.0f : ALPHA_TRANSLUCENT), (isEnlarge ? ALPHA_TRANSLUCENT : 1.0f));
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.setDuration(SCALE_DURATION);
            animatorSet.playTogether(scaleXAnimation, scaleYAnimation, alphaAnimation);
            animatorSet.start();
        }
    }

    /**
     * 获取大小
     *
     * @return
     */
    public RectF getRectF() {
        return mWindowGroup.getRectF();
    }

    /*设置监听************************************************/

    /**
     * 设置长按监听器
     *
     * @param listener
     */
    public void setOnWindowGroupTouchEventListener(OnViewGroupTouchEventListener listener) {
        mOnWindowLongClickListener = listener;
    }

    /* package */ OnViewGroupTouchEventListener getOnViewGroupTouchEventListener() {
        return mOnWindowLongClickListener;
    }

    /**
     * 设置页面切换监听
     *
     * @param listener
     */
    public void setOnPageChangeListener(OnPageChangeListener listener) {
        mOnPageChangeListener = listener;
    }

    /* package */ OnPageChangeListener getOnPageChangeListener() {
        return mOnPageChangeListener;
    }

    /**
     * 设置选中窗口事件回调
     *
     * @param listener
     */
    public void setOnCurrentSelectedWindowListener(OnSingleClickListener listener) {
        mCurrentSelectedListener = listener;
    }

    /* package */ OnSingleClickListener getOnCurrentSelectedWindowListener() {
        return mCurrentSelectedListener;
    }

    /**
     * 设置当前窗口达到边界时候回调
     *
     * @param listener
     */
    public void setOnWindowItemScreenEdgeListener(OnWindowItemScreenEdgeListener listener) {
        mOnWindowItemScreenEdgeListener = listener;
    }

    /* package */ OnWindowItemScreenEdgeListener getOnWindowItemScreenEdgeListener() {
        return mOnWindowItemScreenEdgeListener;
    }

    /**
     * 设置当前分屏模式改变回调
     *
     * @param listener
     */
    public void setOnChangeWindowScreenModeListener(OnChangeWindowScreenModeListener listener) {
        mOnChangeWindowScreenModeListener = listener;
    }

    /**
     * 改变显示窗口最大数回调
     *
     * @param listener
     */
    public void setOnChangeShowWindowMaxCountListener(OnChangeShowWindowMaxCountListener listener) {
        mOnChangeShowWindowMaxCountListener = listener;
    }

    void setViewPagerObserver(WindowGroup.PagerObserver mObserver) {
        this.mObserver = mObserver;
    }

    public void notifyDataSetChanged() {
        mObserver.onChanged();
    }


    /*各种监听事件************************************************/

    public interface OnViewGroupTouchEventListener {
        /**
         * 长按窗口
         *
         * @param windowContainer
         */
        void onLongPress(WindowItemView windowContainer);

        /**
         * 结束长按回调
         *
         * @param page
         * @param currentWindowItem
         * @param replaceWindowItem
         */
        void onLongPressEnd(int page, WindowItemView currentWindowItem, WindowItemView replaceWindowItem);

        /**
         * 替换窗口
         *
         * @param page
         * @param windowContainer
         * @param replaceContainer
         */
        void onReplaceWindowItem(int page, WindowItemView windowContainer, WindowItemView replaceContainer);

        /**
         * 双击窗口
         *
         * @param page
         * @param currentContainer
         * @param windowMode
         * @param toMode
         */
        void onDoubleClick(int page, WindowItemView currentContainer, @WindowGroup.WindowMode int windowMode, @WindowGroup.WindowMode int toMode);

        /**
         * 滑动屏幕成功后调用
         *
         * @param oldPage
         * @param newPage
         * @param currentContainer
         * @param windowMode
         * @param screenCount
         */
        void onSnapToScreenEnd(int oldPage, int newPage, WindowItemView currentContainer, @WindowGroup.WindowMode int windowMode, int screenCount);

    }

    public interface OnPageChangeListener {
        /**
         * 回调切换页面事件
         *
         * @param page
         * @param windowMode
         * @param screenCount 总数
         */
        void onPageChange(int page, @WindowGroup.WindowMode int windowMode, int screenCount);
    }

    public interface OnSingleClickListener {
        /**
         * 某一窗口被点击
         */
        void onWindowSingleClick(WindowItemView currentContainer);
    }

    public interface OnWindowItemScreenEdgeListener {
        /**
         * 上边
         *
         * @param currentContainer - 不再范围内为null
         */
        void onTop(WindowItemView currentContainer);

        /**
         * 左边
         *
         * @param currentContainer - 不再范围内为null
         */
        void onLeft(WindowItemView currentContainer);

        /**
         * 右边
         *
         * @param currentContainer - 不再范围内为null
         */
        void onRight(WindowItemView currentContainer);

        /**
         * 结束完成
         *
         * @param currentContainer
         */
        void onFinish(WindowItemView currentContainer);
    }

    public interface OnChangeWindowScreenModeListener {
        /**
         * 改变的分屏模式
         *
         * @param currWindowModeEnum
         */
        void onChangeMode(int lastPage, int currPage, @WindowGroup.WindowMode int lastWindowModeEnum, @WindowGroup.WindowMode int currWindowModeEnum);
    }

    public interface OnChangeShowWindowMaxCountListener {
        /**
         * 改变显示窗口最大数回调
         *
         * @param currentContainer
         * @param lastPage
         * @param currPage
         * @param lastWindowModeEnum
         * @param currWindowModeEnum
         */
        void onChangeMaxCount(WindowItemView currentContainer, int lastPage, int currPage, @WindowGroup.WindowMode int lastWindowModeEnum, @WindowGroup.WindowMode int currWindowModeEnum);
    }
}
