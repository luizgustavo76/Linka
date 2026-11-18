package com.LinkaProject.linkaLite;

import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.ImageView;

public class UIAnimationHelper {
    public static void rotateWithWeight(final ImageView imageView) {
        if (imageView == null) return;

        RotateAnimation rotate = new RotateAnimation(
                0.0f, -1080.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );

        rotate.setDuration(2200);

        rotate.setInterpolator(new AnticipateOvershootInterpolator(2.0f));

        rotate.setFillAfter(true);
        imageView.startAnimation(rotate);
    }
}