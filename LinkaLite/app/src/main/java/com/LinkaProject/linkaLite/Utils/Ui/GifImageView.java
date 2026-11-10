package com.LinkaProject.linkaLite;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.widget.ImageView;

public class GifImageView extends ImageView {

    private Movie mMovie;
    private long mMovieStart;

    // 1. Construtor para criação programática em código Java
    public GifImageView(Context context) {
        super(context);
    }

    // 2. CONSTRUTOR OBRIGATÓRIO PARA O INFLATER DO XML (Resolve o crash do InflateException)
    public GifImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // 3. Construtor para suporte a estilos customizados no XML
    public GifImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setMovie(Movie movie) {
        this.mMovie = movie;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mMovie != null) {
            long now = SystemClock.uptimeMillis();
            if (mMovieStart == 0) {
                mMovieStart = now;
            }
            int dur = mMovie.duration();
            if (dur == 0) {
                dur = 1000;
            }
            int relTime = (int) ((now - mMovieStart) % dur);
            mMovie.setTime(relTime);
            mMovie.draw(canvas, 0, 0);
            invalidate();
        } else {
            super.onDraw(canvas);
        }
    }
}