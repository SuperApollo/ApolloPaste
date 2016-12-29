package com.apollo.apollopaste.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.apollo.apollopaste.R;
import com.apollo.apollopaste.adapter.ContentAdapter;
import com.apollo.apollopaste.base.BaseApplication;
import com.apollo.apollopaste.bean.ContentBean;
import com.apollo.apollopaste.constants.AppConfig;
import com.apollo.apollopaste.eventbean.ClientMessage;
import com.apollo.apollopaste.service.SocketService;
import com.apollo.apollopaste.utils.AppUtil;
import com.apollo.apollopaste.utils.SharedPreferencesUtils;
import com.apollo.apollopaste.utils.ToastUtils;
import com.apollo.apollopaste.widgets.MyListview;
import com.apollo.apollopaste.widgets.MyScrollView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;

public class MainActivity extends Activity {
    private TextView mTvShow;
    private EditText mEtIp;
    private EditText mEtPort;
    private Button mBtnStart;
    private EditText mEtSend;
    private Button mBtnSend;
    private Context mContext;
    private ToastUtils mToastUtils;
    private boolean mThreadStart;
    private Handler mHandler;
    private final int SOCKET_CONNECTED = 10086;
    private final int SOCKET_DISCONNECTED = 10087;
    private final int RECEIVE_CLIENT_MSG = 10088;
    private final int SOCET_ERR = 10089;
    private final int START_SERVICE = 10090;
    private static final int SOCKET_ENTER = 10091;
    private final int RECEIVE_SERVER_MSG = 10092;
    private final int COPY_TO_BOARD = 10093;
    private boolean stop;
    private final String TAG = MainActivity.class.getSimpleName();
    private MyListview mLvContent;
    private Button mBtnConnect;
    private Socket mSocketClient;
    private boolean mConnectToServer;
    private MyScrollView mScroll;
    private List<ContentBean> mDatas = new ArrayList<>();
    private ContentAdapter mAdapter;
    private boolean isServerOn;
    private ExecutorService mExecutor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);//注册eventbus
        mContext = BaseApplication.getContext();
        mToastUtils = ToastUtils.shareInstance();
        initView();
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case SOCKET_CONNECTED:
                        mToastUtils.show(mContext, "连接成功！");
                        mConnectToServer = true;
                        mBtnConnect.setText("断开连接");

                        break;
                    case SOCKET_DISCONNECTED:
                        mToastUtils.show(mContext, "连接失败！");
                        break;
                    case RECEIVE_CLIENT_MSG://收到客户端消息
                        String receiveStr = (String) msg.obj;
