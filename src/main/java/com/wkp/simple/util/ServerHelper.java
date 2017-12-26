package com.wkp.simple.util;

import com.wkp.simple.util.ServerCallBack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * IP/TCP通信 服务器端帮助类
 * <p>
 *     帮助类提供:
 *     创建服务器{@link #createServer(int, ServerCallBack)},
 *     发送消息{@link #send(int, String, byte[])},
 *     接收消息{@linkplain ReadRunnable}等.
 *     内部多线程并发{@link #sExecutor},支持创建多服务器(仅Port改变),请注意数据调度.
 * </p>
 */
public class ServerHelper {
    private static final String LOCK = "lock";  //同步锁
    private static final byte[] END_FLAG = {-1};
    private ConcurrentMap<Integer, ServerSocket> mServerSocketMap;  //Port-serverSocket对应存储Map
    private ConcurrentMap<String, ClientInfo> mClientInfoMap;   //address-clientSocket对应存储Map
    private ConcurrentMap<Integer, List<String>> mClientAddressMap; //Port-addressList对应存储Map
    private ConcurrentMap<Integer, ServerCallBack> mServerCallBackMap;  //Port-callback对应存储Map
    private static boolean sDebug = false;
    private static Logger sLogger = Logger.getLogger("@wkp ");
    private static ServerHelper sServerHelper;
    private static Executor sExecutor = Executors.newCachedThreadPool();

    private ServerHelper() {
        mServerSocketMap = new ConcurrentHashMap<>();
        mClientInfoMap = new ConcurrentHashMap<>();
        mClientAddressMap = new ConcurrentHashMap<>();
        mServerCallBackMap = new ConcurrentHashMap<>();
        sExecutor.execute(new CloseRunnable());
    }

    /**
     * 获取帮助类实例
     * @param debug 是否开启debug模式 打印日志
     * @return
     */
    public synchronized static ServerHelper getInstance(boolean debug) {
        sDebug = debug;
        if (sServerHelper == null) {
            synchronized (LOCK) {
                sServerHelper = new ServerHelper();
            }
        }
        return sServerHelper;
    }

    /**
     * 创建服务器
     * @param port 端口号
     * @param callBack 回调接口
     */
    public synchronized void createServer(int port, ServerCallBack callBack) {
        ServerSocket socket = mServerSocketMap.get(port);
        if (socket == null || socket.isClosed()) {
            mServerCallBackMap.put(port, callBack);
            if (sDebug) sLogger.log(Level.SEVERE, "Starting Server!");
            sExecutor.execute(new ServerRunnable(port, callBack));
        } else {
            if (sDebug) sLogger.log(Level.SEVERE, "The port of service has been exist! Port: " + port);
            callBack.onError(new IllegalStateException("The port of service has been exist! Port: " + port));
        }
    }

    /**
     * 发送消息
     * @param port 端口号
     * @param address 客户端IP
     * @param data  数据源
     */
    public synchronized void send(int port, String address, byte[] data) {
        sExecutor.execute(new writeRunnable(port, address, data));
    }

    /**
     * 关闭指定客户端
     * @param address 客户端IP
     * @return
     */
    public synchronized boolean closeClient(String address) {
        try {
            ClientInfo clientInfo = mClientInfoMap.get(address);
            if (clientInfo != null) {
                if (!clientInfo.mSocket.isClosed()) {
                    clientInfo.mSocket.close();
                }
                if (sDebug) sLogger.log(Level.SEVERE, "Connection has been removed. Address: " + address);
                mClientInfoMap.remove(address);
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 关闭指定服务器
     * @param port  端口号
     * @return
     */
    public synchronized boolean closeService(int port) {
        try {
            ServerSocket serverSocket = mServerSocketMap.get(port);
            if (serverSocket != null) {
                if (!serverSocket.isClosed()) {
                    List<String> addresses = mClientAddressMap.get(port);
                    if (addresses != null) {
                        for (String address : addresses) {
                            closeClient(address);
                        }
                    }
                    serverSocket.close();
                }
                if (sDebug) sLogger.log(Level.SEVERE, "Service has been removed. Port: " + port);
                mClientAddressMap.remove(port);
                mServerSocketMap.remove(port);
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 创建服务器任务
     */
    private class ServerRunnable implements Runnable {
        private static final String RUN_LOCK = "run_lock";
        private final int mPort;
        private final ServerCallBack mCallBack;

        ServerRunnable(int port, ServerCallBack callBack) {
            mPort = port;
            mCallBack = callBack;
        }

        @Override
        public void run() {
            synchronized (RUN_LOCK) {
                try {
                    ServerSocket serverSocket = new ServerSocket(mPort);
                    mServerSocketMap.put(mPort, serverSocket);
                    List<String> clientAddress = new ArrayList<>();
                    mClientAddressMap.put(mPort, clientAddress);
                    if (sDebug) sLogger.log(Level.SEVERE, "Service has been started. Waiting for clients: ");
                    Socket socket = null;
                    while (!serverSocket.isClosed() && (socket = serverSocket.accept()) != null) {
                        String address = socket.getInetAddress().getHostAddress();
                        if (sDebug) sLogger.log(Level.SEVERE, "Client has been connected. Address: " + address);
                        clientAddress.add(address);
                        ClientInfo clientInfo = new ClientInfo(socket);
                        mClientInfoMap.put(address, clientInfo);
                        mCallBack.onAccept(address);
                        Thread.sleep(100);
                        sExecutor.execute(new ReadRunnable(mPort, address, mCallBack));
                    }
                } catch (Exception e) {
                    mCallBack.onError(e);
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 读取数据任务
     */
    private class ReadRunnable implements Runnable {

        private final ServerCallBack mCallBack;
        private final String mAddress;
        private final int mPort;

        ReadRunnable(int port, String address, ServerCallBack callBack) {
            mCallBack = callBack;
            mAddress = address;
            mPort = port;
        }

        @Override
        public void run() {
            try {
                ServerSocket serverSocket = mServerSocketMap.get(mPort);
                ClientInfo clientInfo = mClientInfoMap.get(mAddress);
                List<Byte> readData = new ArrayList<>();
                while (!serverSocket.isClosed() && !clientInfo.mSocket.isClosed()) {
                    readData.clear();
                    byte[] buffer = new byte[1024];
                    int len = 0;
                    while ((len = clientInfo.mIs.read(buffer)) != -1) {
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
                        if (sDebug) sLogger.log(Level.SEVERE, "Service has received data! " + mAddress);
                        mCallBack.onReceived(mAddress, result);
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
    private class writeRunnable implements Runnable {
        private final String mAddress;
        private final byte[] mData;
        private final int mPort;

        writeRunnable(int port, String address, byte[] data) {
            mAddress = address;
            mData = data;
            mPort = port;
        }

        @Override
        public void run() {
            ServerCallBack callBack = mServerCallBackMap.get(mPort);
            if (callBack != null) {
                try {
                    ClientInfo clientInfo = mClientInfoMap.get(mAddress);
                    if (clientInfo != null) {
                        if (!clientInfo.mSocket.isClosed()) {
                            clientInfo.mOs.write(mData);
                            clientInfo.mOs.write(END_FLAG);
                            clientInfo.mOs.flush();
                            if (sDebug) sLogger.log(Level.SEVERE, "Service has sent data! " + mAddress);
                            callBack.onSent(mAddress,mData);
                        }else {
                            if (sDebug) sLogger.log(Level.SEVERE, "Connection has disconnected! " + mAddress);
                            callBack.onError(new IllegalStateException("Connection has disconnected! " + mAddress));
                        }
                    } else {
                        if (sDebug) sLogger.log(Level.SEVERE, "Find no Client by the address: " + mAddress);
                        callBack.onError(new IllegalArgumentException("Find no Client by the address: " + mAddress));
                    }
                } catch (IOException e) {
                    callBack.onError(e);
                    e.printStackTrace();
                }
            } else {
                if (sDebug) sLogger.log(Level.SEVERE, "No Server! Please create Server first! Port: " + mPort);
                throw new IllegalStateException("No Server! Please create Server first! Port: " + mPort);
            }
        }
    }

    /**
     * 轮询移除已关闭连接
     */
    private class CloseRunnable implements Runnable{

        @Override
        public void run() {
            while (mServerSocketMap.size() > 0) {
                try {
                    Thread.sleep(30000);
                    for (Map.Entry<String, ClientInfo> clientInfo : mClientInfoMap.entrySet()) {
                        if (clientInfo.getValue().mSocket.isClosed()) {
                            if (sDebug) sLogger.log(Level.SEVERE, "Connection has been removed! Address: " + clientInfo.getKey());
                            mClientInfoMap.remove(clientInfo.getKey());
                        }
                    }
                    for (Map.Entry<Integer, ServerSocket> serverSocket : mServerSocketMap.entrySet()) {
                        if (serverSocket.getValue().isClosed()) {
                            List<String> addresses = mClientAddressMap.get(serverSocket.getKey());
                            if (sDebug) sLogger.log(Level.SEVERE, "Server has been removed! Address: " + serverSocket.getKey());
                            mServerSocketMap.remove(serverSocket.getKey());
                            for (String address : addresses) {
                                closeClient(address);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 客户端信息类
     */
    private class ClientInfo {
        Socket mSocket;
        InputStream mIs;
        OutputStream mOs;

        ClientInfo(Socket socket) throws IOException {
            mSocket = socket;
            mIs = socket.getInputStream();
            mOs = socket.getOutputStream();
        }
    }
}
