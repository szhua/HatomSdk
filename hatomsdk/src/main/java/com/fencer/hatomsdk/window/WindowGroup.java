
package com.fencer.hatomsdk.window;

import static android.view.View.MeasureSpec.EXACTLY;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;
import androidx.annotation.IntDef;

import com.blankj.utilcode.util.SizeUtils;
import com.fencer.hatomsdk.PlayWindowView;
import com.fencer.hatomsdk.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;


/**
 * <p> 可以自由拖动的多窗口容器</p>
 *
 * @author 张华洋 2017/6/8 21:12
 * @version V1.1
 * @name WindowGroup
 */
public class WindowGroup extends ViewGroup {
    private static final String TAG = "WindowGroup";
    /*默认分屏模式*/
    public static int DEFAULT_MODE = WindowGroup.WINDOW_MODE_FOUR;
    /*窗口容器分屏模式，分别为1、4、9、16分屏*/
    public static final int WINDOW_MODE_ONE = 1;
    public static final int WINDOW_MODE_FOUR = 2;
    public static final int WINDOW_MODE_NINE = 3;
    public static final int WINDOW_MODE_SIXTEEN = 4;
    /*点击事件模式TouchMode*/
    static final int NORMAL = 1;
    /*长按*/
    static final int LONG = 2;
    /*左右滑动*/
    static final int LANDSCAPE = 3;
    /*双击*/
    static final int DOUBLE_CLICK = 4;

    /**
     * 竖屏窗口的宽高比例
     */
    public static final float ASPECT_RATIO = 0.667f;
    /**
     * 长按事件时间
     */
    private static final long LONG_PRESSED_TIME = 300;
    /**
     * 滑动一页所需要的时间
     */
    private static final int SCROLL_SCREEN_TIME = 800;
    /**
     * 屏幕滑动超过该距离，才会切换屏幕
     */
    private static final int SWITCH_PAGE_SLOP = 100;
    /**
     * 屏幕滑动超过该距离，控件才会跟随移动
     */
    private static final int TOUCH_SLOP = 40;
    /**
     * 屏幕滑动超过该事件，切换屏幕
     */
    private static final int SWITCH_EDGE_SCREEN_TIME_MILLIS = 300;
    /**
     * 最大窗口数
     */
    private int mMaxCount;
    /**
     * 记录上一次的最大窗口数
     */
    private int mLastMaxCount;
    /**
     * 除最大窗口数外，再多添加的窗口数
     */
    private int mMoreCount;
    /**
     *
     */
    private int mWindowMode;
    /**
     * 选择颜色
     */
    private boolean mWindowType;
    /**
     * 滑动控制器
     */
    private Scroller mScroller;
    /**
     * 当前click类型
     */
    @TouchMode
    private int mClickMode = NORMAL;
    /**
     * 默认WindowGroup模式页数窗口计算管理类
     */
    private final WindowGroupAdapter mWindowGroupAdapter = new WindowGroupAdapter(this);
    /**
     * 用户触屏的位置（上下滑动窗口的方法中，横向）
     */
    private float mLastDownRawX;
    /**
     * 用户触屏的位置（上下滑动窗口的方法中，纵向）
     */
    private float mLastDownRawY;
    /**
     * 判断是否可以左右移动
     */
    private boolean mIsLandscapeCanMove = false;
    /**
     * 判断是否可以上下移动
     */
    private boolean mIsPortraitCanMove = false;
    /**
     * 用户触屏的位置(左右滑屏方法中)
     */
    private float mLastLeftRightX;
    /**
     * 用户按下的位置，切换窗口判断中用到
     */
    private float mSwitchDownX;
    /**
     * 记录上次滑边的时间
     */
    private float mLastSwitchEdgeScreenTimeMillis = -1;
    /**
     * 当前View尺寸
     */
    private final RectF mRectF = new RectF();
    /**
     * 布局中设置的高度和实际WindowGroup高度的差值
     */
    private int mOffset;
    /**
     * 拦截事件中按下时的x,y值
     */
    private int mLastInterceptDownX;
    private int mLastInterceptDownY;
    /**
     * 屏幕最小滑动距离
     */
    private int mMinTouchSlop;
    private PagerObserver mObserver;


