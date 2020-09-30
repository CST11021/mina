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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.support.BaseIoAcceptor;
import org.apache.mina.util.NamePreservingRunnable;
import org.apache.mina.util.NewThreadExecutor;

/**
 * {@link IoAcceptor} for socket transport (TCP/IP).
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev: 389042 $, $Date: 2006-03-27 07:49:41Z $
 */
public class SocketAcceptor extends BaseIoAcceptor {

    private final Object lock = new Object();

    private static final AtomicInteger nextId = new AtomicInteger();

    /** 用于命名线程名 */
    private final int id = nextId.getAndIncrement();

    /** 封装服务端的请求/响应处理逻辑 */
    private Worker worker;

    /** 用于执行Worker逻辑的异步执行器 */
    private final Executor executor;

    /** 执行Workder的线程名 */
    private final String threadName = "SocketAcceptor-" + id;

    private SocketAcceptorConfig defaultConfig = new SocketAcceptorConfig();

    /** 保存所有的通道 */
    private final Map<SocketAddress, ServerSocketChannel> channels = new ConcurrentHashMap<SocketAddress, ServerSocketChannel>();

    /** 用于创建Session的请求，当接收到客户端请求时，会创建一个RegistrationRequest到队列中，以便后续创建Session */
    private final Queue<RegistrationRequest> registerQueue = new ConcurrentLinkedQueue<RegistrationRequest>();

    /** 保存服务关闭的请求，Worker中会处理该请求，并将服务关闭 */
    private final Queue<CancellationRequest> cancelQueue = new ConcurrentLinkedQueue<CancellationRequest>();

    /** 用于处理请求的处理器（创建session） */
    private final SocketIoProcessor[] ioProcessors;

    /** 表示ioProcessors的大小 */
    private final int processorCount;

    /** NIO中用于管理通道的Selector */
    private volatile Selector selector;

    private int processorDistributor = 0;




    /**
     * 使用NewThreadExecutor用单个处理线程创建一个接受器，处理客户端请求
     */
    public SocketAcceptor() {
        this(1, new NewThreadExecutor());
    }
    /**
     * Create an acceptor with the desired number of processing threads
     *
     * @param processorCount Number of processing threads
     * @param executor       Executor to use for launching threads
     */
    public SocketAcceptor(int processorCount, Executor executor) {
        if (processorCount < 1) {
            throw new IllegalArgumentException("Must have at least one processor");
        }

        // The default reuseAddress of an accepted socket should be 'true'.
        defaultConfig.getSessionConfig().setReuseAddress(true);

        this.executor = executor;
        this.processorCount = processorCount;
        ioProcessors = new SocketIoProcessor[processorCount];

        for (int i = 0; i < processorCount; i++) {
            ioProcessors[i] = new SocketIoProcessor("SocketAcceptorIoProcessor-" + id + "." + i, executor);
        }
    }




