/*
 * Copyright (c) 2013 NEC Corporation
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.vtn.manager.northbound;

import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.Application;

/**
 * This class is an instance of javax.ws.rs.core.Application used to return
 * the classes that will be instantiated for JAXRS processing,
 * this is necessary because the package scanning in jersey doesn't yet work
 * in OSGi environment.
 */
public class VTNManagerNorthboundRSApplication extends Application {
    /**
     * Return a set of root resource and provider classes.
     *
     * @return  A set of root resource and provider classes.
     */
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(VTenantNorthbound.class);
        classes.add(VBridgeNorthbound.class);
        classes.add(VBridgeInterfaceNorthbound.class);
        return classes;
    }
}