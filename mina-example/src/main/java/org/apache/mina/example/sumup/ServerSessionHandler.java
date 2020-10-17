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

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.example.sumup.message.AddMessage;
import org.apache.mina.example.sumup.message.ResultMessage;
import org.apache.mina.util.SessionLog;

/**
 * {@link IoHandler} for SumUp server.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ServerSessionHandler extends IoHandlerAdapter {

    /**
     * 建立连接时，初始化统计值为0
     *
     * @param session
     */
    public void sessionOpened(IoSession session) {
        // 将空闲时间设置为60秒
        session.setIdleTime(IdleStatus.BOTH_IDLE, 60);

        // 初始总和为零
        session.setAttachment(new Integer(0));
    }

    /**
     * 当接收到客户端请求时，将统一数字累加并返回
     *
     * @param session
     * @param message
     */
    public void messageReceived(IoSession session, Object message) {
        // 客户端仅发送AddMessage。否则，我们将不得不使用instanceof运算符识别其类型。
        AddMessage am = (AddMessage) message;

        // add the value to the current sum.
        int sum = ((Integer) session.getAttachment()).intValue();
        int value = am.getValue();
        long expectedSum = (long) sum + value;
        if (expectedSum > Integer.MAX_VALUE || expectedSum < Integer.MIN_VALUE) {
            // if the sum overflows or underflows, return error message
            ResultMessage rm = new ResultMessage();
            // copy sequence
            rm.setSequence(am.getSequence());
            rm.setOk(false);
            session.write(rm);
        } else {
            // sum up
            sum = (int) expectedSum;
            session.setAttachment(new Integer(sum));

            // return the result message
            ResultMessage rm = new ResultMessage();
            // copy sequence
            rm.setSequence(am.getSequence());
            rm.setOk(true);
            rm.setValue(sum);
            session.write(rm);
        }
    }

    /**
     * 当连接进入空闲状态时调用
     *
     * @param session
     * @param status
     * @throws Exception
     */
    public void sessionIdle(IoSession session, IdleStatus status) {
        SessionLog.info(session, "Disconnecting the idle.");
        // 断开空闲的客户端
        session.close();
    }

    public void exceptionCaught(IoSession session, Throwable cause) {
        // close the connection on exceptional situation
        session.close();
    }

}