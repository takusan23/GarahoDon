package io.github.takusan23.garahodon

import androidx.appcompat.app.AlertDialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.fragment_timeline_layout.*
import okhttp3.*
import org.json.JSONArray
import java.io.IOException
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.lang.Exception
import java.net.URI
import kotlin.math.max


class TimeLineFragment : Fragment() {

    lateinit var timeLineListViewAdapter: TimeLineListViewAdapter
    var listItem = arrayListOf<ArrayList<String>>()

    lateinit var pref_setting: SharedPreferences
    var token = "";
    var instance = "";

    var maxId = ""
    var isNextTimeLineLoad = false

    val arrayList = arrayListOf<ListItem>()
    lateinit var listAdapter: ListAdapter

    lateinit var webSocketClient: WebSocketClient

    //ListViewの位置保存して更新してもそのままでいられるようにする
    var listViewPos = 0
    var listViewPosY = 0

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
            val jsonString = list.list.get(1)
            var notification = false
            if (listItem[0].contains("notification")) {
                notification = true
            }
            val parser = MastodonTimelineAPIParser(jsonString,notification)
            val id = parser.tootID
            val type = list.list.get(0)
            if (type == "timeline") {
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

        //追加読み込みに対応させる
        getNextTimeline()
    }

    fun getNextTimeline() {
        fragment_listview.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(p0: AbsListView?, p1: Int) {

            }

            override fun onScroll(p0: AbsListView?, p1: Int, p2: Int, p3: Int) {
                if (p1 + p2 == p3 && !isNextTimeLineLoad) {
                    //ListViewの位置を保存
                    listViewPos = fragment_listview.firstVisiblePosition
                    listViewPosY = fragment_listview.getChildAt(0).top
                    //TL追加読み込み
                    val url = arguments?.getString("url") ?: "home"
                    //通知と分ける
                    if (!url.contains("notification")) {
                        loadTimeLine(url, maxId)
                    } else {
                        loadNotification(maxId)
                    }
                    isNextTimeLineLoad = true
                }
            }
        })
    }

    fun refreshTL() {
        //TL読み込み
        listAdapter.clear()
        val url = arguments?.getString("url") ?: "home"
        //通知と分ける
        if (!url.contains("notification")) {
            loadTimeLine(url, null)
        } else {
            loadNotification(null)
        }
    }

    /**
     * @param maxid 追加読み込み時に利用してね。使わないときはnullでいいよ
     * */
    fun loadTimeLine(url: String, maxid: String?) {
        var url =
            "https://${instance}/api/v1/timelines/${url}&access_token=${token}&limit=40"
        if (maxid != null) {
            url += "&max_id=${maxid}"
        }
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
                        list.add(jsonArray.getJSONObject(i).toString())
                        list.add(usernme)
                        list.add(displayName)
                        list.add(tootID)
                        list.add(avatar)
                        list.add("")
                        val listItem = ListItem(list)
                        activity?.runOnUiThread {
                            if (maxid != null) {
                                listAdapter.insert(listItem, listAdapter.count)
                            } else {
                                listAdapter.add(listItem)
                            }
                        }
                    }
                    //追加読み込み用にID控える
                    maxId = jsonArray.getJSONObject(jsonArray.length() - 1).getString("id")
                    if (maxid != null) {
                        isNextTimeLineLoad = false
                    }
                    //ListView更新
                    activity?.runOnUiThread {
                        fragment_swipe?.isRefreshing = false
                        fragment_listview?.adapter = listAdapter
                        //追加読み込み時だけListViewの位置を設定する
                        if (maxid != null) {
                            fragment_listview.setSelectionFromTop(listViewPos, listViewPosY)
                        }
                    }
                } else {
                    errorToast()
                }
            }
        })
    }

    fun loadNotification(max_id: String?) {
        var url =
            "https://${instance}/api/v1/notifications?limit=40&access_token=${token}"
        if (max_id != null) {
            url += "&max_id=${max_id}"
        }
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
                        list.add(jsonArray.getJSONObject(i).toString())
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
                    //追加読み込み用にID控える
                    maxId = jsonArray.getJSONObject(jsonArray.length() - 1).getString("id")
                    if (max_id != null) {
                        isNextTimeLineLoad = false
                    }
                    activity?.runOnUiThread {
                        fragment_swipe.isRefreshing = false
                        fragment_listview.adapter = listAdapter
                        //追加読み込み時だけListViewの位置を設定する
                        if (max_id != null) {
                            fragment_listview.setSelectionFromTop(listViewPos, listViewPosY)
                        }
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

    fun closeStreaming() {
        //Streaming接続を切断する
        if (this@TimeLineFragment::webSocketClient.isInitialized) {
            //初期化済みかチェック
            if (!webSocketClient.isClosed) {
                webSocketClient.close()
                activity?.runOnUiThread {
                    Toast.makeText(context, "ストリーミングを切断しました", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun isConnectionStreaming(): Boolean {
        if (this@TimeLineFragment::webSocketClient.isInitialized) {
            //初期化済みかチェック
            if (!webSocketClient.isClosed) {
                return true
            }
        }
        return false
    }

    fun setStreaming(url: String) {
        //TL読み込み
        var isNotification = false
        var webSocketLink = "wss://${instance}/api/v1/streaming/?stream=public:local"
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
        val uri = URI(webSocketLink)
        webSocketClient = object : WebSocketClient(uri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                activity?.runOnUiThread {
                    Toast.makeText(context, "ストリーミングに接続します", Toast.LENGTH_SHORT).show()
                }
                println("せつぞく！")
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                println("しゅうりょう $reason")
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
                } else {
                    val jsonObject = JSONObject(message)
                    val payload = jsonObject.getString("payload")
                    val toot_text_jsonObject = JSONObject(payload)
                    notificationJSONPase(toot_text_jsonObject, true)
                }
            }

            override fun onError(ex: Exception?) {
                println("えらー")
            }
        }

        webSocketClient.connect()

    }

    private fun notificationJSONPase(toot_text_jsonObject: JSONObject, b: Boolean) {
        val jsonObject = toot_text_jsonObject
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
        list.add(toot_text_jsonObject.toString())
        list.add(usernme)
        list.add(displayName)
        list.add(tootID)
        list.add(avatar)
        list.add(type)
        val listItem = ListItem(list)
        activity?.runOnUiThread {
            listAdapter.insert(listItem, 0)
            listAdapter.notifyDataSetChanged()
        }
    }

    override fun onPause() {
        super.onPause()
        if (this@TimeLineFragment::webSocketClient.isInitialized) {
            //初期化済みならWebSocket閉じる
            webSocketClient.close()
            (activity as MainActivity).setStreamingMenuIcon(context?.getDrawable(R.drawable.ic_flash_off))
        }
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
        list.add("timeline")
        list.add(tootJsonobject.toString())
        list.add(usernme)
        list.add(displayName)
        list.add(tootID)
        list.add(avatar)
        list.add("")
        val listItem = ListItem(list)
        activity?.runOnUiThread {
            listAdapter.insert(listItem, 0)
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