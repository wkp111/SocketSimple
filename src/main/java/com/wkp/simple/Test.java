package main.java.com.wkp.simple;

import main.java.com.wkp.simple.util.ServerCallBack;
import main.java.com.wkp.simple.util.ServerHelper;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Test {
    private static Set<String> addresses = new HashSet<>();
    public static void main(String[] args) {
        //创建服务器
        ServerHelper helper = ServerHelper.getInstance(true);
        helper.createServer(9988, new ServerCallBack() {
            @Override
            public void onError(Throwable error) {

            }

            @Override
            public void onAccept(String address) {
                addresses.add(address);
            }

            @Override
            public void onReceived(String address, byte[] result) {
                Iterator<String> iterator = addresses.iterator();
                while (iterator.hasNext()) {
                    String next = iterator.next();
                    if (!next.equals(address)) {
                        System.out.println(new String(result));
                        System.out.println(next);
                        helper.send(9988,next,result);
                    }
                }
            }

            @Override
            public void onSent(String address, byte[] data) {
                System.out.println(new String(data));
            }
        });
    }
}
