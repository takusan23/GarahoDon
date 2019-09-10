package io.github.takusan23.garahodon;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class TimeLineListViewAdapter extends ArrayAdapter<ArrayList<ArrayList<String>>> {

    private int resource;
    private ArrayList<ArrayList<String>> listItem;
    private LayoutInflater layoutInflater;

    public TimeLineListViewAdapter(@NonNull Context context, int resource, ArrayList<ArrayList<String>> items) {
        super(context, resource);
        this.resource = resource;
        this.listItem = items;
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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

        //値取り出す
        String toot = listItem.get(position).get(1);
        String name = listItem.get(position).get(2);
        String displayName = listItem.get(position).get(3);
        String id = listItem.get(position).get(4);
        String avatar = listItem.get(position).get(5);

        //findViewById
        TextView accountTextView = view.findViewById(R.id.adapter_listview_account_textview);
        TextView contentTextView = view.findViewById(R.id.adapter_listview_content_textview);

        //値設定
        accountTextView.setText(displayName + "@" + name);
        contentTextView.setText(toot);

        return view;
    }
}
