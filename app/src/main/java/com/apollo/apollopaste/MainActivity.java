package com.apollo.apollopaste;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;

public class MainActivity extends Activity {
    private TextView mTvShow;
    private EditText mEtIp;
    private EditText mEtPort;
    private Button mBtnStart;
    private EditText mEtTest;
    private Button mBtnSend;
    private Context mContext;
    private ToastUtils mToastUtils;
    private boolean mThreadStart;
    private Handler mHandler;
    private final int SOCKET_CONNECTED = 10086;
    private final int SOCKET_DISCONNECTED = 10087;
    private final int RECEIVE_SOCKET = 10088;
    private final int SOCET_ERR = 10089;
    private final int START_SERVICE = 10090;
    private boolean stop;
    private final String TAG = MainActivity.class.getSimpleName();
    private TextView mTvContent;


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
                        break;
                    case SOCKET_DISCONNECTED:
                        mToastUtils.show(mContext, "连接失败！");
                        break;
                    case RECEIVE_SOCKET:
                        String receiveStr = (String) msg.obj;
//                        String preStr = mTvContent.getText().toString();
//                        mTvContent.setText(preStr + "\n" + receiveStr);
                        //滚动到最后文字
                        mTvContent.append("\n" + receiveStr);
                        int offset = mTvContent.getLineCount() * mTvContent.getLineHeight();
                        if (offset > mTvContent.getHeight()) {
                            mTvContent.scrollTo(0, offset - mTvContent.getHeight());
                        }

                        break;
                    case SOCET_ERR:
                        String errMsg = (String) msg.obj;
                        mToastUtils.show(mContext, errMsg);
                        break;
                    case START_SERVICE:
                        mToastUtils.show(mContext, "开启服务线程");
                        break;
                }

            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void initView() {
        mTvShow = (TextView) findViewById(R.id.tv_main_ip);
        mTvContent = (TextView) findViewById(R.id.tv_main_content);
        mTvContent.setMovementMethod(ScrollingMovementMethod.getInstance());
        mEtIp = (EditText) findViewById(R.id.et_main_ip);
        mEtPort = (EditText) findViewById(R.id.et_main_port);
        mBtnStart = (Button) findViewById(R.id.btn_main_server);
        mEtTest = (EditText) findViewById(R.id.et_main_test);
        mBtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                try {
//                    startSocketServer();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
                Intent intent = new Intent(mContext, SocketService.class);
                startService(intent);

            }
        });
        mBtnSend = (Button) findViewById(R.id.btn_main_client);
        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSocketClient();
            }
        });
        mTvShow.setText("本机地址：" + getLocalIpAddress()+":8888");
    }

    /**
     * 客户端
     */
    private void startSocketClient() {
        String ip = mEtIp.getText().toString().trim();
        int port = Integer.valueOf(mEtPort.getText().toString().trim());
        try {
            Socket socket = new Socket(ip, port);
            InputStream inputStream = new FileInputStream("F://test.txt");
            //从Socket对象获得输出流
            java.io.OutputStream outputStream = socket.getOutputStream();
            int temp = 0;
            byte[] buffer = new byte[1024];
            //向输出流中写要发送的数据
            while ((temp = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, temp);
                System.out.println(new String(buffer, 0, temp));
            }
            outputStream.flush();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 服务端
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
                            msg.what = RECEIVE_SOCKET;
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

    //获取本地IP
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
                msg.what = RECEIVE_SOCKET;
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
     * 定义订阅者，接收eventbus发布者的消息
     *
     * @param message
     */
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(String message) {
        //滚动到最后文字
        mTvContent.append("\n" + message);
        int offset = mTvContent.getLineCount() * mTvContent.getLineHeight();
        if (offset > mTvContent.getHeight()) {
            mTvContent.scrollTo(0, offset - mTvContent.getHeight());
        }
    }

}
