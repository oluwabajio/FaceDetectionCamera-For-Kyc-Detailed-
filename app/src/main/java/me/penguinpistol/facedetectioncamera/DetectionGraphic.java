package me.penguinpistol.facedetectioncamera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;

import java.util.Locale;

public class DetectionGraphic extends View {

    private final Point displaySize;
    private final Paint faceRectPaint;
    private final Paint faceConvertPaint;
    private final TextPaint textPaint;

    private Face face;

    public DetectionGraphic(Context context) {
        this(context, null, 0);
    }

    public DetectionGraphic(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DetectionGraphic(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        displaySize = new Point();
        ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(displaySize);

        float density = context.getResources().getDisplayMetrics().density;

        faceRectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        faceRectPaint.setStyle(Paint.Style.STROKE);
        faceRectPaint.setColor(Color.RED);
        faceRectPaint.setStrokeWidth(2 * density);

        faceConvertPaint = new Paint(faceRectPaint);
        faceConvertPaint.setColor(Color.GREEN);

        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(12 * density);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.d("CameraActivity", "measure >> " + getMeasuredWidth() + ", " + getMeasuredHeight());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(face != null) {
            Rect bound = face.getBoundingBox();
            String strFaceBound = String.format(Locale.getDefault(), "Face Bound : [%d, %d, %d, %d]", bound.left, bound.top, bound.right, bound.bottom);
            String strFaceConv = String.format(Locale.getDefault(), "Face Conv : [%.2f, %.2f, %.2f, %.2f]", faceBound.left, faceBound.top, faceBound.right, faceBound.bottom);
            String strFaceAngle = String.format(Locale.getDefault(), "Face Angle : [%f, %f, %f]", face.getHeadEulerAngleX(), face.getHeadEulerAngleY(), face.getHeadEulerAngleZ());

            canvas.drawText(strFaceBound, 20, 50, textPaint);
            canvas.drawText(strFaceConv, 20, 110, textPaint);
            canvas.drawText(strFaceAngle, 20, 170, textPaint);
            canvas.drawText("rotate : " + rotate, 20, 230, textPaint);

            canvas.drawRect(targetRect, faceRectPaint);
            canvas.drawRect(faceBound, faceConvertPaint);

            if(face != null) {
                for(FaceContour faceContour : face.getAllContours()) {
                    for (PointF p : faceContour.getPoints()) {
                        drawCircle(canvas, p);
                    }
                }
            }

            canvas.drawCircle(0, 0, 10, faceRectPaint);
            canvas.drawCircle(1080, 2047, 10, faceRectPaint);
        }
    }

    private void drawCircle(Canvas canvas, int cx, int cy) {
        canvas.drawCircle(cx, cy, 5, faceRectPaint);
    }

    private void drawCircle(Canvas canvas, PointF p) {
        canvas.drawCircle(displaySize.x - p.x, p.y, 5, faceRectPaint);
    }

    private int rotate;
    private RectF faceBound;
    private RectF targetRect;

    public void setFace(Face face, RectF targetRect, int rotate) {
        this.face = face;
        this.rotate = rotate;

        Rect bound = face.getBoundingBox();
        faceBound = new RectF(
                displaySize.x - bound.left
                , bound.top
                , displaySize.x - bound.right
                , bound.bottom
        );

        this.targetRect = targetRect;

        invalidate();
    }
}
