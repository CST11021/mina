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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteTimeoutException;
import org.apache.mina.common.IoFilter.WriteRequest;
import org.apache.mina.util.NamePreservingRunnable;

/**
 * Performs all I/O operations for sockets which is connected or bound. This class is used by MINA internally.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$,
 */
class SocketIoProcessor {

    /**
     * The maximum loop count for a write operation until
     * {@link #write(IoSession, IoBuffer)} returns non-zero value.
     * It is similar to what a spin lock is for in concurrency programming.
     * It improves memory utilization and write throughput significantly.
     */
    private static final int WRITE_SPIN_COUNT = 256;

    private final Object lock = new Object();

    /** 处理器执行线程的线程名 */
    private final String threadName;
    /** 封装Processor处理器的执行逻辑 */
    private Worker worker;
    /** 用于执行#Worker的执行器，对应SocketAcceptor#executor */
    private final Executor executor;


    private volatile Selector selector;

    private long lastIdleCheckTime = System.currentTimeMillis();

    /** 该队列用于保存有待处理的session：当session创建时，会保存到该队列中 */
    private final Queue<SocketSessionImpl> newSessions = new ConcurrentLinkedQueue<SocketSessionImpl>();
    /** 当读取完客户端的请求数据后，会将session放到该队列中 */
    private final Queue<SocketSessionImpl> removingSessions = new ConcurrentLinkedQueue<SocketSessionImpl>();
    /** 服务端处理完客户端请求后，会将响应数据write到了session中，消费该队列是将Session中的响应数据进行flush（即将响应数据发送给客户端） */
    private final Queue<SocketSessionImpl> flushingSessions = new ConcurrentLinkedQueue<SocketSessionImpl>();

    private final Queue<SocketSessionImpl> trafficControllingSessions = new ConcurrentLinkedQueue<SocketSessionImpl>();


    SocketIoProcessor(String threadName, Executor executor) {
        this.threadName = threadName;
        this.executor = executor;
    }

    /**
     * 添加一个Session，并启动Worker线程处理Session：当有客户请求时，会创建一个Session然后调用该方法，交由Processor来管理session
     *
     * @param session
     * @throws IOException
     */
    void addNew(SocketSessionImpl session) throws IOException {
        newSessions.add(session);
        startupWorker();
    }

    void remove(SocketSessionImpl session) throws IOException {
        scheduleRemove(session);
        startupWorker();
    }

    /**
     * 启动Workder线程
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
            selector.wakeup();
        }
    }

    /**
     * 将session放到flushingSessions队列中，便于后续将响应数据发给客户端
     *
     * @param session
     */
    void flush(SocketSessionImpl session) {
        if (scheduleFlush(session)) {
            Selector selector = this.selector;
            if (selector != null) {
                selector.wakeup();
            }
        }
    }

    void updateTrafficMask(SocketSessionImpl session) {
        scheduleTrafficControl(session);
        Selector selector = this.selector;
        if (selector != null) {
            selector.wakeup();
        }
    }

    /**
     * 将session放到移除队列中，当服务端向客户端发送响应数据后，会处理该session
     *
     * @param session
     */
    private void scheduleRemove(SocketSessionImpl session) {
        removingSessions.add(session);
    }

    /**
     * 释放session中的缓存数据（即向客户端发送响应的数据）
     *
     * @param session
     * @return
     */
    private boolean scheduleFlush(SocketSessionImpl session) {
        if (session.setScheduledForFlush(true)) {
            flushingSessions.add(session);
            return true;
        }

        return false;
    }

    private void scheduleTrafficControl(SocketSessionImpl session) {
        trafficControllingSessions.add(session);
    }

    /**
     * 从newSessions获取一个session，获取session中的通道，注册到Selector，并监听SelectionKey.OP_READ事件（开始监听来自客户端的请求）
     */
    private void doAddNew() {
        Selector selector = this.selector;
        for (; ; ) {
            SocketSessionImpl session = newSessions.poll();

            if (session == null)
                break;

            SocketChannel ch = session.getChannel();
            try {
                ch.configureBlocking(false);
                session.setSelectionKey(ch.register(selector, SelectionKey.OP_READ, session));

                // 这里触发一次session创建事件回调
                session.getServiceListeners().fireSessionCreated(session);
            } catch (IOException e) {
                // Clear the AbstractIoFilterChain.CONNECT_FUTURE attribute
                // and call ConnectFuture.setException().
                session.getFilterChain().fireExceptionCaught(session, e);
            }
        }
    }


