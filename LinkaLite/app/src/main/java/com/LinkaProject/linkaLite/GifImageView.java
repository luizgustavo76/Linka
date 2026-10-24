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

    public GifImageView(Context context) {
        super(context);
    }

    public GifImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GifImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setMovie(Movie movie) {
        this.mMovie = movie;
        this.mMovieStart = 0;
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

            int duration = mMovie.duration();
            if (duration == 0) {
                duration = 1000;
            }

            int relTime = (int) ((now - mMovieStart) % duration);
            mMovie.setTime(relTime);

            mMovie.draw(canvas, 0, 0);
            invalidate();
        } else {
            super.onDraw(canvas);
        }
    }
}