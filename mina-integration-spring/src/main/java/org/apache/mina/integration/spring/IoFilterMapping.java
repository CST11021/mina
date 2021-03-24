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
package org.apache.mina.integration.spring;

import org.apache.mina.common.IoFilter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * 关联一个名词的过滤器
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 * 
 * @see org.apache.mina.integration.spring.IoAcceptorFactoryBean
 * @see org.apache.mina.integration.spring.Binding
 */
public class IoFilterMapping implements InitializingBean {

    private String name = null;

    private IoFilter filter = null;

    public IoFilterMapping() {
    }
    public IoFilterMapping(String name, IoFilter filter) {
        Assert.notNull(name, "Argument 'name' may not be null");
        Assert.notNull(filter, "Argument 'filter' may not be null");

        this.name = name;
        this.filter = filter;
    }

    /**
     * Returns the filter of this mapping.
     * 
     * @return the filter.
     */
    public IoFilter getFilter() {
        return filter;
    }

    /**
     * Returns the name associated with the filter.
     * 
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the filter of this mapping.
     * 
     * @param filter the filter.
     * @throws IllegalArgumentException if the specified value is 
     *         <code>null</code>.
     */
    public void setFilter(IoFilter filter) {
        Assert.notNull(filter, "Argument 'filter' may not be null");
        this.filter = filter;
    }

    /**
     * Sets the name associated with the filter.
     * 
     * @param name the name.
     * @throws IllegalArgumentException if the specified value is 
     *         <code>null</code>.
     */
    public void setName(String name) {
        Assert.notNull(name, "Argument 'name' may not be null");
        this.name = name;
    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(name, "Argument 'name' may not be null");
        Assert.notNull(filter, "Argument 'filter' may not be null");
    }
}