    /**
     * 处理通道监听的事件：
     * 1、当监听的"读就绪事件"时，则读取Channel中客户端的请求数据，并触发过滤器的fireMessageReceived()事件回调
     * 2、当监听的是可写状态时，将session放到flushingSessions队列中，后续的doFlush()方法，会轮询flushingSessions队列，处理session，向客户端回写响应数据
     *
     *
     * @param selectedKeys
     */
    private void process(Set<SelectionKey> selectedKeys) {
        for (SelectionKey key : selectedKeys) {
            SocketSessionImpl session = (SocketSessionImpl) key.attachment();

            // 从session中获取客户端的请求数据，并处理请求
            if (key.isReadable() && session.getTrafficMask().isReadable()) {
                read(session);
            }

            // 向客户端发送响应数据
            if (key.isWritable() && session.getTrafficMask().isWritable()) {
                scheduleFlush(session);
            }
        }

        selectedKeys.clear();
    }

    /**
     * 从session中获取客户端的请求数据，并处理请求，这里的会调用过滤器的fireMessageReceived()
     *
     * @param session
     */
    private void read(SocketSessionImpl session) {
        // 用于缓存客户端的请求数据
        ByteBuffer buf = ByteBuffer.allocate(session.getReadBufferSize());
        SocketChannel ch = session.getChannel();
        try {
            int readBytes = 0;
            int ret;

            try {
                // read()方法返回的int值表示读了多少字节进Buffer里。如果返回的是-1，表示已经读到了流的末尾
                while ((ret = ch.read(buf.buf())) > 0) {
                    readBytes += ret;
                }
            } finally {
                buf.flip();
            }

            session.increaseReadBytes(readBytes);

            if (readBytes > 0) {
                // 服务端接收到客户端的请求后，调用fireMessageReceived(session, buf)方法进行处理
                session.getFilterChain().fireMessageReceived(session, buf);
                buf = null;

                if (readBytes * 2 < session.getReadBufferSize()) {
                    session.decreaseReadBufferSize();
                } else if (readBytes == session.getReadBufferSize()) {
                    session.increaseReadBufferSize();
                }
            }

            if (ret < 0) {
                // 当读取完客户端的请求数据后，将session放到移除队列中
                scheduleRemove(session);
            }
        } catch (Throwable e) {
            if (e instanceof IOException)
                scheduleRemove(session);
            session.getFilterChain().fireExceptionCaught(session, e);
        } finally {
            // 读取完客户端的请求数据后，要释放缓存
            if (buf != null)
                buf.release();
        }
    }

    /**
     * session处理完一次请求后，会从removingSessions队列中移除session，将session中的通道关闭，断开与客户端的连接，然后清除session的中保存的响应数据的缓存，并触发fireSessionDestroyed()监听器方法
     */
    private void doRemove() {
        for (; ; ) {
            SocketSessionImpl session = removingSessions.poll();
            if (session == null)
                break;

            SocketChannel ch = session.getChannel();
            SelectionKey key = session.getSelectionKey();
            // 如果会话尚未完全初始化，请稍后重试
            if (key == null) {
                scheduleRemove(session);
                break;
            }

            // 如果通道已经关闭，则跳过
            if (!key.isValid()) {
                continue;
            }

            // 关闭NIO的监听和通道
            try {
                key.cancel();
                ch.close();
            } catch (IOException e) {
                session.getFilterChain().fireExceptionCaught(session, e);
            } finally {
                // 走到这里，已经向客户端发送完响应数据了，这里会释放session中保存的响应数据的缓存
                releaseWriteBuffers(session);
                // 触发session销毁的监听方法
                session.getServiceListeners().fireSessionDestroyed(session);
            }

        }
    }

    /**
     * 处理完一次请求后进行事件统计，并检查是否发生了WRITER_IDLE事件，如果发生了则触发一次fireExceptionCaught事件回调
     */
    private void notifyIdleness() {
        // process idle sessions
        long currentTime = System.currentTimeMillis();
        // 上一次空闲检查事件距离当前事件超过一秒
        if ((currentTime - lastIdleCheckTime) >= 1000) {
            lastIdleCheckTime = currentTime;
            Set<SelectionKey> keys = selector.keys();
            if (keys != null) {
                for (SelectionKey key : keys) {
                    SocketSessionImpl session = (SocketSessionImpl) key.attachment();
                    // 处理完一次请求后进行事件统计，并检查是否发生了WRITER_IDLE事件，如果发生了则触发一次fireExceptionCaught事件回调
                    notifyIdleness(session, currentTime);
                }
            }
        }
    }

