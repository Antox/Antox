package im.tox.antox.utils

import java.util

import android.content.Context
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.{ArrayAdapter, ImageView, TextView}
import im.tox.antoxnightly.R

class DrawerArrayAdapter(context: Context, resourceId: Int, items: util.List[DrawerItem])
  extends ArrayAdapter[DrawerItem](context, resourceId, items) {

  private var mItems: util.List[DrawerItem] = items

  def getList: util.List[DrawerItem] = mItems

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

