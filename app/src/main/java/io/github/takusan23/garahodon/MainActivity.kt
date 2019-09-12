package io.github.takusan23.garahodon

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.menu.MenuBuilder
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_toot_layout.view.*
import kotlinx.android.synthetic.main.fragment_timeline_layout.*
import okhttp3.*
import org.json.JSONArray
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule

class MainActivity : AppCompatActivity() {

    //ListView
    lateinit var timeLineListViewAdapter: TimeLineListViewAdapter
    var listItem = arrayListOf<ArrayList<String>>()

    lateinit var pref_setting: SharedPreferences
    var token = "";
    var instance = "";

    var fragmentTimeLineName = "home"

    lateinit var timelineListViewAdapter: TimeLineFragmentPagerAdapter

    lateinit var menu: Menu

    //テンキー操作ガイド
    val keyGuideList = arrayListOf("発信キー：投稿", "１：更新", "２：お気に入り", "３：ブースト", "０：ストリーミング接続・切断")
    val timer = Timer()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //ログイン情報がないときはログインActivityを開く
        pref_setting = PreferenceManager.getDefaultSharedPreferences(this);
        token = pref_setting.getString("token", "") ?: ""
        instance = pref_setting.getString("instance", "") ?: ""

        if (token.isEmpty()) {
            //ログイン画面
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        } else {
            //ViewPagerせってい
            timelineListViewAdapter = TimeLineFragmentPagerAdapter(supportFragmentManager)
            mainactivity_viewpager.adapter = timelineListViewAdapter
            //TabLayoutとつなげる
            mainactivity_tablayout.setupWithViewPager(mainactivity_viewpager)

            //テンキー操作ガイドを定期的に切り替える
            var count = 0
            timer.schedule(0, 5000) {
                runOnUiThread {
                    timeline_listview_key_controll_guide?.text =
                        keyGuideList[count]
                    count += 1
                    //一周したら戻す
                    if (count == keyGuideList.size) {
                        count = 0
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_menu, menu)
        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }

        menu?.getItem(3)?.isChecked = pref_setting.getBoolean("hide_image", false)

        if (menu != null) {
            this.menu = menu
        }

        return super.onCreateOptionsMenu(menu);
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mainactivity_menu_toot -> {
                //投稿
                showTootDialog()
            }
            R.id.mainactivity_menu_streaming -> {
                val fragment =
                    supportFragmentManager.findFragmentByTag("android:switcher:" + mainactivity_viewpager.id + ":" + mainactivity_viewpager.currentItem)
                if (fragment is TimeLineFragment) {
                    //接続・切断
                    if (fragment.isConnectionStreaming()) {
                        fragment.closeStreaming()
                        //アイコン変更
                        item.icon = getDrawable(R.drawable.ic_flash_off)
                    } else {
                        fragment.setStreaming(fragmentTimeLineName)
                        //アイコン変更
                        item.icon = getDrawable(R.drawable.ic_flash_on)
                    }
                }
            }
            R.id.mainactivity_menu_refresh -> {
                //更新
                val fragment =
                    supportFragmentManager.findFragmentByTag("android:switcher:" + mainactivity_viewpager.id + ":" + mainactivity_viewpager.currentItem)
                if (fragment is TimeLineFragment) {
                    fragment.refreshTL()
                }
            }
            R.id.mainactivity_menu_hide_image -> {
                //画像非表示
                val editor = pref_setting.edit()
                if (item.isChecked == false) {
                    item.isChecked = true
                    editor.putBoolean("hide_image", true)
                    editor.apply()
                } else {
                    item.isChecked = false
                    editor.putBoolean("hide_image", false)
                    editor.apply()
                }
            }
            R.id.mainactivity_menu_hide_guide -> {
                //テンキー操作ガイド消す
                if (timeline_listview_key_controll_guide.visibility == GONE) {
                    timeline_listview_key_controll_guide.visibility = VISIBLE
                } else {
                    timeline_listview_key_controll_guide.visibility = GONE
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun showTootDialog() {
        println("おした")
        val layoutInflater = LayoutInflater.from(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_toot_layout, null);
        AlertDialog.Builder(this)
            .setTitle("投稿")
            .setView(dialogView)
            .setPositiveButton("投稿", DialogInterface.OnClickListener { dialogInterface, i ->
                //投稿ボタン押した
                postStatus(dialogView.dialog_toot_edittext.text.toString())
            })
            .setNegativeButton("キャンセル", DialogInterface.OnClickListener { dialogInterface, i ->
                //キャンセル

            }).create().show()
    }

    fun postStatus(status: String) {
        //投稿する
        val url = "https://${instance}/api/v1/statuses"
        val formBody = FormBody.Builder().add("access_token", token).add("status", status).build()
        val request = Request.Builder().url(url).post(formBody).build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                //成功した
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "問題が発生しました。", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    //成功した
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "投稿しました。", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    //成功した
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "問題が発生しました。", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    fun setStreamingMenuIcon(drawable: Drawable?) {
        menu.getItem(1).icon = drawable
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
    }
}
