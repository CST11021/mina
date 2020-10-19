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
 * 将消息消息对应的字节缓冲区解码（反序列化）为对象，然后保存到ProtocolDecoderOutput，后续通过ProtocolDecoderOutput#flush()方法，调用下一个过滤器继续后续逻辑
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface ProtocolDecoder {

    /**
     * 将消息消息对应的字节缓冲区解码（反序列化）为对象，然后保存到ProtocolDecoderOutput，后续通过ProtocolDecoderOutput#flush()方法，调用下一个过滤器继续后续逻辑
     *
     * @param session
     * @param in        消息对应的字节缓冲区
     * @param out
     * @throws Exception
     */
    void decode(IoSession session, ByteBuffer in, ProtocolDecoderOutput out) throws Exception;

    /**
     * 当指定的会话关闭时调用，当您处理未指定消息长度的协议（例如，没有内容长度标头的HTTP响应）时，此方法很有用，实现此方法以处理#decode()方法未完全处理的剩余数据
     * 
     * @throws Exception if the read data violated protocol specification
     */
    void finishDecode(IoSession session, ProtocolDecoderOutput out) throws Exception;

    /**
     * 释放与此解码器有关的所有资源
     * 
     * @throws Exception if failed to dispose all resources
     */
    void dispose(IoSession session) throws Exception;

}