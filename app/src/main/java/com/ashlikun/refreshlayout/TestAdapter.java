package com.ashlikun.refreshlayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

/**
 * 作者　　: 李坤
 * 创建时间:2017/5/3 0003　15:47
 * 邮箱　　：496546144@qq.com
 * <p>
 * 功能介绍：
 */

public class TestAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<String> list;

    public TestAdapter(List<String> list) {
        this.list = list;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_test,null);
        return new MyViewHolder(view);
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        public MyViewHolder(View itemView) {
            super(itemView);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
