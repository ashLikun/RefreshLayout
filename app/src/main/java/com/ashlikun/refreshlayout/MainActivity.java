package com.ashlikun.refreshlayout;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;

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
//        recyclerView = (RecyclerView) findViewById(R.id.recycle);
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefresh);
        refreshLayout.setOnRefreshListener(this);
        //refreshLayout.setTotalDragDistance(400);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//        recyclerView.setAdapter(adapter);
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
        refreshLayout.setRefreshStyle(SwipeRefreshLayout.FLOAT);
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
        }, 1000);
//        refreshLayout.setRefreshing(true);
    }

    @Override
    public void onRefresh() {
        refreshLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
//                adapter.notifyDataSetChanged();
                refreshLayout.setRefreshing(false);
            }
        }, 700);
    }

    public void onClick(View view) {
        refreshLayout.setRefreshing(true);
        refreshLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshLayout.setRefreshing(false);
            }
        }, 15);
    }
}
