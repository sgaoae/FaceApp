package com.example.gaoshenlai.faceapp.utils.imageviewhelper;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.media.FaceDetector;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.gaoshenlai.faceapp.MainMenu;

/**
 * Created by gaoshenlai on 31/1/17.
 */

public class imagevieweffecthelper {
    public static void addZoomEffect(ImageView imageview){
        final ImageView iv = imageview;
        iv.setOnTouchListener(new View.OnTouchListener() {
            final PointF pressPoint0 = new PointF();
            final Matrix transformMatrix = new Matrix();
            private static final int IMAGE_ZOOM = 1;
            private static final int IMAGE_DRAG = 2;
            protected float beforeSize;
            private int IMAGE_ACTION;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // handle touch event
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        pressPoint0.set(event.getX(), event.getY());
                        transformMatrix.set(iv.getImageMatrix());
                        IMAGE_ACTION = IMAGE_DRAG;
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        beforeSize = getDistance(event);
                        IMAGE_ACTION = IMAGE_ZOOM;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        switch (IMAGE_ACTION) {
                            case IMAGE_DRAG:
                                PointF press = new PointF(event.getX(), event.getY());
                                transformMatrix.postTranslate
                                        ((press.x - pressPoint0.x), (press.y - pressPoint0.y));
                                pressPoint0.set(press);
                                break;
                            case IMAGE_ZOOM:
                                float afterSize = getDistance(event);
                                PointF middlePointF = getMiddlePointF(event);
                                float zoomScale = afterSize / beforeSize;
                                beforeSize = afterSize;
                                iv.setScaleType(ImageView.ScaleType.MATRIX);
                                transformMatrix.postScale(zoomScale,
                                        zoomScale, middlePointF.x, middlePointF.y);
                                break;
                        }
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        IMAGE_ACTION = 0;
                        break;
                    case MotionEvent.ACTION_UP:
                        IMAGE_ACTION = 0;
                        break;
                }
                iv.setImageMatrix(transformMatrix);
                iv.invalidate();
                return true;
            }
            // used only for zoom
            protected float getDistance(MotionEvent event){
                float xDistance = event.getX(0) - event.getX(1);
                float yDistance = event.getY(0) - event.getY(1);
                return (float) Math.sqrt(xDistance*xDistance+yDistance*yDistance);
            }
            // used only for zoom
            protected PointF getMiddlePointF(MotionEvent event){
                PointF result = new PointF();
                result.set((event.getX(0)+event.getX(1))/2,(event.getY(0)+event.getY(1))/2);
                return result;
            }
        });
    }
    public static void highlightFaces(ImageView image, FaceDetector.Face[] faces,int numOfFaces){
        if(numOfFaces==0)return;
        PointF[] leftUp = new PointF[numOfFaces];
        PointF[] rightDown = new PointF[numOfFaces];
        for(int i=0;i<numOfFaces;++i){
            PointF temp = new PointF();
            faces[i].getMidPoint(temp);
            float dis = faces[i].eyesDistance();
            leftUp[i]=new PointF(temp.x-dis,temp.y-dis);
            rightDown[i]=new PointF(temp.x+dis,temp.y+dis);
        }

        Bitmap bitmap = getBitmapFromImageView(image);
        Bitmap tempbitmap = Bitmap.createBitmap(bitmap.getWidth(),bitmap.getHeight(), Bitmap.Config.RGB_565);

        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        Canvas canvas = new Canvas(tempbitmap);
        canvas.drawBitmap(bitmap,0,0,null);
        for(int i=0;i<numOfFaces;++i){
            canvas.drawRect(leftUp[i].x,leftUp[i].y,rightDown[i].x,rightDown[i].y,paint);
        }
        image.setImageDrawable(new BitmapDrawable(image.getResources(),tempbitmap));
    }
    public static Bitmap getBitmapFromImageView(ImageView iv){
        //Log.d("error","getBitmapFromImageView() is called");
        BitmapDrawable temp = (BitmapDrawable) iv.getDrawable();
        if(temp==null)return null;
        return temp.getBitmap();
    }
}
