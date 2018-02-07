package com.wkp.simple;



import com.wkp.simple.util.ClientCallBack;
import com.wkp.simple.util.ClientHelper;

import java.util.Scanner;

/**
 * 客户端2测试类
 */
public class Test2 {
    public static void main(String[] args) {
        //创建客户端2
        final ClientHelper helper = ClientHelper.getInstance(false);
        helper.createClient("127.0.0.1", 9988, new ClientCallBack() {
            //异常回调
            @Override
            public void onError(Throwable e) {

            }

            //连接服务端回调
            @Override
            public void onConnected(final String address, final int port) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Scanner scanner = new Scanner(System.in);
                        while (scanner.hasNextLine()) {
                            String s = scanner.nextLine();
                            if (s.equals("exit")) {
                                helper.closeClient(address,port);
                                helper.recycle();
                                return;
                            }
                            //向服务端发送消息
                            helper.send(address,port,s.getBytes());
                        }
                    }
                }).start();
            }

            //接收服务端消息回调
            @Override
            public void onReceived(String address, int port, byte[] result) {
                System.out.println("Client2-Received: " + (new String(result)));
            }

            //向服务端发送成功回调
            @Override
            public void onSent(String address, int port, byte[] data) {
                System.out.println("Client2-Sent: " + (new String(data)));
            }
        });}
}
