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

import java.net.SocketAddress;

import org.apache.mina.filter.ReferenceCountingIoFilter;

/**
 * mina中过滤器的接口
 */
public interface IoFilter {

    /**
     * 过滤器的初始化方法
     *
     * @throws Exception
     */
    void init() throws Exception;

    /**
     * Invoked by {@link ReferenceCountingIoFilter} when this filter
     * is not used by any {@link IoFilterChain} anymore, so you can destroy
     * shared resources.  Please note that this method is never called if
     * you don't wrap a filter with {@link ReferenceCountingIoFilter}.
     */
    void destroy() throws Exception;

    // 添加和移除过滤器扩展方法

    /**
     * 添加一个过滤器到链之前会调用该方法
     *
     * @param parent
     * @param name
     * @param nextFilter
     * @throws Exception
     */
    void onPreAdd(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception;

    /**
     * Invoked after this filter is added to the specified <tt>parent</tt>.
     * Please note that this method can be invoked more than once if
     * this filter is added to more than one parents.  This method is not
     * invoked before {@link #init()} is invoked.
     *
     * @param parent     the parent who called this method
     * @param name       the name assigned to this filter
     * @param nextFilter the {@link NextFilter} for this filter.  You can reuse
     *                   this object until this filter is removed from the chain.
     */
    void onPostAdd(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception;

    /**
     * Invoked before this filter is removed from the specified <tt>parent</tt>.
     * Please note that this method can be invoked more than once if
     * this filter is removed from more than one parents.
     * This method is always invoked before {@link #destroy()} is invoked.
     *
     * @param parent     the parent who called this method
     * @param name       the name assigned to this filter
     * @param nextFilter the {@link NextFilter} for this filter.  You can reuse
     *                   this object until this filter is removed from the chain.
     */
    void onPreRemove(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception;

    /**
     * 从指定的父级删除此过滤器后调用该方法
     * 请注意，如果从多个父级中删除此过滤器，则可以多次调用此方法。
     * 始终在调用{@link #destroy()}之前调用此方法。
     *
     * @param parent     the parent who called this method
     * @param name       the name assigned to this filter
     * @param nextFilter the {@link NextFilter} for this filter.  You can reuse
     *                   this object until this filter is removed from the chain.
     */
    void onPostRemove(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception;


    // session相关的事件方法

    /**
     * 当session被创建时，从Session获取过滤器链，然后调用每个过滤器的sessionCreated方法，如：{@link IoSession#getFilterChain()#sessionCreated(NextFilter, IoSession)}
     *
     * Filters {@link IoHandler#sessionCreated(IoSession)} event.
     *
     * @param nextFilter
     * @param session
     * @throws Exception
     */
    void sessionCreated(NextFilter nextFilter, IoSession session) throws Exception;

    /**
     * Filters {@link IoHandler#sessionOpened(IoSession)} event.
     */
    void sessionOpened(NextFilter nextFilter, IoSession session) throws Exception;

    /**
     * Filters {@link IoHandler#sessionClosed(IoSession)} event.
     */
    void sessionClosed(NextFilter nextFilter, IoSession session) throws Exception;

    /**
     * Filters {@link IoHandler#sessionIdle(IoSession, IdleStatus)} event.
     */
    void sessionIdle(NextFilter nextFilter, IoSession session, IdleStatus status) throws Exception;

    /**
     * Filters {@link IoHandler#exceptionCaught(IoSession, Throwable)} event.
     */
    void exceptionCaught(NextFilter nextFilter, IoSession session, Throwable cause) throws Exception;

    /**
     * Filters {@link IoHandler#messageReceived(IoSession, Object)} event.
     */
    void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception;

    /**
     * Filters {@link IoHandler#messageSent(IoSession, Object)} event.
     */
    void messageSent(NextFilter nextFilter, IoSession session, Object message) throws Exception;

    /**
     * Filters {@link IoSession#close()} method invocation.
     */
    void filterClose(NextFilter nextFilter, IoSession session) throws Exception;

    /**
     * 内部实现逻辑主要是{@link IoSession#write(Object)}方法的调用，向客户端回写响应数据
     *
     * @param nextFilter
     * @param session
     * @param writeRequest
     * @throws Exception
     */
    void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception;





    /**
     * Represents the next {@link IoFilter} in {@link IoFilterChain}.
     */
    public interface NextFilter {
        /**
         * Forwards <tt>sessionCreated</tt> event to next filter.
         */
        void sessionCreated(IoSession session);

        /**
         * Forwards <tt>sessionOpened</tt> event to next filter.
         */
        void sessionOpened(IoSession session);

        /**
         * Forwards <tt>sessionClosed</tt> event to next filter.
         */
        void sessionClosed(IoSession session);

        /**
         * Forwards <tt>sessionIdle</tt> event to next filter.
         */
        void sessionIdle(IoSession session, IdleStatus status);

        /**
         * Forwards <tt>exceptionCaught</tt> event to next filter.
         */
        void exceptionCaught(IoSession session, Throwable cause);

        /**
         * Forwards <tt>messageReceived</tt> event to next filter.
         */
        void messageReceived(IoSession session, Object message);

        /**
         * Forwards <tt>messageSent</tt> event to next filter.
         */
        void messageSent(IoSession session, Object message);

        /**
         * Forwards <tt>filterWrite</tt> event to next filter.
         */
        void filterWrite(IoSession session, WriteRequest writeRequest);

        /**
         * Forwards <tt>filterClose</tt> event to next filter.
         */
        void filterClose(IoSession session);
    }

    /**
     * 表示由{@link IoSession#write(Object)}触发的写请求
     */
    public static class WriteRequest {

        private static final WriteFuture UNUSED_FUTURE = new WriteFuture() {
            public boolean isWritten() {
                return false;
            }

            public void setWritten(boolean written) {
            }

            public IoSession getSession() {
                return null;
            }

            public void join() {
            }

            public boolean join(long timeoutInMillis) {
                return true;
            }

            public boolean isReady() {
                return true;
            }

            public void addListener(IoFutureListener listener) {
                throw new IllegalStateException(
                        "You can't add a listener to a dummy future.");
            }

            public void removeListener(IoFutureListener listener) {
                throw new IllegalStateException(
                        "You can't add a listener to a dummy future.");
            }

            public Object getLock() {
                return this;
            }
        };

        private final Object message;

        private final WriteFuture future;

        private final SocketAddress destination;

        /**
         * Creates a new instance without {@link WriteFuture}.  You'll get
         * an instance of {@link WriteFuture} even if you called this constructor
         * because {@link #getFuture()} will return a bogus future.
         */
        public WriteRequest(Object message) {
            this(message, null, null);
        }

        /**
         * Creates a new instance with {@link WriteFuture}.
         */
        public WriteRequest(Object message, WriteFuture future) {
            this(message, future, null);
        }

        /**
         * Creates a new instance.
         *
         * @param message     a message to write
         * @param future      a future that needs to be notified when an operation is finished
         * @param destination the destination of the message.  This property will be
         *                    ignored unless the transport supports it.
         */
        public WriteRequest(Object message, WriteFuture future, SocketAddress destination) {
            if (message == null) {
                throw new NullPointerException("message");
            }

            if (future == null) {
                future = UNUSED_FUTURE;
            }

            this.message = message;
            this.future = future;
            this.destination = destination;
        }

        /**
         * Returns {@link WriteFuture} that is associated with this write request.
         */
        public WriteFuture getFuture() {
            return future;
        }

        /**
         * Returns a message object to be written.
         */
        public Object getMessage() {
            return message;
        }

        /**
         * Returne the destination of this write request.
         *
         * @return <tt>null</tt> for the default destination
         */
        public SocketAddress getDestination() {
            return destination;
        }

        @Override
        public String toString() {
            return message.toString();
        }
    }

}
