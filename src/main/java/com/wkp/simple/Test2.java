package com.wkp.simple;



import com.wkp.simple.util.ClientCallBack;
import com.wkp.simple.util.ClientHelper;

import java.util.Scanner;

public class Test2 {
    public static void main(String[] args) {
        //创建客户端2
        final ClientHelper helper = ClientHelper.getInstance(false);
        helper.createClient("127.0.0.1", 9988, new ClientCallBack() {
            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onConnected(final String address, final int port) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Scanner scanner = new Scanner(System.in);
                        while (scanner.hasNextLine()) {
                            String s = scanner.nextLine();
                            if (s.equals("exit")) {
                                helper.closeClient(address, port);
                                return;
                            }
                            helper.send(address,port,s.getBytes());
                        }
                    }
                }).start();
            }

            @Override
            public void onReceived(String address, int port, byte[] result) {
                System.out.println("Received: " + (new String(result)));
            }

            @Override
            public void onSent(String address, int port, byte[] data) {
                System.out.println("Sent: " + (new String(data)));
            }
        });}
}
