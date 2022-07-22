package com.fencer.hatomsdk.window;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.TextureView;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * <p> 带有放大缩小功能，以及鱼眼ptz功能手势回调的TextureView</p>
 * <p> TextureView和SurfaceView不同，它不会在WindowManageService中单独创建窗口，而是作为View hierachy中的一个普通View，
 * 因此TextureView可以和其它普通View一样进行移动，旋转，缩放，动画等变化。</p>
 *
 * @author 张华洋 2017/5/31 21:26
 * @version V1.2.0
 * @name ISMSTextureView
 */
public class PlayTextureView extends TextureView {
    /*定义三种触摸模式*/
    private static final int NONE = 0;
    private static final int ZOOM_DRAG = 1;
    private static final int ZOOM_SCALE = 2;

    private static final int INVALID_POINTER = -1;
    private static final float UNIT_SCALE_RATIO = 0.003f;

    private static final int TOUCH_SLOP = 30;
    /*最大放大倍数*/
    public static final int MAX_SCALE = 10;
    /*双击事件间隔*/
    private static final int DOUBLE_CLICK_TIME = 300;

    private float mLastMotionY = 0;
    private float mLastMotionX = 0;
    private float mRatioX = 1;
    private float mRatioY = 1;

    private float mLastDis = 0;
    private float mLastScale = 1;
    @TouchMode
    private int mClickMode = NONE;
    private int mActionPointerId = INVALID_POINTER;
    private final CustomRect mOriginalRect = new CustomRect();
    private final CustomRect mVirtualRect = new CustomRect();
    private final Rect oRect = new Rect();
    private final Rect curRect = new Rect();
    private OnZoomListener mZoomListener = null;
    private OnZoomScaleListener mOnZoomScaleListener;
    /*判断是否在等待双击 false - 表示单击，true - 表示双击*/
    private boolean mWaitDouble = true;
    /*用于判断click事件*/
    private float mLastClickX;
    /*用于判断click事件*/
    private float mLastClickY;
    /*单拍手势监听*/
    private GestureDetector mGestureDetector;
    /*鱼眼云台ptz手势回调*/
    private OnFECPTZActionListener mFECPTZActionListener;
    /*记录两指同时放在屏幕上时，中心点的横坐标值*/
    private float mCenterPointX;
    /*记录两指同时放在屏幕上时，中心点的纵坐标值*/
    private float mCenterPointY;
    /*记录上次两指之间的距离*/
    private double lastFingerDis;
    /*记录总缩放比例*/
    private float totalRatio = 0.0f;
    /*每次手势缩放的比例*/
    private float scaleRatio = 0.005f;
    /*3D缩放上次距离*/
    private float lastFinger3DDis;
    /*3D缩放比例*/
    private float total3DRatio = 0.0f;
    /*鱼眼ptz模式*/
    private boolean mIsFECPTZMode = false;

