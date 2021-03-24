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

import java.util.List;

import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoFilter;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.util.Assert;

/**
 * 用于创建{@link DefaultIoFilterChainBuilder}实例的Spring工厂Bean
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class DefaultIoFilterChainBuilderFactoryBean extends AbstractFactoryBean {

    private IoFilterMapping[] filterMappings = new IoFilterMapping[0];

    private String prefix = "filter";

    protected Object createInstance() throws Exception {
        DefaultIoFilterChainBuilder builder = new DefaultIoFilterChainBuilder();
        for (int i = 0; i < filterMappings.length; i++) {
            String name = filterMappings[i].getName();
            if (name == null) {
                name = prefix + i;
            }
            builder.addLast(name, filterMappings[i].getFilter());
        }

        return builder;
    }

    public Class getObjectType() {
        return DefaultIoFilterChainBuilder.class;
    }

    /**
     * Sets the prefix used to create the names for automatically named filters
     * added using setFilters(IoFilter[]). The default prefix is
     * <tt>filter</tt>.
     * 
     * @param prefix the prefix.
     * @throws IllegalArgumentException if the specified value is 
     *         <code>null</code>.
     */
    public void setFilterNamePrefix(String prefix) {
        Assert.notNull(prefix, "Property 'filterNamePrefix' may not be null");
        this.prefix = prefix;
    }

    /**
     * Sets a number of filters which will be added to the filter
     * chain created by this factory bean. The specified list must contain either
     * {@link IoFilter} or {@link IoFilterMapping} objects. Filters which
     * haven't been wrapped in {@link IoFilterMapping} objects will be assigned 
     * automatically generated names (<code>&lt;filterNamePrefix&gt;0</code>, 
     * <code>&lt;filterNamePrefix&gt;1</code>, etc).
     * 
     * @param filters the list of {@link IoFilter} and/or 
     *        {@link IoFilterMapping} objects.
     * @throws IllegalArgumentException if the specified value is 
     *         <code>null</code> or contains objects of the wrong type.
     * @see #setFilterNamePrefix(String)
     */
    public void setFilters(List filters) {
        Assert.notNull(filters, "Property 'filters' may not be null");
        IoFilterMapping[] filterMappings = new IoFilterMapping[filters.size()];

        for (int i = 0; i < filterMappings.length; i++) {
            Object o = filters.get(i);
            if (o instanceof IoFilterMapping) {
                filterMappings[i] = (IoFilterMapping) o;
            } else if (o instanceof IoFilter) {
                filterMappings[i] = new IoFilterMapping();
                filterMappings[i].setFilter((IoFilter) o);
            } else {
                throw new IllegalArgumentException(
                        "List may only contain "
                                + "IoFilter or IoFilterMapping objects. Found object of "
                                + "type " + o.getClass().getName()
                                + " at position " + i + ".");
            }
        }

        this.filterMappings = filterMappings;
    }

}
