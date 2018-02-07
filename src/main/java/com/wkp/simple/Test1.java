package com.wkp.simple;



import com.wkp.simple.util.ClientCallBack;
import com.wkp.simple.util.ClientHelper;
import java.util.Scanner;

/**
 * 客户端1测试类
 */
public class Test1 {
    public static void main(String[] args) {
        //创建客户端1
        final ClientHelper helper = ClientHelper.getInstance(true);
        helper.createClient("192.168.1.114", 9988, new ClientCallBack() {
            //异常回调
            @Override
            public void onError(Throwable e) {

            }

            //连接服务器回调
            @Override
            public void onConnected(final String address, final int port) {
                //开线程获取键盘录入（注意：因为键盘录入会堵塞线程，所以当直接在该方法使用时会导致该方法所在线程堵塞）
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
                            //向服务器发送消息
                            helper.send(address,port,s.getBytes());
                        }
                    }
                }).start();
            }

            //接收服务器消息回调
            @Override
            public void onReceived(String address, int port, byte[] result) {
                System.out.println("Client1-Received: " + (new String(result)));
            }

            //发送服务器消息成功回调
            @Override
            public void onSent(String address, int port, byte[] data) {
                String s = new String(data);
                System.out.println("Client1-Sent: " + s);
            }
        });
    }
}
