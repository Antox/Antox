package chat.tox.antox.utils

import java.util

import android.content.Context
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.{ArrayAdapter, ImageView, TextView}
import chat.tox.antox.R

class DrawerArrayAdapter(context: Context, resourceId: Int, initialItems: util.List[DrawerItem])
  extends ArrayAdapter[DrawerItem](context, resourceId, initialItems) {

  private val items: util.List[DrawerItem] = initialItems

  def getList: util.List[DrawerItem] = items

  override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
    var holder: ViewHolder = null
    val rowItem = getItem(position)
    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE).asInstanceOf[LayoutInflater]
    val newConvertView = inflater.inflate(R.layout.rowlayout_drawer, null)
    holder = new ViewHolder()
    holder.txtLabel = newConvertView.findViewById(R.id.textView).asInstanceOf[TextView]
    holder.imageView = newConvertView.findViewById(R.id.imageView).asInstanceOf[ImageView]
    holder.txtLabel.setText(rowItem.getLabel)
    holder.imageView.setBackgroundResource(rowItem.getResId)
    newConvertView.setTag(holder)
    newConvertView
  }

  private class ViewHolder {

    var txtLabel: TextView = _

    var imageView: ImageView = _
  }
}

