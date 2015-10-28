package com.xycoding.labellerdemo.activity;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import com.xycoding.labellerdemo.R;
import com.xycoding.labellerdemo.utils.Utils;

import java.io.File;

public class MainActivity extends ActionBarActivity {

    private PopupWindow mPopup;

    private Dialog loadingDialog;

    private File mSaveDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSaveDir = Utils.getDiskCacheDir(this, "novcapture");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        showPopup();
        return super.onOptionsItemSelected(item);
    }

    private void showPopup() {
        if (mPopup == null) {
            View view = LayoutInflater.from(this).inflate(R.layout.view_popup, null);
            view.findViewById(R.id.tv_screen)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            captureScreen();
                        }
                    });
            mPopup = new PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            mPopup.setAnimationStyle(R.style.PopupAnimation);
            mPopup.setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
            mPopup.setOutsideTouchable(true);
        }
        if (mPopup.isShowing()) {
            mPopup.dismiss();
        } else {
            mPopup.showAtLocation(findViewById(android.R.id.content),
                    Gravity.BOTTOM, 0, 0);
        }
    }

    private void captureScreen() {
        // 截取整个屏幕需减去状态栏高度
        Rect frame = new Rect();
        // 获取状态栏高度
        getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
        new CaptureAsyncTask().execute(frame.top);
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

    private void startCaptureActivity(Uri uri) {
        Intent intent = new Intent(this, CaptureActivity.class);
        intent.setData(uri);
        startActivity(intent);
    }

    private class CaptureAsyncTask extends AsyncTask<Integer, Void, Uri> {

        @Override
        protected void onPreExecute() {
            onShowLoadingDialog();
        }

        @Override
        protected Uri doInBackground(Integer... params) {
            int statusBarHeight = params[0];
            Bitmap bitmap = Utils.captureScreenByDraw(getWindow().getDecorView());
            Bitmap finalBitmap = Bitmap.createBitmap(bitmap, 0, statusBarHeight, bitmap.getWidth(), bitmap.getHeight() - statusBarHeight);
            bitmap.recycle();

            return Utils.saveBitmapToFile(mSaveDir, finalBitmap);
        }

        @Override
        protected void onPostExecute(Uri uri) {
            onDismissLoadingDialog();

            startCaptureActivity(uri);
        }
    }
}
