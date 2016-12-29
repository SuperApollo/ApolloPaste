package com.apollo.apollopaste.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.util.Log;

import com.apollo.apollopaste.constants.AppConfig;
import com.apollo.apollopaste.eventbean.ClientMessage;
import com.apollo.apollopaste.utils.SharedPreferencesUtils;
import com.apollo.apollopaste.utils.ToastUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
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

/**
 * Created by zayh_yf20160909 on 2016/12/27.
 */

public class SocketService extends Service {
    private static final int COPY_TO_BOARD = 10086;
    private final int CLIENT_ENTER = 10087;
    private final int SOCKET_ERR = 10088;
    private final String TAG = SocketService.class.getSimpleName();
    private boolean mServerStop = false;
    private ExecutorService mExecutorService;
    private List<Socket> mClientList = new ArrayList<>();

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case COPY_TO_BOARD:
                    //给粘贴板赋值
                    String content = (String) msg.obj;
                    ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (!TextUtils.isEmpty(content)) {
                        cb.setText(content);
                        Log.i(TAG, "复制到粘贴板: " + content);
                    }
                    break;
                case CLIENT_ENTER:
                    String str = (String) msg.obj;
                    ToastUtils.shareInstance().show(SocketService.this, str);
                    break;
                case SOCKET_ERR:
                    String errMsg = (String) msg.obj;
                    ToastUtils.shareInstance().show(SocketService.this, errMsg);
                    break;
            }
        }
    };
    private AsyncTask mServerTask;
    private ServerSocket serverSocket;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
//        EventBus.getDefault().register(this);
        initSocket();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        EventBus.getDefault().unregister(this);
        SharedPreferencesUtils.putBoolean(AppConfig.LOCAL_SERVER_ON, false);
        //关闭客户端socket
        for (Socket client : mClientList) {
            try {
                //告诉客户端，服务器关闭了
                PrintStream printStream = new PrintStream(client.getOutputStream());
                String serverCloseNotice = "server_close" + getLocalIpAddress() + " 关闭了";
                printStream.println(serverCloseNotice);
                printStream.flush();
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //关闭服务器socket
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //关闭线程池中的服务器线程
        mExecutorService.shutdownNow();
        //关闭服务器监听
        mServerTask.cancel(true);
        Log.i(TAG, "服务器关闭成功...");
        ToastUtils.shareInstance().show(this, "服务器关闭成功");
    }

    private void initSocket() {
        mServerTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
//                serverSocket = null;
                try {
                    serverSocket = new ServerSocket(8888);
                    mExecutorService = Executors.newCachedThreadPool();  //创建线程池
                    Log.i(TAG, "服务器启动成功...");
                    Log.i(TAG, "等待客户端连接...");
                    while (!mServerStop) {
                        Socket clientSocket = serverSocket.accept();
                        Log.i(TAG, "客户端 " + clientSocket.getInetAddress() + " 成功接入...");
                        Message message = new Message();
                        message.what = CLIENT_ENTER;
                        message.obj = clientSocket.getInetAddress() + " 成功接入";
                        mHandler.sendMessage(message);
                        mClientList.add(clientSocket);//保存客户端
                        mExecutorService.execute(new ServerThread(clientSocket));

                    }
                } catch (IOException e) {
//                    e.printStackTrace();
//                    Message msg = new Message();
//                    msg.what = SOCKET_ERR;
//                    msg.obj = e.getMessage();
//                    mHandler.sendMessage(msg);
                }
                return null;
            }
        }.execute();
        ToastUtils.shareInstance().show(this, "服务启动成功");
        SharedPreferencesUtils.putBoolean(AppConfig.LOCAL_SERVER_ON, true);
    }

    /**
     * 服务器线程
     */
    private class ServerThread implements Runnable {
        private InputStream inStream = null;
        private byte[] buf;
        private String str = null;
        PrintWriter out = null;
        //客户端socket
        private Socket socket;
        private BufferedReader bufferedReader = null;

        ServerThread(Socket socket) {
            this.socket = socket;
            try {
                //获取输入流
                this.inStream = socket.getInputStream();
                // 发送给客户端的消息
                this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "GB2312"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                String content = null;
                //循环读取
                while ((content = bufferedReader.readLine()) != null) {
                    //回传给客户端
                    PrintStream printStream = new PrintStream(socket.getOutputStream());
                    printStream.println(content);
                    printStream.flush();
                    parseMessage(content);
                }

            } catch (Exception e) {
                e.printStackTrace();
                Message msg = new Message();
                msg.what = SOCKET_ERR;
                msg.obj = socket.getInetAddress() + " 断开连接";
                Looper.prepare();
                mHandler.sendMessage(msg);
            }

        }

        /**
         * 解析json
         *
         * @param content
         */
        private void parseMessage(String content) {
            if (content.startsWith("client_offline")) {//客户端下线通知
                ToastUtils.shareInstance().show(SocketService.this, content);
            } else {//普通客户端消息
                //复制到粘贴板
                Message msg = new Message();
                msg.what = COPY_TO_BOARD;
                msg.obj = content;
                mHandler.sendMessage(msg);
                //发送到UI界面显示
                content = socket.getInetAddress() + " : " + content;
                sendContent(content);
            }


        }
    }

    /**
     * 发送到mainactivity显示
     *
     * @param content
     */
    private void sendContent(String content) {
        ClientMessage clientMessage = new ClientMessage(content);
        EventBus.getDefault().post(clientMessage);

    }

//    /**
//     * 定义订阅者，接收eventbus发布者的消息
//     *
//     * @param notice
//     */
//    @Subscribe(threadMode = ThreadMode.MainThread)
//    public void receiveClientOfflineNotice(ClientOfflineNotice notice) {
//        ToastUtils.shareInstance().show(SocketService.this, notice.getNotice());
//
//    }

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
}
