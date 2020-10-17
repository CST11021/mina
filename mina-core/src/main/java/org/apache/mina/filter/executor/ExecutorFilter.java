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
package org.apache.mina.filter.executor;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用于将session的创建、关系、写入、读取、发送消息、接收消息等回调方法委托给线程池进行处理，默认的串行处理的
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 350169 $, $Date: 2005-12-01 00:17:41 -0500 (Thu, 01 Dec 2005) $
 */
public class ExecutorFilter extends IoFilterAdapter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /** 用于执行过滤器的线程池，corePoolSize：16， maximumPoolSize：16，keepAliveTime：16s */
    private final Executor executor;


    public ExecutorFilter() {
        this(new ThreadPoolExecutor(16, 16, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>()));
    }
    public ExecutorFilter(Executor executor) {
        if (executor == null) {
            throw new NullPointerException("executor");
        }

        this.executor = executor;
    }

    /**
     * 获取用于执行过滤器的线程池
     *
     * @return
     */
    public Executor getExecutor() {
        return executor;
    }

    // 下面这三个方法都是直接执行适配器的逻辑的，没什么用，可以干掉，不需要重写适配器方法

    public void sessionCreated(NextFilter nextFilter, IoSession session) {
        nextFilter.sessionCreated(session);
    }
    public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) {
        nextFilter.filterWrite(session, writeRequest);
    }
    public void filterClose(NextFilter nextFilter, IoSession session) throws Exception {
        nextFilter.filterClose(session);
    }


    /**
     * 对应的事件委托给线程池去执行
     *
     * @param nextFilter
     * @param session
     * @param type
     * @param data
     */
    private void fireEvent(NextFilter nextFilter, IoSession session, EventType type, Object data) {
        Event event = new Event(type, nextFilter, data);
        // 从session获取（或创建）一个SessionBuffer对象
        SessionBuffer buf = SessionBuffer.getSessionBuffer(session);

        boolean execute;
        synchronized (buf.eventQueue) {
            // 往队列添加一个事件
            buf.eventQueue.offer(event);

            // 如果session之前的事件任务都处理完成了，再次启动任务进行处理，否则不需要再次创建任务，直接将事件放到队列中，等待被处理就可以了
            if (buf.processingCompleted) {
                buf.processingCompleted = false;
                execute = true;
            } else {
                execute = false;
            }
        }

        if (execute) {
            if (logger.isDebugEnabled()) {
                logger.debug("Launching thread for " + session.getRemoteAddress());
            }

            // 遍历session的所有事件，并依次进行处理，知道事件全部处理完成
            executor.execute(new ProcessEventsRunnable(buf));
        }
    }
    public void sessionOpened(NextFilter nextFilter, IoSession session) {
        fireEvent(nextFilter, session, EventType.OPENED, null);
    }
    public void sessionClosed(NextFilter nextFilter, IoSession session) {
        fireEvent(nextFilter, session, EventType.CLOSED, null);
    }
    public void sessionIdle(NextFilter nextFilter, IoSession session, IdleStatus status) {
        fireEvent(nextFilter, session, EventType.IDLE, status);
    }
    public void exceptionCaught(NextFilter nextFilter, IoSession session, Throwable cause) {
        fireEvent(nextFilter, session, EventType.EXCEPTION, cause);
    }
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) {
        fireEvent(nextFilter, session, EventType.RECEIVED, message);
    }
    public void messageSent(NextFilter nextFilter, IoSession session, Object message) {
        fireEvent(nextFilter, session, EventType.SENT, message);
    }





    protected void processEvent(NextFilter nextFilter, IoSession session, EventType type, Object data) {
        if (type == EventType.RECEIVED) {
            nextFilter.messageReceived(session, data);
        } else if (type == EventType.SENT) {
            nextFilter.messageSent(session, data);
        } else if (type == EventType.EXCEPTION) {
            nextFilter.exceptionCaught(session, (Throwable) data);
        } else if (type == EventType.IDLE) {
            nextFilter.sessionIdle(session, (IdleStatus) data);
        } else if (type == EventType.OPENED) {
            nextFilter.sessionOpened(session);
        } else if (type == EventType.CLOSED) {
            nextFilter.sessionClosed(session);
        }
    }

    /**
     * 遍历session的所有事件，并依次进行处理，知道事件全部处理完成
     */
    private class ProcessEventsRunnable implements Runnable {

        private final SessionBuffer buffer;

        ProcessEventsRunnable(SessionBuffer buffer) {
            this.buffer = buffer;
        }

        public void run() {
            while (true) {
                Event event;

                synchronized (buffer.eventQueue) {
                    event = buffer.eventQueue.poll();

                    if (event == null) {
                        buffer.processingCompleted = true;
                        break;
                    }
                }

                // 处理事件
                processEvent(event.getNextFilter(), buffer.session, event.getType(), event.getData());
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Exiting since queue is empty for " + buffer.session.getRemoteAddress());
            }
        }
    }

    private static class SessionBuffer {
        private static final String KEY = SessionBuffer.class.getName() + ".KEY";

        private static SessionBuffer getSessionBuffer(IoSession session) {
            synchronized (session) {
                SessionBuffer buf = (SessionBuffer) session.getAttribute(KEY);
                if (buf == null) {
                    buf = new SessionBuffer(session);
                    session.setAttribute(KEY, buf);
                }
                return buf;
            }
        }

        /** session对象 */
        private final IoSession session;

        /** 表示该session发生的事件，将事件放到队列中，再依次进行处理 */
        private final Queue<Event> eventQueue = new LinkedList<Event>();

        /**  */
        private boolean processingCompleted = true;

        private SessionBuffer(IoSession session) {
            this.session = session;
        }
    }

    /**
     * 对应session事件类型，包括：
     * OPENED：session创建
     * CLOSED：session关闭
     * READ：从session读取数据
     * WRITTEN：往session写入数据
     * RECEIVED：从session接收到消息
     * SENT：通过session发送消息，发送前需要先通过filterWrite()方法进行写数据
     * IDLE：session发生长时间没有写入或者读取的空闲事件
     * EXCEPTION：异常状态
     *
     */
    protected static class EventType {

        public static final EventType OPENED = new EventType("OPENED");

        public static final EventType CLOSED = new EventType("CLOSED");

        public static final EventType READ = new EventType("READ");

        public static final EventType WRITTEN = new EventType("WRITTEN");

        public static final EventType RECEIVED = new EventType("RECEIVED");

        public static final EventType SENT = new EventType("SENT");

        public static final EventType IDLE = new EventType("IDLE");

        public static final EventType EXCEPTION = new EventType("EXCEPTION");

        private final String value;

        private EventType(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }
    }

    /**
     * 事件
     */
    protected static class Event {

        /** 事件类型 */
        private final EventType type;

        /** 事件上下文数据 */
        private final Object data;

        /** 处理该事件的下一个过滤器 */
        private final NextFilter nextFilter;

        Event(EventType type, NextFilter nextFilter, Object data) {
            this.type = type;
            this.nextFilter = nextFilter;
            this.data = data;
        }

        public Object getData() {
            return data;
        }

        public NextFilter getNextFilter() {
            return nextFilter;
        }

        public EventType getType() {
            return type;
        }
    }
}
