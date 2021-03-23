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
package org.apache.mina.common;

import java.net.SocketAddress;
import java.util.Set;

/**
 * A handle which represents connection between two endpoints regardless of
 * transport types.
 * <p>
 * {@link IoSession} provides user-defined attributes.  User-defined attributes
 * are application-specific data which is associated with a session.
 * It often contains objects that represents the state of a higher-level protocol
 * and becomes a way to exchange data between filters and handlers.
 *
 * <h3>Adjusting Transport Type Specific Properties</h3>
 * <p>
 * You can simply downcast the session to an appropriate subclass.
 * </p>
 *
 * <h3>Thread Safety</h3>
 * <p>
 * {@link IoSession} is thread-safe.  But please note that performing
 * more than one {@link #write(Object)} calls at the same time will
 * cause the {@link IoFilter#filterWrite(IoFilter.NextFilter, IoSession, IoFilter.WriteRequest)}
 * is executed simnutaneously, and therefore you have to make sure the
 * {@link IoFilter} implementations you're using are thread-safe, too.
 * </p>
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface IoSession {

    /**
     * 返回为该会话提供I/O服务的IoService
     *
     * @return
     */
    IoService getService();

    /**
     * 返回此会话的Io服务配置
     *
     * @return
     */
    IoServiceConfig getServiceConfig();

    /**
     * session生命周期的回调处理器
     *
     * @return
     */
    IoHandler getHandler();

    /**
     * 返回此会话的配置
     *
     * @return
     */
    IoSessionConfig getConfig();

    /**
     * Returns the filter chain that only affects this session.
     */
    IoFilterChain getFilterChain();

    /**
     * 往客户端回写一个消息
     *
     * @param message
     * @return
     */
    WriteFuture write(Object message);


    /**
     * Returns <code>true</code> if this session is connected with remote peer.
     */
    boolean isConnected();

    /**
     * 关闭session
     *
     * @return
     */
    CloseFuture close();

    /**
     * Returns <code>true</tt> if and only if this session is being closed
     * (but not disconnected yet) or is closed.
     */
    boolean isClosing();

    /**
     * Returns the {@link CloseFuture} of this session.  This method returns the same instance whenever user calls it.
     */
    CloseFuture getCloseFuture();




    /**
     * Returns an attachment of this session.
     * This method is identical with <tt>getAttribute( "" )</tt>.
     */
    Object getAttachment();

    /**
     * Sets an attachment of this session.
     * This method is identical with <tt>setAttribute( "", attachment )</tt>.
     *
     * @return Old attachment.  <tt>null</tt> if it is new.
     */
    Object setAttachment(Object attachment);

    /**
     * Returns the value of user-defined attribute of this session.
     *
     * @param key the key of the attribute
     * @return <tt>null</tt> if there is no attribute with the specified key
     */
    Object getAttribute(String key);

    /**
     * Sets a user-defined attribute.
     *
     * @param key   the key of the attribute
     * @param value the value of the attribute
     * @return The old value of the attribute.  <tt>null</tt> if it is new.
     */
    Object setAttribute(String key, Object value);

    /**
     * 给session设置一个标记属性，默认的value为：{@link Boolean#TRUE}
     *
     * @param key the key of the attribute
     * @return The old value of the attribute.  <tt>null</tt> if it is new.
     */
    Object setAttribute(String key);

    /**
     * 移除属性
     *
     * @param key
     * @return
     */
    Object removeAttribute(String key);

    /**
     * 判断session中是否包含该属性
     *
     * @param key
     * @return
     */
    boolean containsAttribute(String key);

    /**
     * 返回所有的属性
     *
     * @return
     */
    Set<String> getAttributeKeys();


    /**
     * 返回此会话的传输类型
     *
     * @return
     */
    TransportType getTransportType();

    /**
     * 获取客户端地址
     *
     * @return
     */
    SocketAddress getRemoteAddress();

    /**
     * 获取创建socket的本地地址，即服务端地址
     *
     * @return
     */
    SocketAddress getLocalAddress();

    /**
     * 获取服务端地址
     *
     * @return
     */
    SocketAddress getServiceAddress();


    // session读写空闲时间相关配置

    /**
     * 返回指定空闲类型的空闲时间（以秒为单位）
     *
     * @param status
     * @return
     */
    int getIdleTime(IdleStatus status);
    /**
     * 返回指定空闲类型的空闲时间（以毫秒为单位）。
     *
     * @param status
     * @return
     */
    long getIdleTimeInMillis(IdleStatus status);
    /**
     * 为指定的空闲类型设置空闲时间，以秒为单位
     *
     * @param status
     * @param idleTime
     */
    void setIdleTime(IdleStatus status, int idleTime);

    // session写超时时间相关配置

    /**
     * 返回写入超时（以秒为单位）
     *
     * @return
     */
    int getWriteTimeout();
    /**
     * 返回写入超时（以毫秒为单位）
     *
     * @return
     */
    long getWriteTimeoutInMillis();
    /**
     * 设置写超时（以秒为单位）
     *
     * @param writeTimeout
     */
    void setWriteTimeout(int writeTimeout);




    /**
     * Returns the current {@link TrafficMask} of this session.
     */
    TrafficMask getTrafficMask();

    /**
     * Sets the {@link TrafficMask} of this session which will result
     * the parent {@link IoService} to start to control the traffic
     * of this session immediately.
     */
    void setTrafficMask(TrafficMask trafficMask);

    /**
     * A shortcut method for {@link #setTrafficMask(TrafficMask)} that
     * suspends read operations for this session.
     */
    void suspendRead();

    /**
     * A shortcut method for {@link #setTrafficMask(TrafficMask)} that
     * suspends write operations for this session.
     */
    void suspendWrite();

    /**
     * A shortcut method for {@link #setTrafficMask(TrafficMask)} that resumes read operations for this session.
     */
    void resumeRead();

    /**
     * A shortcut method for {@link #setTrafficMask(TrafficMask)} that resumes write operations for this session.
     */
    void resumeWrite();

    /**
     *返回从该会话读取的字节总数
     */
    long getReadBytes();

    /**
     * 返回写入此会话的字节总数
     */
    long getWrittenBytes();

    /**
     * 返回从此会话读取和解码的消息总数
     */
    long getReadMessages();

    /**
     * 返回此会话编写和编码的消息总数
     */
    long getWrittenMessages();

    /**
     * 返回已写入此会话的写请求总数
     */
    long getWrittenWriteRequests();

    /**
     * 返回计划写入此会话的写入请求数
     */
    int getScheduledWriteRequests();

    /**
     * 返回计划写入此会话的字节数
     */
    int getScheduledWriteBytes();

    /**
     * 返回创建此会话时的时间（以毫秒为单位）
     */
    long getCreationTime();

    /**
     * 以毫秒为单位返回上一次发生I/O的时间
     */
    long getLastIoTime();

    /**
     * 返回上一次读取操作发生的时间（以毫秒为单位）
     */
    long getLastReadTime();

    /**
     * 返回上一次发生写操作的时间，以毫秒为单位
     */
    long getLastWriteTime();

    /**
     * 判断该session当前是否处于指定的空闲状态
     *
     * @param status
     * @return
     */
    boolean isIdle(IdleStatus status);

    /**
     * 返回该session发生IdleStatus事件的次数
     *
     * @param status
     * @return
     */
    int getIdleCount(IdleStatus status);

    /**
     * 返回该session最后一次发生IdleStatus事件的时间（单位毫秒）
     */
    long getLastIdleTime(IdleStatus status);

}
