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

/**
 * A configuration which is used to configure {@link IoService}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface IoServiceConfig extends Cloneable {

    /**
     * Resturns the default configuration of the new {@link IoSession}s.
     */
    IoSessionConfig getSessionConfig();

    /**
     * 获取Server配置的过滤链，便于后续将Server的过滤器添加到Session中
     *
     * @return
     */
    IoFilterChainBuilder getFilterChainBuilder();

    /**
     * 设置服务的过滤器
     *
     * @param builder
     */
    void setFilterChainBuilder(IoFilterChainBuilder builder);
    /**
     * 将过滤器转为DefaultIoFilterChainBuilder，如果Server配置的过滤器不是DefaultIoFilterChainBuilder的子类，则抛异常
     *
     * @return
     */
    DefaultIoFilterChainBuilder getFilterChain();

    /**
     * Returns the default {@link ThreadModel} of the {@link IoService}.
     * The default value is a {@link ExecutorThreadModel}() whose service name is
     * <tt>'AnonymousIoService'</tt> and which has 16 maximum active threads.
     * It is strongly recommended to set a new {@link ExecutorThreadModel} by calling
     * {@link ExecutorThreadModel#getInstance(String)}.
     */
    ThreadModel getThreadModel();

    /**
     * Sets the default {@link ThreadModel} of the {@link IoService}.
     * If you specify <tt>null</tt>, this property will be set to the
     * default value.
     * The default value is an {@link ExecutorThreadModel} whose service name is
     * <tt>'AnonymousIoService'</tt> with 16 threads.
     * It is strongly recommended to set a new {@link ExecutorThreadModel} by calling
     * {@link ExecutorThreadModel#getInstance(String)}.
     */
    void setThreadModel(ThreadModel threadModel);

    /**
     * Returns a deep clone of this configuration.
     */
    Object clone();

}
