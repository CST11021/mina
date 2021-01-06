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
package org.apache.mina.example.tennis;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoSession;
import org.apache.mina.transport.vmpipe.VmPipeAcceptor;
import org.apache.mina.transport.vmpipe.VmPipeAddress;
import org.apache.mina.transport.vmpipe.VmPipeConnector;

/**
 * "虚拟机内管道"示例，用于模拟客户端和服务器之间的网球比赛，客户端连接到服务器后比赛开始：
 *      首先，客户端发送TennisBall的TTL值是"10"。
 *      接收方（服务器或客户端）减小接收到的球的TTL值，并将其返回给对等方。
 *      谁得到TTL为0的球算输了。
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class Main {

    private static VmPipeAddress address = new VmPipeAddress(8080);

    public static void main(String[] args) throws Exception {
        // 1、创建并启动Server
        IoAcceptor acceptor = new VmPipeAcceptor();
        acceptor.bind(address, new TennisPlayer());

        // 2、创建Client并连接Server
        VmPipeConnector connector = new VmPipeConnector();
        ConnectFuture future = connector.connect(address, new TennisPlayer());
        // 阻塞直到连接上服务端
        future.join();

        // 3、发送第一个PING
        IoSession session = future.getSession();
        session.write(new TennisBall(10));

        // 4、获取session关闭的Future，这里阻塞，直到会话关闭
        session.getCloseFuture().join();
        acceptor.unbind(address);
    }
}
