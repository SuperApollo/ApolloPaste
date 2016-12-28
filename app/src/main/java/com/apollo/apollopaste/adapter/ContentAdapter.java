package com.apollo.apollopaste.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.apollo.apollopaste.R;
import com.apollo.apollopaste.bean.ContentBean;

import java.util.List;

/**
 * Created by zayh_yf20160909 on 2016/12/28.
 */

public class ContentAdapter extends BaseAdapter {
    Context context;
    List<ContentBean> datas;
    LayoutInflater layoutInflater;

    public ContentAdapter(List<ContentBean> datas, Context context) {
        this.datas = datas;
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public List<ContentBean> getDatas() {
        return datas;
    }

    public void setDatas(List<ContentBean> datas) {
        this.datas = datas;
    }

    @Override
    public int getCount() {
        return datas.size();
    }

    @Override
    public Object getItem(int position) {
        return datas.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        ContentBean bean = (ContentBean) getItem(position);
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = layoutInflater.inflate(R.layout.item_content, null);
            holder.tv = (TextView) convertView.findViewById(R.id.tv_item_content);

            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.tv.setText(bean.getContent());

        return convertView;
    }

    class ViewHolder {
        TextView tv;
    }
}
