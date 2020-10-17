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
package org.apache.mina.common;

import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.common.support.BaseByteBuffer;

/**
 * A simplistic {@link ByteBufferAllocator} which simply allocates a new
 * buffer every time.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class SimpleByteBufferAllocator implements ByteBufferAllocator {

    private static final int MINIMUM_CAPACITY = 1;

    public SimpleByteBufferAllocator() {
    }

    /**
     * 创建一个缓存区
     *
     * @param capacity  缓存的大小
     * @param direct    是否直接使用内核缓存
     * @return
     */
    public ByteBuffer allocate(int capacity, boolean direct) {
        java.nio.ByteBuffer nioBuffer;
        if (direct) {
            // 直接使用内核缓存，当Java程序接收到外部传来的数据时，首先是被系统内存所获取，然后在由系统内存复制复制到JVM内存中供Java程序使用，使用allocateDirect()能够省去复制这一步操作，效率上会有所提高。可是系统级内存的分配比起JVM内存的分配要耗时得多
            nioBuffer = java.nio.ByteBuffer.allocateDirect(capacity);
        } else {
            // 当Java程序接收到外部传来的数据时，首先是被系统内存所获取，然后在由系统内存复制复制到JVM内存中供Java程序使用
            nioBuffer = java.nio.ByteBuffer.allocate(capacity);
        }
        return new SimpleByteBuffer(nioBuffer);
    }

    /**
     * 创建一个SimpleByteBuffer类型的缓存区
     *
     * @param nioBuffer
     * @return
     */
    public ByteBuffer wrap(java.nio.ByteBuffer nioBuffer) {
        return new SimpleByteBuffer(nioBuffer);
    }

    public void dispose() {
    }

    private static class SimpleByteBuffer extends BaseByteBuffer {
        private java.nio.ByteBuffer buf;

        private final AtomicInteger refCount = new AtomicInteger();

        protected SimpleByteBuffer(java.nio.ByteBuffer buf) {
            this.buf = buf;
            buf.order(ByteOrder.BIG_ENDIAN);
            refCount.set(1);
        }

        @Override
        public void acquire() {
            if (refCount.get() <= 0) {
                throw new IllegalStateException("Already released buffer.");
            }

            refCount.incrementAndGet();
        }

        @Override
        public void release() {
            if (refCount.get() <= 0) {
                refCount.set(0);
                throw new IllegalStateException("Already released buffer.  You released the buffer too many times.");
            }

            refCount.decrementAndGet();
        }

        @Override
        public java.nio.ByteBuffer buf() {
            return buf;
        }

        @Override
        public boolean isPooled() {
            return false;
        }

        @Override
        public void setPooled(boolean pooled) {
        }

        @Override
        protected void capacity0(int requestedCapacity) {
            int newCapacity = MINIMUM_CAPACITY;
            while (newCapacity < requestedCapacity) {
                newCapacity <<= 1;
            }

            java.nio.ByteBuffer oldBuf = this.buf;
            java.nio.ByteBuffer newBuf;
            if (isDirect()) {
                newBuf = java.nio.ByteBuffer.allocateDirect(newCapacity);
            } else {
                newBuf = java.nio.ByteBuffer.allocate(newCapacity);
            }

            newBuf.clear();
            oldBuf.clear();
            newBuf.put(oldBuf);
            this.buf = newBuf;
        }

        /**
         * 调用duplicate方法实际上会创建原缓存区的一个拷贝，不是深拷贝，是浅拷贝，什么意思呢，就是这两个缓存区会共享数据元素，但每个缓存区的上界、容量、位置等属性是各自独立的；
         * 修改其中一个缓存区的元素会影响另一个拷贝缓存区
         *
         * @return
         */
        @Override
        public ByteBuffer duplicate() {
            return new SimpleByteBuffer(this.buf.duplicate());
        }

        /**
         * 例如，对charbuffer1进行slice后，返回一个charbuffer2：
         * charbuffer1：mark = -1; position = 2; limit = 5; capacity = 10;
         * charbuffer2：mark = -1; position = 0; limit = 3; capacity = 3;
         *
         * @return
         */
        @Override
        public ByteBuffer slice() {
            return new SimpleByteBuffer(this.buf.slice());
        }

        @Override
        public ByteBuffer asReadOnlyBuffer() {
            return new SimpleByteBuffer(this.buf.asReadOnlyBuffer());
        }

        @Override
        public byte[] array() {
            return buf.array();
        }

        @Override
        public int arrayOffset() {
            return buf.arrayOffset();
        }
    }
}
