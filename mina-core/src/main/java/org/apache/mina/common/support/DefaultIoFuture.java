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
package org.apache.mina.common.support;

import java.util.ArrayList;
import java.util.List;

import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoSession;

/**
 * A default implementation of {@link IoFuture}.
 *  
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class DefaultIoFuture implements IoFuture {

    private final Object lock;

    /** 表示该异步I/O操作关联的session */
    private final IoSession session;
    /** 第一个I/O处理完成事件监听器 */
    private IoFutureListener firstListener;
    /** 其他I/O处理完成事件监听器 */
    private List<IoFutureListener> otherListeners;
    /** 表示本地I/O请求的处理结果 */
    private Object result;
    /** 表示本次I/O请求操作是否完成 */
    private boolean ready;
    /**  */
    private int waiters;

    public DefaultIoFuture(IoSession session) {
        this.session = session;
        this.lock = this;
    }
    public DefaultIoFuture(IoSession session, Object lock) {
        this.session = session;
        this.lock = lock;
    }

    public IoSession getSession() {
        return session;
    }

    public Object getLock() {
        return lock;
    }

    /**
     * 阻塞线程，知道I/O请求处理完成
     */
    public void join() {
        // 阻塞线程，知道I/O请求处理完成
        awaitUninterruptibly();
    }

    /**
     * 阻塞线程，知道I/O请求处理完成
     *
     * @param timeoutMillis
     * @return
     */
    public boolean join(long timeoutMillis) {
        return awaitUninterruptibly(timeoutMillis);
    }

    /**
     * 表示本次I/O请求操作是否完成
     *
     * @return
     */
    public boolean isReady() {
        synchronized (lock) {
            return ready;
        }
    }

    /**
     * 添加I/O请求处理完成的事件监听器
     *
     * @param listener
     */
    public void addListener(IoFutureListener listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }

        boolean notifyNow = false;
        synchronized (lock) {
            if (ready) {
                notifyNow = true;
            } else {
                if (firstListener == null) {
                    firstListener = listener;
                } else {
                    if (otherListeners == null) {
                        otherListeners = new ArrayList<IoFutureListener>(1);
                    }
                    otherListeners.add(listener);
                }
            }
        }

        if (notifyNow) {
            notifyListener(listener);
        }
    }

    /**
     * 移除I/O请求处理完成的事件监听器
     *
     * @param listener
     */
    public void removeListener(IoFutureListener listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }

        synchronized (lock) {
            if (!ready) {
                if (listener == firstListener) {
                    if (otherListeners != null && !otherListeners.isEmpty()) {
                        firstListener = otherListeners.remove(0);
                    } else {
                        firstListener = null;
                    }
                } else if (otherListeners != null) {
                    otherListeners.remove(listener);
                }
            }
        }
    }




    /**
     * 阻塞线程，直到I/O请求处理完成
     *
     * @return
     */
    private IoFuture awaitUninterruptibly() {
        synchronized (lock) {
            while (!ready) {
                waiters++;
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                } finally {
                    waiters--;
                }
            }
        }

        return this;
    }

    /**
     * 阻塞线程，直到I/O请求处理完成
     *
     * @param timeoutMillis 阻塞等待的超时时间
     * @return
     */
    private boolean awaitUninterruptibly(long timeoutMillis) {
        try {
            // 该方法阻塞等待I/O请求的结果，如果超过该时间，无论请求是否完成，立即返回
            return await0(timeoutMillis, false);
        } catch (InterruptedException e) {
            throw new InternalError();
        }
    }

    /**
     * 该方法阻塞等待I/O请求的结果，如果超过该时间，无论请求是否完成，立即返回
     *
     * @param timeoutMillis 该方法阻塞等待的超时时间
     * @param interruptable 表示阻塞等待，线程中断时，是否抛异常
     * @return
     * @throws InterruptedException
     */
    private boolean await0(long timeoutMillis, boolean interruptable) throws InterruptedException {
        long startTime = timeoutMillis <= 0 ? 0 : System.currentTimeMillis();
        long waitTime = timeoutMillis;

        synchronized (lock) {
            if (ready) {
                return ready;
            } else if (waitTime <= 0) {
                return ready;
            }

            waiters++;
            try {
                for (;;) {
                    try {
                        lock.wait(waitTime);
                    } catch (InterruptedException e) {
                        if (interruptable) {
                            throw e;
                        }
                    }

                    if (ready) {
                        return true;
                    } else {
                        waitTime = timeoutMillis - (System.currentTimeMillis() - startTime);
                        if (waitTime <= 0) {
                            return ready;
                        }
                    }
                }
            } finally {
                waiters--;
            }
        }
    }

    /**
     * 设置异步操作的结果，并将其标记为完成
     *
     * @param newValue
     */
    protected void setValue(Object newValue) {
        synchronized (lock) {
            // Allow only once.
            if (ready) {
                return;
            }

            result = newValue;
            ready = true;
            if (waiters > 0) {
                lock.notifyAll();
            }
        }

        notifyListeners();
    }

    /**
     * 返回异步操作的结果
     *
     * @return
     */
    protected Object getValue() {
        synchronized (lock) {
            return result;
        }
    }

    /**
     * 触发所有的事件监听器
     */
    private void notifyListeners() {
        // 不会有任何可见性问题或并发修改，因为将针对addListener和removeListener调用检查“ready”标志。
        if (firstListener != null) {
            notifyListener(firstListener);
            firstListener = null;

            if (otherListeners != null) {
                for (IoFutureListener l : otherListeners) {
                    notifyListener(l);
                }
                otherListeners = null;
            }
        }
    }

    /**
     * 触发指定的事件监听器
     *
     * @param l
     */
    @SuppressWarnings("unchecked")
    private void notifyListener(IoFutureListener l) {
        try {
            l.operationComplete(this);
        } catch (Throwable t) {
            ExceptionMonitor.getInstance().exceptionCaught(t);
        }
    }
}