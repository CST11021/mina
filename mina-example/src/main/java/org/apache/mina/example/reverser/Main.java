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
package org.apache.mina.example.reverser;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import org.apache.mina.common.*;
import org.apache.mina.filter.LoggingFilter;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketConnector;

/**
 * 该服务用于测试反转字符串
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$,
 */
public class Main {
    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        IoAcceptor acceptor = new SocketAcceptor();

        // Prepare the configuration
        SocketAcceptorConfig cfg = new SocketAcceptorConfig();
        cfg.setReuseAddress(true);
        cfg.getFilterChain().addLast("logger", new LoggingFilter());
        cfg.getFilterChain().addLast("codec", new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName("UTF-8"))));

        // Bind
        acceptor.bind(new InetSocketAddress(PORT), new ReverseProtocolHandler(), cfg);

        System.out.println("Listening on port " + PORT);

    }

    public static class ReverseProtocolHandler extends IoHandlerAdapter {

        /**
         * 将收到字符串消息进行反转，然后回写，例如：
         * request：123abc
         * response：cba321
         *
         * @param session
         * @param message
         */
        @Override
        public void messageReceived(IoSession session, Object message) {
            System.out.println("接收到消息：" + message.toString());
            String str = message.toString();
            StringBuffer buf = new StringBuffer(str.length());
            for (int i = str.length() - 1; i >= 0; i--) {
                buf.append(str.charAt(i));
            }

            // session.write("response:" + buf.toString());
            write(session, message.toString());
        }

        /**
         * 使用这种方式，telnet的时候才能看到返回的结果
         *
         * @param session
         * @param message
         */
        private void write(IoSession session, String message) {
            ByteBuffer wb = ByteBuffer.allocate(1024);
            wb.put(message.getBytes());
            wb.flip();
            session.write(wb);
        }

        @Override
        public void sessionClosed(IoSession session) throws Exception {
            System.out.println("sessionClosed");
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause) {
            cause.printStackTrace();
            session.close();
        }

    }

}
