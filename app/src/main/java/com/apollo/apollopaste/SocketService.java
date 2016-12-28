package com.apollo.apollopaste;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;

/**
 * Created by zayh_yf20160909 on 2016/12/27.
 */

public class SocketService extends Service {
    private static final int COPY_TO_BOARD = 10086;
    private final int CLIENT_ENTER = 10087;
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
                        Log.i(TAG, "复制到粘贴板...");
                    }
                    break;
                case CLIENT_ENTER:
                    String str = (String) msg.obj;
                    ToastUtils.shareInstance().show(SocketService.this, str);
                    break;
            }
        }
    };


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
        initSocket();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void initSocket() {

        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                ServerSocket serverSocket = null;
                try {
                    serverSocket = new ServerSocket(8888);
                    mExecutorService = Executors.newCachedThreadPool();  //创建线程池
                    Log.i(TAG, "服务器启动成功...");
                    Log.i(TAG, "等待客户端连接...");
                    while (!mServerStop) {
                        Socket clientSocket = serverSocket.accept();
                        Log.i(TAG, "客户端 " + clientSocket.getInetAddress() + " 连接进来...");
                        Message message = new Message();
                        message.what = CLIENT_ENTER;
                        message.obj = clientSocket.getInetAddress() + "连接成功";
                        mHandler.sendMessage(message);
                        mClientList.add(clientSocket);//保存客户端
                        mExecutorService.execute(new ServerThread(clientSocket));

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
        ToastUtils.shareInstance().show(this, "服务启动成功");
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
                    sendContent(content);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private void sendContent(String content) {
        Log.i(TAG, content);
        //发送到mainactivity显示
        EventBus.getDefault().post(content);

        Message msg = new Message();
        msg.what = COPY_TO_BOARD;
        msg.obj = content;
        mHandler.sendMessage(msg);

    }

    /**
     * 定义订阅者，接收eventbus发布者的消息
     *
     * @param stop
     */
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(String stop) {
        mServerStop = TextUtils.equals("1", stop) ? true : false;
        if (mServerStop)
            Log.i(TAG, "服务器停止...");
    }
}
