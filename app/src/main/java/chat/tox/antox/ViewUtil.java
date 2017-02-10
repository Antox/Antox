package chat.tox.antox;

import android.os.Build;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.LinearLayout;

public class ViewUtil
{
    public static void setY(final @NonNull View v, final int y)
    {
        if (Build.VERSION.SDK_INT >= 11)
        {
            ViewCompat.setY(v, y);
        }
        else
        {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.topMargin = y;
            v.setLayoutParams(params);
        }
    }

    public static float getY(final @NonNull View v)
    {
        if (Build.VERSION.SDK_INT >= 11)
        {
            return ViewCompat.getY(v);
        }
        else
        {
            return ((ViewGroup.MarginLayoutParams) v.getLayoutParams()).topMargin;
        }
    }

    public static float getX(final @NonNull View v)
    {
        if (Build.VERSION.SDK_INT >= 11)
        {
            return ViewCompat.getX(v);
        }
        else
        {
            return ((LinearLayout.LayoutParams) v.getLayoutParams()).leftMargin;
        }
    }


    @SuppressWarnings("unchecked")
    public static <T extends View> T findById(@NonNull View parent, @IdRes int resId)
    {
        return (T) parent.findViewById(resId);
    }

    public static void animateIn(final @NonNull View view, final @NonNull Animation animation)
    {
        if (view.getVisibility() == View.VISIBLE)
        {
            return;
        }

        view.clearAnimation();
        animation.reset();
        animation.setStartTime(0);
        view.setVisibility(View.VISIBLE);
        view.startAnimation(animation);
    }


    public static void animateOut(final @NonNull View view, final @NonNull Animation animation, final int visibility)
    {
        if (view.getVisibility() == visibility)
        {
            // future.set(true);
        }
        else
        {
            view.clearAnimation();
            animation.reset();
            animation.setStartTime(0);
            animation.setAnimationListener(new Animation.AnimationListener()
            {
                @Override
                public void onAnimationStart(Animation animation)
                {
                }

                @Override
                public void onAnimationRepeat(Animation animation)
                {
                }

                @Override
                public void onAnimationEnd(Animation animation)
                {
                    view.setVisibility(visibility);
                    // future.set(true);
                }
            });
            view.startAnimation(animation);
        }
    }
}
