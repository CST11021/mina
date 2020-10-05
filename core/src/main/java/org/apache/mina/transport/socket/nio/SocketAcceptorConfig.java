/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.mina.transport.socket.nio;

import java.io.IOException;
import java.net.ServerSocket;

import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoAcceptorConfig;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.common.support.BaseIoAcceptorConfig;

/**
 * An {@link IoAcceptorConfig} for {@link SocketAcceptor}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class SocketAcceptorConfig extends BaseIoAcceptorConfig {

    private SocketSessionConfig sessionConfig = new SocketSessionConfigImpl();

    /** Socket绑定端口时候会用到该参数，backlog参数内核监听队列的最大长度，如果监听队列的长度超过backlog，服务器将不受理新的客户连接，客户端也将收到ECONNREFUSED错误信息 */
    private int backlog = 50;

    // 设置该选项：public void setResuseAddress(boolean on)throws SocketException
    // 读取该选项 public void getResuseAddress(boolean on)throws SocketException
    // 当接受方通过Socket的close()方法关闭Socket时，如果网络上还有发送到这个Socket的数据，那么底层的Socket不会立刻释放本地端口，而是会等待一段时间，确保收到了网络上发送过来的延迟数据，然再释放该端口。Socket接受到延迟数据后，不会对这些数据做任何处理。Socket接受延迟数据的目的是，确保这些数据不会被其他碰巧绑定到同样端口的新进程接收到。
    // 客户程序一般采用随机端口，因此会出现两个客户端程序绑定到同样端口的可能性不大。许多服务器都使用固定的端口。当服务器进程关闭后，有可能它的端口还会被占用一段时间，如果此时立刻在同一主机上重启服务器程序，由于端口已经被占用，使得服务器无法绑定到该端口，启动失败。为了确保一个进程被关闭后，即使它还没有释放该端口，同一个主机上的其他进程还可以立刻重用该端口，可以调用Socket的setResuseAddress(true)方法：
    //
    // if(!socket.getResuseAddress())
    //     socket.setResuseAddress(true);
    //
    // 值得注意的是：socket.setResuseAddress(true)方法必须在Socket还没有绑定到一个本地端口之前调用，否则执行socket.setResuseAddress(true)方法无效
    // 因此必须按照以下方法创建Socket对象，然后在连接远程服务器：
    //
    // Socket socket=new Socket();//此时socket端口未绑定本地端口，并且未连接远程服务器
    // socket.setReuseAddress(true);
    // SocketAddress socketAddress=new InetSocketAddress("remotehost",8000);
    // socket.connect(socketAddress);
    //
    // //或者
    // Socket socket=new Socket();//此时socket端口未绑定本地端口，并且未连接远程服务器
    // socket.setReuseAddress(true);
    // socketAddress localAddr=new InetSocketAddress("localhost",9000);
    //
    // socketAddress remoteAddr=new InetSocketAddress("remotehost",8000);
    // socket.bind(localAddr);//与本地端口绑定
    // socket.connect(remoteAddr);//连接远程服务器
    //
    // 此外，两个共用同一个端口的进程必须都调用socket.setReuseAddress(true)放方法才能使得一个进程关闭Socket后，另一个进程的Socket能够立刻重用相同的端口。
    private boolean reuseAddress;

    /**
     * Creates a new instance.
     *
     * @throws RuntimeIOException if failed to get the default configuration
     */
    public SocketAcceptorConfig() {
        ServerSocket s = null;
        try {
            s = new ServerSocket();
            reuseAddress = s.getReuseAddress();
        } catch (IOException e) {
            throw new RuntimeIOException(
                    "Failed to get the default configuration.", e);
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
        }

        sessionConfig.setReuseAddress(true);
    }

    public SocketSessionConfig getSessionConfig() {
        return sessionConfig;
    }



    public boolean isReuseAddress() {
        return reuseAddress;
    }
    public void setReuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }
    public int getBacklog() {
        return backlog;
    }
    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }
}
