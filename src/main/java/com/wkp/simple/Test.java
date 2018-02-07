package com.wkp.simple;

import com.wkp.simple.util.ServerCallBack;
import com.wkp.simple.util.ServerHelper;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 服务端测试类
 */
public class Test {
    private static Set<String> addresses = new HashSet<String>();
    public static void main(String[] args) {
        //创建服务器
        final ServerHelper helper = ServerHelper.getInstance(true);
        helper.createServer(9988, new ServerCallBack() {
            //异常回调
            @Override
            public void onError(Throwable error) {

            }

            //客户端连接回调
            @Override
            public void onAccept(String address) {
                addresses.add(address);
            }

            //收到客户端消息回调
            @Override
            public void onReceived(String address, byte[] result) {
                String x = new String(result);
                Iterator<String> iterator = addresses.iterator();
                while (iterator.hasNext()) {
                    String next = iterator.next();
                    if (!next.equals(address)) {
                        System.out.println(x);
                        System.out.println(next);
                        //向客户端发送消息
                        helper.send(9988,next,result);
                    }
                }
            }

            //向客户端发送成功回调
            @Override
            public void onSent(String address, byte[] data) {
                System.out.println(new String(data));
            }
        });
    }
}
