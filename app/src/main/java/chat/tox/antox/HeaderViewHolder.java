package chat.tox.antox;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

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
