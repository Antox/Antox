/**
 * Created by zoff99 on 28.01.2017.
 */

package chat.tox.antox;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build.VERSION;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;


public class RecyclerViewFastScroller extends LinearLayout
{
    private static final int BUBBLE_ANIMATION_DURATION = 100;
    public static final int TRACK_SNAP_RANGE = 5;

    private
    @NonNull
    TextView bubble;

    private
    @NonNull
    View handle;

    private
    @NonNull
    View fastscroller_handle_pane;

    private
    @Nullable
    RecyclerView recyclerView;

    private int height;
    private ObjectAnimator currentAnimator;

    private final RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener()
    {
        @Override
        public void onScrolled(final RecyclerView recyclerView, final int dx, final int dy)
        {
            if (handle.isSelected())
            {
                return;
            }
            computeBubbleAndHandlePosition();
        }
    };

    public RecyclerViewFastScroller(final Context context)
    {
        this(context, null);
    }

    public RecyclerViewFastScroller(final Context context, final AttributeSet attrs)
    {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setClipChildren(false);
        setScrollContainer(true);
        inflate(context, R.layout.recycler_view_fast_scroller, this);
        bubble = ViewUtil.findById(this, R.id.fastscroller_bubble);
        handle = ViewUtil.findById(this, R.id.fastscroller_handle);
        fastscroller_handle_pane = ViewUtil.findById(this, R.id.fastscroller_handle_pane);

        fastscroller_handle_pane.setOnTouchListener(new OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                final int action = event.getAction();
                switch (action)
                {
                    case MotionEvent.ACTION_DOWN:
                        if (event.getY() < ViewUtil.getY(handle) - handle.getPaddingTop() || event.getY() > ViewUtil.getY(handle) + handle.getHeight() + handle.getPaddingBottom())
                        {
                            return false;
                        }

                        if (currentAnimator != null)
                        {
                            currentAnimator.cancel();
                        }

                        if (bubble.getVisibility() != VISIBLE)
                        {
                            showBubble();
                        }

                        handle.setSelected(true);

                    case MotionEvent.ACTION_MOVE:
                        final float y = event.getY();
                        setBubbleAndHandlePosition(y / height);
                        setRecyclerViewPosition(y);
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        handle.setSelected(false);
                        hideBubble();
                        return true;

                }
                return false;
            }
        });

        TypedArray styledAttributes = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.RecyclerViewFastScroller, 0, 0);
        bubble.setTextSize(TypedValue.COMPLEX_UNIT_PX, styledAttributes.getDimension(R.styleable.RecyclerViewFastScroller_textSize, 0));
        styledAttributes.recycle();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);
        if (height != h)
        {
            height = h;
            computeBubbleAndHandlePosition();
        }
    }

    public void setRecyclerView(final @NonNull RecyclerView recyclerView)
    {
        if (this.recyclerView != null)
        {
            this.recyclerView.removeOnScrollListener(onScrollListener);
        }
        this.recyclerView = recyclerView;
        recyclerView.addOnScrollListener(onScrollListener);
        recyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener()
        {
            @Override
            public boolean onPreDraw()
            {
                recyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                if (handle.isSelected())
                {
                    return true;
                }
                computeBubbleAndHandlePosition();
                return true;
            }
        });
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();
        if (recyclerView != null)
        {
            recyclerView.removeOnScrollListener(onScrollListener);
        }
    }

    public float getHandleY()
    {
        return ViewUtil.getY(handle);
    }

    public int getHandleHeight()
    {
        return handle.getHeight();
    }

    public int getMyHeight()
    {
        return height;
    }

    private void setRecyclerViewPosition(float y)
    {
        if (recyclerView != null)
        {
            final int itemCount = recyclerView.getAdapter().getItemCount();
            float proportion;
            if (ViewUtil.getY(handle) == 0)
            {
                proportion = 0f;
            }
            else
            {
                if (ViewUtil.getY(handle) + handle.getHeight() >= height - TRACK_SNAP_RANGE)
                {
                    proportion = 1f;
                }
                else
                {
                    proportion = y / (float) height;
                }
            }

            final int targetPos = translatedChildPosition(Util.clamp((int) (proportion * (float) itemCount), 0, itemCount - 1));
            ((LinearLayoutManager) recyclerView.getLayoutManager()).scrollToPositionWithOffset(targetPos, 0);

            final CharSequence bubbleText = ((chat.tox.antox.adapters.ChatMessagesAdapter) recyclerView.getAdapter()).getBubbleText(targetPos);
            bubble.setText(bubbleText);
        }
    }

    private void setBubbleAndHandlePosition(float y)
    {
        final int handleHeight = handle.getHeight();
        final int bubbleHeight = bubble.getHeight();
        final int handleY = Util.clamp((int) ((height - handleHeight) * y), 0, height - handleHeight);
        ViewUtil.setY(handle, handleY);
        ViewUtil.setY(bubble, Util.clamp(handleY - bubbleHeight - bubble.getPaddingBottom() + handleHeight, 0, height - bubbleHeight));
    }

    private void computeBubbleAndHandlePosition()
    {
        if (recyclerView != null)
        {
            final int offset = recyclerView.computeVerticalScrollOffset();
            final int range = recyclerView.computeVerticalScrollRange();
            final int extent = recyclerView.computeVerticalScrollExtent();
            final int offsetRange = Math.max(range - extent, 1);
            setBubbleAndHandlePosition((float) Util.clamp(offset, 0, offsetRange) / offsetRange);
        }
    }

    @TargetApi(11)
    private void showBubble()
    {
        bubble.setVisibility(VISIBLE);
        if (VERSION.SDK_INT >= 11)
        {
            if (currentAnimator != null)
            {
                currentAnimator.cancel();
            }
            currentAnimator = ObjectAnimator.ofFloat(bubble, "alpha", 0f, 1f).setDuration(BUBBLE_ANIMATION_DURATION);
            currentAnimator.start();
        }
    }

    @TargetApi(11)
    private void hideBubble()
    {
        if (VERSION.SDK_INT >= 11)
        {
            if (currentAnimator != null)
            {
                currentAnimator.cancel();
            }
            currentAnimator = ObjectAnimator.ofFloat(bubble, "alpha", 1f, 0f).setDuration(BUBBLE_ANIMATION_DURATION);
            currentAnimator.addListener(new AnimatorListenerAdapter()
            {
                @Override
                public void onAnimationEnd(Animator animation)
                {
                    super.onAnimationEnd(animation);
                    bubble.setVisibility(INVISIBLE);
                    currentAnimator = null;
                }

                @Override
                public void onAnimationCancel(Animator animation)
                {
                    super.onAnimationCancel(animation);
                    bubble.setVisibility(INVISIBLE);
                    currentAnimator = null;
                }
            });
            currentAnimator.start();
        }
        else
        {
            bubble.setVisibility(INVISIBLE);
        }
    }

    private boolean isReverseLayout(final RecyclerView parent)
    {
        return (parent.getLayoutManager() instanceof LinearLayoutManager) && ((LinearLayoutManager) parent.getLayoutManager()).getReverseLayout();
    }

    public int translatedChildPosition(int position)
    {
        if (recyclerView == null)
        {
            return position;
        }
        return isReverseLayout(recyclerView) ? recyclerView.getAdapter().getItemCount() - 1 - position : position;
    }
}
