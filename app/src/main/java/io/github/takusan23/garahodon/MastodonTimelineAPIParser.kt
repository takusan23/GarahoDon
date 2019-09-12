package io.github.takusan23.garahodon

import org.json.JSONObject

/**
 * 二個目の引数は通知をパースするときにtrueにしてね
 * */
class MastodonTimelineAPIParser(jsonString: String, notification: Boolean) {

    var tootContent = ""
    var tootID = ""
    var createAt = ""
    var client = ""
    var displayName = ""
    var userName = ""
    var avatar = ""
    var notificationType = ""

    init {
        val jsonObject = JSONObject(jsonString)
        if (!notification) {
            //タイムラインのJSONパース
            val jsonAccountObject = jsonObject.getJSONObject("account")
            tootContent = jsonObject.getString("content")
            userName = jsonAccountObject.getString("username")
            displayName = jsonAccountObject.getString("display_name")
            tootID = jsonObject.getString("id")
            avatar = jsonAccountObject.getString("avatar_static")
        } else {
            //通知のパース
            if (jsonObject.has("status")) {
                //フォローの通知にはstatusのオブジェクトはない
                val tootJsonObject = jsonObject.getJSONObject("status");
                tootContent = tootJsonObject.getString("content")
                tootID = tootJsonObject.getString("id")
            }
            val jsonAccountObject = jsonObject.getJSONObject("account");
            userName = jsonAccountObject.getString("username")
            displayName = jsonAccountObject.getString("display_name")
            avatar = jsonAccountObject.getString("avatar_static")
            notificationType = jsonObject.getString("type")
        }
    }

    fun getNotificationType(type: String): String {
        when (type) {
            "favourite" -> return "お気に入り"
            "reblog" -> return "ブースト"
            "follow" -> return "フォロー"
        }
        return "通知"
    }
}