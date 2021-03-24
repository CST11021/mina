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
package org.apache.mina.filter.support;

import java.io.IOException;

import org.apache.mina.common.ByteBuffer;

import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZStream;

/**
 * 使用JZlib库的压缩工具类. 该类既充当压缩器又充当解压缩器，但一次仅充当一个。
 * The only flush method supported is Z_SYNC_FLUSH also known as Z_PARTIAL_FLUSH
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class Zlib {

    /** 压缩速度慢，但是压缩率最高的实现，value = 9 */
    public static final int COMPRESSION_MAX = JZlib.Z_BEST_COMPRESSION;

    /** 压缩速度最快，但是压缩率最小的实现，value = 1 */
    public static final int COMPRESSION_MIN = JZlib.Z_BEST_SPEED;

    /** 不压缩的实现，value = 0 */
    public static final int COMPRESSION_NONE = JZlib.Z_NO_COMPRESSION;

    /** 默认的压缩实现，value = -1 */
    public static final int COMPRESSION_DEFAULT = JZlib.Z_DEFAULT_COMPRESSION;

    /** 表示压缩模式 */
    public static final int MODE_DEFLATER = 1;

    /** 表示解压模式 */
    public static final int MODE_INFLATER = 2;

    private int compressionLevel;

    /** JZlib库中的压缩实现 */
    private ZStream zStream = null;

    /** 压缩模式：1：压缩，2：解压 */
    private int mode = -1;

    /**
     * @param compressionLevel 应该使用的压缩级别
     * @param mode             实例运行的模式。可以是MODE_DEFLATER或MODE_INFLATER
     */
    public Zlib(int compressionLevel, int mode) {
        switch (compressionLevel) {
            case COMPRESSION_MAX:
            case COMPRESSION_MIN:
            case COMPRESSION_NONE:
            case COMPRESSION_DEFAULT:
                this.compressionLevel = compressionLevel;
                break;
            default:
                throw new IllegalArgumentException("invalid compression level specified");
        }

        zStream = new ZStream();

        switch (mode) {
            case MODE_DEFLATER:
                zStream.deflateInit(this.compressionLevel);
                break;
            case MODE_INFLATER:
                zStream.inflateInit();
                break;
            default:
                throw new IllegalArgumentException("invalid mode specified");
        }
        this.mode = mode;
    }

    /**
     * 解压方法
     *
     * @param inBuffer 要解压的字节
     *
     * @return 返回解压后的数据
     * @throws IOException if the decompression of the data failed for some reason.
     */
    public ByteBuffer inflate(ByteBuffer inBuffer) throws IOException {
        if (mode == MODE_DEFLATER) {
            throw new IllegalStateException("not initialized as INFLATER");
        }

        byte[] inBytes = new byte[inBuffer.remaining()];
        inBuffer.get(inBytes).flip();

        // We could probably do this better, if we're willing to return multiple buffers
        //  (e.g. with a callback function)
        byte[] outBytes = new byte[inBytes.length * 2];
        ByteBuffer outBuffer = ByteBuffer.allocate(outBytes.length);
        outBuffer.setAutoExpand(true);

        zStream.next_in = inBytes;
        zStream.next_in_index = 0;
        zStream.avail_in = inBytes.length;
        zStream.next_out = outBytes;
        zStream.next_out_index = 0;
        zStream.avail_out = outBytes.length;
        int retval = 0;

        do {
            retval = zStream.inflate(JZlib.Z_SYNC_FLUSH);
            switch (retval) {
                case JZlib.Z_OK:
                    // completed decompression, lets copy data and get out
                case JZlib.Z_BUF_ERROR:
                    // need more space for output. store current output and get more
                    outBuffer.put(outBytes, 0, zStream.next_out_index);
                    zStream.next_out_index = 0;
                    zStream.avail_out = outBytes.length;
                    break;
                default:
                    // unknown error
                    outBuffer.release();
                    outBuffer = null;
                    if (zStream.msg == null)
                        throw new IOException("Unknown error. Error code : "
                                + retval);
                    else
                        throw new IOException("Unknown error. Error code : "
                                + retval + " and message : " + zStream.msg);
            }
        } while (zStream.avail_in > 0);

        return outBuffer.flip();
    }

    /**
     * 压缩方法
     *
     * @param inBuffer 要压缩的字节
     * @return 返回压缩后的字节
     * @throws IOException if the compression of teh buffer failed for some reason
     */
    public ByteBuffer deflate(ByteBuffer inBuffer) throws IOException {
        if (mode == MODE_INFLATER) {
            throw new IllegalStateException("not initialized as DEFLATER");
        }

        byte[] inBytes = new byte[inBuffer.remaining()];
        inBuffer.get(inBytes).flip();

        // according to spec, destination buffer should be 0.1% larger
        // than source length plus 12 bytes. We add a single byte to safeguard
        // against rounds that round down to the smaller value
        int outLen = (int) Math.round(inBytes.length * 1.001) + 1 + 12;
        byte[] outBytes = new byte[outLen];

        zStream.next_in = inBytes;
        zStream.next_in_index = 0;
        zStream.avail_in = inBytes.length;
        zStream.next_out = outBytes;
        zStream.next_out_index = 0;
        zStream.avail_out = outBytes.length;

        int retval = zStream.deflate(JZlib.Z_SYNC_FLUSH);
        if (retval != JZlib.Z_OK) {
            outBytes = null;
            inBytes = null;
            throw new IOException("Compression failed with return value : "
                    + retval);
        }

        ByteBuffer outBuf = ByteBuffer
                .wrap(outBytes, 0, zStream.next_out_index);

        return outBuf;
    }

    /**
     * Cleans up the resources used by the compression library.
     */
    public void cleanUp() {
        if (zStream != null)
            zStream.free();
    }
}
