package main.java.com.wkp.simple.util;

/**
 * IP/TCP通信 客户端回调接口
 */
public interface ClientCallBack {
    /**
     * 异常回调
     * @param e
     */
    void onError(Throwable e);

    /**
     * 连接服务器回调
     * @param address
     * @param port
     */
    void onConnected(String address, int port);

    /**
     * 收到消息回调
     * @param address
     * @param port
     * @param result
     */
    void onReceived(String address, int port, byte[] result);

    /**
     * 发送消息回调
     * @param address
     * @param port
     * @param data
     */
    void onSent(String address, int port, byte[] data);
}
