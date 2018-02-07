# SocketSimple
IP/TCP通信帮助类，主要用于java中利用Socket通信时，服务端与客户端快速创建，支持长连接，支持多服务器、多客户端创建。
# 集成方式
> Jar包下载<a href="https://github.com/wkp111/SocketSimple/blob/master/SocketSimple.jar">SocketSimple.jar</a><br/>
> Maven中央仓库<a href="http://search.maven.org/">http://search.maven.org/</a>搜索 SocketSimple
# 代码示例
```java
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
                                //关闭客户端
                                helper.closeClient(address,port);
                                //回收资源
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
```
Note：例子为两个客户端聊天示例。
## 寄语
控件支持直接代码创建，还有更多API请观看源码内的注释说明。<br/>
欢迎大家使用，感觉好用请给个Star鼓励一下，谢谢！<br/>
大家如果有更好的意见或建议以及好的灵感，请邮箱作者，谢谢！<br/>
QQ邮箱：1535514884@qq.com<br/>
163邮箱：15889686524@163.com<br/>
Gmail邮箱：wkp15889686524@gmail.com<br/>

## 版本更新
* v1.0.2<br/>
新增资源回收，避免线程池占用进程资源<br/><br/>
* v1.0.1<br/>
新创建IP/TCP通信帮助库
## License

   Copyright 2017 wkp

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

