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

import java.util.EventListener;

/**
 * 当异步I/O操作完成后会调用该监听器
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface IoFutureListener extends EventListener {

    /**
     * I/O操作完成后将session关闭的监听器
     */
    static IoFutureListener CLOSE = new IoFutureListener() {

        /**
         * I/O操作完成后，将session关闭
         *
         * @param future  调用此回调的对象实例
         */
        public void operationComplete(IoFuture future) {
            future.getSession().close();
        }

    };

    /**
     * 当异步I/O操作完成后会调用该方法
     * 
     * @param future  调用此回调的对象实例
     */
    void operationComplete(IoFuture future);
}