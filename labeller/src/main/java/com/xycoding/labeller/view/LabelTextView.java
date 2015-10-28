package com.xycoding.labeller.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by xuyang on 15/8/29.
 */
public class LabelTextView extends TextView {

    private CharSequence mCurText;

    public LabelTextView(Context context) {
        this(context, null);
    }

    public LabelTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LabelTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

    }

    @Override
    public void setBackgroundResource(int resid) {
        super.setBackgroundResource(resid);
    }

    /**
     * 设置文本，并调整格式
     *
     * @param text
     */
    public void setLabelText(CharSequence text) {
        mCurText = text;
        int len = text.length();
        StringBuilder strBuilder = new StringBuilder();
        if (len <= 6) {
            strBuilder.append(text);
        } else {
            int mid = Math.round(len / 2.0f);
            strBuilder.append(text.subSequence(0, mid));
            strBuilder.append("\n");
            strBuilder.append(text.subSequence(mid, len));
        }
        super.setText(strBuilder);
    }

    @Override
    public CharSequence getText() {
        return mCurText;
    }
}
