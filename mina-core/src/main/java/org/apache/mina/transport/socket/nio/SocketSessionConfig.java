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

import java.net.Socket;

import org.apache.mina.common.IoSessionConfig;

/**
 * 都是与Socket相关的配置
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface SocketSessionConfig extends IoSessionConfig {

    // 端口复用配置：Socket#getReuseAddress()

    /**
     * @see Socket#getReuseAddress()
     */
    boolean isReuseAddress();
    /**
     * @see Socket#setReuseAddress(boolean)
     */
    void setReuseAddress(boolean reuseAddress);

    // 接收数据的大小限制：Socket#getReceiveBufferSize()

    /**
     * @see Socket#getReceiveBufferSize()
     */
    int getReceiveBufferSize();
    /**
     * @see Socket#setReceiveBufferSize(int)
     */
    void setReceiveBufferSize(int receiveBufferSize);

    // 发送数据的大小限制：Socket#getSendBufferSize()

    /**
     * @see Socket#getSendBufferSize()
     */
    int getSendBufferSize();
    /**
     * @see Socket#setSendBufferSize(int)
     */
    void setSendBufferSize(int sendBufferSize);

    // Socket#getTrafficClass()方法的作用是为从此Socket上发送的包获取IP头中的流量类别或服务类型，当向IP头中设置了流量类型后，路由器或交换机就会根据这个流量类型来进行不同的处理，同时必须要硬件设备进行参与处理

    /**
     * @see Socket#getTrafficClass()
     */
    int getTrafficClass();
    /**
     * @see Socket#setTrafficClass(int)
     */
    void setTrafficClass(int trafficClass);


    // keepalive选项，socket连接建立之后，只要双方均未主动关闭连接，那这个连接就是会一直保持的，keepalive只是为了防止连接的双方发生意外而通知不到对方，导致一方还持有连接，占用资源，该选项的意思是TCP连接空闲时是否需要向对方发送探测包，实际上是依赖于底层的TCP模块实现的，java中只能设置是否开启，不能设置其详细参数，只能依赖于系统配置。
    // 表示底层的TCP 实现会监视该连接是否有效。默认值为 false, 表示TCP 不会监视连接是否有效, 不活动的客户端可能会永远存在下去, 而不会注意到服务器已经崩溃。（当连接处于空闲状态(连接的两端没有互相传送数据) 超过了 2 小时时, 本地的TCP 实现会发送一个数据包给远程的 Socket. 如果远程Socket 没有发回响应, TCP实现就会持续尝试 11 分钟, 直到接收到响应为止. 如果在 12 分钟内未收到响应, TCP 实现就会自动关闭本地Socket, 断开连接. 在不同的网络平台上, TCP实现尝试与远程Socket 对话的时限有所差别.）

    /**
     * @see Socket#getKeepAlive()
     */
    boolean isKeepAlive();
    /**
     * @see Socket#setKeepAlive(boolean)
     */
    void setKeepAlive(boolean keepAlive);

    //默认值为 false。为 true 时, 表示支持发送一个字节的 TCP 紧急数据. Socket 类的 sendUrgentData(int data) 方法用于发送一个字节的 TCP紧急数据。 为 false的这种情况下, 当接收方收到紧急数据时不作任何处理, 直接将其丢弃. 如果用户希望发送紧急数据, 应该把 OOBINLINE 设为 true。

    /**
     * @see Socket#getOOBInline()
     */
    boolean isOobInline();
    /**
     * @see Socket#setOOBInline(boolean)
     */
    void setOobInline(boolean oobInline);

    // 启用/禁用具有指定逗留时间（以秒为单位）的 SO_LINGER。最大超时值是特定于平台的。 该设置仅影响套接字关闭。默认值为-1，表示禁用。
    // 这个 Socket 选项可以影响close 方法的行为。在默认情况下，当调用close 方法后，将立即返回；如果这时仍然有未被送出的数据包，那么这些数据包将被丢弃。如果将linger 参数设为一个正整数n 时(n 的值最大是65,535) ，在调用close 方法后，将最多被阻塞n 秒。在这n 秒内，系统将尽量将未送出的数据包发送出去；如果超过了n 秒，如果还有未发送的数据包，这些数据包将全部被丢弃；而close 方法会立即返回。如果将linger 设为0 ，和关闭SO_LINGER 选项的作用是一样的。

    /**
     * Please note that enabling <tt>SO_LINGER</tt> in Java NIO can result
     * in platform-dependent behavior and unexpected blocking of I/O thread.
     * 
     * @see Socket#getSoLinger()
     * @see <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6179351">Sun Bug Database</a>
     */
    int getSoLinger();
    /**
     * Please note that enabling <tt>SO_LINGER</tt> in Java NIO can result
     * in platform-dependent behavior and unexpected blocking of I/O thread.
     * 
     * @param soLinger Please specify a negative value to disable <tt>SO_LINGER</tt>.
     * 
     * @see Socket#setSoLinger(boolean, int)
     * @see <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6179351">Sun Bug Database</a>
     */
    void setSoLinger(int soLinger);

    //
    // 关于TcpNoDelay参数的作用：https://blog.csdn.net/huang_xw/article/details/7340241

    /**
     * @see Socket#getTcpNoDelay()
     */
    boolean isTcpNoDelay();
    /**
     * @see Socket#setTcpNoDelay(boolean)
     */
    void setTcpNoDelay(boolean tcpNoDelay);
}
