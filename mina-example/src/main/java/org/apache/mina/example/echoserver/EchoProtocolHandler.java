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
package org.apache.mina.example.echoserver;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.TransportType;
import org.apache.mina.filter.SSLFilter;
import org.apache.mina.transport.socket.nio.SocketSessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link IoHandler} implementation for echo server. 
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$,
 */
public class EchoProtocolHandler extends IoHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(EchoProtocolHandler.class);

    public void sessionCreated(IoSession session) {
        if (session.getTransportType() == TransportType.SOCKET) {
            ((SocketSessionConfig) session.getConfig()).setReceiveBufferSize(2048);
        }

        // 设置session读写空闲时间为10秒，在10秒内，如果终端（客户端或者服务端）没有对session进行读写操作，则触发sessionIdle()方法
        session.setIdleTime(IdleStatus.BOTH_IDLE, 10);

        // We're going to use SSL negotiation notification.
        session.setAttribute(SSLFilter.USE_NOTIFICATION);
    }

    /**
     * 当接收到消息时调用该方法，这里是从客户端什么消息就回复什么消息
     *
     * @param session
     * @param message
     * @throws Exception
     */
    public void messageReceived(IoSession session, Object message) throws Exception {
        if (!(message instanceof ByteBuffer)) {
            return;
        }

        ByteBuffer rb = (ByteBuffer) message;
        // Write the received data back to remote peer
        ByteBuffer wb = ByteBuffer.allocate(rb.remaining());
        wb.put(rb);
        // 将一个处于存数据状态的缓冲区变为一个处于准备取数据的状态，当往session写数据时，需要将数据从缓冲区读出来
        wb.flip();
        session.write(wb);
    }

    /**
     * 当连接进入空闲状态时调用：即在session中设置的各种空闲类型的时间，如果再指定时间内，终端（客户端或者服务端）没有对该session进行读写操作，则触发该方法
     *
     * @param session
     * @param status
     * @throws Exception
     */
    public void sessionIdle(IoSession session, IdleStatus status) {
        log.info("*** IDLE #" + session.getIdleCount(IdleStatus.BOTH_IDLE) + " ***");
    }

    public void exceptionCaught(IoSession session, Throwable cause) {
        cause.printStackTrace();
        session.close();
    }

}