    /**
     * 触摸模式注解
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({NONE, ZOOM_DRAG, ZOOM_SCALE})
    private @interface TouchMode {
    }


    public PlayTextureView(Context context) {
        this(context, null);
    }

    public PlayTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlayTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mOriginalRect.setValue(l, t, r, b);
        if (changed) {
            mVirtualRect.setValue(l, t, r, b);
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (isFECPTZMode()) {
            ptzFishEye(ev);
            return true;
        }

        if (mZoomListener == null) {
            return false;
        }

        // 点击事件未被“双击事件”消耗
        if (!click(ev)) {
            zoom(ev);
        }

        return true;
    }


    /**
     * 判断点击事件
     *
     * @param ev 触摸事件
     */
    private boolean click(MotionEvent ev) {
        if (ev.getPointerCount() != 1) {
            return false;
        }

        final int action = ev.getAction();
        boolean isEventConsume = false;

        if (mGestureDetector != null) {
            mGestureDetector.onTouchEvent(ev);
        }
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mLastClickX = ev.getX(0);
                mLastClickY = ev.getY(0);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                float deltaX = (ev.getX(0) - mLastClickX);
                float deltaY = (ev.getY(0) - mLastClickY);
                if (isClick(deltaX, deltaY)) {
                    if (mWaitDouble) {
                        mWaitDouble = false;
                        postDelayed(new ProcessSingleClick(), DOUBLE_CLICK_TIME);
                    } else {
                        mWaitDouble = true;

                        if (mLastScale == MAX_SCALE) {
                            midPointDoubleClick(ev);
                            if (mOnZoomScaleListener != null) {
                                mOnZoomScaleListener.onZoomScale(1);
                            }
                            scale(1);
                        } else {
                            if (mOnZoomScaleListener != null) {
                                mOnZoomScaleListener.onZoomScale(MAX_SCALE);
                            }
                            midPointDoubleClick(ev);
                            scale(MAX_SCALE);
                        }

                        isEventConsume = true;
                    }
                }
                break;
            default:
                break;
        }
        return isEventConsume;
    }


    /**
     * 处理手势放大和缩小事件
     *
     * @param ev 触摸事件
     */
    private void zoom(MotionEvent ev) {
        final int action = ev.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mClickMode = ZOOM_DRAG;

                if (ev.getPointerCount() < 1) {
                    return;
                }

                mActionPointerId = ev.getPointerId(0);

                if (mActionPointerId < 0) {
                    return;
                }

                mLastMotionX = ev.getX();
                mLastMotionY = ev.getY();

                break;
            case MotionEvent.ACTION_MOVE:
                if (ZOOM_DRAG == mClickMode) {
                    final int index = ev.findPointerIndex(mActionPointerId);
                    if (index < 0) {
                        return;
                    }

                    final float x = ev.getX(index);
                    final float y = ev.getY(index);

                    move(mLastMotionX, mLastMotionY, x, y);

                    mLastMotionX = x;
                    mLastMotionY = y;
                } else if (ZOOM_SCALE == mClickMode) {
                    if (ev.getPointerCount() != 2) {
                        return;
                    }

                    float dis = spacing(ev);
                    float scale = mLastScale + (dis - mLastDis) * UNIT_SCALE_RATIO;

                    mLastDis = dis;

                    if (scale > MAX_SCALE) {
                        scale = MAX_SCALE;
                    }

                    //这个回调需要在两个倍率重置之间，避免出现倍率超过和无法通知上层需要关闭电子放大的问题
                    if (mOnZoomScaleListener != null) {
                        mOnZoomScaleListener.onZoomScale(scale);
                    }

                    if (scale < 1) {
                        scale = 1;
                    }

                    scale(scale);

                    midPoint(ev);

                }
                break;
            case MotionEvent.ACTION_UP:
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mLastDis = spacing(ev);
                mClickMode = ZOOM_SCALE;
                midPoint(ev);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                mClickMode = ZOOM_DRAG;
                break;
        }
    }


    /**
     * 处理鱼眼云台事件
     *
     * @param event 触摸事件
     */
    private void ptzFishEye(MotionEvent event) {
        int pointerCount = event.getPointerCount();

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                //记录ACTION_DOWN的位置
                float x = event.getX();
                float y = event.getY();

                //回调PTZ事件
                if (null != mFECPTZActionListener) {
                    mFECPTZActionListener.onFECPTZActionDown(x, y);
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (pointerCount == 2) {
                    // 当有两个手指按在屏幕上时，计算两指之间的距离
                    lastFingerDis = spacing(event);
                    lastFinger3DDis = spacing(event);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (pointerCount == 1) {
                    float x1 = event.getX();
                    float y1 = event.getY();
                    //记录PTZ事件ACTION_MOVE的位置

                    //回调PTZ事件
                    if (null != mFECPTZActionListener) {
                        mFECPTZActionListener.onFECPTZActionMove(false, 0.0f, 0.0f, x1, y1);
                    }
                } else if (pointerCount == 2) {
                    centerPointBetweenFingers(event);
                    double fingerDis = spacing(event);
                    if (fingerDis > lastFingerDis) {
                        //放大
                        totalRatio += scaleRatio;
                        if (totalRatio >= 1.0f) {
                            totalRatio = 1.0f;
                        }
                    } else {
                        //缩小
                        totalRatio -= scaleRatio;
                        if (totalRatio <= 0.0f) {
                            totalRatio = 0.0f;
                        }
                    }

                    if (fingerDis > 0) {
                        //3D缩放时缩放参数，此值为正数时设置后画面缩小
                        total3DRatio = (float) (lastFinger3DDis - fingerDis);
                        lastFinger3DDis = (float) fingerDis;
                    }

                    //回调ZOOM事件
                    if (null != mFECPTZActionListener) {
                        mFECPTZActionListener.onFECPTZActionMove(true, totalRatio, total3DRatio, mCenterPointX, mCenterPointY);
                    }
                    lastFingerDis = fingerDis;
                }
                break;
            case MotionEvent.ACTION_UP:
            default:
                break;
        }
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


    private void scale(float newScale) {
        float w = mOriginalRect.getWidth() * newScale;
        float h = mOriginalRect.getHeight() * newScale;

        float newL = mVirtualRect.getLeft() - mRatioX * (w - mVirtualRect.getWidth());
        float newT = mVirtualRect.getTop() - mRatioY * (h - mVirtualRect.getHeight());
        float newR = newL + w;
        float newB = newT + h;

        mVirtualRect.setValue(newL, newT, newR, newB);

        judge(mOriginalRect, mVirtualRect);

        if (mZoomListener != null) {
            mLastScale = newScale;
            mZoomListener.onZoomChange(changeToRect(mOriginalRect, oRect), changeToRect(mVirtualRect, curRect));
        }
    }

    private void midPoint(MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);

        mRatioX = Math.abs(x / 2 - mVirtualRect.getLeft()) / mVirtualRect.getWidth();
        mRatioY = Math.abs(y / 2 - mVirtualRect.getTop()) / mVirtualRect.getHeight();
    }


    private void midPointDoubleClick(MotionEvent event) {
        float x = event.getX(0);
        float y = event.getY(0);

        mRatioX = Math.abs(x - mVirtualRect.getLeft()) / mVirtualRect.getWidth();
        mRatioY = Math.abs(y - mVirtualRect.getTop()) / mVirtualRect.getHeight();
    }

    private void move(float lastX, float lastY, float curX, float curY) {

        final float deltaX = curX - lastX;
        final float deltaY = curY - lastY;

        float left = mVirtualRect.getLeft();
        float top = mVirtualRect.getTop();
        float right = mVirtualRect.getRight();
        float bottom = mVirtualRect.getBottom();

        float newL = left + deltaX;
        float newT = top + deltaY;
        float newR = right + deltaX;
        float newB = bottom + deltaY;

        mVirtualRect.setValue(newL, newT, newR, newB);

        judge(mOriginalRect, mVirtualRect);

        if (mZoomListener != null) {
            mZoomListener.onZoomChange(changeToRect(mOriginalRect, oRect), changeToRect(mVirtualRect, curRect));
        }

    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = ev.getPointerId(pointerIndex);

        final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
        mLastMotionX = ev.getX(newPointerIndex);
        mLastMotionY = ev.getY(newPointerIndex);
        if (pointerId == mActionPointerId) {
            mActionPointerId = ev.getPointerId(newPointerIndex);
        }
    }

    private void judge(CustomRect oRect, CustomRect curRect) {

        float oL = oRect.getLeft();
        float oT = oRect.getTop();
        float oR = oRect.getRight();
        float oB = oRect.getBottom();

        float newL = curRect.getLeft();
        float newT = curRect.getTop();
        float newR = curRect.getRight();
        float newB = curRect.getBottom();

        float newW = curRect.getWidth();
        float newH = curRect.getHeight();

        if (newL > oL) {
            newL = oL;
        }
        newR = newL + newW;

        if (newT > oT) {
            newT = oT;
        }
        newB = newT + newH;

        if (newR < oR) {
            newR = oR;
            newL = oR - newW;
        }

        if (newB < oB) {
            newB = oB;
            newT = oB - newH;
        }

        //Logger.i("move, " + "scale 1 move: " + " newL: " + newL + " newT: " + newT + " newR: " + newR + " newB: " + newB);
        curRect.setValue(newL, newT, newR, newB);
    }

    private Rect changeToRect(CustomRect customRect, Rect rect) {
        rect.left = (int) customRect.getLeft();
        rect.right = (int) customRect.getRight();
        rect.top = (int) customRect.getTop();
        rect.bottom = (int) customRect.getBottom();
        return rect;
    }

    /**
     * 计算两个手指之间中心点的坐标。
     *
     * @param event 触摸事件
     */
    private void centerPointBetweenFingers(MotionEvent event) {
        float xPoint0 = event.getX(0);
        float yPoint0 = event.getY(0);
        float xPoint1 = event.getX(1);
        float yPoint1 = event.getY(1);
        mCenterPointX = (xPoint0 + xPoint1) / 2;
        mCenterPointY = (yPoint0 + yPoint1) / 2;
    }


    /**
     * 判断是否是点击事件
     *
     * @param deltaX x坐标
     * @param deltaY y坐标
     * @return 是否
     */
    private boolean isClick(float deltaX, float deltaY) {
        return !((Math.abs(deltaX) > TOUCH_SLOP) || (Math.abs(deltaY) > TOUCH_SLOP));
    }


    private class ProcessSingleClick implements Runnable {

        public void run() {
            if (!mWaitDouble) {
                mWaitDouble = true;
            }
        }
    }


    /**
     * 设置鱼眼ptz触控模式
     */
    public void setFECPTZMode(boolean ptzMode) {
        mIsFECPTZMode = ptzMode;
    }


    /**
     * 判断是否是鱼眼ptz触控模式
     */
    public boolean isFECPTZMode() {
        return mIsFECPTZMode;
    }


    /**
     * 设置电子放大监听，设置“null”时表示取消电子放大监听，设置有效的监听时，当前CustomTextureView会截获父控件的touch事件。
     *
     * @param listener 放大监听接口
     */
    public void setOnZoomListener(OnZoomListener listener) {
        mZoomListener = listener;

        if (mZoomListener == null) {
            mVirtualRect.setValue(mOriginalRect.getLeft(), mOriginalRect.getTop(), mOriginalRect.getRight(),
                    mOriginalRect.getBottom());

            mLastMotionY = 0;
            mLastMotionX = 0;
            mLastDis = 0;
            mRatioX = 1;
            mRatioY = 1;
            mLastScale = 1;
        }
    }


    /**
     * 设置电子放大倍率监听
     *
     * @param onZoomScaleListener 电子放大倍率监听接口
     */
    public void setOnZoomScaleListener(OnZoomScaleListener onZoomScaleListener) {
        this.mOnZoomScaleListener = onZoomScaleListener;
    }

    /**
     * 设置手势监听
     *
     * @param detector df
     */
    public void setGestureDetector(GestureDetector detector) {
        mGestureDetector = detector;
    }


    /**
     * 设置鱼眼ptz手势回调
     *
     * @param listener OnFECPTZActionListener
     */
    public void setOnFECPTZActionListener(OnFECPTZActionListener listener) {
        mFECPTZActionListener = listener;
    }

    /**
     * 电子放大回调接口
     */
    public interface OnZoomListener {

        void onZoomChange(Rect oRect, Rect curRect);
    }

    /**
     * 电子放大倍率回调接口
     */
    public interface OnZoomScaleListener {

        void onZoomScale(float scale);
    }

    /**
     * 鱼眼ptz功能手势回调接口
     */
    public interface OnFECPTZActionListener {

        /**
         * 手指按下去的事件
         *
         * @param originalX 手指按下去的X坐标
         * @param originalY 手指按下去的Y坐标
         */
        void onFECPTZActionDown(float originalX, float originalY);

        /**
         * 手指移动的事件
         *
         * @param isZoom 是否是电子放大，默认为false
         * @param zoom   放大系数
         * @param zoom3D 3D缩放比例
         * @param curX   手指移动时X坐标
         * @param curY   手指移动时Y坐标
         */
        void onFECPTZActionMove(boolean isZoom, float zoom, float zoom3D, float curX, float curY);
    }


    private static class CustomRect {
        private float mLeft = 0.0F;
        private float mTop = 0.0F;
        private float mRight = 0.0F;
        private float mBottom = 0.0F;

        public CustomRect() {
        }

        public void setValue(float l, float t, float r, float b) {
            this.mLeft = l;
            this.mTop = t;
            this.mRight = r;
            this.mBottom = b;
        }

        public float getLeft() {
            return this.mLeft;
        }

        public float getTop() {
            return this.mTop;
        }

        public float getRight() {
            return this.mRight;
        }

        public float getBottom() {
            return this.mBottom;
        }

        public float getWidth() {
            return this.mRight - this.mLeft;
        }

        public float getHeight() {
            return this.mBottom - this.mTop;
        }
    }
}