    /**
     * 窗口模式注解
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({WINDOW_MODE_ONE, WINDOW_MODE_FOUR, WINDOW_MODE_NINE, WINDOW_MODE_SIXTEEN})
    public @interface WindowMode {
    }

    /**
     * 触摸模式注解
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({NORMAL, LONG, LANDSCAPE, DOUBLE_CLICK})
    private @interface TouchMode {
    }

    public WindowGroup(Context context) {
        this(context, null);
    }

    public WindowGroup(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WindowGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
        initViews(context);
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray t = context.obtainStyledAttributes(attrs, R.styleable.play_win_group_view);
        mWindowMode = t.getInteger(R.styleable.play_win_group_view_window_mode, DEFAULT_MODE);
        mMaxCount = t.getInteger(R.styleable.play_win_group_view_item_max_count, 16);
        mWindowType = t.getBoolean(R.styleable.play_win_group_view_item_load_view_is_preview, true);
        mMoreCount = t.getInteger(R.styleable.play_win_group_view_except_max_more_item, 0);
        mLastMaxCount = mMaxCount;
        t.recycle();
    }

    private void initViews(Context context) {
        setBackground(null);
        mScroller = new Scroller(context);
        mWindowGroupAdapter.init(mWindowMode, mMaxCount);
        mMinTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initAdapter();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        int windowMode = mWindowGroupAdapter.getWindowMode();

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            int itemWidth = widthSpecSize / windowMode;
            int itemHeight = (int) (itemWidth * ASPECT_RATIO);

            mOffset = 0;
            heightSpecSize = itemHeight * windowMode;
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mOffset = 0;//横屏时要将偏差置为0；
            if (!mWindowType) {//横屏的窗口需要空出时间条的宽度
                heightSpecSize = heightSpecSize - SizeUtils.dp2px(50f);
            }
        }


        mWindowGroupAdapter.setWindowGroupWidth(widthSpecSize);
        mWindowGroupAdapter.setWindowGroupHeight(heightSpecSize);

        int childWidth = widthSpecSize / windowMode;
        int childHeight = heightSpecSize / windowMode;
        //要求所有子View测量自己的大小
        measureChildren(MeasureSpec.makeMeasureSpec(childWidth, EXACTLY), MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY));
        //设置测量完成的尺寸
        setMeasuredDimension(widthMeasureSpec, MeasureSpec.makeMeasureSpec(heightSpecSize, MeasureSpec.EXACTLY));

        scrollTo(mWindowGroupAdapter.getCurrentPage() * widthSpecSize, 0);
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int windowMode = mWindowGroupAdapter.getWindowMode();
        int groupWidth = getMeasuredWidth();
        int groupHeight = getMeasuredHeight();
        int windowItemWidth = groupWidth / windowMode;
        int windowItemHeight = groupHeight / windowMode;
        mWindowGroupAdapter.setWindowItemWidth(windowItemWidth);
        mWindowGroupAdapter.setWindowItemHeight(windowItemHeight);

        //重新计算显示的画面
        getWindowStructList().clear();
        for (WindowItemView item : getWindowStructAllList()) {
            if (item.getVisibility() != View.GONE) {
                calcWindowItem(windowMode, item, item.getWindowSerial());
                int itemLeft = left + groupWidth * item.getScreenIndex() + windowItemWidth * item.getColumnIndex();
                int itemRight = itemLeft + windowItemWidth;
                int itemTop = mOffset / 2 + windowItemHeight * item.getRowIndex();//偏移量的一半就是WindowGroup的paddingTop和paddingBottom
                int itemBottom = itemTop + windowItemHeight;
                item.layout(itemLeft, itemTop, itemRight, itemBottom);
                item.setRectF(left + windowItemWidth * item.getColumnIndex(), itemTop, left + windowItemWidth * item.getColumnIndex() + windowItemWidth, itemBottom);
                getWindowStructList().add(item);
            }
        }

        mRectF.top = top;
        mRectF.left = left;
        mRectF.right = right;
        mRectF.bottom = bottom;
    }


    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }
    }


    /**
     * 获取 WindowGroup 的矩形大小
     *
     * @return RectF 所占矩形大小
     */
    public RectF getRectF() {
        return mRectF;
    }


    /**
     * 设置选定当前窗口
     *
     * @param currentWindowItem 当前窗口
     */
    public void setCurrentWindowItem(final WindowItemView currentWindowItem) {
        if (currentWindowItem != getCurrentWindowItem()) {
            mWindowGroupAdapter.setLastContainer(getCurrentWindowItem());
            mWindowGroupAdapter.setCurrentWindowItem(currentWindowItem);

            if (null != currentWindowItem) {
                for (final WindowItemView container : getWindowStructAllList()) {
                    if (container != currentWindowItem) {
                        container.setViewSelected(false);
                    }
                }
                currentWindowItem.bringToFront();
                currentWindowItem.setViewSelected(true);
            }
        }

        final WindowGroupAdapter.OnSingleClickListener onCurrentSelectedWindowListener = mWindowGroupAdapter.getOnCurrentSelectedWindowListener();
        if (onCurrentSelectedWindowListener != null) {
            onCurrentSelectedWindowListener.onWindowSingleClick(currentWindowItem);
        }
    }

    private List<WindowItemView> getWindowStructList() {
        return mWindowGroupAdapter.getWindowItemStructList();
    }

    private List<WindowItemView> getWindowStructAllList() {
        return mWindowGroupAdapter.getWindowItemStructAllList();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mWindowGroupAdapter.isTouchEnable()) {
            Log.d(TAG, "mWindowGroupAdapter---  dispatchTouchEvent!!" + event.getPointerCount());
            return false;
        }

