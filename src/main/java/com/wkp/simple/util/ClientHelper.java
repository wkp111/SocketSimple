package main.java.com.wkp.simple.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * IP/TCP通信 客户端帮助类
 * <p>
 *     帮助类提供:
 *     创建客户端{@link #createClient(String, int, ClientCallBack)},
 *     发送消息{@link #send(String, int, byte[])},
 *     接收消息{@linkplain ReadRunnable}等.
 *     内部多线程并发{@link #sExecutor},支持创建多客户端(需IP和Port改变),请注意数据调度.
 * </p>
 */
public class ClientHelper {
    private static final String LOCK = "lock";
    private static final byte[] END_FLAG = {-1};
    private static boolean sDebug = false;
    private static Logger sLogger = Logger.getLogger("@wkp ");
    private static ClientHelper sClientHelper;
    private static Executor sExecutor = Executors.newCachedThreadPool();
    private ConcurrentMap<String, ClientCallBack> mClientCallBackMap;  //address-port-clientCallBack IP+Port对应回调接口存储Map
    private ConcurrentMap<String, ClientInfo> mClientInfoMap;  //address-port-clientInfo IP+Port对应客户端信息存储Map

    private ClientHelper() {
        mClientCallBackMap = new ConcurrentHashMap<>();
        mClientInfoMap = new ConcurrentHashMap<>();
    }

    /**
     * 获取帮助类单例
     * @param debug 是否开启debug模式 打印日志
     * @return
     */
    public synchronized static ClientHelper getInstance(boolean debug) {
        sDebug = debug;
        if (sClientHelper == null) {
            synchronized (LOCK) {
                sClientHelper = new ClientHelper();
            }
        }
        return sClientHelper;
    }

    /**
     * 创建客户端
     * @param address IP地址
     * @param port 端口号
     * @param callBack 回调接口
     */
    public synchronized void createClient(String address, int port, ClientCallBack callBack) {
        ClientInfo clientInfo = mClientInfoMap.get(address + port);
        if (clientInfo == null || clientInfo.mSocket.isClosed()) {
            mClientCallBackMap.put(address + port, callBack);
            if (sDebug) sLogger.log(Level.INFO, "Starting client!");
            sExecutor.execute(new ClientRunnable(address, port, callBack));
        } else {
            if (sDebug) sLogger.log(Level.INFO, "The client has been exist! Address: " + address + " Port: " + port);
            callBack.onError(new IllegalStateException("The client has been exist! Address: " + address + " Port: " + port));
        }
    }

    /**
     * 发送数据
     * @param address IP地址
     * @param port 端口号
     * @param data 数据源
     */
    public synchronized void send(String address,int port,byte[] data) {
        sExecutor.execute(new WriteRunnable(address,port,data));
    }

    /**
     * 关闭客户端
     * @param address IP地址
     * @param port 端口号
     * @return
     */
    public synchronized boolean closeClient(String address,int port) {
        try {
            ClientInfo clientInfo = mClientInfoMap.get(address + port);
            if (clientInfo != null) {
                clientInfo.mIs.close();
                clientInfo.mOs.close();
                clientInfo.mSocket.close();
                mClientInfoMap.remove(address + port);
            }
            if (sDebug) sLogger.log(Level.INFO, "Client has been removed. Address: " + address + " Port: " + port);
            return true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 创建客户端任务
     */
    private class ClientRunnable implements Runnable {

        private final String mAddress;
        private final int mPort;
        private final ClientCallBack mCallBack;

        ClientRunnable(String address, int port, ClientCallBack callBack) {
            mAddress = address;
            mPort = port;
            mCallBack = callBack;
        }

        @Override
        public void run() {
            try {
                Socket socket = new Socket(mAddress, mPort);
                if (sDebug) sLogger.log(Level.INFO, "Connected to Server. Address: " + mAddress + " Port: " + mPort);
                ClientInfo clientInfo = new ClientInfo(socket, socket.getInputStream(), socket.getOutputStream());
                mClientInfoMap.put(mAddress + mPort, clientInfo);
                mCallBack.onConnected(mAddress, mPort);
                Thread.sleep(100);
                if (sDebug) sLogger.log(Level.INFO, "Starting to read data. Address: " + mAddress + " Port: " + mPort);
                sExecutor.execute(new ReadRunnable(mAddress, mPort, clientInfo, mCallBack));
            } catch (Exception e) {
                mCallBack.onError(e);
                e.printStackTrace();
            }
        }
    }

    /**
     * 读取数据任务
     */
    private class ReadRunnable implements Runnable {

        private final ClientInfo mClientInfo;
        private final ClientCallBack mCallBack;
        private final String mAddress;
        private final int mPort;

        ReadRunnable(String address, int port, ClientInfo clientInfo, ClientCallBack callBack) {
            mClientInfo = clientInfo;
            mCallBack = callBack;
            mAddress = address;
            mPort = port;
        }

        @Override
        public void run() {
            try {
                List<Byte> readData = new ArrayList<>();
                while (!mClientInfo.mSocket.isClosed()) {
                    readData.clear();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = mClientInfo.mIs.read(buffer)) != -1) {
                        byte[] read = Arrays.copyOf(buffer, len);
                        for (byte b : read) {
                            readData.add(b);
                        }
                        if (read.length > 0 && read[read.length - 1] == -1) {
                            readData.remove(readData.size() - 1);
                            break;
                        }
                    }
                    if (!readData.isEmpty()) {
                        byte[] result = new byte[readData.size()];
                        for (int i = 0; i < readData.size(); i++) {
                            result[i] = readData.get(i);
                        }
                        if (sDebug) sLogger.log(Level.INFO, "Client has received data! Address: " + mAddress + " Port: " + mPort);
                        mCallBack.onReceived(mAddress, mPort, result);
                    }
                }
            } catch (Exception e) {
                mCallBack.onError(e);
                e.printStackTrace();
            }
        }
    }

    /**
     * 写入数据任务
     */
    private class WriteRunnable implements Runnable{

        private final String mAddress;
        private final int mPort;
        private final byte[] mData;

        WriteRunnable(String address, int port, byte[] data) {
            mAddress = address;
            mPort = port;
            mData = data;
        }

        @Override
        public void run() {
            ClientCallBack callBack = mClientCallBackMap.get(mAddress + mPort);
            if (callBack != null) {
                try {
                    ClientInfo clientInfo = mClientInfoMap.get(mAddress + mPort);
                    if (clientInfo != null) {
                        if (!clientInfo.mSocket.isClosed()) {
                            clientInfo.mOs.write(mData);
                            clientInfo.mOs.write(END_FLAG);
                            clientInfo.mOs.flush();
                            if (sDebug) sLogger.log(Level.INFO, "Client has sent data! Address: " + mAddress + " Port: " + mPort);
                            callBack.onSent(mAddress,mPort,mData);
                        }else {
                            if (sDebug) sLogger.log(Level.INFO, "Connection has disconnected! Address: " + mAddress + " Port: " + mPort);
                            callBack.onError(new IllegalStateException("Connection has disconnected! Address: " + mAddress + " Port: " + mPort));
                        }
                    }else {
                        if (sDebug) sLogger.log(Level.INFO, "Find no Client by the address: " + mAddress + " Port: " + mPort);
                        callBack.onError(new IllegalArgumentException("Find no Client by the address: " + mAddress + " Port: " + mPort));
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }else {
                if (sDebug) sLogger.log(Level.INFO, "No Client! Please create Client first! Address: " + mAddress + " Port: " + mPort);
                throw new IllegalStateException("No Client! Please create Client first! Address: " + mAddress + " Port: " + mPort);
            }
        }
    }

    /**
     * 客户端信息
     */
    private class ClientInfo {
        Socket mSocket;
        InputStream mIs;
        OutputStream mOs;

        ClientInfo(Socket socket, InputStream is, OutputStream os) {
            mSocket = socket;
            mIs = is;
            mOs = os;
        }
    }
}
