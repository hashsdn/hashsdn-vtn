/*
 * Copyright (c) 2014 NEC Corporation
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.vtn.manager.internal.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.vtn.manager.VTNException;
import org.opendaylight.vtn.manager.flow.filter.PassFilter;
import org.opendaylight.vtn.manager.flow.filter.FlowFilter;

/**
 * This class describes PASS flow filter, which lets packets through the
 * virtual node.
 *
 * <p>
 *   Although this class is public to other packages, this class does not
 *   provide any API. Applications other than VTN Manager must not use this
 *   class.
 * </p>
 */
public final class PassFlowFilterImpl extends FlowFilterImpl {
    /**
     * Version number for serialization.
     */
    private static final long serialVersionUID = 5852721392105729083L;

    /**
     * Logger instance.
     */
    private static final Logger  LOG =
        LoggerFactory.getLogger(PassFlowFilterImpl.class);

    /**
     * Construct a new instance.
     *
     * @param idx     An index number to be assigned.
     * @param filter  A {@link FlowFilter} instance.
     * @throws VTNException
     *    {@code filter} contains invalid value.
     */
    protected PassFlowFilterImpl(int idx, FlowFilter filter)
        throws VTNException {
        super(idx, filter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PassFilter getFilterType() {
        return new PassFilter();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
