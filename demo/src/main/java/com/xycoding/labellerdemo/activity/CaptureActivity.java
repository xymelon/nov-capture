package com.xycoding.labellerdemo.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import com.xycoding.labeller.view.CaptureView;
import com.xycoding.labellerdemo.R;
import com.xycoding.labellerdemo.utils.Toaster;
import com.xycoding.labellerdemo.utils.Utils;

import java.io.File;

/**
 * Created by xuyang on 15/10/19.
 */
public class CaptureActivity extends Activity implements View.OnClickListener {

    private SaveBitmapAsyncTask mAsyncTask;

    private Dialog loadingDialog;

    private CaptureView mCaptureView;

    private File mSaveDir;

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        mContext = this;
        mSaveDir = Utils.getDiskCacheDir(this, "novcapture");

        findViewById(R.id.iv_left).setOnClickListener(this);
        findViewById(R.id.tv_right).setOnClickListener(this);

        mCaptureView = (CaptureView) findViewById(R.id.capture_view);
        // 标签最多字数
        mCaptureView.setLabelMaxTextLength(20);
        // 标签默认文本
        mCaptureView.setLabelDefaultText("明明能靠脸偏偏靠才华");
        // 设置内容
        mCaptureView.setImage(getIntent().getData());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_left:
                finish();
                break;
            case R.id.tv_right:
                mAsyncTask = new SaveBitmapAsyncTask();
                mAsyncTask.execute(mCaptureView.getContentBitmap());
                break;
        }
    }

    protected void onDestroy() {
        super.onDestroy();

        if (mAsyncTask != null) {
            mAsyncTask.cancel(true);
            mAsyncTask = null;
        }
    }

    public void onShowLoadingDialog() {
        loadingDialog = Utils.createLoadingDialog(this,
                getResources().getString(R.string.waiting_hint));
        loadingDialog.show();
    }

    public void onDismissLoadingDialog() {
        if (loadingDialog != null) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }

    private class SaveBitmapAsyncTask extends AsyncTask<Bitmap, Void, Uri> {

        @Override
        protected void onPreExecute() {
            onShowLoadingDialog();
        }

        @Override
        protected Uri doInBackground(Bitmap... params) {
            return Utils.saveBitmapToFile(mSaveDir, params[0]);
        }

        @Override
        protected void onPostExecute(Uri uri) {
            onDismissLoadingDialog();

            if (uri != null) {
                Toaster.showMsgLong(mContext, "图片已保存到\n" + mSaveDir);
            } else {
                Toaster.show(mContext, R.string.image_save_error);
            }
        }
    }
}
