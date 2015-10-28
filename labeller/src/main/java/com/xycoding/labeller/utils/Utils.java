package com.xycoding.labeller.utils;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.MotionEvent;

/**
 * Created by xuyang on 15/7/17.
 */
public class Utils {

    /**
     * 返回作用于资源文件的显示参数
     * <p>注：常用于控件大小、位置的动态计算
     *
     * @param ctx 上下文
     * @return 作用于资源文件的显示参数
     */
    public static DisplayMetrics getResourceDisplayMetrics(Context ctx) {
        return ctx.getResources().getDisplayMetrics();
    }

    /**
     * 单位转换，dip转 px
     *
     * @param ctx 上下文
     * @param dpValue dip单位表示的值
     * @return px单位表示的值
     */
    public static int dip2px(Context ctx, float dpValue) {
        DisplayMetrics dm = getResourceDisplayMetrics(ctx);
        final float scale = dm.density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 双指计算距离
     *
     * @param event
     * @return
     */
    public static float calcDistance(MotionEvent event) {
        return calcDistance(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
    }

    /**
     * 计算距离
     *
     * @param firstX
     * @param firstY
     * @param secondX
     * @param secondY
     * @return
     */
    public static float calcDistance(float firstX, float firstY, float secondX, float secondY) {
        float deltaX = secondX - firstX;
        float deltaY = secondY - firstY;
        return (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    /**
     * 双指计算旋转角度
     *
     * @param event
     * @return
     */
    public static float calcRotation(MotionEvent event) {
        return calcRotation(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
    }

    /**
     * 计算旋转角度
     *
     * @param firstX
     * @param firstY
     * @param secondX
     * @param secondY
     * @return
     */
    public static float calcRotation(float firstX, float firstY, float secondX, float secondY) {
        float deltaX = secondX - firstX;
        float deltaY = secondY - firstY;
        return (float) Math.toDegrees(Math.atan2(deltaY, deltaX));
    }

}
