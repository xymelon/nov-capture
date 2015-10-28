package com.xycoding.labeller.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import com.xycoding.labeller.R;
import com.xycoding.labeller.utils.ImageUtils;
import com.xycoding.labeller.utils.Utils;

import java.lang.ref.WeakReference;

/**
 * Created by xuyang on 15/10/6.
 */
public class DashFrameLayout extends FrameLayout {

    private final static String TAG = DashFrameLayout.class.getSimpleName();
    private final static float MAX_SCALE = 3.f;

    /* 虚线框相关 begin */
    private final static int MSG_DRAW = 1;
    private final static int MSG_ERASE = 2;

    /**
     * 默认刷新间隔
     */
    private final static int REFRESH_INTERVAL = 10;

    /**
     * 默认虚线段长度
     */
    private final static int DASH_ON_WIDTH = 15;

    /**
     * 默认虚线段之间空白长度
     */
    private final static int DASH_OFF_WIDTH = 8;

    private Paint mDashPaint;
    private Path mDashPath;

    /**
     * 虚线段“on”,"off"长度
     */
    private float[] mDashFloats;

    /**
     * PathEffect数组，绘画时复用
     */
    private PathEffect[] mPathEffects;

    /**
     * 当前偏移量
     */
    private int mDashPhase;

    /**
     * 若要动画连续，则PathEffect最少个数为虚线段和空白长度
     */
    private int mDashMinSize = DASH_ON_WIDTH + DASH_OFF_WIDTH;

    private RefreshHandler mRefreshHandler;
    /* 虚线框相关 end */

    /**
     * 动作标志：无
     */
    private static final int NONE = 0;

    /**
     * 动作标志：拖动
     */
    private static final int DRAG = 1;

    /**
     * 动作标志：单指缩放与旋转
     */
    private static final int SINGLE_ZOOM = 2;

    /**
     * 动作标志：双指缩放与旋转
     */
    private static final int DOUBLE_ZOOM = 3;

    /**
     * 初始化动作标志
     */
    private int mode = NONE;

    /**
     * 变换矩阵，包括移动、旋转和缩放等
     */
    private Matrix mViewMatrix = new Matrix();

    /**
     * 变换矩阵，但不包括旋转
     */
    private Matrix mViewNoRotateMatrix = new Matrix();

    /**
     * 上次缩放距离
     */
    private float mPreDistance;

    /**
     * 上次旋转角度
     */
    private float mPreRotation;

    /**
     * 当前缩放比例，初始为1
     */
    private float mCurScaleFactor = 1.f;

    /**
     * 当前旋转角度，初始为0
     */
    private float mCurRotation = 0.f;

    /**
     * 标签
     */
    private Bitmap mLabelBitmap;

    /**
     * 标签中心坐标
     */
    private float[] mCenterPoint = new float[2];

    /**
     * 删除按钮
     */
    private Bitmap mDelBitmap;

    /**
     * 删除按钮初始中心坐标
     */
    private float[] mDelPoint = new float[2];

    /**
     * 删除按钮移动、旋转和缩放后的中心坐标
     */
    private float[] mDelDrawPoint = new float[2];

    /**
     * 拖放按钮
     */
    private Bitmap mDragBitmap;

    /**
     * 删除按钮初始中心坐标
     */
    private float[] mDragPoint = new float[2];

    /**
     * 删除按钮移动、旋转和缩放后的中心坐标
     */
    private float[] mDragDrawPoint = new float[2];

    /**
     * 虚线框初始rect
     */
    private RectF mDashRectF = new RectF();
    private RectF mDashDrawRectF = new RectF();

    /**
     * 删除按钮rect
     */
    private RectF mDelDrawRectF = new RectF();

    /**
     * 拖放按钮rect
     */
    private RectF mDragDrawRectF = new RectF();

    /**
     * 是否显示虚线框
     */
    private boolean mShowDashBorder = true;
    private boolean mPreShowDashBorder;

    /**
     * 是否为点击事件
     */
    private boolean mAlwaysInTapRegion;

    private final int mTouchSlopSquare;

    private float mInitTouchX;
    private float mInitTouchY;

    private float mLastTouchX;
    private float mLastTouchY;

    private LabelListener mListener;

    private View mChildView;

    private boolean isRunPreDrawListener = true;

