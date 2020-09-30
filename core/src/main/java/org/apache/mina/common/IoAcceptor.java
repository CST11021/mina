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

import java.io.IOException;
import java.net.SocketAddress;

/**
 * Accepts incoming connection, communicates with clients, and fires events to
 * {@link IoHandler}s.
 * <p>
 * Please refer to
 * <a href="../../../../../xref-examples/org/apache/mina/examples/echoserver/Main.html">EchoServer</a>
 * example.
 * <p>
 * You should bind to the desired socket address to accept incoming
 * connections, and then events for incoming connections will be sent to
 * the specified default {@link IoHandler}.
 * <p>
 * Threads accept incoming connections start automatically when
 * {@link #bind(SocketAddress, IoHandler)} is invoked, and stop when all
 * addresses are unbound.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface IoAcceptor extends IoService {

    /**
     * 启动服务（开始监听客户端请求）
     *
     * @param address
     * @param handler
     * @throws IOException
     */
    void bind(SocketAddress address, IoHandler handler) throws IOException;

    /**
     * 启动服务（开始监听客户端请求）
     *
     * @param address
     * @param handler
     * @param config
     * @throws IOException
     */
    void bind(SocketAddress address, IoHandler handler, IoServiceConfig config) throws IOException;

    /**
     * 断开所有客户端的连接，并关闭服务
     *
     * @param address
     */
    void unbind(SocketAddress address);

    /**
     * 取消绑定此接受者绑定的所有地址
     */
    void unbindAll();

    /**
     * 创建一个会话
     *
     * @param remoteAddress     客户端地址
     * @param localAddress      服务端地址
     * @return
     */
    IoSession newSession(SocketAddress remoteAddress, SocketAddress localAddress);

}