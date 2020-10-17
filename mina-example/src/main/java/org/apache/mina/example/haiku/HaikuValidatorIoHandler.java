/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mina.example.haiku;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;

/**
 * @author Apache Mina Project (dev@mina.apache.org)
 * @version $Rev: $, $Date:  $
 */

public class HaikuValidatorIoHandler extends IoHandlerAdapter {

    private final HaikuValidator validator = new HaikuValidator();

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        System.out.println("接收到消息：" + message.toString());

        Haiku haiku = (Haiku) message;
        try {
            validator.validate(haiku);
            // session.write("HAIKU!");
            write(session, "HAIKU!");
        } catch (Exception e) {
            // session.write("NOT A HAIKU!");
            write(session, "NOT A HAIKU!");
        }
    }

    /**
     * 使用这种方式，telnet的时候才能看到校验结果
     *
     * @param session
     * @param message
     */
    private void write(IoSession session, String message) {
        ByteBuffer wb = ByteBuffer.allocate(1024);
        wb.put(message.getBytes());
        wb.flip();
        session.write(wb);
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        System.out.println("发送消息" + message.toString());
    }

}
