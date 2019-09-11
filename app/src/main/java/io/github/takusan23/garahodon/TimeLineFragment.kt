package io.github.takusan23.garahodon

import androidx.appcompat.app.AlertDialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.fragment_timeline_layout.*
import okhttp3.*
import org.json.JSONArray
import java.io.IOException
import android.widget.ArrayAdapter
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.lang.Exception
import java.net.URI


class TimeLineFragment : Fragment() {

    lateinit var timeLineListViewAdapter: TimeLineListViewAdapter
    var listItem = arrayListOf<ArrayList<String>>()

    lateinit var pref_setting: SharedPreferences
    var token = "";
    var instance = "";

    val arrayList = arrayListOf<ListItem>()
    lateinit var listAdapter: ListAdapter

    lateinit var webSocketClient: WebSocketClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_timeline_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //ログイン情報がないときはログインActivityを開く
        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
        token = pref_setting.getString("token", "") ?: ""
        instance = pref_setting.getString("instance", "") ?: ""

        //ListView
        listAdapter = ListAdapter(context, R.layout.adapter_listview_layout, arrayList)

        //TL読み込み
        refreshTL()


        //スワイプしたとき
        fragment_swipe.setOnRefreshListener {
            listItem.clear()
            refreshTL()
        }


        //Activityに今開いてるTL入れとく
        val mainActivity = context as MainActivity
        val url = arguments?.getString("url") ?: "home"
        mainActivity.fragmentTimeLineName = url

