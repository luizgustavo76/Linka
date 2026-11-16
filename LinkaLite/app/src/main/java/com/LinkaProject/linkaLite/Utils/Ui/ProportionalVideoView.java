package com.LinkaProject.linkaLite;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;

public class ProportionalVideoView extends VideoView {

    private int mVideoWidth = 0;
    private int mVideoHeight = 0;

    public ProportionalVideoView(Context context) {
        super(context);
    }

    public ProportionalVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProportionalVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setVideoSize(int width, int height) {
        this.mVideoWidth = width;
        this.mVideoHeight = height;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
        int height = getDefaultSize(mVideoHeight, heightMeasureSpec);

        if (mVideoWidth > 0 && mVideoHeight > 0) {
            int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
            int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
            int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

            if (widthSpecMode == MeasureSpec.EXACTLY && heightSpecMode == MeasureSpec.EXACTLY) {
                width = widthSpecSize;
                height = heightSpecSize;

                if (mVideoWidth * height < width * mVideoHeight) {
                    width = height * mVideoWidth / mVideoHeight;
                } else if (mVideoWidth * height > width * mVideoHeight) {
                    height = width * mVideoHeight / mVideoWidth;
                }
            } else if (widthSpecMode == MeasureSpec.EXACTLY) {
                // Caso padrão do feed: largura fixa (fill_parent), altura proporcional
                width = widthSpecSize;
                height = (width * mVideoHeight) / mVideoWidth;

                // Trava de segurança: limita a altura máxima a 1.35x da largura
                // para vídeos verticais 9:16 não tomarem a tela inteira em celulares pequenos
                int maxHeight = (int) (width * 1.35f);
                if (height > maxHeight) {
                    height = maxHeight;
                    width = (height * mVideoWidth) / mVideoHeight;
                }
            } else if (heightSpecMode == MeasureSpec.EXACTLY) {
                height = heightSpecSize;
                width = (height * mVideoWidth) / mVideoHeight;
            } else {
                width = mVideoWidth;
                height = mVideoHeight;
            }
        }

        setMeasuredDimension(width, height);
    }
}