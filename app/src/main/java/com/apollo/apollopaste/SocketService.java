package com.apollo.apollopaste;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by zayh_yf20160909 on 2016/12/27.
 */

public class SocketService extends Service {
    private final String TAG = SocketService.class.getSimpleName();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initSocket();
    }

    private void initSocket() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(8888);
            Log.i(TAG, "服务器启动成功...");
            Log.i(TAG, "等待客户端连接...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Log.i(TAG, "客户端 " + clientSocket.getInetAddress() + " 连接进来...");
                new ServerThread(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
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
        //客户端socket
        private Socket socket;
        private BufferedReader bufferedReader = null;

        ServerThread(Socket socket) {
            this.socket = socket;
            try {
                //获取输入流
                this.inStream = socket.getInputStream();
                // 发送给客户端的消息
                this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
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
                    sendContent(content);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private void sendContent(String content) {
        Log.i(TAG, content);
    }
}
