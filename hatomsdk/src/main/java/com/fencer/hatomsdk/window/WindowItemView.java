package com.fencer.hatomsdk.window;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;

import androidx.annotation.ColorInt;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.blankj.utilcode.util.ScreenUtils;
import com.blankj.utilcode.util.Utils;
import com.fencer.hatomsdk.R;


/**
 * <p>填充到WindowGroup中的子View</p>
 *
 * @author 张华洋 2017/6/8 21:12
 * @version V1.1
 * @name WindowItemView
 */
public class WindowItemView extends ConstraintLayout {

    protected boolean isPreviewWindow = true;
    /**
     * 窗口要被替换时显示的颜色
     */
    private static final int WINDOW_REPLACE_COLOR = ContextCompat.getColor(Utils.getApp(),
            R.color.window_will_replace_color);
    /**
     * 窗口普通状态时的外框颜色
     */
    private static final int WINDOW_NORMAL_PORTRAIT_COLOR = ContextCompat.getColor(Utils.getApp(),
            R.color.portrait_windowgroup_bg);
    private static final int WINDOW_NORMAL_LAND_COLOR = ContextCompat.getColor(Utils.getApp(),
            R.color.land_windowgroup_bg);

    /**
     * 窗口被选中时的颜色
     */
    private int mSelectColor = ContextCompat.getColor(Utils.getApp(), R.color.playback_timebar_color);

    /**
     * 窗口间隔距离，像素
     */
    /* package */ static final float SPACE = 2.0f;
    /**
     * 双击事件间隔
     */
    private static final int DOUBLE_CLICK_TIME = 200;
    /**
     * 窗口序列号，一、多窗口模式：-1 - 隐藏的窗口， >1 - 排布的位置; 二、单窗口模式
     */
    private int mWindowSerial = -1;
    /**
     * 窗口所处的页
     */
    private int mScreenIndex = 0;
    /**
     * 窗口所处的行
     */
    private int mRowIndex = 0;
    /**
     * 窗口所处的列
     */
    private int mColumnIndex = 0;
    /**
     * 判断是否在等待双击 false - 表示单击，true - 表示双击
     */
    private boolean mWaitDouble = true;
    /**
     * 被选定的窗口
     */
    private boolean mIsViewSelected = false;
    /**
     * 被替换的窗口
     */
    private boolean mIsViewReplaced = false;
    /**
     * 用户是否可见这个窗口,默认用户是可见的
     */
    private boolean mUserVisibleHint = true;
    /**
     * 窗口选择监听
     */
    private OnWindowSelectedListener mSelectedListener;
    /**
     * 单击监听事件
     */
    private OnSingleClickListener mOnSingleClickListener;
    /**
     * 电子放大开启关闭监听
     */
    private OnOpenDigitalZoomListener mOnOpenDigitalZoomListener;
    /**
     * 父容器
     */
    private WindowGroup mWindowGroup;
    /**
     * 绘制
     */
    private Paint mPaint;
    private RectF mRect;

    private boolean mOnDoubleClickEnable = true;
    /**
     * View大小
     */
    private final RectF mRectF = new RectF();
    /**
     * 拖动窗口放大缩小动画使能
     */
    private boolean mAnimatorScaleEnable = true;
    /**
     * 是否是双击事件
     */
    private boolean mIsDoubleClick;
    /**
     * 上一次两指之间的距离
     */
    private float mLastDis;
    /**
     * 电子放大回调接口是否执行
     */
    private boolean mOpenDigitalZoomListenerCall;
    /**
     * 上一次选中的窗口
     */
    private WindowItemView mLastSelectWindowItem;
    /**
     * 当前选中的窗口
     */
    private WindowItemView mCurrentSelectWindowItemView;

    public WindowItemView(Context context) {
        super(context);
        init();
    }

