package chat.tox.antox.fragments

import android.app.Activity
import android.content.Context
import android.content.res.{ColorStateList, TypedArray}
import android.graphics.drawable.shapes.OvalShape
import android.graphics.drawable.{Drawable, RippleDrawable, ShapeDrawable, StateListDrawable}
import android.os.Build
import android.support.v7.app.AlertDialog
import android.view.View.OnClickListener
import android.view.{View, ViewGroup}
import android.widget._
import chat.tox.antox.R
import chat.tox.antox.theme.ThemeManager

object ColorPickerDialog {

  trait Callback {
    def onColorSelection(index: Int, color: Int, darker: Int): Unit
  }

}

class ColorPickerDialog(activity: Activity, callback: ColorPickerDialog.Callback) {

  private var colors: Array[Int] = _

  def onClickColor(v: View) {
    if (v.getTag != null) {
      val index: Integer = v.getTag.asInstanceOf[Integer]
      callback.onColorSelection(index, colors(index), ThemeManager.darkenColor(colors(index)))
      close()
    }
  }

  private def setBackgroundCompat(view: View, d: Drawable) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      view.setBackground(d)
    } else {
      view.setBackgroundDrawable(d)
    }
  }

  var mDialog: Option[AlertDialog] = None

  def show(preselect: Option[Int]): Unit = {
    val inflator = activity.getLayoutInflater
    val view = inflator.inflate(R.layout.dialog_color_chooser, null)

    val rawColorArray: TypedArray = activity.getResources.obtainTypedArray(R.array.theme_colors)
    colors = new Array[Int](rawColorArray.length).indices.map(i => rawColorArray.getColor(i, 0)).toArray
    rawColorArray.recycle()

    val list: GridView = view.findViewById(R.id.color_grid).asInstanceOf[GridView]
    list.setAdapter(new ColorCircleAdapter(activity, colors, preselect))

    mDialog = Some(new AlertDialog.Builder(activity, R.style.AppCompatAlertDialogStyle)
      .setView(view).setTitle(R.string.dialog_color_picker_title).create())

    mDialog.foreach(dialog => {
      if (dialog.isShowing) close()
      dialog.show()
    })
  }

  def isShowing: Boolean = mDialog.exists(_.isShowing)

  def close(): Unit = {
    mDialog.foreach(_.cancel())
  }

  private class ColorCircleAdapter(context: Context, colors: Array[Int], preselect: Option[Int]) extends BaseAdapter {
    override def getCount: Int = colors.length

    override def getItemId(position: Int): Long = 0

    override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
      val child = activity.getLayoutInflater.inflate(R.layout.color_circle, parent, false).asInstanceOf[FrameLayout]
      val color = colors(position)

      child.setTag(position)
      child.setOnClickListener(new OnClickListener {
        override def onClick(v: View): Unit = {
          onClickColor(v)
        }
      })

      child.getChildAt(0).setVisibility(preselect match {
        case Some(selection) if selection == color => View.VISIBLE
        case _ => View.GONE
      })

      val selector: Drawable = createSelector(color)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val states: Array[Array[Int]] = Array[Array[Int]](Array[Int](-android.R.attr.state_pressed), Array[Int](android.R.attr.state_pressed))
        val colors: Array[Int] = Array[Int](ThemeManager.darkenColor(color), color)
        val rippleColors: ColorStateList = new ColorStateList(states, colors)
        setBackgroundCompat(child, new RippleDrawable(rippleColors, selector, null))
      } else {
        setBackgroundCompat(child, selector)
      }

      child
    }

    private def createSelector(color: Int): Drawable = {
      val coloredCircle: ShapeDrawable = new ShapeDrawable(new OvalShape)
      coloredCircle.getPaint.setColor(color)

      val darkerCircle: ShapeDrawable = new ShapeDrawable(new OvalShape)
      darkerCircle.getPaint.setColor(ThemeManager.darkenColor(color))

      val stateListDrawable: StateListDrawable = new StateListDrawable
      stateListDrawable.addState(Array[Int](-android.R.attr.state_pressed), coloredCircle)
      stateListDrawable.addState(Array[Int](android.R.attr.state_pressed), darkerCircle)
      stateListDrawable
    }

    override def getItem(position: Int): AnyRef = null
  }
}