    /**
     * Binds to the specified <code>address</code> and handles incoming connections with the specified
     * <code>handler</code>.  Backlog value is configured to the value of <code>backlog</code> property.
     *
     * @throws IOException if failed to bind
     */
    public void bind(SocketAddress address, IoHandler handler, IoServiceConfig config) throws IOException {
        // 参数校验
        if (handler == null) {
            throw new NullPointerException("handler");
        }
        if (address != null && !(address instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unexpected address type: " + address.getClass());
        }

        // 获取服务配置
        if (config == null) {
            config = getDefaultConfig();
        }


        RegistrationRequest request = new RegistrationRequest(address, handler, config);
        synchronized (lock) {
            // 创建一个线程一直轮询：从registerQueue队列中获取一个请求，然后创建一个通道并注册到Selector上，监听"接收就绪"状态的通道，然后创建Session，交给processor处理
            startupWorker();
            // 添加请求到队列中
            registerQueue.add(request);
            // 换线Worker中会执行selector.select()一直阻塞线程，然后这里调用selector.wakeup()方法来唤醒线程
            selector.wakeup();
        }

        try {
            // this.registerNew()方法，给请求创建对应的通道，然后注册到Selector后，会调用req.done.countDown()
            request.done.await();
        } catch (InterruptedException e) {
            ExceptionMonitor.getInstance().exceptionCaught(e);
        }

        if (request.exception != null) {
            throw request.exception;
        }
    }

    /**
     * 断开所有客户端的连接，并关闭服务
     *
     * @param address
     */
    public void unbind(SocketAddress address) {
        if (address == null) {
            throw new NullPointerException("address");
        }

        CancellationRequest request = new CancellationRequest(address);

        synchronized (lock) {
            try {
                startupWorker();
            } catch (IOException e) {
                // IOException is thrown only when Worker thread is not
                // running and failed to open a selector.  We simply throw
                // IllegalArgumentException here because we can simply
                // conclude that nothing is bound to the selector.
                throw new IllegalArgumentException("Address not bound: " + address);
            }

            // 添加一个服务关闭的请求，Worker中会处理该请求，并将服务关闭
            cancelQueue.add(request);

            selector.wakeup();
        }

        try {
            request.done.await();
        } catch (InterruptedException e) {
            ExceptionMonitor.getInstance().exceptionCaught(e);
        }

        if (request.exception != null) {
            request.exception.fillInStackTrace();

            throw request.exception;
        }
    }

    /**
     * 断开所有客户端的连接，并关闭服务
     */
    public void unbindAll() {
        List<SocketAddress> addresses = new ArrayList<SocketAddress>(channels.keySet());

        for (SocketAddress address : addresses) {
            unbind(address);
        }
    }

    /**
     * 创建一个线程一直轮询：从registerQueue队列中获取一个请求，然后创建一个通道并注册到Selector上，监听"接收就绪"状态的通道，然后创建Session，交给processor处理
     *
     * @throws IOException
     */
    private void startupWorker() throws IOException {
        synchronized (lock) {
            if (worker == null) {
                selector = Selector.open();
                worker = new Worker();
                executor.execute(new NamePreservingRunnable(worker, threadName));
            }
        }
    }

    private SocketIoProcessor nextProcessor() {
        if (this.processorDistributor == Integer.MAX_VALUE) {
            this.processorDistributor = Integer.MAX_VALUE % this.processorCount;
        }

        return ioProcessors[processorDistributor++ % processorCount];
    }

    /**
     * 从registerQueue获取一个请求，然后创建一个通道并注册到Selector上
     */
    private void registerNew() {
        if (registerQueue.isEmpty()) {
            return;
        }

        Selector selector = this.selector;
        for (; ; ) {

            RegistrationRequest req = registerQueue.poll();
            if (req == null) {
                break;
            }

            ServerSocketChannel ssc = null;
            try {
                ssc = ServerSocketChannel.open();
                ssc.configureBlocking(false);

                // 获取socket配置
                SocketAcceptorConfig cfg;
                if (req.config instanceof SocketAcceptorConfig) {
                    cfg = (SocketAcceptorConfig) req.config;
                } else {
                    cfg = getDefaultConfig();
                }


                // 设置该选项：public void setResuseAddress(boolean on)throws SocketException
                // 读取该选项 public void getResuseAddress(boolean on)throws SocketException
                // 当接受方通过Socket的close()方法关闭Socket时，如果网络上还有发送到这个Socket的数据，那么底层的Socket不会立刻释放本地端口，而是会等待一段时间，确保收到了网络上发送过来的延迟数据，然再释放该端口。Socket接受到延迟数据后，不会对这些数据做任何处理。Socket接受延迟数据的目的是，确保这些数据不会被其他碰巧绑定到同样端口的新进程接收到。
                // 客户程序一般采用随机端口，因此会出现两个客户端程序绑定到同样端口的可能性不大。许多服务器都使用固定的端口。当服务器进程关闭后，有可能它的端口还会被占用一段时间，如果此时立刻在同一主机上重启服务器程序，由于端口已经被占用，使得服务器无法绑定到该端口，启动失败。为了确保一个进程被关闭后，及时它还没有释放该端口，同一个主机上的其他进程还可以立刻重用该端口，可以调用Socket的setResuseAddress(true)方法：
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
                ssc.socket().setReuseAddress(cfg.isReuseAddress());
                // Scoket缓冲区必须在连接之前去创建：缓冲区大小需要根据具体情况进行设置，一般要低于64K（TCP能够指定的最大负重载数据量，TCP的窗口大小是由16bit来确定的），增大缓冲区可以增大网络I/O的性能，而减少缓冲区有助于减少传入数据的backlog（就是缓冲长度，因此提高响应速度）。对于Socket和SeverSocket如果需要指定缓冲区大小，必须在连接之前完成缓冲区的设定。
                ssc.socket().setReceiveBufferSize(cfg.getSessionConfig().getReceiveBufferSize());

                // backlog参数提示内核监听队列的最大长度。如果监听队列的长度超过backlog，服务器将不受理新的客户连接，客户端也将收到ECONNREFUSED错误信息。在内核版本2.2之前，backlog是指所有处于半连接状态（SYN_RCVD）和完全连接状态（ESTABLISHED）的socket上限。但在内核版本2.2以后， 它只表示处于完全连接状态的socket上限，处于半连接状态的socket上限则由/proc/sys/net/ipv4/tcp_max_syn_backlog内核参数定义。
                ssc.socket().bind(req.address, cfg.getBacklog());
                if (req.address == null || req.address.getPort() == 0) {
                    req.address = (InetSocketAddress) ssc.socket().getLocalSocketAddress();
                }

                // 向Selector注册通道，并监听"准备就绪"事件，并向事件上添加附加信息RegistrationRequest，在NIO中，一个Selector可以管理多个通道
                ssc.register(selector, SelectionKey.OP_ACCEPT, req);

                channels.put(req.address, ssc);

                // 当服务启动时，触发监听
                getListeners().fireServiceActivated(this, req.address, req.handler, req.config);
            } catch (IOException e) {
                req.exception = e;
            } finally {
                // 请求创建完通道后，会将请求的标记为处理完成
                req.done.countDown();

                if (ssc != null && req.exception != null) {
                    try {
                        ssc.close();
                    } catch (IOException e) {
                        ExceptionMonitor.getInstance().exceptionCaught(e);
                    }
                }
            }
        }
    }

    /**
     * 如果cancelQueue不为空，说明服务要求挂壁，则移除Selector监听的事件，并关闭通道
     */
    private void cancelKeys() {
        if (cancelQueue.isEmpty()) {
            return;
        }

        Selector selector = this.selector;
        for (; ; ) {
            CancellationRequest request = cancelQueue.poll();
            if (request == null) {
                break;
            }

            ServerSocketChannel ssc = channels.remove(request.address);

            // 关闭通道
            try {
                if (ssc == null) {
                    request.exception = new IllegalArgumentException("Address not bound: " + request.address);
                } else {
                    SelectionKey key = ssc.keyFor(selector);
                    request.registrationRequest = (RegistrationRequest) key.attachment();
                    key.cancel();

                    // 再次唤醒以触发线程死亡，
                    // 某个线程调用select()方法后阻塞了，即使没有通道已经就绪，也有办法让其从select()方法返回。只要让其它线程在第一个线程调用select()方法的那个对象上调用Selector.wakeup()方法即可。阻塞在select()方法上的线程会立马返回。
                    // 如果有其它线程调用了wakeup()方法，但当前没有线程阻塞在select()方法上，下个调用select()方法的线程会立即“醒来（wake up）”。
                    selector.wakeup();
                    ssc.close();
                }
            } catch (IOException e) {
                ExceptionMonitor.getInstance().exceptionCaught(e);
            } finally {
                request.done.countDown();

                if (request.exception == null) {
                    // 当服务关闭后触发监听
                    getListeners().fireServiceDeactivated(this,
                            request.address,
                            request.registrationRequest.handler,
                            request.registrationRequest.config);
                }
            }
        }
    }

    public SocketAcceptorConfig getDefaultConfig() {
        return defaultConfig;
    }
    /**
     * Sets the config this acceptor will use by default.
     *
     * @param defaultConfig the default config.
     * @throws NullPointerException if the specified value is <code>null</code>.
     */
    public void setDefaultConfig(SocketAcceptorConfig defaultConfig) {
        if (defaultConfig == null) {
            throw new NullPointerException("defaultConfig");
        }
        this.defaultConfig = defaultConfig;
    }

    /**
     * 一直轮询：从registerQueue获取一个请求，然后创建一个通道并注册到Selector上，监听"接收就绪"状态的通道，然后创建Session，交给processor处理
     */
    private class Worker implements Runnable {

        public void run() {
            Selector selector = SocketAcceptor.this.selector;
            for (; ; ) {
                try {
                    // 阻塞到至少有一个通道在你注册的事件上就绪了，select()方法返回的int值表示有多少通道已经就绪。即，自上次调用select()方法后有多少通道变成就绪状态。
                    // 如果调用select()方法，因为有一个通道变成就绪状态，返回了1，若再次调用select()方法，如果另一个通道就绪了，它会再次返回1。
                    // 如果对第一个就绪的channel没有做任何操作，现在就有两个就绪的通道，但在每次select()方法调用之间，只有一个通道就绪了。
                    int nKeys = selector.select();

                    // 从registerQueue获取一个请求，然后创建一个通道并注册到Selector上
                    registerNew();

                    if (nKeys > 0) {
                        // 从Selector获取"接收就绪"状态的通道，然后创建Session，交给processor处理
                        processSessions(selector.selectedKeys());
                    }

                    // 如果cancelQueue不为空，说明服务要求挂壁，则移除Selector监听的事件，并关闭通道
                    cancelKeys();

                    // selector监听的事件类型为空时，则关闭selector
                    if (selector.keys().isEmpty()) {
                        synchronized (lock) {
                            if (selector.keys().isEmpty() && registerQueue.isEmpty() && cancelQueue.isEmpty()) {
                                worker = null;
                                try {
                                    selector.close();
                                } catch (IOException e) {
                                    ExceptionMonitor.getInstance().exceptionCaught(e);
                                } finally {
                                    SocketAcceptor.this.selector = null;
                                }
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        ExceptionMonitor.getInstance().exceptionCaught(e1);
                    }
                }
            }
        }

        /**
         * 从Selector获取"接收就绪"状态的通道，然后创建Session，交给processor处理
         *
         * @param keys
         * @throws IOException
         */
        private void processSessions(Set<SelectionKey> keys) throws IOException {
            // 遍历Selector监听的事件类型
            Iterator<SelectionKey> it = keys.iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();

                // 如果不是“接收就绪”事件（即，准备好了接收来自客户端请求）则跳过
                if (!key.isAcceptable()) {
                    continue;
                }

                // 获取通道
                ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                SocketChannel ch = ssc.accept();
                if (ch == null) {
                    continue;
                }

                boolean success = false;
                try {
                    // 从监听事件上获取附加信息
                    RegistrationRequest req = (RegistrationRequest) key.attachment();
                    SocketSessionImpl session = new SocketSessionImpl(SocketAcceptor.this, nextProcessor(), getListeners(), req.config, ch, req.handler, req.address);

                    // 添加过滤器链
                    getFilterChainBuilder().buildFilterChain(session.getFilterChain());
                    req.config.getFilterChainBuilder().buildFilterChain(session.getFilterChain());
                    req.config.getThreadModel().buildFilterChain(session.getFilterChain());

                    // 将会话交给processor处理
                    session.getIoProcessor().addNew(session);
                    success = true;
                } catch (Throwable t) {
                    ExceptionMonitor.getInstance().exceptionCaught(t);
                } finally {
                    if (!success) {
                        ch.close();
                    }
                }
            }
        }

    }

    /**
     * 标识一个启动服务的请求
     */
    private static class RegistrationRequest {
        private InetSocketAddress address;

        private final IoHandler handler;

        private final IoServiceConfig config;

        private final CountDownLatch done = new CountDownLatch(1);

        private volatile IOException exception;

        private RegistrationRequest(SocketAddress address, IoHandler handler, IoServiceConfig config) {
            this.address = (InetSocketAddress) address;
            this.handler = handler;
            this.config = config;
        }
    }

    /**
     * 表示一个关闭服务的请求
     */
    private static class CancellationRequest {

        private final SocketAddress address;

        private final CountDownLatch done = new CountDownLatch(1);

        private RegistrationRequest registrationRequest;

        private volatile RuntimeException exception;

        private CancellationRequest(SocketAddress address) {
            this.address = address;
        }
    }
}