    public WindowItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        this.mPaint = new Paint();
        this.mRect = new RectF();
    }


    public void setWindowGroup(WindowGroup windowGroup) {
        mWindowGroup = windowGroup;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int height = getHeight();
        int width = getWidth();

        if (height > 0 && width > 0) {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(SPACE);
            if (mIsViewReplaced) {
                mPaint.setColor(WINDOW_REPLACE_COLOR);
            } else if (mIsViewSelected) {
                mPaint.setColor(mSelectColor);
            } else {
                if (ScreenUtils.isPortrait()) {
                    mPaint.setColor(WINDOW_NORMAL_PORTRAIT_COLOR);
                } else {
                    mPaint.setColor(WINDOW_NORMAL_LAND_COLOR);
                }
            }

            mPaint.setAntiAlias(true);
            float s = SPACE / 2;
            mRect.left = s;
            mRect.right = width - s;
            mRect.top = s;
            mRect.bottom = height - s;
            canvas.drawRect(mRect, mPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (null == mWindowGroup) {
            return false;
        }
        if (!mWindowGroup.getWindowGroupAdapter().isTouchEnable()) {
            return false;
        }
        handlerMultiTouchZoom(event);
        final int action = event.getAction();
        if (MotionEvent.ACTION_DOWN == action) {
            //拿到上一次选中的窗口对象
            mLastSelectWindowItem = mWindowGroup.getWindowGroupAdapter().getCurrentWindowItem();
            handlerDownTouch();
            //拿到当前选中的窗口对象
            mCurrentSelectWindowItemView = mWindowGroup.getWindowGroupAdapter().getCurrentWindowItem();
        } else if (MotionEvent.ACTION_UP == action) {
            if (event.getPointerCount() >= 2) {
                return false;
            }
            //如果是开启电子放大，就不执行后面的点击事件
            if (mCurrentSelectWindowItemView.isZoom()) {
                return false;
            }
            if (mOnSingleClickListener != null) {
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mWaitDouble && !mIsDoubleClick) {
                            int lastWindowSerial = 0;
                            if (mLastSelectWindowItem != null) {
                                lastWindowSerial = mLastSelectWindowItem.getWindowSerial();
                            }
                            mOnSingleClickListener.onSingleClick(lastWindowSerial, mCurrentSelectWindowItemView.getWindowSerial());
                        }
                    }
                }, DOUBLE_CLICK_TIME + 50);//延时回调单击事件：等待明确了不是双击事件时再回调
            }
        }
        return true;
    }

    /**
     * 处理多点操作开启电子放大的操作
     *
     * @param event 触摸事件
     */
    private void handlerMultiTouchZoom(MotionEvent event) {
        final int action = event.getAction();
        //检测是否是开启关闭电子放大的逻辑
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mOpenDigitalZoomListenerCall = false;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mLastDis = spacing(event);
                break;
            case MotionEvent.ACTION_MOVE:
                //如果电子放大已经开启就不需要再次回调
                WindowItemView windowItemView = mWindowGroup.getWindowGroupAdapter().getCurrentWindowItem();
                if (windowItemView.isZoom()) {
                    return;
                }
                if (event.getPointerCount() == 2 && !mOpenDigitalZoomListenerCall) {
                    float dis = spacing(event);
                    if (dis > mLastDis) {
                        //如果当前缩放等级是1，且是放大手势，就开启电子放大，并将触摸事件交给子view
                        if (mOnOpenDigitalZoomListener != null) {
                            mOnOpenDigitalZoomListener.onOpenDigitalZoom();
                            mOpenDigitalZoomListenerCall = true;
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * 处理触摸事件ACTION_DOWN的操作
     */
    private void handlerDownTouch() {
        playSoundEffect(SoundEffectConstants.CLICK);
        mIsDoubleClick = false;
        if (!mIsViewSelected) {
            setCurrentWindowItemSelected();
        } else {
            if (mWaitDouble) {
                mWaitDouble = false;
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!mWaitDouble) {
                            mWaitDouble = true;
                        }
                    }
                }, DOUBLE_CLICK_TIME);
            } else {
                mWaitDouble = true;
                mIsDoubleClick = true;
                {
                    // 如果前后点击的控件不是同一个，则作不处理
                    mWindowGroup.doubleClick(this);
                }
            }
        }
    }

    public boolean isZoom() {
        return false;
    }

    public boolean isOpenFishEye() {
        return false;
    }

    /**
     * 当有两个手指按在屏幕上时，计算两指之间的距离
     *
     * @param event 触摸事件
     * @return 两指之间的距离
     */
    private float spacing(MotionEvent event) {
        if (event.getPointerCount() < 2) {
            return 0;
        }
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);

        return (float) Math.sqrt(x * x + y * y);
    }

    public void setViewSelected(boolean isSelected) {
        if (mIsViewSelected != isSelected) {
            mIsViewSelected = isSelected;
            if (null != mSelectedListener) {
                mSelectedListener.onWindowSelected(this, isSelected);
            }
            this.postInvalidate();
        }
    }

    public void setWindowSelectedColor(@ColorInt int color) {
        mSelectColor = color;
        this.postInvalidate();
    }


    /* package */  void setViewReplaced(boolean isReplaced) {
        if (mIsViewReplaced != isReplaced) {
            mIsViewReplaced = isReplaced;
            this.postInvalidate();
        }
    }

    /* package */  void setPadding(float left, float top, float right, float bottom) {
        super.setPadding((int) left, (int) top, (int) right, (int) bottom);
    }


    /* package*/ void setRectF(float left, float top, float right, float bottom) {
        mRectF.set(left, top, right, bottom);
    }


    /**
     * 当前窗口是否在移动
     */
    public boolean isMove() {
        if (null == mWindowGroup) {
            return false;
        }
        return this.mWindowGroup.getClickMode() != WindowGroup.NORMAL;
    }

    public boolean isViewSelected() {
        return mIsViewSelected;
    }

    public boolean isViewReplaced() {
        return mIsViewReplaced;
    }

    /**
     * @return The current value of the user-visible hint on this windowItem.
     * @see #setUserVisibleHint(boolean)
     */
    public boolean getUserVisibleHint() {
        return this.mUserVisibleHint;
    }

    /**
     * Set a hint to the user about whether this windowItem's UI is currently visible
     * to the user.
     *
     * @param isVisibleToUser true if this windowItem's UI is currently visible to the user (default),
     *                        false if it is not.
     */
    public void setUserVisibleHint(boolean isVisibleToUser) {
        this.mUserVisibleHint = isVisibleToUser;
    }

    /**
     * 拖动窗口放大缩小动画使能
     *
     * @param isEnable you know
     */
    public void setAnimatorScaleEnable(boolean isEnable) {
        mAnimatorScaleEnable = isEnable;

        if (!isEnable) {
            //禁用前恢复窗口
            synchronized (this) {
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setScaleX(1.0f);
                        setScaleY(1.0f);
                        setAlpha(1.0f);
                    }
                }, 500);
            }
        }
    }

    public boolean isAnimatorScaleEnable() {
        return mAnimatorScaleEnable;
    }


    public int getScreenIndex() {
        return mScreenIndex;
    }

    public int getColumnIndex() {
        return mColumnIndex;
    }

    public int getRowIndex() {
        return mRowIndex;
    }

    /* package */  void setRowIndex(int rowIndex) {
        mRowIndex = rowIndex;
    }

    /* package */  void setScreenIndex(int screenIndex) {
        mScreenIndex = screenIndex;
    }

    /* package */  void setColumnIndex(int columnIndex) {
        mColumnIndex = columnIndex;
    }

    /* package */  void setWindowSerial(int windowSerial) {
        mWindowSerial = windowSerial;
    }

    public RectF getRectF() {
        return mRectF;
    }

    public int getWindowSerial() {
        return mWindowSerial;
    }

    public void setCurrentWindowItemSelected() {
        if (mWindowGroup != null) {
            mWindowGroup.setCurrentWindowItem(this);
        }
    }

    public void setOnDoubleClickEnable(boolean onDoubleClickEnable) {
        mOnDoubleClickEnable = onDoubleClickEnable;
    }

    public boolean isDoubleClickEnable() {
        return mOnDoubleClickEnable;
    }

    //************************** 监听  **************************


    public interface OnWindowSelectedListener {
        void onWindowSelected(WindowItemView item, boolean isSelected);
    }

    public void setOnWindowSelectedListener(OnWindowSelectedListener listener) {
        mSelectedListener = listener;
    }

    public interface OnSingleClickListener {
        void onSingleClick(int lastWindowSerial, int currentWindowSerial);
    }

    public void setOnSingleClickListener(OnSingleClickListener onSingleClickListener) {
        this.mOnSingleClickListener = onSingleClickListener;
    }

    public interface OnOpenDigitalZoomListener {
        void onOpenDigitalZoom();
    }

    public void setOnOpenDigitalZoomListener(OnOpenDigitalZoomListener onOpenDigitalZoomListener) {
        this.mOnOpenDigitalZoomListener = onOpenDigitalZoomListener;
    }
}
