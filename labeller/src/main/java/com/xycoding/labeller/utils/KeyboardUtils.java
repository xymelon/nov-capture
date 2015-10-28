package com.xycoding.labeller.utils;


import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.view.View;

/**
 * Created by xuyang on 15/10/10.
 */
public class KeyboardUtils {

    /**
     * 主动显示软键盘
     *
     * @param mContext
     * @param editText
     */
    public static void requestFocusAndShowKeyboard(Context mContext, EditText editText) {
        editText.requestFocus();
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editText, InputMethodManager.SHOW_FORCED);
    }

    /**
     * 主动隐藏软键盘
     *
     * @param mContext
     * @param v
     */
    public static void hideSystemKeyBoard(Context mContext, View v) {
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    /**
     * 切换显示/隐藏软键盘
     *
     * @param context
     * @param view
     */
    public static void showSoftKeyBoard(Context context, View view) {
        if (context != null && view != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) context
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_FORCED);
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
    }
}