        if (!isPointerInSpace((int) event.getRawX(), (int) event.getRawY()) && (event.getAction() == MotionEvent.ACTION_DOWN)) {
            return false;
        }
        handleTouchEvent(event);
        return true;
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        WindowItemView windowItemView = mWindowGroupAdapter.getCurrentWindowItem();
        //如果是正在电子放大就直接由子view处理
        if (windowItemView.isZoom()) {
            resetActionDownPoint();
            return false;
        }
        //如果是开启鱼眼就直接由子view处理
        if (windowItemView.isOpenFishEye()) {
            resetActionDownPoint();
            return false;
        }
        //默认不拦截触摸事件，由子view处理
        boolean intercept = false;
        int rawX = (int) ev.getRawX();
        int rawY = (int) ev.getRawY();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handleTouchEvent(ev);
                mLastInterceptDownX = rawX;
                mLastInterceptDownY = rawY;
                intercept = false;
                break;
            case MotionEvent.ACTION_MOVE:
                //如果操作view的手指超过两个，就不拦截触摸事件，由子view处理，避免电子放大时出现冲突
                if (ev.getPointerCount() >= 2) {
                    return false;
                }
                int distanceX = Math.abs(rawX - mLastInterceptDownX);
                int distanceY = Math.abs(rawY - mLastInterceptDownY);
                //如果认定是滑动或者长按就拦截，让windowGroup处理该触摸事件
                intercept = mMinTouchSlop <= distanceX || mMinTouchSlop <= distanceY ||
                        isLongPressed(distanceX, distanceY, ev.getDownTime(), ev.getEventTime());
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handleTouchEvent(ev);
                intercept = false;
                break;
        }
        return intercept;
    }

    /**
     * 复位按下时的坐标数据，避免滑动操作时，其他窗口
     * 还未退出功能控制状态，就直接划到下一屏了
     */
    private void resetActionDownPoint() {
        //横向滑动相关
        mLastLeftRightX = 0;
        mSwitchDownX = 0;
        //竖向滑动相关
        mLastDownRawX = 0;
        mLastDownRawY = 0;
    }

    private void handleTouchEvent(MotionEvent event) {
        if ((null != getCurrentWindowItem()) && (mClickMode != LANDSCAPE) && (DOUBLE_CLICK != mClickMode)) {
            if (!mWindowGroupAdapter.isForbidWindowMoved()) {
                upDownMoveWindow(event);
            }
        }

        if (LONG != mClickMode && (DOUBLE_CLICK != mClickMode) && null != getCurrentWindowItem()) {
            if (mWindowGroupAdapter.getIsAllowScroller()) {
                switchScreen(event);
            }
        } else if (LONG == mClickMode) {
            if (mWindowGroupAdapter.getIsAllowScroller()) {
                switchEdgeScreen(event);
            }
        }

        if (MotionEvent.ACTION_UP == event.getAction() || MotionEvent.ACTION_CANCEL == event.getAction()) {
            mClickMode = NORMAL;
        }
    }

    private boolean isPointerInSpace(int rawX, int rawY) {
        WindowItemView windowItem = getCurrentWindowItem();
        if (null == windowItem) {
            return false;
        }
        int[] location = new int[2];
        windowItem.getLocationInWindow(location);
        int width = windowItem.getWidth();
        int height = windowItem.getHeight();
        return ((rawX > location[0]) && (rawX < (location[0] + width)) && (rawY > location[1]) && (rawY < (location[1] + height)));
    }

    /**
     * 左右切换预览分屏+窗口移动
     */
    private void switchEdgeScreen(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_MOVE && mLastDownRawX != 0) {
            int destinationScreen = mWindowGroupAdapter.getCurrentPage();
            int scaledEdgeSlop = mWindowGroupAdapter.getScaledEdgeSlop();
            int deleteEdgeSlop = mWindowGroupAdapter.getDeleteEdgeSlop();
            final float rawX = event.getRawX();
            float deltaX = rawX - mLastDownRawX;
            if ((SystemClock.currentThreadTimeMillis() - mLastSwitchEdgeScreenTimeMillis > SWITCH_EDGE_SCREEN_TIME_MILLIS)) {
                if (rawX < getWidth() && rawX > (getWidth() - scaledEdgeSlop)) {
                    if (destinationScreen < (getScreenCount() - 1)) {
                        destinationScreen++;
                        snapToScreen(destinationScreen, true);
                        move(getCurrentWindowItem(), (int) deltaX + getWidth(), 0);
                    }
                } else if (rawX < scaledEdgeSlop && rawX > 0) {
                    if (destinationScreen > 0) {
                        destinationScreen = destinationScreen - 1;
                        snapToScreen(destinationScreen, true);
                        move(getCurrentWindowItem(), (int) deltaX - getWidth(), 0);
                    }
                }
                mLastSwitchEdgeScreenTimeMillis = SystemClock.currentThreadTimeMillis();
            } else if (mLastSwitchEdgeScreenTimeMillis < 0) {
                mLastSwitchEdgeScreenTimeMillis = SystemClock.currentThreadTimeMillis();
            }

            int[] center = getCenterPoint(getCurrentWindowItem());

            /* 临界回调 **/
            if (center[0] > (getWidth() - scaledEdgeSlop)) {
                final WindowGroupAdapter.OnWindowItemScreenEdgeListener edgeListener = mWindowGroupAdapter.getOnWindowItemScreenEdgeListener();
                if (edgeListener != null) {
                    edgeListener.onRight(getCurrentWindowItem());
                }
            } else if (center[0] < scaledEdgeSlop) {
                final WindowGroupAdapter.OnWindowItemScreenEdgeListener edgeListener = mWindowGroupAdapter.getOnWindowItemScreenEdgeListener();
                if (edgeListener != null) {
                    edgeListener.onLeft(getCurrentWindowItem());
                }

            } else {
                final WindowGroupAdapter.OnWindowItemScreenEdgeListener edgeListener = mWindowGroupAdapter.getOnWindowItemScreenEdgeListener();
                if (edgeListener != null) {
                    edgeListener.onLeft(null);
                    edgeListener.onRight(null);
                }
            }

            final WindowGroupAdapter.OnWindowItemScreenEdgeListener edgeListener = mWindowGroupAdapter.getOnWindowItemScreenEdgeListener();
            if (center[1] < deleteEdgeSlop) {
                if (edgeListener != null) {
                    edgeListener.onTop(getCurrentWindowItem());
                }
            } else {
                if (edgeListener != null) {
                    edgeListener.onTop(null);
                }
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            mLastSwitchEdgeScreenTimeMillis = -1;
            final WindowGroupAdapter.OnWindowItemScreenEdgeListener edgeListener = mWindowGroupAdapter.getOnWindowItemScreenEdgeListener();
            if (edgeListener != null && LONG == mClickMode) {
                edgeListener.onFinish(getCurrentWindowItem());
            }
        }
    }

    /**
     * 滑动到第几屏
     *
     * @param screenIndex 索引
     */
    private boolean snapToScreen(int screenIndex, boolean isAnimal) {
        screenIndex = Math.max(0, Math.min(screenIndex, getScreenCount() - 1));
        if (getScrollX() != (screenIndex * getWidth())) {
            final int delta = screenIndex * getWidth() - getScrollX();

            mScroller.startScroll(getScrollX(), 0, delta, 0, isAnimal ? SCROLL_SCREEN_TIME : 0);

            mWindowGroupAdapter.setCurrentPage(screenIndex);
            mWindowGroupAdapter.setPageChangeEvent(mWindowGroupAdapter.getCurrentPage(), mWindowGroupAdapter.getWindowMode());
            return true;
        }
        return false;
    }

    /**
     * 滑动到第几屏
     *
     * @param screenIndex 分屏页码
     */
    public boolean snapToScreen(int screenIndex) {
        if (snapToScreen(screenIndex, false)) {
            selectedCurrFirstWindow();
            return true;
        }
        return false;
    }

    /**
     * 选中当前页面第一个窗口
     */
    public void selectedCurrFirstWindow() {
        int windowMode = mWindowGroupAdapter.getWindowMode();
        final int firstIndex = mWindowGroupAdapter.getCurrentPage() * windowMode * windowMode;//当前页面第一个
        //final int endIndex = (screenIndex + 1) * (windowMode * windowMode) - 1;//当前页面最后一个索引
        for (WindowItemView windowItemView : getWindowStructList()) {
            if (windowItemView.getWindowSerial() == firstIndex) {
                setCurrentWindowItem(windowItemView);//设置当前页第一个窗口为选择
                break;
            }
        }
    }

    public WindowGroupAdapter getWindowGroupAdapter() {
        return mWindowGroupAdapter;
    }


    // *******************  包内 *****************************

    /**
     * 加载需要的最大子窗口数
     */
    void initAdapter() {
        this.removeAllViews();
        getWindowStructAllList().clear();
        getWindowStructList().clear();
        int windowMode = mWindowGroupAdapter.getWindowMode();
        for (int i = 0; i < mMaxCount + mMoreCount; i++) {
            WindowItemView windowItem = new PlayWindowView(getContext());
            windowItem.isPreviewWindow = mWindowType;
            windowItem.setWindowGroup(this);
            windowItem.setPadding(WindowItemView.SPACE, WindowItemView.SPACE, WindowItemView.SPACE, WindowItemView.SPACE);
            windowItem.setWindowSerial(i);
            calcWindowItem(windowMode, windowItem, i);
            addView(windowItem);
            getWindowStructAllList().add(windowItem);
            getWindowStructList().add(windowItem);
        }
        if (mObserver == null) {
            mObserver = new PagerObserver();
        }
        mWindowGroupAdapter.setViewPagerObserver(mObserver);
    }


    /**
     * 设置WindowGroup中的窗口模式
     *
     * @param windowMode 窗口模式
     */
    void setWindowMode(@WindowMode int windowMode) {
        for (WindowItemView windowStruct : getWindowStructList()) {
            calcWindowItem(windowMode, windowStruct, windowStruct.getWindowSerial());
        }

        requestLayout();
    }


    /**
     * 处理双击子窗口的事件
     *
     * @param windowItem 被双击的子窗口
     */
    void doubleClick(WindowItemView windowItem) {
        if (!mWindowGroupAdapter.getAllDoubleClickEnable()) {
            return;
        }
        if (!windowItem.isDoubleClickEnable()) {
            return;
        }

        int page;
        int windowMode = mWindowGroupAdapter.getWindowMode();
        int lastWindowMode = mWindowGroupAdapter.getLastWindowMode();
        int toMode;
        if (WINDOW_MODE_ONE != windowMode) {
            int one = WINDOW_MODE_ONE;
            page = mWindowGroupAdapter.getCurrentPage() * (windowMode * windowMode) + getCurrentWindowItem().getRowIndex() * windowMode + getCurrentWindowItem().getColumnIndex();
            mWindowGroupAdapter.setWindowMode(one);
            toMode = one;
        } else {
            //当默认窗口模式为一分屏时，lastWindowMode是0，此处进行处理，设置上次的窗口模式为四分屏，避免崩溃
            if (lastWindowMode == 0) {
                lastWindowMode = WINDOW_MODE_FOUR;
            }
            page = mWindowGroupAdapter.getCurrentPage() / (lastWindowMode * lastWindowMode);
            mWindowGroupAdapter.setWindowMode(lastWindowMode);
            toMode = lastWindowMode;
        }

        mWindowGroupAdapter.setCurrentPage(page);

        final WindowGroupAdapter.OnViewGroupTouchEventListener onViewGroupTouchEventListener = mWindowGroupAdapter.getOnViewGroupTouchEventListener();
        if (null != onViewGroupTouchEventListener) {
            onViewGroupTouchEventListener.onDoubleClick(page, getCurrentWindowItem(), windowMode, toMode);
        }

        snapToScreen(mWindowGroupAdapter.getCurrentPage(), false);

        mClickMode = DOUBLE_CLICK;
    }

    /* package */
    @TouchMode
    int getClickMode() {
        return mClickMode;
    }


    // *********************************  私有 **********************

    private int getScreenCount() {
        return mWindowGroupAdapter.getScreenCount();
    }


    /**
     * 计算孩子坐标位置
     *
     * @param windowMode 模式
     * @param windowItem 子View
     */
    private void calcWindowItem(@WindowMode int windowMode, WindowItemView windowItem, int serial) {
        windowItem.setRowIndex(serial % (windowMode * windowMode) / windowMode);
        windowItem.setColumnIndex(serial % windowMode);
        windowItem.setScreenIndex(serial / (windowMode * windowMode));
    }

    private WindowItemView getCurrentWindowItem() {
        return mWindowGroupAdapter.getCurrentWindowItem();
    }

    private WindowItemView getReplaceWindowItem() {
        return mWindowGroupAdapter.getReplaceWindowItem();
    }


    /**
     * 得到替换的窗口
     *
     * @param curWindowStruct 当前的窗口
     * @param center          中心坐标
     * @return 返回替换的窗口
     */
    private WindowItemView getReplaceWindowItem(WindowItemView curWindowStruct, int[] center) {
        if (!mWindowGroupAdapter.isAllowWindowSwap()) {
            return null;
        }
        if (curWindowStruct != null) {
            WindowItemView finalWindowItem = null;
            for (WindowItemView container : getWindowStructList()) {
                container.setViewReplaced(false);
                if (container != curWindowStruct) {
                    if (container.getScreenIndex() != curWindowStruct.getScreenIndex()) {
                        //TODO Fixed: 如果不是当前页面，则不进行交换。如果以后要做跨页交换时，有可能会导致冲突
                        continue;
                    }
                    if (center[0] > container.getLeft() && center[0] < container.getRight() && center[1] > container.getTop() && center[1] < container.getBottom()) {
                        container.setViewReplaced(true);
                        finalWindowItem = container;
                    }
                }
            }
            return finalWindowItem;
        }
        return null;
    }

    /**
     * 上下滑动窗口，放开后的操作， 交换窗口位置信息
     *
     * @param currentWindowItem 当前窗口
     * @param replaceWindowItem 被替换窗口
     */
    private void recoverChildView(WindowItemView currentWindowItem, WindowItemView replaceWindowItem) {
        if (currentWindowItem != null && replaceWindowItem != null) {
            //交换窗口在getWindowStructAllList()中的位置
            int currentWindowIndex = getWindowStructAllList().indexOf(currentWindowItem);
            int replaceWindowIndex = getWindowStructAllList().indexOf(replaceWindowItem);
            getWindowStructAllList().set(currentWindowIndex, replaceWindowItem);
            getWindowStructAllList().set(replaceWindowIndex, currentWindowItem);
            //交换视图的行数/列数/页数/坐标
            int curScreenIndex = currentWindowItem.getScreenIndex();
            int curColumnIndex = currentWindowItem.getColumnIndex();
            int curRowIndex = currentWindowItem.getRowIndex();
            int windowSerial = currentWindowItem.getWindowSerial();
            currentWindowItem.setScreenIndex(replaceWindowItem.getScreenIndex());
            currentWindowItem.setColumnIndex(replaceWindowItem.getColumnIndex());
            currentWindowItem.setRowIndex(replaceWindowItem.getRowIndex());
            currentWindowItem.setWindowSerial(replaceWindowItem.getWindowSerial());

            replaceWindowItem.setScreenIndex(curScreenIndex);
            replaceWindowItem.setColumnIndex(curColumnIndex);
            replaceWindowItem.setRowIndex(curRowIndex);
            replaceWindowItem.setWindowSerial(windowSerial);
            replaceWindowItem.setViewReplaced(false);
        }
        //解决交换位置后，UI没有交换，只有点击一下获取焦点后才交换的问题
        requestLayout();
    }

    /**
     * 是否长按
     */
    private boolean isLongPressed(float deltaX, float deltaY, long lastDownTime, long thisEventTime) {
        long intervalTime = thisEventTime - lastDownTime;
        return !isCanMove(deltaX, deltaY) && intervalTime >= LONG_PRESSED_TIME;
    }

    /**
     * 上下移动
     *
     * @param event 手势事件
     */
    private void upDownMoveWindow(MotionEvent event) {
        final float rawX = event.getRawX();
        final float rawY = event.getRawY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastDownRawX = rawX;
                mLastDownRawY = rawY;

                mIsPortraitCanMove = false;
                break;
            case MotionEvent.ACTION_MOVE:
                //如果记录的按下点位坐标是0的话，就说明当前操作的按下事件被阻拦了，
                // 不是一个完整的操作，不能往下走
                if (mLastDownRawX == 0 || mLastDownRawY == 0) {
                    break;
                }
                float deltaX = rawX - mLastDownRawX;
                float deltaY = rawY - mLastDownRawY;

                if (!mIsPortraitCanMove) {
                    mIsPortraitCanMove = isCanMove(deltaX, deltaY);
                }

                if (mIsPortraitCanMove) {
                    if (Math.abs(deltaX) < Math.abs(deltaY)) {
                        if (NORMAL == mClickMode && event.getPointerCount() <= 1) {
                            final WindowGroupAdapter.OnViewGroupTouchEventListener onViewGroupTouchEventListener = mWindowGroupAdapter.getOnViewGroupTouchEventListener();
                            if (onViewGroupTouchEventListener != null) {
                                onViewGroupTouchEventListener.onLongPress(getCurrentWindowItem());
                            }
                            mClickMode = LONG;
                            if (getCurrentWindowItem() != null) {
                                mWindowGroupAdapter.animatorScale(getCurrentWindowItem(), true);
                            }
                        }
                    }
                }

                if (LONG != mClickMode) {
                    boolean isLongPressed = isLongPressed(deltaX, deltaY, event.getDownTime(), event.getEventTime());
                    if (isLongPressed && event.getPointerCount() <= 1) {
                        final WindowGroupAdapter.OnViewGroupTouchEventListener onViewGroupTouchEventListener = mWindowGroupAdapter.getOnViewGroupTouchEventListener();
                        if (onViewGroupTouchEventListener != null) {
                            onViewGroupTouchEventListener.onLongPress(getCurrentWindowItem());
                        }
                        mClickMode = LONG;
                        if (getCurrentWindowItem() != null) {
                            mWindowGroupAdapter.animatorScale(getCurrentWindowItem(), true);
                        }
                    }
                }
                if (LONG == mClickMode) {
                    int[] center = move(getCurrentWindowItem(), (int) deltaX, (int) deltaY);

                    final WindowItemView replaceWindowItem = getReplaceWindowItem(getCurrentWindowItem(), center);
                    mWindowGroupAdapter.setReplaceWindowItem(replaceWindowItem);

                    final WindowGroupAdapter.OnViewGroupTouchEventListener onViewGroupTouchEventListener = mWindowGroupAdapter.getOnViewGroupTouchEventListener();
                    if (onViewGroupTouchEventListener != null) {
                        onViewGroupTouchEventListener.onReplaceWindowItem(mWindowGroupAdapter.getCurrentPage(), getCurrentWindowItem(), replaceWindowItem);
                    }
                    mLastDownRawX = rawX;
                    mLastDownRawY = rawY;
                }

                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (LONG == mClickMode) {
                    recoverChildView(getCurrentWindowItem(), getReplaceWindowItem());
                    final WindowGroupAdapter.OnViewGroupTouchEventListener onViewGroupTouchEventListener = mWindowGroupAdapter.getOnViewGroupTouchEventListener();
                    if (onViewGroupTouchEventListener != null) {
                        onViewGroupTouchEventListener.onLongPressEnd(mWindowGroupAdapter.getCurrentPage(), getCurrentWindowItem(), getReplaceWindowItem());
                    }
                    //mClickMode = TouchMode.NORMAL;
                    if (getCurrentWindowItem() != null) {
                        mWindowGroupAdapter.animatorScale(getCurrentWindowItem(), false);
                    }
                }
                break;
        }
    }


    /**
     * 判断是否可以左右滑动
     *
     * @param deltaX x轴偏移
     * @return 是否能移动
     */
    private boolean isCanMove(int deltaX) {
        int minTouchSLop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                TOUCH_SLOP >> 1, getResources().getDisplayMetrics());
        if (Math.abs(deltaX) < minTouchSLop) {
            return false;
        }

        if ((getScrollX() <= 0) && (deltaX < 0)) {
            return false;
        }

        return getScrollX() < ((getScreenCount() - 1) * getWidth()) || (deltaX <= 0);
    }


    /**
     * 移动窗口
     *
     * @param view   窗口
     * @param deltaX x轴偏移量
     * @param deltaY y轴偏移量
     * @return 移动后的坐标数组
     */
    private int[] move(WindowItemView view, int deltaX, int deltaY) {
        if (view == null) {
            throw new NullPointerException("windowStruct is null...  空指针了...");
        }
        int left = view.getLeft() + deltaX;
        int right = left + view.getWidth();
        int top = view.getTop() + deltaY;
        int bottom = top + view.getHeight();
        view.layout(left, top, right, bottom);
        view.invalidate();

        int curCenterX = (right + left) / 2;
        int curCenterY = (top + bottom) / 2;
        return new int[]{curCenterX, curCenterY};
    }

    /**
     * 获取中心点
     *
     * @param view 窗口
     * @return 当前窗口所在的中心坐标
     */
    private int[] getCenterPoint(WindowItemView view) {
        if (view == null) {
            throw new NullPointerException("windowStruct is null...  空指针了...");
        }
        int left = view.getLeft() - view.getScreenIndex() * getWidth();
        int right = left + view.getWidth();
        int top = view.getTop();
        int bottom = top + view.getHeight();
        int curCenterX = (right + left) / 2;
        int curCenterY = (top + bottom) / 2;
        return new int[]{curCenterX, curCenterY};
    }

    /**
     * 判断是否可以上下左右移动
     *
     * @param deltaX 水平移动的距离
     * @param deltaY 垂直移动的距离
     * @return true-可以移动 false-不可以移动
     */
    private boolean isCanMove(float deltaX, float deltaY) {
        float v = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TOUCH_SLOP,
                getResources().getDisplayMetrics());
        return (Math.abs(deltaX) > v) || (Math.abs(deltaY) > v);
    }

    /**
     * 左右切换预览分屏
     *
     * @param event 手势事件
     */
    private void switchScreen(MotionEvent event) {
        final float x = event.getX();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                mLastLeftRightX = x;
                mSwitchDownX = x;
                mIsLandscapeCanMove = false;
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                //如果记录的按下点位坐标是0的话，就说明当前操作的按下事件被阻拦了，
                // 不是一个完整的操作，不能往下走
                if (mLastLeftRightX == 0) {
                    break;
                }
                int deltaX = (int) (mLastLeftRightX - x);
                if (!mIsLandscapeCanMove) {
                    mIsLandscapeCanMove = isCanMove(deltaX);
                }

                if (mIsLandscapeCanMove) {
                    if (NORMAL == mClickMode) {
                        mClickMode = LANDSCAPE;
                    }
                    mLastLeftRightX = x;
                    //如果超出了边界就不允许继续滑动
                    if (getScrollX() <= 0 && deltaX < 0 || getScrollX() >= (getScreenCount() - 1) * getWidth()
                            && deltaX > 0) {
                        mScroller.abortAnimation();
                        break;
                    } else {
                        scrollBy(deltaX, 0);
                    }

                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                snapToDestination();
            }
        }
    }

    /**
     * 滑动播放区域的事件处理
     */
    private void snapToDestination() {
        int width = getWidth();

        if (width <= 0) {
            return;
        }

        //如果记录的按下坐标值是0，认为不是一次完整的滑动操作，需要阻止
        if (mSwitchDownX == 0) {
            return;
        }

        int currentPageScreen = mWindowGroupAdapter.getCurrentPage();
        int destinationScreen = mWindowGroupAdapter.getCurrentPage();

        if (Math.abs((mLastLeftRightX - mSwitchDownX)) > SWITCH_PAGE_SLOP) {
            if (mLastLeftRightX > mSwitchDownX) {
                if (destinationScreen > 0) {
                    destinationScreen = destinationScreen - 1;
                }
            } else {
                if (destinationScreen < (getScreenCount() - 1)) {
                    destinationScreen++;
                }
            }
        }
        if (snapToScreen(destinationScreen, true)) {
            selectedCurrFirstWindow();
            postInvalidate();

            final WindowGroupAdapter.OnViewGroupTouchEventListener onViewGroupTouchEventListener = mWindowGroupAdapter.getOnViewGroupTouchEventListener();
            if (null != onViewGroupTouchEventListener) {
                onViewGroupTouchEventListener.onSnapToScreenEnd(currentPageScreen, destinationScreen, getCurrentWindowItem(), mWindowGroupAdapter.getWindowMode(), getScreenCount());
            }
        }
    }

    /*****************以下是编写窗体数量变动后的逻辑处理************************/
    class PagerObserver extends DataSetObserver {
        PagerObserver() {
        }

        @Override
        public void onChanged() {
            dataSetChanged();
        }

        @Override
        public void onInvalidated() {
            dataSetChanged();
        }
    }

    /**
     * 播放窗体数量发生变化
     */
    private void dataSetChanged() {
        int currentMoreCount = mWindowGroupAdapter.getMaxCount() - mLastMaxCount;
        Log.i("WindowGroup", "CurrentMax:" + mWindowGroupAdapter.getMaxCount() + " lastMax :" + mLastMaxCount);
        //如果等于0说明播放窗口没有变化就不需要刷新
        if (currentMoreCount == 0) {
            return;
        }
        if (currentMoreCount > 0) {
            refreshAndAddMoreWindow(currentMoreCount);
        } else {
            refreshAndDelWindow();
        }
        mLastMaxCount = mWindowGroupAdapter.getMaxCount();
    }

    /**
     * 减少并刷新当前播放窗体的数量
     */
    private void refreshAndDelWindow() {
        for (int i = mLastMaxCount; i > mWindowGroupAdapter.getMaxCount(); i--) {
            WindowItemView windowItem = getWindowStructAllList().get(i - 1);
            removeView(windowItem);
            getWindowStructAllList().remove(windowItem);
            getWindowStructList().remove(windowItem);
        }
        mWindowGroupAdapter.refreshWindow();
    }


    /**
     * 增加并刷新播放窗体的数量
     *
     * @param currentMoreCount 当前需要增加的数量
     */
    private void refreshAndAddMoreWindow(int currentMoreCount) {
        for (int i = 0; i < currentMoreCount; i++) {
            WindowItemView windowItem = new PlayWindowView(getContext());
            windowItem.isPreviewWindow = mWindowType;
            windowItem.setWindowGroup(this);
            windowItem.setPadding(WindowItemView.SPACE, WindowItemView.SPACE, WindowItemView.SPACE, WindowItemView.SPACE);
            windowItem.setWindowSerial(mLastMaxCount + i);
            addView(windowItem);
            getWindowStructAllList().add(windowItem);
            getWindowStructList().add(windowItem);
        }
        mWindowGroupAdapter.refreshWindow();
    }

    /**
     * 根据记录了每一分屏有多少个窗口在使用的数组来删除未使用的分屏页
     *
     * @param screenIndexArray 记录了每一分屏有多少个窗口在使用的数组
     */
    public void deleteNotUseWindowGroupByScreen(int[] screenIndexArray) {
        boolean isNeedRefreshWindow = false;
        List<WindowItemView> deleteWindowItemViewList = new ArrayList<>();
        for (int i = 0; i < screenIndexArray.length; i++) {
            //当前分屏页码
            //当前分屏中正在使用的窗体数量
            int usedWindowNum = screenIndexArray[i];
            if (usedWindowNum > 0) {
                continue;
            }
            //如果窗体数量最后只剩下4个就不需要删除了，需要保留
            if (getWindowStructAllList().size() <= 4) {
                break;
            }
            //如果为0的话，说明没在使用，需要删除第i分屏
            int perScreenWindNum = mWindowMode * mWindowMode;
            for (int j = 0; j < perScreenWindNum; j++) {
                WindowItemView windowItem = getWindowStructAllList().get(i * perScreenWindNum + j);
                removeView(windowItem);
                deleteWindowItemViewList.add(windowItem);
            }
            isNeedRefreshWindow = true;
        }
        //如果没有删除窗体，就不需要更新窗体序列状态
        if (!isNeedRefreshWindow) {
            return;
        }
        getWindowStructAllList().removeAll(deleteWindowItemViewList);
        getWindowStructList().removeAll(deleteWindowItemViewList);
        deleteWindowItemViewList.clear();
        //重新给剩下的播放窗口设置序列号
        for (int i = 0; i < getWindowStructAllList().size(); i++) {
            WindowItemView windowItem = getWindowStructAllList().get(i);
            windowItem.setWindowSerial(i);
        }
        //重置实际需要的窗体数量
        mWindowGroupAdapter.setCurrentNeedWindowCount(getWindowStructAllList().size());
        //设置当前窗口为剩下窗口的第一个
        mWindowGroupAdapter.setCurrentWindowItem(getWindowStructAllList().get(0));
        //刷新窗体
        mWindowGroupAdapter.refreshWindow();
    }
}
