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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.mina.common.IoFilter.NextFilter;
import org.apache.mina.common.IoFilterChain.Entry;

/**
 * The default implementation of {@link IoFilterChainBuilder} which is useful
 * in most cases.  {@link DefaultIoFilterChainBuilder} has an identical interface
 * with {@link IoFilter}; it contains a list of {@link IoFilter}s that you can
 * modify. The {@link IoFilter}s which are added to this builder will be appended
 * to the {@link IoFilterChain} when {@link #buildFilterChain(IoFilterChain)} is
 * invoked.
 * <p>
 * However, the identical interface doesn't mean that it behaves in an exactly
 * same way with {@link IoFilterChain}.  {@link DefaultIoFilterChainBuilder}
 * doesn't manage the life cycle of the {@link IoFilter}s at all, and the
 * existing {@link IoSession}s won't get affected by the changes in this builder.
 * {@link IoFilterChainBuilder}s affect only newly created {@link IoSession}s.
 *
 * <pre>
 * IoAcceptor acceptor = ...;
 * DefaultIoFilterChainBuilder builder = acceptor.getFilterChain();
 * builder.addLast( "myFilter", new MyFilter() );
 * ...
 * </pre>
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class DefaultIoFilterChainBuilder implements IoFilterChainBuilder, Cloneable {

    /** 保存过滤器的集合 */
    private final List<Entry> entries;

    /**
     * 创建一个空的过滤器链集合
     */
    public DefaultIoFilterChainBuilder() {
        // CopyOnWriteArrayList用于替代遍历操作为主情况下的同步List
        entries = new CopyOnWriteArrayList<Entry>();
    }

    public void buildFilterChain(IoFilterChain chain) throws Exception {
        for (Iterator i = entries.iterator(); i.hasNext(); ) {
            Entry e = (Entry) i.next();
            chain.addLast(e.getName(), e.getFilter());
        }
    }

    /**
     * 根据过滤器名称获取过滤器
     *
     * @param name
     * @return
     */
    public Entry getEntry(String name) {
        for (Entry e : entries) {
            if (e.getName().equals(name)) {
                return e;
            }
        }

        return null;
    }

    /**
     * 根据过滤器名称获取过滤器
     *
     * @param name
     * @return
     */
    public IoFilter get(String name) {
        Entry e = getEntry(name);
        if (e == null) {
            return null;
        }

        return e.getFilter();
    }

    /**
     * 获取所有的过滤器
     *
     * @return
     */
    public List<Entry> getAll() {
        return new ArrayList<Entry>(entries);
    }

    /**
     * 将所有的过滤器顺序反转然后返回
     *
     * @return
     */
    public List<Entry> getAllReversed() {
        List<Entry> result = getAll();
        Collections.reverse(result);
        return result;
    }

    /**
     * 是否包含指定名称的过滤器
     *
     * @param name
     * @return
     */
    public boolean contains(String name) {
        return getEntry(name) != null;
    }

    /**
     * 判断过滤器链中，是否存在该过滤器
     *
     * @param filter
     * @return
     */
    public boolean contains(IoFilter filter) {
        for (Iterator i = entries.iterator(); i.hasNext(); ) {
            Entry e = (Entry) i.next();
            if (e.getFilter() == filter) {
                return true;
            }
        }

        return false;
    }

    /**
     * 判断过滤器链中是否包含指定类型的过滤
     *
     * @param filterType
     * @return
     */
    public boolean contains(Class<? extends IoFilter> filterType) {
        for (Iterator i = entries.iterator(); i.hasNext(); ) {
            Entry e = (Entry) i.next();
            if (filterType.isAssignableFrom(e.getFilter().getClass())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 添加一个过滤器到过滤器链的最开始位置
     *
     * @param name
     * @param filter
     */
    public synchronized void addFirst(String name, IoFilter filter) {
        register(0, new EntryImpl(name, filter));
    }

    /**
     * 添加一个过滤器到过滤器链的最后位置
     *
     * @param name
     * @param filter
     */
    public synchronized void addLast(String name, IoFilter filter) {
        register(entries.size(), new EntryImpl(name, filter));
    }

    /**
     * 将过滤器添加到指定过滤器的前面
     *
     * @param baseName
     * @param name
     * @param filter
     */
    public synchronized void addBefore(String baseName, String name, IoFilter filter) {
        checkBaseName(baseName);

        for (ListIterator<Entry> i = entries.listIterator(); i.hasNext(); ) {
            Entry base = i.next();
            if (base.getName().equals(baseName)) {
                register(i.previousIndex(), new EntryImpl(name, filter));
                break;
            }
        }
    }

    /**
     * 将过滤器添加到指定过滤器的后面
     *
     * @param baseName
     * @param name
     * @param filter
     */
    public synchronized void addAfter(String baseName, String name, IoFilter filter) {
        checkBaseName(baseName);

        for (ListIterator<Entry> i = entries.listIterator(); i.hasNext(); ) {
            Entry base = i.next();
            if (base.getName().equals(baseName)) {
                register(i.nextIndex(), new EntryImpl(name, filter));
                break;
            }
        }
    }

    /**
     * 移除过滤器
     *
     * @param name
     * @return
     */
    public synchronized IoFilter remove(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }

        for (ListIterator<Entry> i = entries.listIterator(); i.hasNext(); ) {
            Entry e = i.next();
            if (e.getName().equals(name)) {
                entries.remove(i.previousIndex());
                return e.getFilter();
            }
        }

        throw new IllegalArgumentException("Unknown filter name: " + name);
    }

    /**
     * 移除所有过滤器
     *
     * @throws Exception
     */
    public synchronized void clear() throws Exception {
        entries.clear();
    }

    private void checkBaseName(String baseName) {
        if (baseName == null) {
            throw new NullPointerException("baseName");
        }

        if (!contains(baseName)) {
            throw new IllegalArgumentException("Unknown filter name: "
                    + baseName);
        }
    }

    private void register(int index, Entry e) {
        if (contains(e.getName())) {
            throw new IllegalArgumentException(
                    "Other filter is using the same name: " + e.getName());
        }

        entries.add(index, e);
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("{ ");

        boolean empty = true;

        for (Iterator i = entries.iterator(); i.hasNext(); ) {
            Entry e = (Entry) i.next();
            if (!empty) {
                buf.append(", ");
            } else {
                empty = false;
            }

            buf.append('(');
            buf.append(e.getName());
            buf.append(':');
            buf.append(e.getFilter());
            buf.append(')');
        }

        if (empty) {
            buf.append("empty");
        }

        buf.append(" }");

        return buf.toString();
    }

    public Object clone() {
        DefaultIoFilterChainBuilder ret = new DefaultIoFilterChainBuilder();
        for (Entry e : entries) {
            ret.addLast(e.getName(), e.getFilter());
        }
        return ret;
    }

    private static class EntryImpl implements Entry {
        private final String name;

        private final IoFilter filter;

        private EntryImpl(String name, IoFilter filter) {
            if (name == null) {
                throw new NullPointerException("name");
            }
            if (filter == null) {
                throw new NullPointerException("filter");
            }

            this.name = name;
            this.filter = filter;
        }

        public String getName() {
            return name;
        }

        public IoFilter getFilter() {
            return filter;
        }

        public NextFilter getNextFilter() {
            throw new IllegalStateException();
        }

        public String toString() {
            return "(" + getName() + ':' + filter + ')';
        }
    }
}