//                        String preStr = mLvContent.getText().toString();
//                        mLvContent.setText(preStr + "\n" + receiveStr);
                        //滚动到最后文字
                        mDatas.add(new ContentBean(receiveStr));
                        mAdapter.notifyDataSetChanged();
                        if (mDatas.size() > 0)
                            mLvContent.setSelection(mDatas.size() - 1);

                        break;
                    case SOCET_ERR:
                        String errMsg = (String) msg.obj;
                        mToastUtils.show(mContext, errMsg);
                        break;
                    case START_SERVICE:
                        mToastUtils.show(mContext, "开启服务线程");
                        break;
                    case SOCKET_ENTER:
                        String str = (String) msg.obj;
                        mToastUtils.show(mContext, str);
                        break;
                    case RECEIVE_SERVER_MSG://收到服务端消息
                        String clientMsg = (String) msg.obj;
                        ContentBean bean = new ContentBean(clientMsg);
                        mDatas.add(bean);
                        mAdapter.notifyDataSetChanged();
                        mLvContent.setSelection(mDatas.size() - 1);
                        break;
                    case COPY_TO_BOARD:
                        String copyContent = (String) msg.obj;
                        ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        if (!TextUtils.isEmpty(copyContent)) {
                            cb.setText(copyContent);
                            Log.i(TAG, "复制到粘贴板..." + copyContent);
                        }
                        break;
                }

            }
        };
    }

    @Override
    protected void onStop() {
        super.onStop();
        //关闭线程池
        if (mExecutor != null) {
            mExecutor.shutdownNow();
            mExecutor = null;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void initView() {
        isServerOn = SharedPreferencesUtils.getBoolean(AppConfig.LOCAL_SERVER_ON, false);
        mScroll = (MyScrollView) findViewById(R.id.scroll_main);
        mTvShow = (TextView) findViewById(R.id.tv_main_ip);
        mLvContent = (MyListview) findViewById(R.id.lv_main_content);
        ContentBean firstBean = new ContentBean("以下是复制到粘贴板的内容：");
        mDatas.add(firstBean);
        mAdapter = new ContentAdapter(mDatas, this);
        mLvContent.setAdapter(mAdapter);
        mScroll.setChildView(mLvContent);

        mEtIp = (EditText) findViewById(R.id.et_main_ip);
        mEtPort = (EditText) findViewById(R.id.et_main_port);
        String ip = SharedPreferencesUtils.getString(AppConfig.LAST_CONNECTED_IP);
        int port = SharedPreferencesUtils.getInt(AppConfig.LAST_CONNECTED_PORT);
        if (!TextUtils.isEmpty(ip))
            mEtIp.setText(ip);
        if (port != 0)
            mEtPort.setText(port + "");

        mBtnStart = (Button) findViewById(R.id.btn_main_server);
        mBtnStart.setText(AppUtil.isServiceRunning(mContext, SocketService.class.getName()) ? "关闭本地服务器" : "开启本地服务器");
        mEtSend = (EditText) findViewById(R.id.et_main_test);
        mBtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtil.isServiceRunning(mContext, SocketService.class.getName())) {
                    startLocalserver();
                } else {
                    closeLocalServer();
                }


            }
        });
        mBtnConnect = (Button) findViewById(R.id.btn_main_connect);
        mBtnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mConnectToServer) {//连接
                    connectToServer();
                } else {//断开连接
                    noticeServer();
                    disConnctToServer();

                }

            }
        });
        mBtnSend = (Button) findViewById(R.id.btn_main_client);
        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mConnectToServer) {
                    String sendMsg = mEtSend.getText().toString().trim();
                    sendMsg(sendMsg);
                } else
                    mToastUtils.show(mContext, "请先连接服务器");
            }
        });


        mTvShow.setText("本机地址：" + getLocalIpAddress() + " : 8888");
    }

    /**
     * 通知服务端我断开连接
     */
    private void noticeServer() {
        String notice = mSocketClient.getInetAddress() + " 断开连接";
        notice = "client_offline" + notice;
        sendMsg(notice);
    }

    /**
     * 客户端和服务器断开连接
     */
    private void disConnctToServer() {
        if (mSocketClient != null) {
            try {
                mSocketClient.close();
                mConnectToServer = false;
                mBtnConnect.setText("连接");
                mToastUtils.show(mContext, "断开成功");
            } catch (IOException e) {
                e.printStackTrace();
                mToastUtils.show(mContext, e.getMessage());
            }
        }
    }

    /**
     * 打开本地服务器
     */
    private void closeLocalServer() {
        Intent intent = new Intent(mContext, SocketService.class);
        startService(intent);
        mBtnStart.setText("关闭本地服务器");
    }


    /**
     * 关闭本地服务器
     */
    private void startLocalserver() {
        AppUtil.stopRunningService(mContext, SocketService.class.getName());
        mBtnStart.setText("开启本地服务器");
    }


    /**
     * 客户端发消息
     */
    private void sendMsg(String msg) {
        try {
            if (mSocketClient != null && mSocketClient.isConnected()) {
                //从Socket对象获得输出流
                OutputStream outputStream = mSocketClient.getOutputStream();
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, "GB2312")));
                out.println(msg);
                out.flush();
                mEtSend.setText("");
            } else {
                mToastUtils.show(mContext, "连接已断开");
            }

        } catch (IOException e) {
            e.printStackTrace();
            mToastUtils.show(mContext, e.getMessage());
        }
    }

    /**
     * 客户端连接到服务器
     */
    private void connectToServer() {
        final String ip = mEtIp.getText().toString().trim();
        String portStr = mEtPort.getText().toString().trim();
        if (TextUtils.isEmpty(ip) || TextUtils.isEmpty(portStr)) {
            mToastUtils.show(mContext, "服务器地址不能为空");
            return;
        }
        final int port = Integer.valueOf(portStr);

        //创建线程池
        if (mExecutor == null)
            mExecutor = Executors.newCachedThreadPool();
        mExecutor.execute(new ClientThread(ip, port));

    }

    /**
     * 客户端线程
     */
    private class ClientThread implements Runnable {
        String ip;
        int port;

        public ClientThread(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        @Override
        public void run() {
            try {
                mSocketClient = new Socket(ip, port);
                Message msg = new Message();
                msg.what = SOCKET_CONNECTED;
                msg.obj = "成功连接到 " + ip;
                mHandler.sendMessage(msg);
                SharedPreferencesUtils.putString(AppConfig.LAST_CONNECTED_IP, ip);
                SharedPreferencesUtils.putInt(AppConfig.LAST_CONNECTED_PORT, port);
                receiveMsg();
            } catch (IOException e) {
                e.printStackTrace();
                Message msg = new Message();
                msg.what = SOCET_ERR;
                msg.obj = e.getMessage();
                mHandler.sendMessage(msg);
            }

        }

        /**
         * 客户端接收服务端消息
         */
        private void receiveMsg() {
            try {
                if (!mSocketClient.isClosed()) {
                    if (mSocketClient.isConnected()) {
                        if (!mSocketClient.isInputShutdown()) {
                            BufferedReader br = new BufferedReader(new InputStreamReader(mSocketClient.getInputStream()));
                            String content;
                            while ((content = br.readLine()) != null) {
                                parseMessage(content);
                            }
                        }
                    }
                }

            } catch (Exception e) {
//                e.printStackTrace();
//                Message msg = new Message();
//                msg.what = SOCET_ERR;
//                msg.obj = e.getMessage();
//                mHandler.sendMessage(msg);
            }

        }

        /**
         * 解析json
         *
         * @param content
         */
        private void parseMessage(String content) {
            //复制到粘贴板
            Message msg = new Message();
            msg.what = COPY_TO_BOARD;
            msg.obj = content;
            mHandler.sendMessage(msg);
            //发送到UI界面显示
            content = mSocketClient.getInetAddress() + " : " + content;
            Message msg1 = new Message();
            msg1.what = RECEIVE_SERVER_MSG;
            msg1.obj = content;
            mHandler.sendMessage(msg1);

        }
    }


    /**
     * 开启服务器
     */
    private void startSocketServer() throws IOException {

        if (!mThreadStart) {
            new AsyncTask() {
                @Override
                protected Object doInBackground(Object[] params) {
                    Message msg = new Message();
                    msg.what = START_SERVICE;
                    mHandler.sendMessage(msg);
                    mThreadStart = true;
                    ServerSocket serverSocket = null;
                    try {
                        serverSocket = new ServerSocket(8888);
                        Log.i(TAG, "服务器启动成功...");
                        Log.i(TAG, "等待客户端连接...");
                        while (!stop) {
                            Socket clientSocket = serverSocket.accept();
                            Log.i(TAG, "客户端 " + clientSocket.getInetAddress() + " 连接进来...");
                            new ServerThread(clientSocket).start();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    return null;
                }
            }.execute();


/*            new Thread() {
                *//* (non-Javadoc)
                 * @see java.lang.Thread#run()
                 *//*
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    try {
                        //创建ServerSocket对象监听8888端口
                        ServerSocket serverSocket = new ServerSocket(8888);

                        //接收tcp连接返回socket对象
                        Socket client = serverSocket.accept();
                        System.out.println("S: Receiving...");
                        if (client.isConnected()) {
                            mHandler.sendEmptyMessage(SOCKET_CONNECTED);
                        } else {
                            mHandler.sendEmptyMessage(SOCKET_DISCONNECTED);
                        }
                        try {
                            //接收客户端消息
                            InputStream inputStream = client.getInputStream();
                            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
                            // 发送给客户端的消息
                            PrintWriter out = new PrintWriter(new BufferedWriter(
                                    new OutputStreamWriter(client.getOutputStream())), true);
//                        byte[] byteBuffer = new byte[1024];
//                        StringBuilder sb = new StringBuilder();
//                        int temp;
//                        //读取接收的数据
//                        while ((temp = inputStream.read(byteBuffer)) != -1) {
//                            sb.append(new String(byteBuffer, 0, temp));
//                            Log.i(MainActivity.class.getSimpleName(), new String(byteBuffer, 0, temp));
//                        }
//                        final String receiveStr = sb.toString();
//                        Log.i(MainActivity.class.getSimpleName(), receiveStr);
                            String inString = in.readLine();
                            Log.i(MainActivity.class.getSimpleName(), inString);
                            Message msg = new Message();
                            msg.what = RECEIVE_CLIENT_MSG;
                            msg.obj = inString;
                            mHandler.sendMessage(msg);
                            //将接收到的消息发送给客户端
                            if (!TextUtils.isEmpty(inString)) {
                                out.println("from server: " + inString);
                                out.flush();
                            }

                            client.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }


                    } catch (IOException e) {
                        e.printStackTrace();
                        Message msg = new Message();
                        msg.what = SOCET_ERR;
                        msg.obj = e.getMessage();
                        mHandler.sendMessage(msg);
                        mThreadStart = false;
                    }
                }

            }.start();*/

        } else {
            mToastUtils.show(mContext, "服务线程运行中！");
        }
    }


    /**
     * 服务器线程
     */
    private class ServerThread extends Thread {
        private InputStream inStream = null;
        private byte[] buf;
        private String str = null;
        PrintWriter out = null;
        private Socket socket;

        ServerThread(Socket socket) {
            this.socket = socket;
            try {
                //获取输入流
                this.inStream = socket.getInputStream();
                // 发送给客户端的消息
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (!stop) {
                this.buf = new byte[512];
                try {
                    //读取输入数据（阻塞）
                    this.inStream.read(this.buf);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //字符编码转换
                try {
                    this.str = new String(this.buf, "GB2312").trim();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, this.str);
                Message msg = new Message();
                msg.what = RECEIVE_CLIENT_MSG;
                msg.obj = this.str;
                mHandler.sendMessage(msg);

                //将接收到的消息发送给客户端
                if (!TextUtils.isEmpty(this.str)) {
                    out.println("from server: " + this.str);
                    out.flush();
                }

                if (!this.socket.isConnected()) {
                    Log.i(TAG, "客户端 " + socket.getInetAddress() + " 断开连接...");
                }

            }
        }
    }


    /**
     * 获取本地IP
     *
     * @return
     */
    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("WifiPreference IpAddress", ex.toString());
        }
        return null;
    }

    /**
     * 定义订阅者，接收eventbus发布者的消息
     *
     * @param message
     */
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void receiveClientMsg(ClientMessage message) {
        String msg = message.getMessage();
        ContentBean bean = new ContentBean(msg);
        mDatas.add(bean);
        mAdapter.notifyDataSetChanged();
        if (mDatas.size() > 0)
            mLvContent.setSelection(mDatas.size() - 1);

    }

}
