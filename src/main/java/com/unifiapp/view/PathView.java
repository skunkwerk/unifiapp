package com.unifiapp.view;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ComposePathEffect;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PathMeasure;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.unifiapp.R;

public class PathView extends View
{
    Path global_path;
    Paint paint;
    float length;

    public PathView(Context context)
    {
        super(context);
    }

    public PathView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public PathView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    public void init(Path path)
    {
        paint = new Paint();
        paint.setColor(getResources().getColor(R.color.wifi_blue));
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.STROKE);

        global_path = path;
        Log.d("pathview","isEmpty:" + String.valueOf(path.isEmpty()));

        // Measure the path
        PathMeasure measure = new PathMeasure(path, false);
        length = measure.getLength();
        Log.d("pathview","length:" + String.valueOf(length));

        float[] intervals = new float[]{length, length};

        ObjectAnimator animator = ObjectAnimator.ofFloat(PathView.this, "phase", 1.0f, 0.0f);
        animator.setDuration(3000);
        animator.start();
    }

    //is called by animator object
    public void setPhase(float phase)
    {
        Log.d("pathview","setPhase called with:" + String.valueOf(phase));
        paint.setPathEffect(createPathEffect(length, phase, 0.0f));
        invalidate();//will calll onDraw
    }

    private static PathEffect createPathEffect(float pathLength, float phase, float offset)
    {
        return new ComposePathEffect(new CornerPathEffect(15.0f), new DashPathEffect(new float[] { pathLength, pathLength },
                Math.max(phase * pathLength, offset)));
    }

    @Override
    public void onDraw(Canvas c)
    {
        super.onDraw(c);
        //initially, onDraw will be called before init (as init needs to know the dimensions)
        if(global_path!=null && paint!=null)
        {
            c.drawPath(global_path, paint);
        }
    }
}
