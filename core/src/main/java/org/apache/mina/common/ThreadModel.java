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
 * 该接口实例主要用于执行过滤器，组装过滤器链的时候，会将该接口实例作为第一个过滤器，从而开始执行过滤器链中的过滤器，可以将ThreadModel理解为一个线程池服务，
 * 一个服务实例共享一个ThreadModel，ThreadModel内部使用线程池实现，服务端处理客户端请求，是通过hreadModel中的线程池来处理的
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface ThreadModel extends IoFilterChainBuilder {
    /**
     * A {@link ThreadModel} which make MINA not manage a thread model at all.
     */
    static final ThreadModel MANUAL = new ThreadModel() {
        public void buildFilterChain(IoFilterChain chain) throws Exception {
            // Do nothing.
        }
    };
}
