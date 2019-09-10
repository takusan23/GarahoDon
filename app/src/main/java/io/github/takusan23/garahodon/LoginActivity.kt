package io.github.takusan23.garahodon

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_login.*
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    lateinit var pref_setting: SharedPreferences

    var client_id = ""
    var client_secret = ""
    var redirect_url = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        pref_setting = PreferenceManager.getDefaultSharedPreferences(this)

        login_activity_login_button.setOnClickListener {
            getClientID()
        }

    }

    /*
    * ClientID / ClientSecret 取得
    * */
    fun getClientID() {
        val url =
            "https://" + login_activity_instance_name_edittext.text.toString() + "/api/v1/apps"
        //OkHttp
        //ぱらめーたー
        val requestBody = FormBody.Builder()
            .add("client_name", login_activity_client_name_edittext.text.toString())
            .add("redirect_uris", "https://takusan23.github.io/Kaisendon-Callback-Website/")
            .add("scopes", "read write follow")
            .add(
                "website",
                "https://play.google.com/store/apps/details?id=io.github.takusan23.kaisendon"
            )
            .build()
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                errorToast()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val response_string = response.body?.string()
                    try {
                        val jsonObject = JSONObject(response_string)
                        //ぱーす
                        client_id = jsonObject.getString("client_id")
                        client_secret = jsonObject.getString("client_secret")
                        redirect_url = jsonObject.getString("redirect_uri")
                        //アクセストークン取得のときにActivity再起動されるので保存しておく
                        val editor = pref_setting.edit()
                        editor.putString("client_id", client_id)
                        editor.putString("client_secret", client_secret)
                        editor.putString("redirect_uri", redirect_url)
                        //リダイレクト時にインスタンス名飛ぶので保存
                        editor.putString(
                            "register_instance",
                            login_activity_instance_name_edittext.text.toString()
                        )
                        editor.apply()
                        //Step2:認証画面を表示させる
                        showApplicationRequest()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                } else {
                    errorToast()
                }
            }
        })
    }

    /**
     * 認証画面表示
     */
    fun showApplicationRequest() {
        //PINを生成する
        val url =
            Uri.parse("https://" + login_activity_instance_name_edittext.text.toString() + "/oauth/authorize?client_id=" + client_id + "&redirect_uri=" + redirect_url + "&response_type=code&scope=read%20write%20follow")
        //移動
        val intent = Intent(Intent.ACTION_VIEW, url)
        this@LoginActivity.startActivity(intent)
    }

    /**
     * アクセストークン取得
     */
    override fun onResume() {
        super.onResume()
        //認証最後の仕事、アクセストークン取得
        //URLスキーマからの起動のときの処理
        if (intent.data != null) {
            //code URLパース
            val code = intent.data!!.getQueryParameter("code") ?: ""
            //Step3:アクセストークン取得
            getAccessToken(code)
        }
    }

    fun getAccessToken(code: String) {
        val url = "https://" + pref_setting.getString("register_instance", "") + "/oauth/token"
        //OkHttp
        //ぱらめーたー
        val requestBody = FormBody.Builder()
            .add("client_id", pref_setting.getString("client_id", "")!!)
            .add("client_secret", pref_setting.getString("client_secret", "")!!)
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", pref_setting.getString("redirect_uri", "")!!)
            .build()
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                errorToast()
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    //エラー
                    //失敗
                    errorToast()
                } else {
                    //成功
                    val response_string = response.body?.string()
                    try {
                        val jsonObject = JSONObject(response_string)
                        val access_token = jsonObject.getString("access_token")
                        //保存
                        val editor = pref_setting.edit()
                        editor.putString("token", access_token)
                        editor.putString(
                            "instance",
                            pref_setting.getString("register_instance", "")
                        )
                        editor.apply()
                        //Activity移動
                        val mainActivity = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(mainActivity);
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                }
            }
        })
    }


    fun errorToast() {
        runOnUiThread {
            Toast.makeText(this, "読み込みに失敗しました", Toast.LENGTH_SHORT).show()
        }
    }

}
