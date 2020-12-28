package com.ashlikun.refreshlayout;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ashlikun.swiperefreshlayout.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {
    SwipeRefreshLayout refreshLayout;
    FrameLayout frameLayout;
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
        frameLayout = findViewById(R.id.frameLayout);
        recyclerView = findViewById(R.id.recyclerView);
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setTotalDragDistance(200);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

//        adapter.notifyDataSetChanged();
        // WebView webView = findViewById(R.id.webView);
        // webView.getSettings().setJavaScriptEnabled(true);
//        webView.setWebViewClient(new WebViewClient() {
//
//            @Override
//            public boolean shouldOverrideUrlLoading(WebView view, String url) {
//                if (url.startsWith("http:") || url.startsWith("https:")) {
//                    view.loadUrl(url);
//                    return false;
//                } else {
////                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
////                    startActivity(intent);
//                    return true;
//                }
//            }
//        });
//        webView.loadUrl("http://www.baidu.com");
//        refreshLayout.setRefreshView(new NewVersionRefreshView(this),
//                new SwipeRefreshLayout.LayoutParams(SwipeRefreshLayout.LayoutParams.MATCH_PARENT,
//                        SwipeRefreshLayout.LayoutParams.WRAP_CONTENT));
//        refreshLayout.setRefreshStyle(SwipeRefreshLayout.NORMAL);
        refreshLayout.setColorSchemeColors(0xff234567, 0xff1129f9, 0xff345678);
//        refreshLayout.setRefreshing(true);
        final View view = findViewById(R.id.aaaaa);
        refreshLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
//                view.requestLayout();
//                Toast.makeText(MainActivity.this, "aaa", Toast.LENGTH_SHORT).show();
//                refreshLayout.postDelayed(this, 3000);
            }
        }, 200);
//        refreshLayout.setRefreshing(true);
    }

    @Override
    public void onRefresh() {
        getData();
    }

    public int getRandom() {
        int max = 30, min = 20;
        return (int) (Math.random() * (max - min) + min);
    }

    private void getData() {
        Log.e("getData", "isRefresh = " + refreshLayout.isRefreshing());
        //开始网络请求
        if (frameLayout.getVisibility() == View.GONE) {
            frameLayout.setVisibility(View.VISIBLE);
            refreshLayout.setRefreshing(false);
        }
        frameLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                //完成网络请求
                refreshLayout.setRefreshing(false);

            }
        }, getRandom());

    }

    public void onClick(View view) {
        Log.e("onClick", "isRefresh = " + refreshLayout.isRefreshing());
        if (frameLayout.getVisibility() == View.GONE) {
            frameLayout.setVisibility(View.VISIBLE);
        }
    }

    public void onClick2(View view) {
        Log.e("onClick2", "isRefresh = " + refreshLayout.isRefreshing());
        if (!refreshLayout.isRefreshing()) {
            frameLayout.setVisibility(View.VISIBLE);
            refreshLayout.setRefreshing(true);
            frameLayout.setVisibility(View.GONE);
        }
    }
}
