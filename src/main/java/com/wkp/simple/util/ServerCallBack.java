package main.java.com.wkp.simple.util;

/**
 * IP/TCP通信 服务器端回调接口
 */
public interface ServerCallBack {
    /**
     * 异常回调
     * @param error
     */
    void onError(Throwable error);

    /**
     * 客户端连接回调
     * @param address
     */
    void onAccept(String address);

    /**
     * 接收消息回调
     * @param address
     * @param result
     */
    void onReceived(String address, byte[] result);

    /**
     * 消息已发送回调
     * @param address
     * @param data
     */
    void onSent(String address, byte[] data);
}
