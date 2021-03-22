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

import java.net.SocketAddress;
import java.util.Set;

import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoFilterChainBuilder;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceListener;
import org.apache.mina.common.IoSession;

/**
 * Base implementation of {@link IoService}s.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class BaseIoService implements IoService {

    /** 用于管理所有的Service监听器 */
    private final IoServiceListenerSupport listeners;
    /** 用于创建过滤器链的builder */
    private IoFilterChainBuilder filterChainBuilder = new DefaultIoFilterChainBuilder();


    protected BaseIoService() {
        this.listeners = new IoServiceListenerSupport();
    }




    // 过滤器相关

    /**
     * 获取过滤器链的Builder，该Builder用于创建过滤器链
     *
     * @return
     */
    public DefaultIoFilterChainBuilder getFilterChain() {
        if (filterChainBuilder instanceof DefaultIoFilterChainBuilder) {
            return (DefaultIoFilterChainBuilder) filterChainBuilder;
        } else {
            throw new IllegalStateException("Current filter chain builder is not a DefaultIoFilterChainBuilder.");
        }
    }
    /**
     * 获取过滤器链的Builder，该Builder用于创建过滤器链
     *
     * @return
     */
    public IoFilterChainBuilder getFilterChainBuilder() {
        return filterChainBuilder;
    }
    /**
     * 过滤器链的Builder，该Builder用于创建过滤器链
     *
     * @param builder
     */
    public void setFilterChainBuilder(IoFilterChainBuilder builder) {
        if (builder == null) {
            builder = new DefaultIoFilterChainBuilder();
        }
        filterChainBuilder = builder;
    }






    // 监听器相关接口：一下接口都是直接委托IoServiceListenerSupport组件来处理

    /**
     * 获取IoServiceListener管理器
     *
     * @return
     */
    protected IoServiceListenerSupport getListeners() {
        return listeners;
    }
    /**
     * 添加IoServiceListener
     *
     * @param listener
     */
    public void addListener(IoServiceListener listener) {
        getListeners().add(listener);
    }
    /**
     * 移除IoServiceListener
     *
     * @param listener
     */
    public void removeListener(IoServiceListener listener) {
        getListeners().remove(listener);
    }

    /**
     * 获取所有被管理的监听器的服务地址
     *
     * @return
     */
    public Set<SocketAddress> getManagedServiceAddresses() {
        return getListeners().getManagedServiceAddresses();
    }

    /**
     * 获取指定服务的会话集合
     *
     * @param serviceAddress the address to return all sessions for.
     * @return
     */
    public Set<IoSession> getManagedSessions(SocketAddress serviceAddress) {
        return getListeners().getManagedSessions(serviceAddress);
    }

    public boolean isManaged(SocketAddress serviceAddress) {
        return getListeners().isManaged(serviceAddress);
    }



}
