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
import java.util.EventListener;

/**
 * Something interested in being notified when the result of an {@link IoFuture} becomes available.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface IoServiceListener extends EventListener {


    // 服务启动关闭的监听方法

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
    void serviceActivated(IoService service, SocketAddress serviceAddress, IoHandler handler, IoServiceConfig config);

    /**
     * 当服务关闭时调用该方法
     *
     * @param service
     * @param serviceAddress
     * @param handler
     * @param config
     */
    void serviceDeactivated(IoService service, SocketAddress serviceAddress, IoHandler handler, IoServiceConfig config);




    // 请求创建和销毁的监听方法

    /**
     * 当session被创建时，调用该方法
     *
     * @param session
     */
    void sessionCreated(IoSession session);

    /**
     * 当session被销毁时，调用该方法
     *
     * @param session
     */
    void sessionDestroyed(IoSession session);

}
