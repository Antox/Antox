package chat.tox.antox;

import android.os.Build;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * Created by al on 28.01.2017.
 */

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
}
