package com.apollo.apollopaste;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = BaseApplication.getContext();
        mToastUtils = ToastUtils.shareInstance();
        initView();
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
        mToastUtils.show(mContext, "开启服务端");
        if (!mThreadStart) {
            mThreadStart = true;
            new Thread() {
                /* (non-Javadoc)
                 * @see java.lang.Thread#run()
                 */
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    ServerSocket serverSocket = null;
                    try {
                        //创建ServerSocket对象监听6688端口
                        serverSocket = new ServerSocket(6688);
                        //接收tcp连接返回socket对象
                        Socket socket = serverSocket.accept();
                        if (socket.isConnected()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mToastUtils.show(mContext, "连接成功！");
                                }
                            });

                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mToastUtils.show(mContext, "连接失败！");
                                }
                            });

                        }
                        //获得输入流
                        InputStream inputStream = socket.getInputStream();
                        byte[] byteBuffer = new byte[1024];
                        int temp = 0;
                        //读取接收的数据
                        while ((temp = inputStream.read(byteBuffer)) != -1)
                            System.out.println(new String(byteBuffer, 0, temp));
                        socket.close();
                        serverSocket.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                        mToastUtils.show(mContext, e.toString());
                        mThreadStart = false;
                    }
                }

            }.start();

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
