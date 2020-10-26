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
package org.apache.mina.filter.codec.demux;

import java.util.Set;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

/**
 * Encodes messages of specific types specified by {@link #getMessageTypes()}.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 * 
 * @see DemuxingProtocolCodecFactory
 * @see MessageEncoderFactory
 */
public interface MessageEncoder {

    /**
     * 返回此编码器可以编码的一组消息类，对应#encode(IoSession, Object, ProtocolEncoderOutput)方法支持的message序列化的类型
     */
    Set<Class<?>> getMessageTypes();

    /**
     * 将消息对象序列化为字节，并保存到ProtocolEncoderOutput中
     *
     * @param session
     * @param message
     * @param out
     * @throws Exception
     */
    void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception;
}
