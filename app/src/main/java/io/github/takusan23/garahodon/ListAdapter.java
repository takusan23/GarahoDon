package io.github.takusan23.garahodon;

import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.text.HtmlCompat;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

public class ListAdapter extends ArrayAdapter<ListItem> {

    private int mResource;
    private ArrayList<ListItem> mItems;
    private LayoutInflater mInflater;

    private SharedPreferences pref_setting;

    private ArrayList<String> listItem;

    public ListAdapter(Context context, int resource, ArrayList<ListItem> items) {
        super(context, resource, items);
        mResource = resource;
        mItems = items;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView != null) {
            view = convertView;
        } else {
            view = mInflater.inflate(mResource, null);
        }

        //データを受け取る
        ListItem item = mItems.get(position);
        listItem = item.getList();
        String toot = listItem.get(1);
        String name = listItem.get(2);
        String displayName = listItem.get(3);
        String id = listItem.get(4);
        String avatar = listItem.get(5);
        String type = listItem.get(6);

        //findViewById
        TextView accountTextView = view.findViewById(R.id.adapter_listview_account_textview);
        TextView contentTextView = view.findViewById(R.id.adapter_listview_content_textview);
        ImageView avatarImageView = view.findViewById(R.id.adapter_listview_avatar_imageview);

        Context context = accountTextView.getContext();

        pref_setting = PreferenceManager.getDefaultSharedPreferences(context);


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

    public void addFirst(ListItem item) {
        ArrayList<ListItem> tmpItem = new ArrayList<ListItem>();
        tmpItem.add(item);
        for (int i = 0; i < mItems.size(); i++) {
            add(mItems.get(i));
        }
        notifyDataSetChanged();
    }

}