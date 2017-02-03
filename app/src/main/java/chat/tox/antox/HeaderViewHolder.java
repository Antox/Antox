package chat.tox.antox;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

/**
 * Created by zoff99 on 28.01.2017.
 */

public class HeaderViewHolder extends RecyclerView.ViewHolder
{
    protected TextView textView;

    public HeaderViewHolder(View itemView)
    {
        super(itemView);
        textView = ViewUtil.findById(itemView, R.id.text);
    }

    public HeaderViewHolder(TextView textView)
    {
        super(textView);
        this.textView = textView;
    }

    public void setText(CharSequence text)
    {
        textView.setText(text);
    }
}
