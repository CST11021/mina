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
package org.apache.mina.filter.codec.textline;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

/**
 * A {@link ProtocolEncoder} which encodes a string into a text line
 * which ends with the delimiter.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$,
 */
public class TextLineEncoder extends ProtocolEncoderAdapter {
    private static final String ENCODER = TextLineEncoder.class.getName() + ".encoder";

    /** 消息的编码类型，一般是UTF-8 */
    private final Charset charset;

    /** 附加到文本行末尾的定界符 */
    private final LineDelimiter delimiter;

    private int maxLineLength = Integer.MAX_VALUE;

    public TextLineEncoder() {
        this(Charset.defaultCharset(), LineDelimiter.UNIX);
    }

    public TextLineEncoder(LineDelimiter delimiter) {
        this(Charset.defaultCharset(), delimiter);
    }

    public TextLineEncoder(Charset charset) {
        this(charset, LineDelimiter.UNIX);
    }

    public TextLineEncoder(Charset charset, LineDelimiter delimiter) {
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        if (delimiter == null) {
            throw new NullPointerException("delimiter");
        }
        if (LineDelimiter.AUTO.equals(delimiter)) {
            throw new IllegalArgumentException(
                    "AUTO delimiter is not allowed for encoder.");
        }

        this.charset = charset;
        this.delimiter = delimiter;
    }

    /**
     * Returns the allowed maximum size of the encoded line.
     * If the size of the encoded line exceeds this value, the encoder
     * will throw a {@link IllegalArgumentException}.  The default value
     * is {@link Integer#MAX_VALUE}.
     */
    public int getMaxLineLength() {
        return maxLineLength;
    }

    /**
     * Sets the allowed maximum size of the encoded line.
     * If the size of the encoded line exceeds this value, the encoder
     * will throw a {@link IllegalArgumentException}.  The default value
     * is {@link Integer#MAX_VALUE}.
     */
    public void setMaxLineLength(int maxLineLength) {
        if (maxLineLength <= 0) {
            throw new IllegalArgumentException("maxLineLength: "
                    + maxLineLength);
        }

        this.maxLineLength = maxLineLength;
    }

    /**
     * 文本行编码器，该方法是作用是将字符串类型的消息转为字节换取，然后再追加文本行定界符
     *
     * @param session
     * @param message   文本数据，数据类型一般都是字符串
     * @param out
     * @throws Exception
     */
    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
        // 获取编码类型，一般为UTF-8
        CharsetEncoder encoder = (CharsetEncoder) session.getAttribute(ENCODER);
        if (encoder == null) {
            encoder = charset.newEncoder();
            session.setAttribute(ENCODER, encoder);
        }

        // 文本行编码器的消息都是字符串，直接转为字符串就行
        String value = message.toString();
        ByteBuffer buf = ByteBuffer.allocate(value.length()).setAutoExpand(true);
        buf.putString(value, encoder);
        if (buf.position() > maxLineLength) {
            throw new IllegalArgumentException("Line length: " + buf.position());
        }

        buf.putString(delimiter.getValue(), encoder);
        buf.flip();
        out.write(buf);
    }

    public void dispose() throws Exception {
    }
}