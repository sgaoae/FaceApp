package com.example.gaoshenlai.faceapp.utils.imageviewhelper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.widget.ImageView;

/**
 * Created by gaoshenlai on 31/1/17.
 */

public class bitmaphelper {
    private static final int IMAGEVIEW_WIDTH = 350;
    private static final int IMAGEVIEW_HEIGHT = 400;

    public static Bitmap getProperBitmap(String path){
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, opt);
        opt.inSampleSize = calculateInSampleSize(opt, IMAGEVIEW_WIDTH, IMAGEVIEW_HEIGHT);
        opt.inJustDecodeBounds = false;
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap image = BitmapFactory.decodeFile(path, opt);
        return image;
    }
    /* code from android developer web page http://developer.android.com/training/displaying-bitmaps/load-bitmap.html */
    protected static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
