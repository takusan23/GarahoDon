package io.github.takusan23.garahodon

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

class MainActivity : AppCompatActivity() {

    //ListView
    lateinit var timeLineListViewAdapter: TimeLineListViewAdapter
    var listItem = arrayListOf<ArrayList<String>>()

    lateinit var pref_setting: SharedPreferences
    var token = "";
    var instance = "";

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //ログイン情報がないときはログインActivityを開く
        pref_setting = PreferenceManager.getDefaultSharedPreferences(this);
        token = pref_setting.getString("token", "") ?: ""
        instance = pref_setting.getString("instance", "") ?: ""

        //しょきか
        timeLineListViewAdapter =
            TimeLineListViewAdapter(this, R.layout.adapter_listview_layout, listItem)
        mainactivity_listview.adapter = timeLineListViewAdapter

        if (token.isEmpty()) {
            //ログイン画面
            var intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        } else {
            //タイムライン取得

        }
    }

    fun loadHomeTimeLine(url: String) {
        val url = "https://${instance}/api/v1/timelines/${url}?limit=40&access_token=${token}"
        val request = Request.Builder().url(url).get().build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    errorToast()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val jsonArray = JSONArray(response.body()?.string())
                    for(i in 0 until jsonArray.length()){
                        val jsonObject = jsonArray.getJSONObject(i);
                        val jsonAccountObject = jsonObject.getJSONObject("account");
                        var toot = jsonObject.getString("content")
                        var usernme = jsonAccountObject.getString("username")
                        var displayName = jsonAccountObject.getString("display_name")
                        var tootID = jsonObject.getString("id")
                        var avatar = jsonAccountObject.getString("avatar_static")

                        //配列に入れる
                        var list = arrayListOf<String>()
                        list.add("")
                        list.add(toot)
                        list.add(usernme)
                        list.add(displayName)
                        list.add(tootID)
                            list.add(avatar)
                        listItem.add(list);
                        timeLineListViewAdapter.notifyDataSetChanged()
                    }
                } else {
                    errorToast()
                }
            }
        })
    }

    fun errorToast() {
        Toast.makeText(this, "読み込みに失敗しました", Toast.LENGTH_SHORT).show()
    }


}