        //ListView押したときふぁぼれるようにする
        fragment_listview.setOnItemClickListener { adapterView, view, i, l ->
            val list = adapterView.getItemAtPosition(i) as ListItem
            val id = list.list.get(4)
            val type = list.list.get(0)
            if(type=="timeline") {
                //通知は押せないように
                AlertDialog.Builder(context as MainActivity).setTitle("Fav/BT")
                    .setPositiveButton(
                        "お気に入り",
                        DialogInterface.OnClickListener { dialogInterface, i ->
                            postStatus(id, "favourite", "お気に入りしました。")
                        })
                    .setNegativeButton(
                        "ブースト",
                        DialogInterface.OnClickListener { dialogInterface, i ->
                            postStatus(id, "reblog", "ブーストしました。")
                        })
                    .setNeutralButton(
                        "キャンセル",
                        DialogInterface.OnClickListener { dialogInterface, i ->
                        })
                    .show()
            }
        }
    }

    fun refreshTL() {
        //TL読み込み
        listAdapter.clear()
        val url = arguments?.getString("url") ?: "home"
        //通知と分ける
        if (!url.contains("notification")) {
            loadTimeLine(url)
        } else {
            loadNotification()
        }
    }

    fun loadTimeLine(url: String) {
        val url =
            "https://${instance}/api/v1/timelines/${url}&access_token=${token}"
        fragment_swipe.isRefreshing = true
        val request = Request.Builder().url(url).get().build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                errorToast()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val jsonArray = JSONArray(response.body?.string())
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i);
                        val jsonAccountObject = jsonObject.getJSONObject("account");
                        val toot = jsonObject.getString("content")
                        val usernme = jsonAccountObject.getString("username")
                        val displayName = jsonAccountObject.getString("display_name")
                        val tootID = jsonObject.getString("id")
                        val avatar = jsonAccountObject.getString("avatar_static")

                        //配列に入れる
                        val list = arrayListOf<String>()
                        list.add("timeline")
                        list.add(toot)
                        list.add(usernme)
                        list.add(displayName)
                        list.add(tootID)
                        list.add(avatar)
                        list.add("")
                        val listItem = ListItem(list)
                        activity?.runOnUiThread {
                            listAdapter.add(listItem)
                        }
                    }
                    activity?.runOnUiThread {
                        //ListView更新
                        fragment_swipe.isRefreshing = false
                        fragment_listview.adapter = listAdapter
                    }
                } else {
                    errorToast()
                }
            }
        })
    }

    fun loadNotification() {
        val url =
            "https://${instance}/api/v1/notifications?limit=40&access_token=${token}"
        fragment_swipe.isRefreshing = true
        val request = Request.Builder().url(url).get().build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                errorToast()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val jsonArray = JSONArray(response.body?.string())
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i);
                        var toot = ""
                        var tootID = ""
                        if (jsonObject.has("status")) {
                            val tootJsonObject = jsonObject.getJSONObject("status");
                            toot = tootJsonObject.getString("content")
                            tootID = tootJsonObject.getString("id")
                        }
                        val jsonAccountObject = jsonObject.getJSONObject("account");
                        val usernme = jsonAccountObject.getString("username")
                        val displayName = jsonAccountObject.getString("display_name")
                        val avatar = jsonAccountObject.getString("avatar_static")
                        val type = jsonObject.getString("type")
                        //配列に入れる
                        val list = arrayListOf<String>()
                        list.add("notification")
                        list.add(toot)
                        list.add(usernme)
                        list.add(displayName)
                        list.add(tootID)
                        list.add(avatar)
                        list.add(type)
                        val listItem = ListItem(list)
                        activity?.runOnUiThread {
                            listAdapter.add(listItem)
                        }
                    }
                    activity?.runOnUiThread {
                        fragment_swipe.isRefreshing = false
                        fragment_listview.adapter = listAdapter
                    }
                } else {
                    errorToast()
                }
            }
        })
    }


    fun errorToast() {
        activity?.runOnUiThread {
            Toast.makeText(context, "読み込みに失敗しました", Toast.LENGTH_SHORT).show()
        }
    }

    fun setStreaming(url: String) {
        //TL読み込み
        var isNotification = false
        var webSocketLink = "wss://${instance}/api/v1/streaming/&stream=user"
        if (url.contains("home")) {
            var webSocketLink =
                "wss://${instance}/api/v1/streaming/?stream=user&access_token=${token}"
        }
        if (url.contains("notification")) {
            var webSocketLink =
                "wss://${instance}/api/v1/streaming/?stream=user:notification&access_token=${token}"
            isNotification = true
        }
        if (url.contains("public") && url.contains("local=true")) {
            var webSocketLink =
                "wss://${instance}/api/v1/streaming/?stream=public:local"
        }
        if (url.contains("public")) {
            var webSocketLink =
                "wss://${instance}/api/v1/streaming/?stream=public"
        }
        val uri = URI("wss://best-friends.chat/api/v1/streaming/?stream=public")
        webSocketClient = object : WebSocketClient(uri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                activity?.runOnUiThread {
                    Toast.makeText(context, "ストリーミングに接続します", Toast.LENGTH_SHORT).show()
                }
                println("せつぞく！")
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                println("しゅうりょう " + reason)
            }

            override fun onMessage(message: String?) {
                if (!isNotification) {
                    val jsonObject = JSONObject(message)
                    //一回文字列として取得してから再度JSONObjectにする
                    val payload = jsonObject.getString("payload")
                    //updateのイベントだけ受け付ける
                    //長年悩んだトゥートが増えるバグは新しいトゥート以外の内容でもRecyclerViewの０番目を更新するやつ呼んでたのが原因
                    val event = jsonObject.getString("event")
                    if (event.contains("update")) {
                        val toot_jsonObject = JSONObject(payload)
                        //これでストリーミング有効・無効でもJSONパースになるので楽になる（？）
                        timelineJSONParse(toot_jsonObject, true)
                    }
                }
            }

            override fun onError(ex: Exception?) {
                println("えらー")
            }
        }

        webSocketClient.connect()

    }

    override fun onDestroy() {
        super.onDestroy()
        webSocketClient.close()
    }

    private fun timelineJSONParse(tootJsonobject: JSONObject, b: Boolean) {
        val jsonObject = tootJsonobject
        val jsonAccountObject = jsonObject.getJSONObject("account")
        val toot = jsonObject.getString("content")
        val usernme = jsonAccountObject.getString("username")
        val displayName = jsonAccountObject.getString("display_name")
        val tootID = jsonObject.getString("id")
        val avatar = jsonAccountObject.getString("avatar_static")

        //配列に入れる
        val list = arrayListOf<String>()
        list.add("")
        list.add(toot)
        list.add(usernme)
        list.add(displayName)
        list.add(tootID)
        list.add(avatar)
        list.add("")
        val listItem = ListItem(list)
        activity?.runOnUiThread {
            listAdapter.clear()
            val tmpList = arrayList
            for (item in tmpList) {
                listAdapter.add(item)
            }
            listAdapter.notifyDataSetChanged()
        }
    }

    /**
     * @oaram type favourite/reblog
     * */
    fun postStatus(id: String, type: String, successText: String) {
        //投稿する
        val url = "https://${instance}/api/v1/statuses/${id}/${type}"
        val formBody = FormBody.Builder().add("access_token", token).build()
        val request = Request.Builder().url(url).post(formBody).build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                //失敗
                activity?.runOnUiThread {
                    Toast.makeText(context, "問題が発生しました。", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    //成功した
                    activity?.runOnUiThread {
                        Toast.makeText(context, successText, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "問題が発生しました。", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }


}