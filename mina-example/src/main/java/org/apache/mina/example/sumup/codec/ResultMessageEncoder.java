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
package org.apache.mina.example.sumup.codec;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.example.sumup.message.AbstractMessage;
import org.apache.mina.example.sumup.message.ResultMessage;
import org.apache.mina.filter.codec.demux.MessageEncoder;

/**
 * A {@link MessageEncoder} that encodes {@link ResultMessage}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ResultMessageEncoder extends AbstractMessageEncoder {

    /** 表示支持编码的消息类型，这里是ResultMessage.class */
    private static final Set<Class<?>> TYPES;

    static {
        Set<Class<?>> types = new HashSet<Class<?>>();
        types.add(ResultMessage.class);
        TYPES = Collections.unmodifiableSet(types);
    }

    public ResultMessageEncoder() {
        super(Constants.RESULT);
    }

    /**
     * 将消息对象的数据转为字节，并保存到Mina的ByteBuffer字节缓冲区中
     *
     * @param session
     * @param message
     * @param out
     */
    protected void encodeBody(IoSession session, AbstractMessage message, ByteBuffer out) {
        ResultMessage m = (ResultMessage) message;
        if (m.isOk()) {
            out.putShort((short) Constants.RESULT_OK);
            out.putInt(m.getValue());
        } else {
            out.putShort((short) Constants.RESULT_ERROR);
        }
    }

    /**
     * 返回该编码器支持的序列化对象类型
     *
     * @return
     */
    public Set<Class<?>> getMessageTypes() {
        return TYPES;
    }

    public void dispose() throws Exception {
    }

}
