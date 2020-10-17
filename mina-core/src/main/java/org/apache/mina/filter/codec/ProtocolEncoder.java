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
package org.apache.mina.filter.codec;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;

/**
 * Encodes higher-level message objects into binary or protocol-specific data.
 * MINA invokes {@link #encode(IoSession, Object, ProtocolEncoderOutput)}
 * method with message which is popped from the session write queue, and then
 * the encoder implementation puts encoded {@link ByteBuffer}s into
 * {@link ProtocolEncoderOutput} by calling
 * {@link ProtocolEncoderOutput#write(ByteBuffer)}.
 * <p>
 * Please refer to
 * <a href="../../../../../xref-examples/org/apache/mina/examples/reverser/TextLineEncoder.html"><code>TextLineEncoder</code></a>
 * example. 
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface ProtocolEncoder {

    /**
     * 将更高级别的消息对象编码为二进制或特定于协议的数据。
     * MINA使用从会话写队列中弹出的消息调用{@link #encode(IoSession, Object, ProtocolEncoderOutput)} 方法，然后编码器实现将已编码的{@link ByteBuffer}放入{@link ProtocolEncoderOutput}。
     * 
     * @throws Exception if the message violated protocol specification
     */
    void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception;

    /**
     * 释放与此编码器有关的所有资源
     * 
     * @throws Exception if failed to dispose all resources
     */
    void dispose(IoSession session) throws Exception;
}