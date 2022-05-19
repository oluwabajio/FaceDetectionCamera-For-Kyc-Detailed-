package me.penguinpistol.facedetectioncamera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;

import java.util.ArrayList;
import java.util.List;

public class GraphicOverlay extends View {
    private static final String TAG = GraphicOverlay.class.getSimpleName();

    private final Point displaySize;
    private final Paint faceRectPaint;
    private final Paint faceConvertPaint;
    private final TextPaint textPaint;

    public GraphicOverlay(Context context) {
        this(context, null, 0);
    }

    public GraphicOverlay(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GraphicOverlay(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
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

        Log.i(TAG, "setImageSize: " + getMeasuredWidth() + ", " + getMeasuredHeight());

        float scaleX = (float)getMeasuredWidth() / imageSize.getWidth();
        float scaleY = (float)getMeasuredHeight() / imageSize.getHeight();
        float subScale = Math.abs(scaleX - scaleY);

        if(scaleX > scaleY) {
            scale = scaleX;
            offset.y = -((getMeasuredHeight() - imageSize.getHeight()) >> 1) * subScale;
        } else {
            scale = scaleY;
            offset.x = ((getMeasuredWidth() - imageSize.getHeight()) >> 1) * subScale;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(landmark != null) {
            canvas.drawRect(getFaceRect(landmark), faceConvertPaint);

            for(PointF p : landmark) {
                canvas.drawCircle(p.x, p.y, 2, faceRectPaint);
            }
        }
    }

    private List<PointF> landmark;
    private Size imageSize;
    private float scale;
    private PointF offset;

    public void setImageSize(Size imageSize) {
        this.imageSize = new Size(imageSize.getWidth(), imageSize.getHeight());
        this.offset = new PointF();
        requestLayout();
    }

    public void setFace(FaceContour contour) {
        this.landmark = new ArrayList<>();
        List<PointF> points = contour.getPoints();

        for (PointF p : points) {
            // 전면카메라의 경우 좌우반전
            p.offset(offset.x, offset.y);
            p.set(displaySize.x - (p.x * scale), p.y * scale);
            landmark.add(p);
        }

        invalidate();
    }

    private RectF getFaceRect(List<PointF> landmarks) {
        return new RectF(
                (float)landmarks.stream().mapToDouble(x -> x.x).max().orElse(0),
                (float)landmarks.stream().mapToDouble(x -> x.y).min().orElse(0),
                (float)landmarks.stream().mapToDouble(x -> x.x).min().orElse(0),
                (float)landmarks.stream().mapToDouble(x -> x.y).max().orElse(0)
        );
    }
}
