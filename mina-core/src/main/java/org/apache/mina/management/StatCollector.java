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
package org.apache.mina.management;

import java.net.SocketAddress;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.mina.common.*;

/**
 * Collects statistics of an {@link IoService}. It's polling all the sessions of a given
 * IoService. It's attaching a {@link IoSessionStat} object to all the sessions polled
 * and filling the throughput values.
 * 
 * Usage :
 * <pre>
 * IoService service = ...
 * StatCollector collector = new StatCollector( service );
 * collector.start();
 * </pre>
 * 
 * By default the {@link StatCollector} is polling the sessions every 5 seconds. You can 
 * give a different polling time using a second constructor.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class StatCollector {

    /**
     * The session attribute key for {@link IoSessionStat}.
     */
    public static final String KEY = StatCollector.class.getName() + ".stat";

    /**
     * @noinspection StaticNonFinalField
     */
    private static volatile int nextId = 0;

    private final int id = nextId++;

    private final Object calcLock = new Object();

    private final IoService service;

    private Worker worker;

    private int pollingInterval = 5000;

    /** 保存当前有效的连接数，会话创建时，add到该队列，会话销毁时，从该队列移除 */
    private Queue<IoSession> polledSessions;

    /** 统计会话的创建个数 */
    private AtomicLong totalProcessedSessions = new AtomicLong();

    private float msgWrittenThroughput = 0f;

    private float msgReadThroughput = 0f;

    private float bytesWrittenThroughput = 0f;

    private float bytesReadThroughput = 0f;

    private final IoServiceListener serviceListener = new IoServiceListener() {

        /**
         * 当服务启动时，调用该方法
         *
         * @param service        the {@link IoService}
         * @param serviceAddress the socket address of the {@link IoService} listens
         *                       to manage sessions.  If the service is an {@link IoAcceptor},
         *                       it is a bind address.  If the service is an {@link IoConnector},
         *                       it is a remote address.
         * @param handler        the {@link IoHandler} that serves the new service
         * @param config         the {@link IoServiceConfig} of the new service
         */
        public void serviceActivated(IoService service, SocketAddress serviceAddress, IoHandler handler, IoServiceConfig config) {
        }

        /**
         * 当服务关闭时调用该方法
         *
         * @param service
         * @param serviceAddress
         * @param handler
         * @param config
         */
        public void serviceDeactivated(IoService service, SocketAddress serviceAddress, IoHandler handler, IoServiceConfig config) {
        }

        /**
         * 当session被创建时，调用该方法
         *
         * @param session
         */
        public void sessionCreated(IoSession session) {
            addSession(session);
        }

        /**
         * 当session被销毁时，调用该方法
         *
         * @param session
         */
        public void sessionDestroyed(IoSession session) {
            removeSession(session);
        }

    };




    /**
     * Create a stat collector for the given service with a default polling time of 5 seconds. 
     * @param service the IoService to inspect
     */
    public StatCollector(IoService service) {
        this(service, 5000);
    }
    /**
     * create a stat collector for the given given service
     *
     * @param service           服务对象
     * @param pollingInterval   轮询间隔，单位毫秒
     */
    public StatCollector(IoService service, int pollingInterval) {
        this.service = service;
        this.pollingInterval = pollingInterval;
    }




    /**
     * Start collecting stats for the {@link IoSession} of the service.
     * New sessions or destroyed will be automaticly added or removed.
     */
    public void start() {
        synchronized (this) {
            if (worker != null && worker.isAlive())
                throw new RuntimeException("Stat collecting already started");

            // add all current sessions
            polledSessions = new ConcurrentLinkedQueue<IoSession>();

            for (Iterator<SocketAddress> iter = service.getManagedServiceAddresses().iterator(); iter.hasNext();) {
                SocketAddress element = iter.next();

                for (Iterator<IoSession> iter2 = service.getManagedSessions(element).iterator(); iter2.hasNext();) {
                    addSession(iter2.next());
                }
            }

            // listen for new ones
            service.addListener(serviceListener);

            // start polling
            worker = new Worker();
            worker.start();
        }
    }

    /**
     * Stop collecting stats. all the {@link IoSessionStat} object will be removed of the
     * polled session attachements. 
     */
    public void stop() {
        synchronized (this) {
            if (worker == null) {
                return;
            }

            service.removeListener(serviceListener);

            // stop worker
            worker.stop = true;
            worker.interrupt();
            while (worker.isAlive()) {
                try {
                    worker.join();
                } catch (InterruptedException e) {
                    //ignore since this is shutdown time
                }
            }

            for (Iterator iter = polledSessions.iterator(); iter.hasNext();) {
                IoSession session = (IoSession) iter.next();
                session.removeAttribute(KEY);
            }
            polledSessions.clear();

            worker = null;
        }
    }

    /**
     * is the stat collector started and polling the {@link IoSession} of the {@link IoService}
     * @return true if started
     */
    public boolean isRunning() {
        synchronized (this) {
            return worker != null && worker.stop != true;
        }
    }

    /**
     * 保存连接会话
     *
     * @param session
     */
    private void addSession(IoSession session) {
        IoSessionStat sessionStats = new IoSessionStat();
        session.setAttribute(KEY, sessionStats);
        totalProcessedSessions.incrementAndGet();
        polledSessions.add(session);
    }

    private void removeSession(IoSession session) {
        // remove the session from the list of polled sessions
        polledSessions.remove(session);

        // add the bytes processed between last polling and session closing
        // prevent non seen byte with non-connected protocols like HTTP and datagrams
        IoSessionStat sessStat = (IoSessionStat) session.getAttribute(KEY);

        // computing with time between polling and closing
        long currentTime = System.currentTimeMillis();
        synchronized (calcLock) {
            bytesReadThroughput += (session.getReadBytes() - sessStat.lastByteRead) / ((currentTime - sessStat.lastPollingTime) / 1000f);
            bytesWrittenThroughput += (session.getWrittenBytes() - sessStat.lastByteWrite) / ((currentTime - sessStat.lastPollingTime) / 1000f);
            msgReadThroughput += (session.getReadMessages() - sessStat.lastMessageRead) / ((currentTime - sessStat.lastPollingTime) / 1000f);
            msgWrittenThroughput += (session.getWrittenMessages() - sessStat.lastMessageWrite) / ((currentTime - sessStat.lastPollingTime) / 1000f);
        }

        session.removeAttribute(KEY);
    }

    /**
     * total number of sessions processed by the stat collector
     * @return number of sessions
     */
    public long getTotalProcessedSessions() {
        return totalProcessedSessions.get();
    }

    public float getBytesReadThroughput() {
        return bytesReadThroughput;
    }

    public float getBytesWrittenThroughput() {
        return bytesWrittenThroughput;
    }

    public float getMsgReadThroughput() {
        return msgReadThroughput;
    }

    public float getMsgWrittenThroughput() {
        return msgWrittenThroughput;
    }

    public long getSessionCount() {
        return polledSessions.size();
    }

    private class Worker extends Thread {

        boolean stop = false;

        private Worker() {
            super("StatCollectorWorker-" + id);
        }

        public void run() {

            while (!stop) {
                for (Iterator iter = polledSessions.iterator(); iter.hasNext();) {
                    IoSession session = (IoSession) iter.next();
                    IoSessionStat sessStat = (IoSessionStat) session.getAttribute(KEY);

                    sessStat.lastByteRead = session.getReadBytes();
                    sessStat.lastByteWrite = session.getWrittenBytes();
                    sessStat.lastMessageRead = session.getReadMessages();
                    sessStat.lastMessageWrite = session.getWrittenMessages();
                }

                // wait polling time
                try {
                    Thread.sleep(pollingInterval);
                } catch (InterruptedException e) {

                }

                float tmpMsgWrittenThroughput = 0f;
                float tmpMsgReadThroughput = 0f;
                float tmpBytesWrittenThroughput = 0f;
                float tmpBytesReadThroughput = 0f;

                for (Iterator iter = polledSessions.iterator(); iter.hasNext();) {

                    // upadating individual session statistics
                    IoSession session = (IoSession) iter.next();
                    IoSessionStat sessStat = (IoSessionStat) session.getAttribute(KEY);

                    sessStat.byteReadThroughput = (session.getReadBytes() - sessStat.lastByteRead) / (pollingInterval / 1000f);
                    tmpBytesReadThroughput += sessStat.byteReadThroughput;

                    sessStat.byteWrittenThroughput = (session.getWrittenBytes() - sessStat.lastByteWrite) / (pollingInterval / 1000f);
                    tmpBytesWrittenThroughput += sessStat.byteWrittenThroughput;

                    sessStat.messageReadThroughput = (session.getReadMessages() - sessStat.lastMessageRead) / (pollingInterval / 1000f);
                    tmpMsgReadThroughput += sessStat.messageReadThroughput;

                    sessStat.messageWrittenThroughput = (session.getWrittenMessages() - sessStat.lastMessageWrite) / (pollingInterval / 1000f);
                    tmpMsgWrittenThroughput += sessStat.messageWrittenThroughput;

                    synchronized (calcLock) {
                        msgWrittenThroughput = tmpMsgWrittenThroughput;
                        msgReadThroughput = tmpMsgReadThroughput;
                        bytesWrittenThroughput = tmpBytesWrittenThroughput;
                        bytesReadThroughput = tmpBytesReadThroughput;
                        sessStat.lastPollingTime = System.currentTimeMillis();
                    }

                }

            }

        }
    }

}