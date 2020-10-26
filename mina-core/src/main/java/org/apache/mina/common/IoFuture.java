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
 * 表示一次异步I/O操作的结果
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface IoFuture {

    /**
     * 返回本次I/O请求关联的session
     *
     * @return
     */
    IoSession getSession();

    /**
     * Returns the lock object this future acquires
     */
    Object getLock();

    /**
     * 阻塞，直到等待异步操作结束
     */
    void join();
    boolean join(long timeoutInMillis);

    /**
     * 返回异步操作是否完成
     *
     * @return
     */
    boolean isReady();

    // 添加和移除事件监听

    /**
     * 当异步I/O操作完成后会调用该监听器
     *
     * @param listener
     */
    void addListener(IoFutureListener listener);
    void removeListener(IoFutureListener listener);
}
