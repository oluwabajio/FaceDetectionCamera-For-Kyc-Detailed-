package me.penguinpistol.facedetectioncamera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Size;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.face.FaceContour;

import java.util.ArrayList;
import java.util.List;

public class GraphicOverlay extends View {
    private static final String TAG = GraphicOverlay.class.getSimpleName();

    private final Point displaySize;

    private final Paint faceAreaPaint;
    private final Path faceAreaPath;

    private final RectF targetRectOrigin;
    private final RectF targetRectScaled;

    private final Drawable targetDrawable;

    private List<PointF> landmark;
    private Size imageSize;
    private PointF offset;
    private float scale;

    // 디버그용
    private final Paint debugPaint;
    private final Paint debugTextPaint;
    private RectF faceBound;
    private boolean isDebug;

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

        faceAreaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        faceAreaPaint.setStyle(Paint.Style.FILL);
        // TODO 오버레이 색상 레이아웃에서 변경할 수 있도록 수정필요
        faceAreaPaint.setColor(0x4D00FFFF);

        faceAreaPath = new Path();
        faceBound = new RectF();

        targetRectOrigin = new RectF();
        targetRectScaled = new RectF();

        // TODO 레이아웃에서 변경할 수 있도록 수정필요
        targetDrawable = ContextCompat.getDrawable(context, R.drawable.shape_face_detection_target);

        // 디버그 용 데이터
        debugPaint = new Paint();
        debugPaint.setStyle(Paint.Style.STROKE);
        debugPaint.setColor(Color.MAGENTA);
        debugPaint.setStrokeWidth(2 * density);

        debugTextPaint = new Paint(debugPaint);
        debugTextPaint.setStyle(Paint.Style.FILL);
        debugTextPaint.setTextSize(14 * density);

        isDebug = false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        float scaleX = (float)width / imageSize.getWidth();
        float scaleY = (float)height / imageSize.getHeight();
        float subScale = scaleY - scaleX;

        // 길이 변화가 더 큰 쪽에 맞춤
        scale = Math.max(scaleX, scaleY);

        // 화면 크기와 뷰 크기가 다른 경우 보정값
        float xDiff = ((displaySize.x - width) >> 1) * scale;
        float yDiff = ((displaySize.y - height) >> 1) * scale;

        // scaleX > scaleY
        if(subScale < 0) {
            offset.y = ((height - imageSize.getHeight()) >> 1) * subScale + yDiff;
        } else {
            offset.x = ((width - imageSize.getHeight()) >> 1) * subScale + xDiff;
        }

        targetRectScaled.set(0, 0, targetRectOrigin.width() * scale, targetRectOrigin.height() * scale);
        targetRectScaled.offset((width - targetRectScaled.width()) * 0.5f, (height - targetRectScaled.height()) * 0.5f);

        targetDrawable.setBounds(
                (int)targetRectScaled.left,
                (int)targetRectScaled.top,
                (int)targetRectScaled.right,
                (int)targetRectScaled.bottom
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(isDebug) {
            drawDebugInfo(canvas);
        }

        if(targetDrawable != null) {
            targetDrawable.draw(canvas);
        }

        if(landmark != null) {
            faceAreaPath.reset();
            for(int i = 0; i < landmark.size(); i++) {
                PointF p = landmark.get(i);
                if(i == 0) {
                    faceAreaPath.moveTo(p.x, p.y);
                } else {
                    faceAreaPath.lineTo(p.x, p.y);
                }
            }
            faceAreaPath.close();

            canvas.drawPath(faceAreaPath, faceAreaPaint);
        }
    }

    public void init(Size imageSize, RectF targetRect) {
        this.imageSize = new Size(imageSize.getWidth(), imageSize.getHeight());
        this.targetRectOrigin.set(targetRect);
        this.offset = new PointF();
        requestLayout();
    }

    public void setFaceContour(FaceContour contour) {
        List<PointF> points = contour.getPoints();
        faceBound = getFaceRect(points);

        landmark = new ArrayList<>();
        for (PointF p : points) {
            // 전면카메라의 경우 좌우반전
            p.offset(offset.x, offset.y);
            p.set(getMeasuredWidth() - (p.x * scale), p.y * scale);
            landmark.add(p);
        }

        invalidate();
    }

    //===============================================================================
    // 디버깅 관련
    //===============================================================================

    private RectF getFaceRect(List<PointF> landmarks) {
        return new RectF(
                (float)landmarks.stream().mapToDouble(x -> x.x).max().orElse(0),
                (float)landmarks.stream().mapToDouble(x -> x.y).min().orElse(0),
                (float)landmarks.stream().mapToDouble(x -> x.x).min().orElse(0),
                (float)landmarks.stream().mapToDouble(x -> x.y).max().orElse(0)
        );
    }

    private String debugText = "";

    public void setDebug(boolean debug) {
        isDebug = debug;
    }

    public void setDebugText(String text) {
        debugText = text;
    }

    private void drawDebugInfo(Canvas canvas) {
        float textSize = debugTextPaint.getTextSize() + 10;
        int line = 1;

        // 실제 계산 영역 출력
        debugPaint.setColor(Color.BLACK);
        debugTextPaint.setColor(debugPaint.getColor());
        canvas.drawText("*Analyzer Area", 10, textSize * line++, debugTextPaint);
        canvas.drawRect(0, 0, imageSize.getWidth(), imageSize.getHeight(), debugPaint);

        // 계산 얼굴 영역(좌우반전 x) 출력
        debugPaint.setColor(Color.MAGENTA);
        debugTextPaint.setColor(debugPaint.getColor());
        canvas.drawText("*Face Area Origin : " + faceBound.width(), 10, textSize * line++, debugTextPaint);
        canvas.drawRect(faceBound, debugPaint);

        // 계산 목표 영역 출력
        debugPaint.setColor(Color.GREEN);
        debugTextPaint.setColor(debugPaint.getColor());
        canvas.drawText("*Target Origin : " + targetRectOrigin.width(), 10, textSize * line++, debugTextPaint);
        canvas.drawRect(targetRectOrigin, debugPaint);

        // 그려지는 목표 영역 출력
        debugPaint.setColor(Color.BLUE);
        debugTextPaint.setColor(debugPaint.getColor());
        canvas.drawText("*Target", 10, textSize * line++, debugTextPaint);
        canvas.drawRect(targetRectScaled, debugPaint);

        // 디버그 텍스트 출력
        debugTextPaint.setColor(Color.RED);
        for(String s : debugText.split("\n")) {
            canvas.drawText(s, 10, textSize * line++, debugTextPaint);
        }
    }
}
