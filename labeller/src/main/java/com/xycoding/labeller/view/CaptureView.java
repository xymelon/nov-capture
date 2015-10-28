package com.xycoding.labeller.view;

import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.xycoding.labeller.R;
import com.xycoding.labeller.utils.ImageUtils;
import com.xycoding.labeller.utils.KeyboardUtils;

/**
 * Created by xuyang on 15/10/19.
 */
public class CaptureView extends FrameLayout {

    private final static String TAG = CaptureView.class.getSimpleName();
    private final static int LABEL_MAX_TEXT_LENGTH = 20;
    private final static String LABEL_DEFAULT_TEXT = "朕已阅";

    private FrameLayout mContainerLayout;

    private ImageView mImageIV;

    /* 修改标签文字相关 begin */
    private EditText mEditLabelET;
    private Dialog mDialog;
    /* 修改标签文字相关 end */

    /**
     * 当前焦点标签
     */
    private DashFrameLayout mFocusLabel;

    /**
     * 图片数据
     */
    private Object mObject;

    /**
     * 标签最多字数
     */
    private int mLabelMaxTextLength = LABEL_MAX_TEXT_LENGTH;

    /**
     * 标签默认文字
     */
    private String mLabelDefaultText = LABEL_DEFAULT_TEXT;

    private Context mContext;

    public CaptureView(Context context) {
        this(context, null);
    }

    public CaptureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CaptureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.view_capture, this);

        if (attrs != null) {
            TypedArray typed = context.obtainStyledAttributes(attrs,
                    R.styleable.CaptureStyle);
            try {
                mLabelMaxTextLength = typed.getInt(R.styleable.CaptureStyle_labelMaxTextLength, mLabelMaxTextLength);
                mLabelDefaultText = typed.getString(R.styleable.CaptureStyle_labelDefaultText);
                mLabelDefaultText = TextUtils.isEmpty(mLabelDefaultText) ? LABEL_DEFAULT_TEXT : mLabelDefaultText;
            } finally {
                typed.recycle();
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mContainerLayout = (FrameLayout) findViewById(R.id.fl_container);
        mImageIV = (ImageView) findViewById(R.id.iv_screen);

        if (isInEditMode()) {
            return;
        }
        setListeners();
    }

    private void setListeners() {
        // 计算控件宽度
        mContainerLayout.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                getViewTreeObserver().removeOnPreDrawListener(this);
                decodeImage();
                return true;
            }
        });

        findViewById(R.id.tv_label).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final LabelTextView textView = (LabelTextView) LayoutInflater.from(mContext).inflate(R.layout.view_label_text, null);
                textView.setLabelText(mLabelDefaultText);
                addLabel(textView);
            }
        });
    }

    private void addLabel(View view) {
        final DashFrameLayout dashFrameLayout = new DashFrameLayout(getContext());
        dashFrameLayout.setLabelListener(new DashFrameLayout.LabelListener() {
            @Override
            public void onFocus() {
                mFocusLabel = dashFrameLayout;
            }

            @Override
            public void onClick() {
                onLabelClick();
            }
        });
        dashFrameLayout.addView(view);

        hideFocusLabelDashBorder();
        mFocusLabel = dashFrameLayout;

        mContainerLayout.addView(dashFrameLayout);
    }

    private void hideFocusLabelDashBorder() {
        if (mFocusLabel != null) {
            mFocusLabel.showDashBorder(false);
        }
    }

    private void showFocusLabelDashBorder() {
        if (mFocusLabel != null) {
            mFocusLabel.showDashBorder(true);
        }
    }

    private void setImageBitmap(Bitmap bitmap) {
        mContainerLayout.getLayoutParams().width = bitmap.getWidth();
        mContainerLayout.getLayoutParams().height = bitmap.getHeight();
        mImageIV.getLayoutParams().width = bitmap.getWidth();
        mImageIV.getLayoutParams().height = bitmap.getHeight();
        mImageIV.setImageBitmap(bitmap);
    }

    private void onLabelClick() {
        if (mFocusLabel != null) {
            View view = mFocusLabel.getLabelView();
            if (view instanceof LabelTextView) {
                showEditDialog((LabelTextView) view);
            }
        }
    }

    private void showEditDialog(LabelTextView labelTextView) {
        if (mDialog == null) {
            initEditDialog();
        }
        mDialog.show();
        mEditLabelET.setText(labelTextView.getText());
        mEditLabelET.setSelection(mEditLabelET.length());
        KeyboardUtils.showSoftKeyBoard(getContext(), mEditLabelET);
    }

    private void initEditDialog() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.view_edit_text, this, false);
        final TextView countTV = (TextView) view.findViewById(R.id.tv_count);
        final Button confirmBtn = (Button) view.findViewById(R.id.btn_confirm);
        confirmBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                LabelTextView labelTextView = (LabelTextView) mFocusLabel.getLabelView();
                int len = mEditLabelET.length() < mLabelMaxTextLength ? mEditLabelET.length() : mLabelMaxTextLength;
                labelTextView.setLabelText(mEditLabelET.getText().subSequence(0, len));
                mFocusLabel.addView(labelTextView);
                mDialog.dismiss();
            }
        });
        view.findViewById(R.id.iv_cancel).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
            }
        });
        mEditLabelET = (EditText) view.findViewById(R.id.et_label_text);
        mEditLabelET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                countTV.setText(String.valueOf(mLabelMaxTextLength - s.length()));
                if (s.length() <= 0) {
                    confirmBtn.setEnabled(false);
                } else {
                    confirmBtn.setEnabled(true);
                }
            }
        });
        mDialog = new Dialog(getContext(), R.style.fullScreenDialog);
        mDialog.setContentView(view);
        Window window = mDialog.getWindow();
        window.setGravity(Gravity.BOTTOM);
        window.getAttributes().width = ViewGroup.LayoutParams.MATCH_PARENT;
        window.getAttributes().height = ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    private void decodeImage() {
        new DecodeAsyncTask().execute(mObject, mContainerLayout.getWidth(), mContainerLayout.getHeight());
    }

    public void setImage(Bitmap bitmap) {
        mObject = bitmap;
        if (isShown()) {
            decodeImage();
        }
    }

    public void setImage(Uri uri) {
        mObject = uri;
        if (isShown()) {
            decodeImage();
        }
    }

    public int getLabelMaxTextLength() {
        return mLabelMaxTextLength;
    }

    public void setLabelMaxTextLength(int maxTextLength) {
        this.mLabelMaxTextLength = maxTextLength;
    }

    public String getLabelDefaultText() {
        return mLabelDefaultText;
    }

    public void setLabelDefaultText(String defaultLabelText) {
        this.mLabelDefaultText = defaultLabelText;
    }

    public Bitmap getContentBitmap() {
        hideFocusLabelDashBorder();
        return ImageUtils.captureScreenByDraw(mContainerLayout);
    }

    private class DecodeAsyncTask extends AsyncTask<Object, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Object... params) {
            Object object = params[0];
            int contentWidth = (int) params[1];
            int contentHeight = (int) params[2];
            Bitmap bitmap = null;
            if (object instanceof Bitmap) {
                bitmap = ImageUtils.resize((Bitmap) object, contentWidth, contentHeight);
            } else if (object instanceof Uri) {
                bitmap = ImageUtils.decodeUri((Uri) object, contentHeight);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                setImageBitmap(bitmap);
            }
        }
    }
}
