package com.example.clipheaderlikeqq.app.clip;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import com.example.clipheaderlikeqq.app.R;
import com.example.clipheaderlikeqq.app.util.BitmapUtil;
import com.example.clipheaderlikeqq.app.util.CommonUtil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;


/**
 * 图片裁剪activity
 *

 使用方法：
 intent.setData(uri);//源图片Uri
 intent.putExtra("side_length", 130);//裁剪图片宽高

 */
public class ClipHeaderActivity extends Activity implements OnTouchListener{
    private String TAG = "ClipHeaderActivity";
    private ImageView srcPic;
    private ImageView iv_back;
    private View bt_ok;
    private ClipView clipview;

    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();

    /**
     * 动作标志：无
     */
    private static final int NONE = 0;
    /**
     * 动作标志：拖动
     */
    private static final int DRAG = 1;
    /**
     * 动作标志：缩放
     */
    private static final int ZOOM = 2;
    /**
     * 初始化动作标志
     */
    private int mode = NONE;

    /**
     * 记录起始坐标
     */
    private PointF start = new PointF();
    /**
     * 记录缩放时两指中间点坐标
     */
    private PointF mid = new PointF();
    private float oldDist = 1f;

    private Bitmap bitmap;

    private int side_length;//裁剪区域边长

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.clip_activity);
        init();

    }

    private void init() {
        side_length = getIntent().getIntExtra("side_length",200);

        iv_back = (ImageView) this.findViewById(R.id.iv_back);
        srcPic = (ImageView) this.findViewById(R.id.src_pic);
        clipview =  (ClipView)this.findViewById(R.id.clipView);
        bt_ok = this.findViewById(R.id.bt_ok);

        srcPic.setOnTouchListener(this);

        ViewTreeObserver observer = clipview.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

            @SuppressWarnings("deprecation")
            public void onGlobalLayout() {
                clipview.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                initSrcPic();
            }
        });

        bt_ok.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                generateUriAndReturn();
            }
        });

        iv_back.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }


    /**
     * 初始化图片
     * step 1: decode 出 720*1280 左右的照片  因为原图可能比较大 直接加载出来会OOM
     * step 2: 将图片缩放 移动到imageView 中间
     */
    private void initSrcPic(){
        Uri uri = getIntent().getData();
        String path = CommonUtil.getRealFilePathFromUri(getApplicationContext(), uri);
        if (TextUtils.isEmpty(path)) {
            return;
        }
        bitmap = BitmapUtil.decodeSampledBitmap(path,720,1280);

        if (bitmap == null) {
            return;
        }

        //图片的缩放比
        float scale ;
        if (bitmap.getWidth()>bitmap.getHeight()) {//宽图
            scale = (float)srcPic.getWidth()/bitmap.getWidth();

            //如果高缩放后小于裁剪区域 则将裁剪区域与高的缩放比作为最终的缩放比
            Rect rect = clipview.getClipRect();
            float minScale = rect.height()/bitmap.getHeight();//高的最小缩放比
            if (scale < minScale){
                scale = minScale;
            }
        }else {//高图
            scale = (float)srcPic.getWidth()/2/bitmap.getWidth();//宽缩放到imageview的宽的1/2
        }

        // 缩放
        matrix.postScale(scale, scale);

        // 平移
        int midX = srcPic.getWidth()/2;//imageView的中心x
        int midY = srcPic.getHeight()/2;//imageView的中心y
        int imageMidX = (int)(bitmap.getWidth()*scale/2);//bitmap的中心x
        int imageMidY = (int)(bitmap.getHeight()*scale/2);//bitmap的中心y
        matrix.postTranslate(midX - imageMidX, midY - imageMidY);

        srcPic.setScaleType(ScaleType.MATRIX);
        srcPic.setImageMatrix(matrix);
        srcPic.setImageBitmap(bitmap);

    }

    public boolean onTouch(View v, MotionEvent event) {
        ImageView view = (ImageView) v;
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                // 设置开始点位置
                start.set(event.getX(), event.getY());
                mode = DRAG;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    matrix.set(savedMatrix);
                    matrix.postTranslate(event.getX() - start.x, event.getY()
                            - start.y);
                } else if (mode == ZOOM) {
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        matrix.set(savedMatrix);
                        float scale = newDist / oldDist;
                        matrix.postScale(scale, scale, mid.x, mid.y);
                    }
                }
                break;
        }
        view.setImageMatrix(matrix);
        return true;
    }

    /**
     * 多点触控时，计算最先放下的两指距离
     *
     * @param event
     * @return
     */
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * 多点触控时，计算最先放下的两指中心坐标
     *
     * @param point
     * @param event
     */
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }


    /**
     * 获取缩放后的截图
     * 1.截取裁剪框内bitmap
     * 2.将bitmap缩放到宽高为side_length
     *
     * @return
     */
    private Bitmap getZoomedCropBitmap() {

        srcPic.setDrawingCacheEnabled(true);
        srcPic.buildDrawingCache();

        Rect rect = clipview.getClipRect();

        Bitmap cropBitmap = null;
        Bitmap zoomedCropBitmap = null;
        try {
            cropBitmap = Bitmap.createBitmap(srcPic.getDrawingCache(), rect.left, rect.top, rect.width(), rect.height());
            zoomedCropBitmap = BitmapUtil.zoomBitmap(cropBitmap,side_length,side_length);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (cropBitmap != null) {
            cropBitmap.recycle();
        }

        // 释放资源
        srcPic.destroyDrawingCache();

        return zoomedCropBitmap;
    }


    /**
     * 生成Uri并且通过setResult返回给打开的activity
     */
    private void generateUriAndReturn(){
        Bitmap zoomedCropBitmap = getZoomedCropBitmap();
        if (zoomedCropBitmap == null) {
            Log.e(TAG, "zoomedCropBitmap == null");
            return;
        }

        Uri mSaveUri = Uri.fromFile(new File(getCacheDir(), "cropped_"+System.currentTimeMillis()+".jpg"));

        if (mSaveUri != null) {
            OutputStream outputStream = null;
            try {
                outputStream = getContentResolver().openOutputStream(mSaveUri);
                if (outputStream != null) {
                    zoomedCropBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                }
            } catch (IOException ex) {
                // TODO: report error to caller
                Log.e(TAG, "Cannot open file: " + mSaveUri, ex);
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            Intent intent = new Intent();
            intent.setData(mSaveUri);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

}