    public DashFrameLayout(Context context) {
        this(context, null);
    }

    public DashFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DashFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setWillNotDraw(false);

        final int margin = Utils.dip2px(context, 5);
        mDelBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_stamp_close);
        mDragBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_stamp_drag);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mDashPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDashPaint.setColor(Color.GRAY);
        mDashPaint.setStyle(Paint.Style.STROKE);
        mDashPaint.setStrokeWidth(3);

        mDashPath = new Path();
        mRefreshHandler = new RefreshHandler(this);

        mDashFloats = new float[]{DASH_ON_WIDTH, DASH_OFF_WIDTH};
        mPathEffects = new PathEffect[mDashMinSize];
        showDashBorder(true);

        mTouchSlopSquare = ViewConfiguration.get(context).getScaledTouchSlop() *
                ViewConfiguration.get(context).getScaledTouchSlop();

        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (!isRunPreDrawListener) {
                    return true;
                }
                isRunPreDrawListener = false;

                int childCount = getChildCount();
                if (childCount < 1) {
                    return false;
                }

                Rect rect = new Rect();
                mChildView.getLocalVisibleRect(rect);

                // 计算虚线框绘制位置，注意虚线框与label之间存在margin
                mDashRectF.left = rect.left - margin;
                mDashRectF.top = rect.top - margin;
                mDashRectF.right = rect.right + margin;
                mDashRectF.bottom = rect.bottom + margin;

                // 计算删除按钮初始中心坐标
                mDelPoint[0] = mDashRectF.left;
                mDelPoint[1] = mDashRectF.top;

                // 计算拖放按钮初始中心坐标
                mDragPoint[0] = mDashRectF.right;
                mDragPoint[1] = mDashRectF.bottom;

                if (mViewMatrix.isIdentity()) {
                    mCenterPoint[0] = rect.width() >> 1;
                    mCenterPoint[1] = rect.height() >> 1;
                    int halfWidth = getWidth() >> 1;
                    int halfHeight = getHeight() >> 1;
                    // 移动到中心
                    translate(halfWidth - mCenterPoint[0], halfHeight - mCenterPoint[1]);
                } else {
                    int newCenterX = rect.width() >> 1;
                    int newCenterY = rect.height() >> 1;

                    // 根据新中心调整矩阵
                    mViewMatrix.reset();
                    mViewMatrix.postTranslate(mCenterPoint[0] - newCenterX, mCenterPoint[1] - newCenterY);
                    mViewMatrix.postRotate(mCurRotation, mCenterPoint[0], mCenterPoint[1]);
                    mViewMatrix.postScale(mCurScaleFactor, mCurScaleFactor, mCenterPoint[0], mCenterPoint[1]);

                    mViewNoRotateMatrix.reset();
                    mViewNoRotateMatrix.postTranslate(mCenterPoint[0] - newCenterX, mCenterPoint[1] - newCenterY);
                    mViewNoRotateMatrix.postScale(mCurScaleFactor, mCurScaleFactor, mCenterPoint[0], mCenterPoint[1]);

                    updateBtnDrawRect();
                }
                // 将view转换为bitmap，便于旋转、缩放时提高性能
                mLabelBitmap = ImageUtils.captureScreenByDraw(mChildView);

                mDashPath.reset();
                mDashPath.addRect(mDashRectF, Path.Direction.CCW);
                removeAllViews();
                return true;
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 画标签
        canvas.drawBitmap(mLabelBitmap, mViewMatrix, null);
        if (mShowDashBorder) {
            // 画虚线框
            mDashPaint.setPathEffect(getPathEffect());
            canvas.save();
            canvas.concat(mViewMatrix);
            canvas.drawPath(mDashPath, mDashPaint);
            canvas.restore();

            // 画删除按钮：左上角
            canvas.drawBitmap(mDelBitmap, null, mDelDrawRectF, null);

            // 画拖放按钮：右下角
            canvas.drawBitmap(mDragBitmap, null, mDragDrawRectF, null);
        }
    }

    @Override
    public void addView(View child) {
        removeAllViews();

        // 添加新view后，需再次调用OnPreDrawListener
        isRunPreDrawListener = true;
        mChildView = child;
        super.addView(child, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    public View getLabelView() {
        return mChildView;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = true;
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                // 删除按钮接收事件
                if (mShowDashBorder && mDelDrawRectF.contains(event.getX(), event.getY())) {
                    if (getParent() instanceof ViewGroup) {
                        ((ViewGroup) getParent()).removeView(DashFrameLayout.this);
                    }
                    break;
                }
                // 拖放按钮接收事件
                if (mShowDashBorder && mDragDrawRectF.contains(event.getX(), event.getY())) {
                    mode = SINGLE_ZOOM;
                    showDashBorder(true);

                    mPreDistance = Utils.calcDistance(mCenterPoint[0], mCenterPoint[1], event.getX(), event.getY());
                    mPreRotation = Utils.calcRotation(mCenterPoint[0], mCenterPoint[1], event.getX(), event.getY());
                    break;
                }
                /**
                 * 判断虚线框是否接收事件；
                 * 因旋转后无法直接判断事件是否在rect内，
                 * 可将坐标点反向旋转后再与未进行旋转的rect进行比较判断
                 */
                mDashDrawRectF.set(mDashRectF);
                mViewNoRotateMatrix.mapRect(mDashDrawRectF);
                Matrix inverseMatrix = new Matrix();
                // 获取反向旋转矩阵
                inverseMatrix.postRotate(360 - mCurRotation, mCenterPoint[0], mCenterPoint[1]);
                // 将当前坐标反向旋转，再进行判断
                float[] eventPoint = {event.getX(), event.getY()};
                inverseMatrix.mapPoints(eventPoint);
                if (mDashDrawRectF.contains(eventPoint[0], eventPoint[1])) {
                    mode = DRAG;

                    mInitTouchX = event.getX();
                    mInitTouchY = event.getY();

                    mLastTouchX = event.getX();
                    mLastTouchY = event.getY();
                } else {
                    handled = false;
                }

                showDashBorder(handled);
                if (handled && mListener != null) {
                    mListener.onFocus();
                    mAlwaysInTapRegion = true;
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (mAlwaysInTapRegion) {
                    float deltaX = event.getX() - mInitTouchX;
                    float deltaY = event.getY() - mInitTouchY;
                    // 判断是否在点击区域内
                    if ((deltaX * deltaX) + (deltaY * deltaY) > mTouchSlopSquare) {
                        mAlwaysInTapRegion = false;
                    }
                }

                if (mode == SINGLE_ZOOM || mode == DOUBLE_ZOOM) {
                    float curDistance, endRotation;
                    if (mode == SINGLE_ZOOM) {
                        // 单指缩放、旋转
                        curDistance = Utils.calcDistance(mCenterPoint[0], mCenterPoint[1], event.getX(), event.getY());
                        endRotation = Utils.calcRotation(mCenterPoint[0], mCenterPoint[1], event.getX(), event.getY());
                    } else {
                        // 双指缩放、旋转
                        curDistance = Utils.calcDistance(event);
                        endRotation = Utils.calcRotation(event);
                    }
                    scale(curDistance / mPreDistance);
                    mPreDistance = curDistance;

                    rotate(endRotation - mPreRotation);
                    mPreRotation = endRotation;
                } else if (mode == DRAG) {
                    translate(event.getX() - mLastTouchX, event.getY() - mLastTouchY);
                    mLastTouchX = event.getX();
                    mLastTouchY = event.getY();
                }
                invalidate();
                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                mAlwaysInTapRegion = false;
                if (mode != SINGLE_ZOOM) {
                    mode = DOUBLE_ZOOM;
                    mPreDistance = Utils.calcDistance(event);
                    mPreRotation = Utils.calcRotation(event);
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                mode = NONE;
                mPreRotation = 0.f;

                if (mAlwaysInTapRegion && mPreShowDashBorder && mListener != null) {
                    mListener.onClick();
                }
                mAlwaysInTapRegion = false;
                break;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                if (mode != SINGLE_ZOOM) {
                    mode = NONE;
                    mPreRotation = 0.f;
                }
                break;
            }
        }
        return handled;
    }

    private void rotate(float degree) {
        mCurRotation += degree;
        mViewMatrix.postRotate(degree, mCenterPoint[0], mCenterPoint[1]);

        updateBtnDrawRect();
    }

    private void translate(float dx, float dy) {
        // 改变旋转、缩放中心点
        mCenterPoint[0] += dx;
        mCenterPoint[1] += dy;

        // 判断中心点是否移出截图区域
        if (mCenterPoint[0] <= 0) {
            mCenterPoint[0] = 0;
            dx = 0;
        } else if (mCenterPoint[0] >= getWidth()) {
            mCenterPoint[0] = getWidth();
            dx = 0;
        }

        if (mCenterPoint[1] <= 0) {
            mCenterPoint[1] = 0;
            dy = 0;
        } else if (mCenterPoint[1] >= getHeight()) {
            mCenterPoint[1] = getHeight();
            dy = 0;
        }

        mViewMatrix.postTranslate(dx, dy);
        mViewNoRotateMatrix.postTranslate(dx, dy);

        updateBtnDrawRect();
    }

    private void scale(float scale) {
        float curScale = mCurScaleFactor * scale;
        if (curScale <= MAX_SCALE) {
            mCurScaleFactor = curScale;
        } else {
            scale = MAX_SCALE / mCurScaleFactor;
            mCurScaleFactor = MAX_SCALE;
        }

        mViewMatrix.postScale(scale, scale, mCenterPoint[0], mCenterPoint[1]);
        mViewNoRotateMatrix.postScale(scale, scale, mCenterPoint[0], mCenterPoint[1]);

        updateBtnDrawRect();
    }

    /**
     * 更新当前删除、缩放按钮绘制位置
     */
    private void updateBtnDrawRect() {
        // 计算删除按钮绘制位置
        mDelDrawPoint[0] = mDelPoint[0];
        mDelDrawPoint[1] = mDelPoint[1];
        mViewMatrix.mapPoints(mDelDrawPoint);
        mDelDrawRectF.left = mDelDrawPoint[0] - (mDelBitmap.getWidth() >> 1);
        mDelDrawRectF.top = mDelDrawPoint[1] - (mDelBitmap.getHeight() >> 1);
        mDelDrawRectF.right = mDelDrawPoint[0] + (mDelBitmap.getWidth() >> 1);
        mDelDrawRectF.bottom = mDelDrawPoint[1] + (mDelBitmap.getHeight() >> 1);

        // 计算缩放按钮绘制位置
        mDragDrawPoint[0] = mDragPoint[0];
        mDragDrawPoint[1] = mDragPoint[1];
        mViewMatrix.mapPoints(mDragDrawPoint);
        mDragDrawRectF.left = mDragDrawPoint[0] - (mDelBitmap.getWidth() >> 1);
        mDragDrawRectF.top = mDragDrawPoint[1] - (mDelBitmap.getHeight() >> 1);
        mDragDrawRectF.right = mDragDrawPoint[0] + (mDelBitmap.getWidth() >> 1);
        mDragDrawRectF.bottom = mDragDrawPoint[1] + (mDelBitmap.getHeight() >> 1);
    }

    private PathEffect getPathEffect() {
        mDashPhase %= mDashMinSize;
        PathEffect effect = mPathEffects[mDashPhase];
        if (effect == null) {
            effect = new DashPathEffect(mDashFloats, mDashPhase);
            mPathEffects[mDashPhase] = effect;
        }
        mDashPhase++;
        return effect;
    }

    public void showDashBorder(boolean show) {
        mPreShowDashBorder = mShowDashBorder;
        mShowDashBorder = show;
        if (mShowDashBorder) {
            if (getParent() instanceof ViewGroup) {
                getParent().bringChildToFront(this);
            }
            mRefreshHandler.sendEmptyMessage(MSG_DRAW);
        } else {
            mRefreshHandler.sendEmptyMessage(MSG_ERASE);
        }
    }

    public void setLabelListener(LabelListener listener) {
        mListener = listener;
    }

    private static class RefreshHandler extends Handler {

        private WeakReference<View> mWeakReference;

        public RefreshHandler(View view) {
            mWeakReference = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DRAW:
                    final View view = mWeakReference.get();
                    if (view != null) {
                        view.invalidate();
                        sendMessageDelayed(obtainMessage(MSG_DRAW), REFRESH_INTERVAL);
                    }
                    break;
                case MSG_ERASE:
                    removeMessages(MSG_DRAW);
                    break;
            }
        }
    }

    public interface LabelListener {
        void onFocus();

        void onClick();
    }
}
