package com.apollo.apollopaste;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView mTvShow;
    private EditText mEtIp;
    private EditText mEtPort;
    private Button mBtnStart;
    private EditText mEtTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

    private void initView() {
        mTvShow = (TextView) findViewById(R.id.tv_main_show);
        mEtIp = (EditText) findViewById(R.id.et_main_ip);
        mEtPort = (EditText) findViewById(R.id.et_main_port);
        mBtnStart = (Button) findViewById(R.id.btn_main_start);
        mEtTest = (EditText) findViewById(R.id.et_main_test);

        mBtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSocket();
            }
        });
    }

    private void startSocket() {

    }
}
