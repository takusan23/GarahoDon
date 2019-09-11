package io.github.takusan23.garahodon;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;

public class TimeLineListViewAdapter extends ArrayAdapter<ArrayList<String>> {

    private int resource;
    private Context context;
    private ArrayList<ArrayList<String>> listItem;
    private LayoutInflater layoutInflater;

    private SharedPreferences pref_setting;

    public TimeLineListViewAdapter(@NonNull Context context, int resource, ArrayList<ArrayList<String>> items) {
        super(context, resource);
        this.resource = resource;
        this.listItem = items;
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
    }


    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view;
        if (convertView != null) {
            view = convertView;
        } else {
            view = layoutInflater.inflate(resource, null);
        }

        pref_setting = PreferenceManager.getDefaultSharedPreferences(context);

        //値取り出す
        ArrayList<String> item = listItem.get(position);
        String toot = item.get(1);
        String name = item.get(2);
        String displayName = item.get(3);
        String id = item.get(4);
        String avatar = item.get(5);
        String type = item.get(6);

        //findViewById
        TextView accountTextView = view.findViewById(R.id.adapter_listview_account_textview);
        TextView contentTextView = view.findViewById(R.id.adapter_listview_content_textview);
        ImageView avatarImageView = view.findViewById(R.id.adapter_listview_avatar_imageview);

        //値設定
        accountTextView.setText(displayName + "@" + name + "  " + type);
        contentTextView.setText(HtmlCompat.fromHtml(toot, HtmlCompat.FROM_HTML_MODE_COMPACT));

        //アイコン
        //画像非表示？
        if (pref_setting.getBoolean("hide_image", false)) {
            //非表示
            ((LinearLayout)avatarImageView.getParent()).removeView(avatarImageView);
        } else {
            //表示
            Glide.with(avatarImageView)
                    .load(avatar)
                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(30))) //←この一行追加
                    .into(avatarImageView);
        }

        return view;
    }

    @Override
    public int getCount() {
        // texts 配列の要素数
        return listItem.size();
    }
}
