package com.xycoding.labeller.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.view.View;

/**
 * Created by xuyang on 15/10/19.
 */
public class ImageUtils {

    public static Bitmap captureScreenByDraw(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(),
                view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    public static Bitmap resize(Bitmap bitmap, int maxWidth, int maxHeight) {
        if (maxHeight > 0 && maxWidth > 0) {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > 1) {
                finalWidth = (int) ((float) maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float) maxWidth / ratioBitmap);
            }
            bitmap = Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true);
            return bitmap;
        } else {
            return bitmap;
        }
    }

    /**
     * 根据height缩放图片，若height大于图片实际高度，则直接返回
     *
     * @param uri
     * @param height
     * @return
     */
    public static Bitmap decodeUri(Uri uri, int height) {
        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(uri.getEncodedPath(), decodeOptions);
        int actualWidth = decodeOptions.outWidth;
        int actualHeight = decodeOptions.outHeight;

        decodeOptions.inJustDecodeBounds = false;
        int width = actualWidth;
        if (actualHeight > height && height > 0) {
            //按比例获取宽度
            width = (int) (height * 1.0 / actualHeight * actualWidth);
            decodeOptions.inSampleSize =
                    findBestSampleSize(actualWidth, actualHeight, width, height);
        } else {
            height = actualHeight;
        }
        Bitmap tempBitmap = BitmapFactory.decodeFile(uri.getEncodedPath(), decodeOptions);
        Bitmap bitmap;
        // If necessary, scale down to the maximal acceptable size.
        if (tempBitmap != null && (tempBitmap.getWidth() > width ||
                tempBitmap.getHeight() > height)) {
            bitmap = Bitmap.createScaledBitmap(tempBitmap,
                    width, height, true);
            tempBitmap.recycle();
        } else {
            bitmap = tempBitmap;
        }
        return bitmap;
    }

    private static int findBestSampleSize(
            int actualWidth, int actualHeight, int desiredWidth, int desiredHeight) {
        double wr = (double) actualWidth / desiredWidth;
        double hr = (double) actualHeight / desiredHeight;
        double ratio = Math.min(wr, hr);
        float n = 1.0f;
        while ((n * 2) <= ratio) {
            n *= 2;
        }
        return (int) n;
    }

}
