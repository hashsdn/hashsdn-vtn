/*
 * Copyright (c) 2015, 2016 NEC Corporation. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.vtn.manager.internal.inventory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.vtn.manager.internal.VTNManagerProvider;
import org.opendaylight.vtn.manager.internal.util.ChangedData;
import org.opendaylight.vtn.manager.internal.util.IdentifiedData;
import org.opendaylight.vtn.manager.internal.util.IdentifierTargetComparator;
import org.opendaylight.vtn.manager.internal.util.MiscUtils;
import org.opendaylight.vtn.manager.internal.util.MultiDataStoreListener;
import org.opendaylight.vtn.manager.internal.util.VTNEntityType;
import org.opendaylight.vtn.manager.internal.util.inventory.InventoryUtils;
import org.opendaylight.vtn.manager.internal.util.tx.TxQueueImpl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;

import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.impl.inventory.rev150209.VtnNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.impl.inventory.rev150209.VtnOpenflowVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.impl.inventory.rev150209.vtn.node.info.VtnPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.impl.inventory.rev150209.vtn.nodes.VtnNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.types.rev150209.VtnUpdateType;

/**
 * Internal inventory manager.
 */
public final class VTNInventoryManager
    extends MultiDataStoreListener<VtnNode, Void> {
    /**
     * Logger instance.
     */
    private static final Logger  LOG =
        LoggerFactory.getLogger(VTNInventoryManager.class);

    /**
     * Comparator for the target type of instance identifier that specifies
     * the order of data change event processing.
     *
     * <p>
     *   This comparator associates {@link VtnPort} class with the order
     *   smaller than {@link VtnNode}.
     * </p>
     */
    private static final IdentifierTargetComparator  PATH_COMPARATOR;

    /**
     * VTN Manager provider service.
     */
    private final VTNManagerProvider  vtnProvider;

    /**
     * A list of VTN inventory listeners.
     */
    private final List<VTNInventoryListener>  vtnListeners =
        new CopyOnWriteArrayList<>();

    /**
     * MD-SAL datastore transaction queue for inventory information.
     */
    private final TxQueueImpl  inventoryQueue;

    /**
     * The VTN node manager.
     */
    private final VtnNodeManager  nodeManager = new VtnNodeManager();

    /**
     * A set of node-connector-id values for switch ports present in the
     * vtn-inventory tree.
     */
    private final ConcurrentMap<String, Boolean>  portIds =
        new ConcurrentHashMap<>();

    /**
     * Initialize static fields.
     */
    static {
        PATH_COMPARATOR = new IdentifierTargetComparator().
            setOrder(VtnNode.class, 1).
            setOrder(VtnPort.class, 2);
    }

    /**
     * Construct a new instance.
     *
     * @param provider  A VTN Manager provider service.
     */
    public VTNInventoryManager(VTNManagerProvider provider) {
        super(VtnNode.class);
        vtnProvider = provider;
        TxQueueImpl queue = new TxQueueImpl("VTN Inventory", provider);
        inventoryQueue = queue;
        addCloseable(queue);

        // Initialize MD-SAL inventory listeners.
        DataBroker broker = provider.getDataBroker();
        try {
            addCloseable(new NodeListener(queue, broker));
            addCloseable(new NodeConnectorListener(queue, broker));
            addCloseable(new TopologyListener(queue, broker));

            // Register VTN inventory listener.
            registerListener(broker, LogicalDatastoreType.OPERATIONAL,
                             DataChangeScope.SUBTREE, true);
        } catch (Exception e) {
            String msg = "Failed to initialize inventory service.";
            LOG.error(msg, e);
            close();
            throw new IllegalStateException(msg, e);
        }
    }

    /**
     * Create a new static network topology manager.
     *
     * @return  A {@link StaticTopologyManager} instance.
     */
    public StaticTopologyManager newStaticTopologyManager() {
        return new StaticTopologyManager(vtnProvider, inventoryQueue);
    }

    /**
     * Start the inventory service.
     */
    public void start() {
        inventoryQueue.start();
    }

    /**
     * Add the given VTN inventory listener.
     *
     * @param l  A VTN inventory listener.
     */
    public void addListener(VTNInventoryListener l) {
        vtnListeners.add(l);
    }

    /**
     * Return the VTN node manager.
     *
     * @return  The VTN node manager.
     */
    public VtnNodeManager getVtnNodeManager() {
        return nodeManager;
    }

    /**
     * Shutdown listener service.
     */
    public void shutdown() {
        vtnListeners.clear();
        nodeManager.close();
    }

    /**
     * Handle inventory creation or removal event.
     *
     * @param data  A created or removed data object.
     * @param type  {@link VtnUpdateType#CREATED} on added,
     *              {@link VtnUpdateType#REMOVED} on removed.
     */
    private void onCreatedOrRemoved(IdentifiedData<?> data,
                                    VtnUpdateType type) {
        boolean owner = vtnProvider.isOwner(VTNEntityType.INVENTORY);
        IdentifiedData<VtnNode> nodeData = data.checkType(VtnNode.class);
        if (nodeData != null) {
            onCreatedOrRemoved(nodeData.getValue(), type, owner);
            return;
        }

        IdentifiedData<VtnPort> portData = data.checkType(VtnPort.class);
        if (portData != null) {
            onCreatedOrRemoved(portData.getValue(), type, owner);
            return;
        }

        // This should never happen.
        LOG.warn("{}: Unexpected event: path={}, data={}",
                 type, data.getIdentifier(), data.getValue());
    }

    /**
     * Handle node creation or removal event.
     *
     * @param vnode  A {@link VtnNode} instance.
     * @param type   {@link VtnUpdateType#CREATED} on added,
     *               {@link VtnUpdateType#REMOVED} on removed.
     * @param owner  {@code true} indicates that this process is the owner of
     *               inventory information.
     */
    private void onCreatedOrRemoved(VtnNode vnode, VtnUpdateType type,
                                    boolean owner) {
        String id = updateNode(vnode, type);
        if (id != null) {
            LOG.info("Node has been {}: id={}, proto={}",
                     MiscUtils.toLowerCase(type), id,
                     vnode.getOpenflowVersion());
            if (owner) {
                LOG.trace("Delivering node event: id={}, type={}", id, type);
                postVtnNodeEvent(vnode, type);
            } else {
                LOG.trace("Don't deliver node event: id={}, type={}",
                          id, type);
            }
        }
    }

    /**
     * Handle port creation or removal event.
     *
     * @param vport  A {@link VtnPort} instance.
     * @param type   {@link VtnUpdateType#CREATED} on added,
     *               {@link VtnUpdateType#REMOVED} on removed.
     * @param owner  {@code true} indicates that this process is the owner of
     *               inventory information.
     */
    private void onCreatedOrRemoved(VtnPort vport, VtnUpdateType type,
                                    boolean owner) {
        String id = updatePort(vport, type);
        if (id != null) {
            Boolean state = InventoryUtils.isEnabled(vport);
            Boolean isl = InventoryUtils.hasPortLink(vport);
            LOG.info("Port has been {}: {}", MiscUtils.toLowerCase(type),
                     InventoryUtils.toString(vport));
            if (owner) {
                LOG.trace("Delivering port event: id={}, type={}", id, type);
                postVtnPortEvent(vport, state, isl, type);
            } else {
                LOG.trace("Don't deliver port event: id={}, type={}",
                          id, type);
            }
        }
    }

    /**
     * Handle VTN node change event.
     *
     * @param oldNode  A {@link VtnNode} instance before updated.
     * @param newNode  An updated {@link VtnNode} instance.
     */
    private void onChanged(VtnNode oldNode, VtnNode newNode) {
        VtnOpenflowVersion oldVer = oldNode.getOpenflowVersion();
        VtnOpenflowVersion newVer = newNode.getOpenflowVersion();
        if (oldVer == null && newVer != null) {
            LOG.info("{}: Protocol version has been detected: {}",
                     newNode.getId().getValue(), newVer);
        }
    }

    /**
     * Handle VTN port change event.
     *
     * @param oldPort  A {@link VtnPort} instance before updated.
     * @param newPort  An updated {@link VtnPort} instance.
     */
    private void onChanged(VtnPort oldPort, VtnPort newPort) {
        boolean oldState = InventoryUtils.isEnabled(oldPort);
        boolean newState = InventoryUtils.isEnabled(newPort);
        Boolean state = (oldState == newState)
            ? null : Boolean.valueOf(newState);
        boolean oldIsl = InventoryUtils.hasPortLink(oldPort);
        boolean newIsl = InventoryUtils.hasPortLink(newPort);
        Boolean isl = (oldIsl == newIsl)
            ? null : Boolean.valueOf(newIsl);
        LOG.info("Port has been changed: old={}, new={}",
                 InventoryUtils.toString(oldPort),
                 InventoryUtils.toString(newPort));
        postVtnPortEvent(newPort, state, isl, VtnUpdateType.CHANGED);
    }

    /**
     * Post a VTN node event.
     *
     * @param vnode  A {@link VtnNode} instance.
     * @param type   A {@link VtnUpdateType} instance.
     */
    private void postVtnNodeEvent(VtnNode vnode, VtnUpdateType type) {
        VtnNodeEvent ev = null;
        for (VTNInventoryListener l: vtnListeners) {
            ev = (ev == null)
                ? new VtnNodeEvent(l, vnode, type)
                : new VtnNodeEvent(l, ev);
            vtnProvider.post(ev);
        }
    }

    /**
     * Post a VTN port event.
     *
     * @param vport  A {@link VtnPort} instance.
     * @param state  A {@link Boolean} instance which describes the change of
     *               running state of the given port.
     * @param isl    A {@link Boolean} instance which describes the change of
     *               inter-switch link state.
     * @param type   A {@link VtnUpdateType} instance.
     */
    private void postVtnPortEvent(VtnPort vport, Boolean state, Boolean isl,
                                  VtnUpdateType type) {
        VtnPortEvent ev = null;
        for (VTNInventoryListener l: vtnListeners) {
            ev = (ev == null)
                ? new VtnPortEvent(l, vport, state, isl, type)
                : new VtnPortEvent(l, ev);
            vtnProvider.post(ev);
        }
    }

    /**
     * Update the VTN node information.
     *
     * @param vnode  A {@link VtnNode} instance.
     * @param type   {@link VtnUpdateType#CREATED} indicates the given node
     *               has been created.
     *               {@link VtnUpdateType#REMOVED} indicates the given node
     *               has been removed.
     * @return  The node-id value if the VTN node information was updated.
     *          {@code null} if not updated.
     */
    private String updateNode(VtnNode vnode, VtnUpdateType type) {
        return (type == VtnUpdateType.CREATED)
            ? nodeManager.add(vnode)
            : nodeManager.remove(vnode);
    }

    /**
     * Update {@link #portIds} for the given port.
     *
     * @param vport  A {@link VtnPort} instance.
     * @param type   {@link VtnUpdateType#CREATED} indicates the given port
     *               has been created.
     *               {@link VtnUpdateType#REMOVED} indicates the given port
     *               has been removed.
     * @return  The node-connector-id value if {@link #portIds} was updated.
     *          {@code null} if not updated.
     */
    private String updatePort(VtnPort vport, VtnUpdateType type) {
        String id = vport.getId().getValue();

        if (type == VtnUpdateType.CREATED) {
            if (portIds.putIfAbsent(id, Boolean.TRUE) != null) {
                id = null;
            }
        } else if (portIds.remove(id) == null) {
            id = null;
        }

        return id;
    }

    // AutoCloseable

    /**
     * Close the VTN inventory service.
     */
    @Override
    public void close() {
        shutdown();
        super.close();
    }

    // MultiDataStoreListener

    /**
     * {@inheritDoc}
     */
    @Override
    protected IdentifierTargetComparator getComparator() {
        return PATH_COMPARATOR;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean getOrder(VtnUpdateType type) {
        // Removal events should be processed from inner to outer.
        // Other events should be processed from outer to inner.
        return (type != VtnUpdateType.REMOVED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Void enterEvent(
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> ev) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void exitEvent(Void ectx) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreated(Void ectx, IdentifiedData<?> data) {
        onCreatedOrRemoved(data, VtnUpdateType.CREATED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onUpdated(Void ectx, ChangedData<?> data) {
        if (vtnProvider.isOwner(VTNEntityType.INVENTORY)) {
            ChangedData<VtnNode> nodeData = data.checkType(VtnNode.class);
            if (nodeData != null) {
                onChanged(nodeData.getOldValue(), nodeData.getValue());
                return;
            }

            ChangedData<VtnPort> portData = data.checkType(VtnPort.class);
            if (portData != null) {
                onChanged(portData.getOldValue(), portData.getValue());
                return;
            }

            // This should never happen.
            LOG.warn("CHANGED: Unexpected event: path={}, old={}, new={}",
                     data.getIdentifier(), data.getOldValue(),
                     data.getValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onRemoved(Void ectx, IdentifiedData<?> data) {
        onCreatedOrRemoved(data, VtnUpdateType.REMOVED);
    }

    // AbstractDataChangeListener

    /**
     * {@inheritDoc}
     */
    @Override
    protected InstanceIdentifier<VtnNode> getWildcardPath() {
        return InstanceIdentifier.builder(VtnNodes.class).
            child(VtnNode.class).build();
    }

    // CloseableContainer

    /**
     * {@inheritDoc}
     */
    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
