package dji.v5.ux.core.widget.fpv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class CenteredRectangleView extends View {

    private Paint paint;

    public CenteredRectangleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.parseColor("#F9AF1D")); // Alterado para ciano
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6); // Alterado para aumentar a largura da linha do retângulo
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int rectWidth = getWidth() / 2;
        int rectHeight = getHeight() / 2;

        int left = centerX - (rectWidth / 2);
        int top = centerY - (rectHeight / 2);
        int right = centerX + (rectWidth / 2);
        int bottom = centerY + (rectHeight / 2);

        canvas.drawRect(left, top, right, bottom, paint);
    }
}
