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

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.mina.util.ExpirationListener;
import org.apache.mina.util.ExpiringMap;

/**
 * An {@link IoSessionRecycler} with sessions that time out on inactivity.
 * <p>
 * TODO Document me.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ExpiringSessionRecycler implements IoSessionRecycler {

    /** 过期式的Map缓存，超过指定时间移除Session */
    private ExpiringMap<Object, IoSession> sessionMap;

    /** 用于定时移除过期的Session */
    private ExpiringMap.Expirer mapExpirer;

    public ExpiringSessionRecycler() {
        this(ExpiringMap.DEFAULT_TIME_TO_LIVE);
    }

    public ExpiringSessionRecycler(int timeToLive) {
        this(timeToLive, ExpiringMap.DEFAULT_EXPIRATION_INTERVAL);
    }

    public ExpiringSessionRecycler(int timeToLive, int expirationInterval) {
        sessionMap = new ExpiringMap<Object, IoSession>(timeToLive,
                expirationInterval);
        mapExpirer = sessionMap.getExpirer();
        sessionMap.addExpirationListener(new DefaultExpirationListener());
    }

    /**
     * 保存session
     *
     * @param session the new {@link IoSession}.
     */
    public void put(IoSession session) {
        mapExpirer.startExpiringIfNotStarted();

        // 获取session标识
        Object key = generateKey(session);

        // 保存待回收的session
        if (!sessionMap.containsKey(key)) {
            sessionMap.put(key, session);
        }
    }

    /**
     * 获取要回收的session
     *
     * @param localAddress  服务端地址
     * @param remoteAddress 客户端地址
     * @return
     */
    public IoSession recycle(SocketAddress localAddress, SocketAddress remoteAddress) {
        return sessionMap.get(generateKey(localAddress, remoteAddress));
    }

    /**
     * 将session移除
     *
     * @param session the new {@link IoSession}.
     */
    public void remove(IoSession session) {
        sessionMap.remove(generateKey(session));
    }

    public void stopExpiring() {
        mapExpirer.stopExpiring();
    }

    public int getExpirationInterval() {
        return sessionMap.getExpirationInterval();
    }

    public int getTimeToLive() {
        return sessionMap.getTimeToLive();
    }

    public void setExpirationInterval(int expirationInterval) {
        sessionMap.setExpirationInterval(expirationInterval);
    }

    public void setTimeToLive(int timeToLive) {
        sessionMap.setTimeToLive(timeToLive);
    }

    private Object generateKey(IoSession session) {
        return generateKey(session.getLocalAddress(), session
                .getRemoteAddress());
    }

    private Object generateKey(SocketAddress localAddress, SocketAddress remoteAddress) {
        List<SocketAddress> key = new ArrayList<SocketAddress>(2);
        key.add(remoteAddress);
        key.add(localAddress);
        return key;
    }

    private class DefaultExpirationListener implements ExpirationListener<IoSession> {

        public void expired(IoSession expiredSession) {
            expiredSession.close();
        }

    }
}
