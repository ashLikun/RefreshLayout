package com.ashlikun.refreshlayout;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import com.ashlikun.swiperefreshlayout.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {
    SwipeRefreshLayout refreshLayout;
    RecyclerView recyclerView;
    List<String> list = new ArrayList<>();
    TestAdapter adapter = new TestAdapter(list);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        for (int i = 0; i < 40; i++) {
            list.add(i + "");
        }
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefresh);
//        recyclerView = (RecyclerView) findViewById(R.id.recycle);
        refreshLayout.setOnRefreshListener(this);
        //refreshLayout.setTotalDragDistance(400);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//        recyclerView.setAdapter(adapter);
//        adapter.notifyDataSetChanged();
//        findViewById(R.id.aaaaa).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //  refreshLayout.setRefreshing(true);
//                refreshLayout.setRefreshView(new TestView(MainActivity.this)
//                        , new SwipeRefreshLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 120));
//                refreshLayout.setRefreshView(new JlhRefreshView(getApplication()), new SwipeRefreshLayout.LayoutParams(SwipeRefreshLayout.LayoutParams.MATCH_PARENT, SwipeRefreshLayout.LayoutParams.WRAP_CONTENT));
//            }
//        });
        refreshLayout.setRefreshView(new OgouRefreshView(this),
                new SwipeRefreshLayout.LayoutParams(SwipeRefreshLayout.LayoutParams.MATCH_PARENT,
                        SwipeRefreshLayout.LayoutParams.WRAP_CONTENT));
        refreshLayout.setRefreshStyle(SwipeRefreshLayout.PINNED);
//        refreshLayout.setRefreshing(true);
        final View view = findViewById(R.id.aaaaa);
        refreshLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                view.requestLayout();
                Toast.makeText(MainActivity.this, "aaa", Toast.LENGTH_SHORT).show();
                refreshLayout.postDelayed(this, 3000);
            }
        }, 3000);
    }

    @Override
    public void onRefresh() {
        refreshLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
//                adapter.notifyDataSetChanged();
                refreshLayout.setRefreshing(false);
            }
        }, 3000);
    }
}
