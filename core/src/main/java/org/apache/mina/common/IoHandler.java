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

/**
 * Handles all I/O events fired by MINA.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 * 
 * @see IoHandlerAdapter
 */
public interface IoHandler {

    /**
     * 当一个新的连接建立时，由I/O processor thread调用
     *
     * @param session
     * @throws Exception
     */
    void sessionCreated(IoSession session) throws Exception;

    /**
     * 当连接打开时调用
     *
     * @param session
     * @throws Exception
     */
    void sessionOpened(IoSession session) throws Exception;

    /**
     * 当连接关闭时调用
     *
     * @param session
     * @throws Exception
     */
    void sessionClosed(IoSession session) throws Exception;

    /**
     * 当连接进入空闲状态时调用
     *
     * @param session
     * @param status
     * @throws Exception
     */
    void sessionIdle(IoSession session, IdleStatus status) throws Exception;

    /**
     * 当实现IoHandler的类抛出异常时调用
     *
     * @param session
     * @param cause
     * @throws Exception
     */
    void exceptionCaught(IoSession session, Throwable cause) throws Exception;

    /**
     * 当接收到客户端请求的消息时调用
     *
     * @param session
     * @param message
     * @throws Exception
     */
    void messageReceived(IoSession session, Object message) throws Exception;

    /**
     * 当一个消息被(IoSession#write)发送出去后调用
     *
     * @param session
     * @param message
     * @throws Exception
     */
    void messageSent(IoSession session, Object message) throws Exception;
}