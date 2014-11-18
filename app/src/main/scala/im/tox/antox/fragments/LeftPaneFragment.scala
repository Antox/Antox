package im.tox.antox.fragments

import android.app.ActionBar
import android.app.FragmentTransaction
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v4.view.PagerTabStrip
import com.astuetz.PagerSlidingTabStrip
import com.balysv.materialripple.MaterialRippleLayout
import com.astuetz.PagerSlidingTabStrip.CustomTabProvider
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import im.tox.antox.R
import im.tox.antox.activities.MainActivity

class LeftPaneFragment extends Fragment {

  class LeftPagerAdapter(fm: FragmentManager) extends FragmentPagerAdapter(fm) with CustomTabProvider {

    val ICONS: Array[Int] = Array(R.drawable.ic_action_recent_tab, R.drawable.ic_action_contacts_tab)

    override def getCustomTabView(parent: ViewGroup, position: Int): View = {
         val materialRippleLayout: MaterialRippleLayout = LayoutInflater.from(getActivity)
            .inflate(R.layout.custom_tab, parent, false).asInstanceOf[MaterialRippleLayout]
         materialRippleLayout.findViewById(R.id.image).asInstanceOf[ImageView].setImageResource(ICONS(position))
         materialRippleLayout
    }

    override def getPageTitle(position: Int): CharSequence = {

      position match {
        case 0 => return "Recent"
        case _ => return "Contacts"
      }
/*
      val drawable: Drawable = getResources.getDrawable(drawableId)
      val sb: SpannableStringBuilder = new SpannableStringBuilder("")
      drawable.setBounds(0, 0, drawable.getIntrinsicWidth, drawable.getIntrinsicHeight)
      val span: ImageSpan = new ImageSpan(drawable, DynamicDrawableSpan.ALIGN_BASELINE)
      sb.setSpan(span, 0, 0, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
      sb
      */

      null
    }

    override def getItem(pos: Int): Fragment = pos match {
      case 0 => new RecentFragment()
      case _ => new ContactsFragment()
    }

    override def getCount(): Int = 2
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val thisActivity = this.getActivity.asInstanceOf[MainActivity]
    val actionBar = thisActivity.getActionBar
    val rootView = inflater.inflate(R.layout.fragment_leftpane, container, false)
    val pager = rootView.findViewById(R.id.pager).asInstanceOf[ViewPager]
    val tabs = rootView.findViewById(R.id.pager_tabs).asInstanceOf[PagerSlidingTabStrip]

    val tabListener = new ActionBar.TabListener() {
        def onTabSelected(tab: ActionBar.Tab, ft: FragmentTransaction) = {
          pager.setCurrentItem(tab.getPosition)
        }

        def onTabUnselected(tab: ActionBar.Tab, ft: FragmentTransaction) = {
        }

        def onTabReselected(tab: ActionBar.Tab, ft: FragmentTransaction) = {
        }
    }

    pager.setAdapter(new LeftPagerAdapter(getFragmentManager))
    tabs.setViewPager(pager)
    tabs.setElevation(10)
    

    rootView
  }
}