    /**
     * 处理完一次请求后进行事件统计，并检查是否发生了WriteTimeout事件，如果发生了则触发一次fireExceptionCaught事件回调
     *
     * @param session
     * @param currentTime
     */
    private void notifyIdleness(SocketSessionImpl session, long currentTime) {
        notifyIdleness0(
                session,
                currentTime,
                session.getIdleTimeInMillis(IdleStatus.BOTH_IDLE),
                IdleStatus.BOTH_IDLE,
                Math.max(session.getLastIoTime(), session.getLastIdleTime(IdleStatus.BOTH_IDLE))
        );

        notifyIdleness0(
                session,
                currentTime,
                session.getIdleTimeInMillis(IdleStatus.READER_IDLE),
                IdleStatus.READER_IDLE,
                Math.max(session.getLastReadTime(), session.getLastIdleTime(IdleStatus.READER_IDLE))
        );

        notifyIdleness0(
                session,
                currentTime,
                session.getIdleTimeInMillis(IdleStatus.WRITER_IDLE),
                IdleStatus.WRITER_IDLE,
                Math.max(session.getLastWriteTime(), session.getLastIdleTime(IdleStatus.WRITER_IDLE))
        );

        // 检查Session是否存在写超时情况：当前时间 - 上一次response时间，如果>=Session设置的响应超时时间，则触发fireExceptionCaught()回调方法，表示发生一次WriteTimeoutException异常
        notifyWriteTimeout(session, currentTime, session.getWriteTimeoutInMillis(), session.getLastWriteTime());
    }

    /**
     *
     * @param session
     * @param currentTime
     * @param idleTime
     * @param status
     * @param lastIoTime
     */
    private void notifyIdleness0(SocketSessionImpl session, long currentTime, long idleTime, IdleStatus status, long lastIoTime) {
        if (idleTime > 0 && lastIoTime != 0 && (currentTime - lastIoTime) >= idleTime) {
            // 根据IdleStatus统计每个session的空闲状态
            session.increaseIdleCount(status);
            // 触发空闲状态事件
            session.getFilterChain().fireSessionIdle(session, status);
        }
    }

    /**
     *
     * @param session
     * @param currentTime       当前时间
     * @param writeTimeout      Session的响应超时时间
     * @param lastIoTime        Session最后一次response的时间
     */
    private void notifyWriteTimeout(SocketSessionImpl session, long currentTime, long writeTimeout, long lastIoTime) {
        SelectionKey key = session.getSelectionKey();
        // 当前时间 - 上一次response时间，如果>=Session设置的响应超时时间，则触发fireExceptionCaught()回调方法，表示发生一次WriteTimeoutException异常
        if (writeTimeout > 0
                && (currentTime - lastIoTime) >= writeTimeout
                && key != null && key.isValid()
                && (key.interestOps() & SelectionKey.OP_WRITE) != 0) {

            session.getFilterChain().fireExceptionCaught(session, new WriteTimeoutException());
        }
    }

    /**
     * session处理完一次请求后（即接收客户端请求，然后发起响应了之后）的后置处理动作
     */
    private void doFlush() {
        for (; ; ) {
            SocketSessionImpl session = flushingSessions.poll();
            if (session == null)
                break;

            session.setScheduledForFlush(false);
            if (!session.isConnected()) {
                // 如果会话连接已经断开，则释放session中保存的响应数据的缓存
                releaseWriteBuffers(session);
                continue;
            }

            SelectionKey key = session.getSelectionKey();
            if (key == null) {
                scheduleFlush(session);
                break;
            }

            // 如果通道已关闭，则跳过
            if (!key.isValid()) {
                continue;
            }

            try {
                boolean flushedAll = doFlush(session);
                if (flushedAll && !session.getWriteRequestQueue().isEmpty() && !session.isScheduledForFlush()) {
                    scheduleFlush(session);
                }
            } catch (IOException e) {
                scheduleRemove(session);
                session.getFilterChain().fireExceptionCaught(session, e);
            }
        }
    }

    /**
     * 当处理完请求（即向客户端发送完响应数据）后，会调用该方法释放session中保存的响应数据的缓存
     *
     * @param session
     */
    private void releaseWriteBuffers(SocketSessionImpl session) {
        Queue<WriteRequest> writeRequestQueue = session.getWriteRequestQueue();
        WriteRequest req;

        if ((req = writeRequestQueue.poll()) != null) {
            ByteBuffer buf = (ByteBuffer) req.getMessage();
            try {
                buf.release();
            } catch (IllegalStateException e) {
                session.getFilterChain().fireExceptionCaught(session, e);
            } finally {
                // buf中是否还有未读数据
                if (buf.hasRemaining()) {
                    req.getFuture().setWritten(false);
                } else {
                    session.getFilterChain().fireMessageSent(session, req);
                }
            }

            // Discard others.
            while ((req = writeRequestQueue.poll()) != null) {
                try {
                    ((ByteBuffer) req.getMessage()).release();
                } catch (IllegalStateException e) {
                    session.getFilterChain().fireExceptionCaught(session, e);
                } finally {
                    req.getFuture().setWritten(false);
                }
            }
        }
    }

