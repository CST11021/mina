package org.apache.mina.example.reverser;

import org.apache.mina.common.*;
import org.apache.mina.filter.LoggingFilter;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketConnector;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

/**
 * @Author: wanghz
 * @Date: 2020/10/19 4:03 PM
 */
public class Client {

    public static void main(String[] args) throws Exception {

        SocketConnector connector = new SocketConnector();
        ConnectFuture future = connector.connect(new InetSocketAddress("127.0.0.1", 8080), new ClientHandler(), new SocketAcceptorConfig());
        // 阻塞直到连接上服务端
        future.join();
        IoSession session = future.getSession();
        session.getFilterChain().addLast("logger", new LoggingFilter());
        session.getFilterChain().addLast("codec", new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName("UTF-8"))));

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String message = br.readLine();

            session.write(message);
        }
    }

    public static class ClientHandler extends IoHandlerAdapter {

        @Override
        public void messageSent(IoSession session, Object message) throws Exception {
            System.out.println("发送消息：" + message.toString());
        }

        @Override
        public void messageReceived(IoSession session, Object message) throws Exception {
            System.out.println("接收消息：" + message.toString());
        }

        @Override
        public void sessionCreated(IoSession session) throws Exception {
            System.out.println("sessionCreated");
        }

        @Override
        public void sessionOpened(IoSession session) throws Exception {
            System.out.println("sessionOpened");
        }

        @Override
        public void sessionClosed(IoSession session) throws Exception {
            System.out.println("sessionClosed");
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
            session.close();
            cause.printStackTrace();
        }
    }

}
