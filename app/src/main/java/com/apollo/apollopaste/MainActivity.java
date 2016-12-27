package com.apollo.apollopaste;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
                        mTvShow.setText("本机ip地址：" + getLocalIpAddress() + "\n" + receiveStr);
                        break;
                    case SOCET_ERR:
                        String errMsg = (String) msg.obj;
                        mToastUtils.show(mContext, errMsg);
                        break;
                }

            }
        };
    }

    private void initView() {
        mTvShow = (TextView) findViewById(R.id.tv_main_show);
        mEtIp = (EditText) findViewById(R.id.et_main_ip);
        mEtPort = (EditText) findViewById(R.id.et_main_port);
        mBtnStart = (Button) findViewById(R.id.btn_main_server);
        mEtTest = (EditText) findViewById(R.id.et_main_test);
        mBtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSocketServer();
            }
        });
        mBtnSend = (Button) findViewById(R.id.btn_main_client);
        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSocketClient();
            }
        });
        mTvShow.setText("本机ip地址：" + getLocalIpAddress());
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
    private void startSocketServer() {

        if (!mThreadStart) {
            mToastUtils.show(mContext, "开启服务线程");
            mThreadStart = true;
            new Thread() {
                /* (non-Javadoc)
                 * @see java.lang.Thread#run()
                 */
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    try {
                        //创建ServerSocket对象监听8888端口
                        ServerSocket serverSocket = new ServerSocket(8888);
                        while (true) {
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
                            }catch (Exception e){
                                e.printStackTrace();
                            }finally {
                                client.close();
                                serverSocket.close();
                            }

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

            }.start();

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
}
