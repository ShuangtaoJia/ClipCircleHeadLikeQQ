package com.example.clipheaderlikeqq.app.clip;

import android.content.Context;
import android.graphics.*;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

public class ClipView extends View {

    private Paint paint = new Paint();
    /**
     * 画裁剪区域边框的画笔
     */
    private Paint borderPaint = new Paint();

    /**
     * 裁剪框边框宽度
     */
    private int clipBorderWidth = 2;

    private static final int LAYER_FLAGS = Canvas.MATRIX_SAVE_FLAG | Canvas.CLIP_SAVE_FLAG
            | Canvas.HAS_ALPHA_LAYER_SAVE_FLAG | Canvas.FULL_COLOR_LAYER_SAVE_FLAG
            | Canvas.CLIP_TO_LAYER_SAVE_FLAG;

    private float radiusWidthRatio  = 2f/9;//裁剪圆框的半径占view的宽度的比

    int width;
    int height;

    private Xfermode xfermode;


    public ClipView(Context context) {
        this(context, null);
    }

    public ClipView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClipView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        paint.setAntiAlias(true); //去锯齿

        borderPaint.setStyle(Style.STROKE);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStrokeWidth(clipBorderWidth);
        borderPaint.setAntiAlias(true); //去锯齿

        xfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
         width = this.getWidth();
         height = this.getHeight();

        //通过Xfermode的DST_OUT来产生中间的圆，一定要另起一个Layer（层），
        // 直接在canvas原本的那一层来做的话，最后中间那个圆空了以后，不是透明的，而是黑色的，
        //我觉得这应该是因为canvas默认在被“掏空”以后，下面是黑色的，而另起一层的话，被“掏空”就是透明的，
        // 然后再把这层加到canvas上就满足了我们的需求

        //saveLayer相当于新入栈一个图层，接下来的操作都会在该图层上进行
        canvas.saveLayer(0, 0, width, height, null, LAYER_FLAGS);
        canvas.drawColor(Color.parseColor("#a8000000"));
        paint.setXfermode(xfermode);
        //中间的透明的圆
        canvas.drawCircle(width / 2, height / 2, width * radiusWidthRatio, paint);

        //白色的圆边框
        canvas.drawCircle(width / 2, height / 2, width * radiusWidthRatio + clipBorderWidth, borderPaint);
        //出栈，恢复到之前的图层，意味着新建的图层会被删除，新建图层上的内容会被绘制到canvas (or the previous layer)
        canvas.restore();

    }

    /**
     * 获取裁剪区域的Rect
     * @return
     */
    public Rect getClipRect(){
        Rect rect = new Rect();
        rect.left = (int)(width/2 - width * radiusWidthRatio);//宽度的一半 - 圆的半径
        rect.right = (int)(width/2 + width * radiusWidthRatio);//宽度的一半 + 圆的半径
        rect.top = (int)(height/2 - width * radiusWidthRatio);//高度的一半 - 圆的半径
        rect.bottom = (int)(height/2 + width * radiusWidthRatio);//高度的一半 + 圆的半径

        return rect;

    }

}
