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
import java.util.Set;

/**
 * 所有提供I/O服务和管理IoSession的服务，是IoAcceptor和IoConnector都实现了该接口
 * IoAcceptor：是服务端的顶级接口
 * IoConnector：是客户端的顶级接口
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface IoService {

    // 服务绑定和配置

    /**
     * Returns the default configuration which is used when you didn't specify any configuration.
     */
    IoServiceConfig getDefaultConfig();
    /**
     * 如果此服务正在管理指定的serviceAddress，则返回true。
     * 1、当服务是IoAcceptor，则serviceAddress是绑定地址；
     * 2、当服务是IoConnector，则serviceAddress是远程地址。
     */
    boolean isManaged(SocketAddress serviceAddress);
    /**
     * Returns all {@link SocketAddress}es this service is managing.
     * If this service is an {@link IoAcceptor}, a set of bind addresses will be returned.
     * If this service is an {@link IoConnector}, a set of remote addresses will be returned.
     */
    Set<SocketAddress> getManagedServiceAddresses();


    // 获取session集合

    /**
     * Returns all sessions with the specified remote or local address, which are currently managed by this service.
     * {@link IoAcceptor} will assume the specified <tt>address</tt> is a local address, and {@link IoConnector} will assume it's a remote address.
     *
     * @param serviceAddress the address to return all sessions for.
     * @return the sessions. An empty collection if there's no session.
     * @throws IllegalArgumentException if the specified <tt>address</tt> has not been bound.
     * @throws UnsupportedOperationException if this operation isn't supported for the particular transport type implemented by this {@link IoService}.
     */
    Set<IoSession> getManagedSessions(SocketAddress serviceAddress);


    // 添加和移除监听

    /**
     * 添加监听器：监听Server启动/关闭和请求创建/销毁的监听
     *
     * @param listener
     */
    void addListener(IoServiceListener listener);
    void removeListener(IoServiceListener listener);



    // 过滤器相关

    /**
     * 获取过滤器链的Builder，该Builder用于创建过滤器链
     *
     * @return
     */
    IoFilterChainBuilder getFilterChainBuilder();
    /**
     * 过滤器链的Builder，该Builder用于创建过滤器链
     *
     * @param builder
     */
    void setFilterChainBuilder(IoFilterChainBuilder builder);
    /**
     * 获取过滤器链的Builder，该Builder用于创建过滤器链
     *
     * @return
     */
    DefaultIoFilterChainBuilder getFilterChain();
}