    private boolean doFlush(SocketSessionImpl session) throws IOException {
        SocketChannel ch = session.getChannel();
        if (!ch.isConnected()) {
            scheduleRemove(session);
            return false;
        }

        // Clear OP_WRITE
        SelectionKey key = session.getSelectionKey();
        key.interestOps(key.interestOps() & (~SelectionKey.OP_WRITE));

        Queue<WriteRequest> writeRequestQueue = session.getWriteRequestQueue();

        int writtenBytes = 0;
        int maxWrittenBytes = ((SocketSessionConfig) session.getConfig()).getSendBufferSize() << 1;
        try {
            for (; ; ) {
                WriteRequest req = writeRequestQueue.peek();

                if (req == null)
                    break;

                ByteBuffer buf = (ByteBuffer) req.getMessage();
                if (buf.remaining() == 0) {
                    writeRequestQueue.poll();

                    buf.reset();

                    if (!buf.hasRemaining()) {
                        session.increaseWrittenMessages();
                    }

                    // 向客户端发送响应数据
                    session.getFilterChain().fireMessageSent(session, req);
                    continue;
                }

                int localWrittenBytes = 0;
                for (int i = WRITE_SPIN_COUNT; i > 0; i--) {
                    localWrittenBytes = ch.write(buf.buf());
                    if (localWrittenBytes != 0 || !buf.hasRemaining()) {
                        break;
                    }
                }

                writtenBytes += localWrittenBytes;

                if (localWrittenBytes == 0 || writtenBytes >= maxWrittenBytes) {
                    // Kernel buffer is full or wrote too much.
                    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                    return false;
                }
            }
        } finally {
            session.increaseWrittenBytes(writtenBytes);
        }

        return true;
    }

    private void doUpdateTrafficMask() {
        if (trafficControllingSessions.isEmpty())
            return;

        for (; ; ) {
            SocketSessionImpl session = trafficControllingSessions.poll();
            if (session == null)
                break;

            SelectionKey key = session.getSelectionKey();
            // 如果会话尚未完全初始化，请稍后重试。
            if (key == null) {
                scheduleTrafficControl(session);
                break;
            }

            // 如果通道已经关闭，则跳过
            if (!key.isValid()) {
                continue;
            }

            // 正常情况是OP_READ，如果会话的写队列中有写请求，则将OP_WRITE设置为触发刷新。
            int ops = SelectionKey.OP_READ;
            Queue<WriteRequest> writeRequestQueue = session.getWriteRequestQueue();
            synchronized (writeRequestQueue) {
                if (!writeRequestQueue.isEmpty()) {
                    ops |= SelectionKey.OP_WRITE;
                }
            }

            // 现在，使用当前会话的遮罩遮盖首选操作
            int mask = session.getTrafficMask().getInterestOps();
            key.interestOps(ops & mask);
        }
    }

    /**
     * 1、从newSessions获取一个session，获取session中的通道，注册到Selector，并监听SelectionKey.OP_READ事件（开始监听来自客户端的请求）
     *
     */
    private class Worker implements Runnable {

        public void run() {
            Selector selector = SocketIoProcessor.this.selector;
            for (; ; ) {
                try {
                    int nKeys = selector.select(1000);

                    // 从newSessions获取一个session，获取session中的通道，注册到Selector，并监听SelectionKey.OP_READ事件（开始监听来自客户端的请求）
                    doAddNew();

                    doUpdateTrafficMask();

                    if (nKeys > 0) {
                        // 处理通道监听的事件：获取客户端的请求，并进行处理，再向客户端发送响应数据
                        process(selector.selectedKeys());
                    }

                    // session处理完一次请求后（即接收客户端请求，然后发起响应了之后）的后置处理动作
                    doFlush();

                    // session处理完一次请求后，会从removingSessions队列中移除session，将session中的通道关闭，断开与客户端的连接，然后清除session的中保存的响应数据的缓存，并触发fireSessionDestroyed()监听器方法
                    doRemove();

                    // 处理完一次请求后进行事件统计，并检查是否发生了WRITER_IDLE事件，如果发生了则触发一次fireExceptionCaught事件回调
                    notifyIdleness();

                    // 如果监听的全部被移除（即服务关闭），则关闭selector，停止Workder线程
                    if (selector.keys().isEmpty()) {
                        synchronized (lock) {
                            if (selector.keys().isEmpty() && newSessions.isEmpty()) {
                                worker = null;

                                try {
                                    selector.close();
                                } catch (IOException e) {
                                    ExceptionMonitor.getInstance().exceptionCaught(e);
                                } finally {
                                    selector = null;
                                }

                                break;
                            }
                        }
                    }
                } catch (Throwable t) {
                    ExceptionMonitor.getInstance().exceptionCaught(t);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        ExceptionMonitor.getInstance().exceptionCaught(e1);
                    }
                }
            }
        }

    }
}
