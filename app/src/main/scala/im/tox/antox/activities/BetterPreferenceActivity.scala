package im.tox.antox.activities

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceActivity
import android.support.v7.app.{ActionBar, AppCompatDelegate}
import android.support.v7.widget.Toolbar
import android.view.{ViewGroup, View, MenuInflater}

abstract class BetterPreferenceActivity extends PreferenceActivity with SharedPreferences.OnSharedPreferenceChangeListener {

  private var mDelegate: AppCompatDelegate = _

  override protected def onPostCreate(savedInstanceState: Bundle) {
    super.onPostCreate(savedInstanceState)
    getDelegate.onPostCreate(savedInstanceState)
  }

  def getSupportActionBar: ActionBar = {
    getDelegate.getSupportActionBar
  }

  def setSupportActionBar(toolbar: Toolbar) {
    getDelegate.setSupportActionBar(toolbar)
  }

  override def getMenuInflater: MenuInflater = {
    getDelegate.getMenuInflater
  }

  override def setContentView(layoutResID: Int) {
    getDelegate.setContentView(layoutResID)
  }

  override def setContentView(view: View) {
    getDelegate.setContentView(view)
  }

  override def setContentView(view: View, params: ViewGroup.LayoutParams) {
    getDelegate.setContentView(view, params)
  }

  override def addContentView(view: View, params: ViewGroup.LayoutParams) {
    getDelegate.addContentView(view, params)
  }

  override protected def onPostResume(): Unit = {
    super.onPostResume()
    getDelegate.onPostResume()
  }

  override protected def onTitleChanged(title: CharSequence, color: Int) {
    super.onTitleChanged(title, color)
    getDelegate.setTitle(title)
  }

  def onConfigurationChanged(newConfig: Nothing) {
    super.onConfigurationChanged(newConfig)
    getDelegate.onConfigurationChanged(newConfig)
  }

  override protected def onStop(): Unit = {
    super.onStop()
    getDelegate.onStop()
  }

  override protected def onDestroy() {
    super.onDestroy()
    getDelegate.onDestroy()
  }

  override def invalidateOptionsMenu(): Unit = {
    getDelegate.invalidateOptionsMenu()
  }

  def getDelegate: AppCompatDelegate = {
    if (mDelegate == null) {
      mDelegate = AppCompatDelegate.create(this, null)
    }

    mDelegate
  }
}
