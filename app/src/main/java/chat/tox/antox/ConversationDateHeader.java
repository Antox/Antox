package chat.tox.antox;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;


public class ConversationDateHeader extends HeaderViewHolder
{

    private final Animation animateIn;
    private final Animation animateOut;

    private boolean pendingHide = false;
    TextView tv;

    public ConversationDateHeader(Context context, TextView tv2)
    {
        super((TextView) tv2);
        this.tv = tv2;
        this.animateIn = AnimationUtils.loadAnimation(context, R.anim.slide_from_top);
        this.animateOut = AnimationUtils.loadAnimation(context, R.anim.slide_to_top);

        this.animateIn.setDuration(100);
        this.animateOut.setDuration(100);
    }

    public void show()
    {
        if (pendingHide)
        {
            pendingHide = false;
        }
        else
        {
            ViewUtil.animateIn(this.tv, animateIn);
        }
    }

    public void hide()
    {
        pendingHide = true;
        this.tv.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                if (pendingHide)
                {
                    pendingHide = false;
                    ViewUtil.animateOut(tv, animateOut, View.GONE);
                }
            }
        }, 400);
    }


}
