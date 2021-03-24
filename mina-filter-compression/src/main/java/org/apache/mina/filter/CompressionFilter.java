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
package org.apache.mina.filter;

import java.io.IOException;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.support.Zlib;

/**
 * 该过滤器的作用是：当接收数据的时候进行解压，当发送数据的进行压缩，以减少网络带宽
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class CompressionFilter extends IoFilterAdapter {

    /** 压缩速度慢，但是压缩率高 */
    public static final int COMPRESSION_MAX = Zlib.COMPRESSION_MAX;

    /** 压缩速度快，但是压缩率低 */
    public static final int COMPRESSION_MIN = Zlib.COMPRESSION_MIN;

    /** 不对数据进行压缩和解压 */
    public static final int COMPRESSION_NONE = Zlib.COMPRESSION_NONE;

    /**
     * The default compression level used. Provides the best balance between speed and compression
     */
    public static final int COMPRESSION_DEFAULT = Zlib.COMPRESSION_DEFAULT;

    /** 压缩实例{@link Zlib}对应的会话属性key */
    private static final String DEFLATER = CompressionFilter.class.getName() + ".Deflater";

    /** 解压实例{@link Zlib}对应的会话属性key */
    private static final String INFLATER = CompressionFilter.class.getName() + ".Inflater";

    /**
     * A flag that allows you to disable compression once.
     */
    public static final String DISABLE_COMPRESSION_ONCE = CompressionFilter.class.getName() + ".DisableCompressionOnce";

    /** 表示是否解压输入的数据 */
    private boolean compressInbound = true;

    /** 表示是否压缩输出的数据 */
    private boolean compressOutbound = true;

    /** 压缩等级 */
    private int compressionLevel;


    public CompressionFilter() {
        this(true, true, COMPRESSION_DEFAULT);
    }
    public CompressionFilter(final int compressionLevel) {
        this(true, true, compressionLevel);
    }
    /**
     * Creates a new instance.
     *
     * @param compressInbound  是否要对读取的数据进行解压缩
     * @param compressOutbound 是否要压缩写出的数据
     * @param compressionLevel the level of compression to be used. Must be one of {@link #COMPRESSION_DEFAULT},
     *                         {@link #COMPRESSION_MAX},
     *                         {@link #COMPRESSION_MIN}, and
     *                         {@link #COMPRESSION_NONE}.
     */
    public CompressionFilter(final boolean compressInbound, final boolean compressOutbound, final int compressionLevel) {
        this.compressionLevel = compressionLevel;
        this.compressInbound = compressInbound;
        this.compressOutbound = compressOutbound;
    }


    /**
     * 接收到数据的时候进行解压
     *
     * @param nextFilter
     * @param session
     * @param message
     * @throws Exception
     */
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        if (!compressInbound || !(message instanceof ByteBuffer)) {
            nextFilter.messageReceived(session, message);
            return;
        }

        // 获取解压工具
        Zlib inflater = (Zlib) session.getAttribute(INFLATER);
        if (inflater == null) {
            throw new IllegalStateException();
        }

        // 进行解压
        ByteBuffer inBuffer = (ByteBuffer) message;
        ByteBuffer outBuffer = inflater.inflate(inBuffer);
        inBuffer.release();
        nextFilter.messageReceived(session, outBuffer);
    }

    /**
     * 当session.write方法被调用时，会调用该方法，对数据进行压缩
     *
     * @param nextFilter
     * @param session
     * @param writeRequest
     * @throws Exception
     */
    public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws IOException {
        if (!compressOutbound) {
            nextFilter.filterWrite(session, writeRequest);
            return;
        }

        if (session.containsAttribute(DISABLE_COMPRESSION_ONCE)) {
            // Remove the marker attribute because it is temporary.
            session.removeAttribute(DISABLE_COMPRESSION_ONCE);
            nextFilter.filterWrite(session, writeRequest);
            return;
        }

        // 获取压缩工具
        Zlib deflater = (Zlib) session.getAttribute(DEFLATER);
        if (deflater == null) {
            throw new IllegalStateException();
        }

        ByteBuffer inBuffer = (ByteBuffer) writeRequest.getMessage();
        if (!inBuffer.hasRemaining()) {
            nextFilter.filterWrite(session, writeRequest);
        } else {
            ByteBuffer outBuf = deflater.deflate(inBuffer);
            inBuffer.release();
            nextFilter.filterWrite(session, new WriteRequest(outBuf, writeRequest.getFuture()));
        }
    }

    /**
     * 添加一个过滤器到链之前会调用该方法
     *
     * @param parent
     * @param name
     * @param nextFilter
     * @throws Exception
     */
    public void onPreAdd(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
        if (parent.contains(CompressionFilter.class)) {
            throw new IllegalStateException(
                    "A filter chain cannot contain more than"
                            + " one Stream Compression filter.");
        }

        Zlib deflater = new Zlib(compressionLevel, Zlib.MODE_DEFLATER);
        Zlib inflater = new Zlib(compressionLevel, Zlib.MODE_INFLATER);

        IoSession session = parent.getSession();

        session.setAttribute(DEFLATER, deflater);
        session.setAttribute(INFLATER, inflater);
    }

    /**
     * 从指定的父级删除此过滤器后调用该方法
     * 请注意，如果从多个父级中删除此过滤器，则可以多次调用此方法。
     * 始终在调用{@link #destroy()}之前调用此方法。
     *
     * @param parent     the parent who called this method
     * @param name       the name assigned to this filter
     * @param nextFilter the {@link NextFilter} for this filter.  You can reuse
     *                   this object until this filter is removed from the chain.
     */
    public void onPostRemove(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
        super.onPostRemove(parent, name, nextFilter);
        IoSession session = parent.getSession();
        if (session == null) {
            return;
        }

        Zlib inflater = (Zlib) session.getAttribute(INFLATER);
        Zlib deflater = (Zlib) session.getAttribute(DEFLATER);
        if (deflater != null) {
            deflater.cleanUp();
        }

        if (inflater != null) {
            inflater.cleanUp();
        }
    }

    /**
     * 如果正在压缩输入数据，则返回true。
     *
     * @return
     */
    public boolean isCompressInbound() {
        return compressInbound;
    }

    /**
     * Sets if incoming data has to be compressed.
     */
    public void setCompressInbound(boolean compressInbound) {
        this.compressInbound = compressInbound;
    }

    /**
     * Returns <tt>true</tt> if the filter is compressing data being written.
     */
    public boolean isCompressOutbound() {
        return compressOutbound;
    }

    /**
     * Set if outgoing data has to be compressed.
     */
    public void setCompressOutbound(boolean compressOutbound) {
        this.compressOutbound = compressOutbound;
    }



}
