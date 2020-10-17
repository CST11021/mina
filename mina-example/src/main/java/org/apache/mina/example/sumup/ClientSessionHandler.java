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
package org.apache.mina.example.sumup;

import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.example.sumup.message.AddMessage;
import org.apache.mina.example.sumup.message.ResultMessage;
import org.apache.mina.util.SessionLog;

/**
 * {@link IoHandler} for SumUp client.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ClientSessionHandler extends IoHandlerAdapter {

    private final int[] values;

    private boolean finished;

    public ClientSessionHandler(int[] values) {
        this.values = values;
    }

    public boolean isFinished() {
        return finished;
    }

    /**
     * 将value数组的值依次发送到服务端
     *
     * @param session
     */
    public void sessionOpened(IoSession session) {
        // send summation requests
        for (int i = 0; i < values.length; i++) {
            AddMessage m = new AddMessage();
            m.setSequence(i);
            m.setValue(values[i]);
            session.write(m);
        }
    }

    public void messageReceived(IoSession session, Object message) {

        ResultMessage rm = (ResultMessage) message;
        if (rm.isOk()) {
            // 如果收到的结果消息具有最后的序列号，则该断开连接了
            if (rm.getSequence() == values.length - 1) {
                // 打印总和并断开连接
                SessionLog.info(session, "The sum: " + rm.getValue());
                System.out.println("The sum: " + rm.getValue());
                session.close();
                finished = true;
            }
        } else {
            // 由于溢出等原因，server返回了错误代码
            SessionLog.warn(session, "Server error, disconnecting...");
            System.out.println("Server error, disconnecting...");
            session.close();
            finished = true;
        }
    }

    public void exceptionCaught(IoSession session, Throwable cause) {
        session.close();
    }
}