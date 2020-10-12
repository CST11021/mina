package org.apache.mina.example.echoserver.ssl;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import junit.framework.TestCase;

import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.SSLFilter;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.SocketAcceptor;

public class SSLFilterTest extends TestCase {

    private static final int PORT = 17887;

    private IoAcceptor acceptor;

    SocketAddress socketAddress = new InetSocketAddress(PORT);

    protected void setUp() throws Exception {
        super.setUp();
        acceptor = new SocketAcceptor();
    }

    protected void tearDown() throws Exception {
        acceptor.unbindAll();
        super.tearDown();
    }

    public void testMessageSentIsCalled() throws Exception {
        testMessageSentIsCalled(false);
    }

    public void testMessageSentIsCalled_With_SSL() throws Exception {
        testMessageSentIsCalled(true);
    }

    private void testMessageSentIsCalled(boolean useSSL) throws Exception {
        SSLFilter sslFilter = null;
        if (useSSL) {
            sslFilter = new SSLFilter(BogusSSLContextFactory.getInstance(true));
            acceptor.getFilterChain().addLast("sslFilter", sslFilter);
        }
        acceptor.getFilterChain().addLast(
                "codec",
                new ProtocolCodecFilter(new TextLineCodecFactory(Charset
                        .forName("UTF-8"))));

        EchoHandler handler = new EchoHandler();
        acceptor.bind(socketAddress, handler);
        System.out.println("MINA server started.");

        Socket socket = getClientSocket(useSSL);
        int bytesSent = 0;
        bytesSent += writeMessage(socket, "test-1\n");

        if (useSSL) {
            // Test renegotiation
            SSLSocket ss = (SSLSocket) socket;
            //ss.getSession().invalidate();
            ss.startHandshake();
        }

        bytesSent += writeMessage(socket, "test-2\n");
        byte[] response = new byte[bytesSent];
        for (int i = 0; i < response.length; i++) {
            response[i] = (byte) socket.getInputStream().read();
        }
        
        if (useSSL) {
            // Read SSL close notify.
            while (socket.getInputStream().read() >= 0) {
                continue;
            }
        }
        
        socket.close();
        
        long millis = System.currentTimeMillis();
        while (handler.sentMessages.size() < 2
                && System.currentTimeMillis() < millis + 5000) {
            Thread.sleep(200);
        }
        assertEquals("received what we sent", "test-1\ntest-2\n", new String(
                response, "UTF-8"));

        System.out.println("handler: " + handler.sentMessages);
        assertEquals("handler should have sent 2 messages:", 2,
                handler.sentMessages.size());
        assertTrue(handler.sentMessages.contains("test-1"));
        assertTrue(handler.sentMessages.contains("test-2"));
    }

    private int writeMessage(Socket socket, String message) throws Exception {
        byte request[] = message.getBytes("UTF-8");
        socket.getOutputStream().write(request);
        return request.length;
    }

    private Socket getClientSocket(boolean ssl) throws Exception {
        if (ssl) {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustManagers, null);
            return ctx.getSocketFactory().createSocket("localhost", PORT);
        }
        return new Socket("localhost", PORT);
    }

    private static class EchoHandler extends IoHandlerAdapter {

        List<String> sentMessages = new ArrayList<String>();

        public void exceptionCaught(IoSession session, Throwable cause)
                throws Exception {
            cause.printStackTrace();
        }

        public void messageReceived(IoSession session, Object message)
                throws Exception {
            session.write(message);
        }

        public void messageSent(IoSession session, Object message)
                throws Exception {
            sentMessages.add(message.toString());
            System.out.println(message);
            if (sentMessages.size() >= 2) {
                session.close();
            }
        }

    }

    TrustManager[] trustManagers = new TrustManager[] { new TrustAnyone() };

    private static class TrustAnyone implements X509TrustManager {
        public void checkClientTrusted(
                java.security.cert.X509Certificate[] x509Certificates, String s)
                throws CertificateException {
        }

        public void checkServerTrusted(
                java.security.cert.X509Certificate[] x509Certificates, String s)
                throws CertificateException {
        }

        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[0];
        }
    }

}
