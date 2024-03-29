/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2011 INRIA/University of
 *                 Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 * $$PROACTIVE_INITIAL_DEV$$
 */
package org.ow2.proactive_grid_cloud_portal.rm.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.ow2.proactive_grid_cloud_portal.common.client.Images;
import org.ow2.proactive_grid_cloud_portal.rm.client.NodeSource.Host;
import org.ow2.proactive_grid_cloud_portal.rm.client.NodeSource.Host.Node;
import org.ow2.proactive_grid_cloud_portal.rm.client.RMListeners.NodeSelectedListener;
import org.ow2.proactive_grid_cloud_portal.rm.client.RMListeners.NodesListener;

import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.TreeModelType;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.MenuItemSeparator;
import com.smartgwt.client.widgets.menu.events.ClickHandler;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeGridField;
import com.smartgwt.client.widgets.tree.TreeNode;
import com.smartgwt.client.widgets.tree.events.NodeClickEvent;
import com.smartgwt.client.widgets.tree.events.NodeClickHandler;
import com.smartgwt.client.widgets.tree.events.NodeContextClickEvent;
import com.smartgwt.client.widgets.tree.events.NodeContextClickHandler;


/**
 * Displays current nodes in a hierarchical tree view
 * <p>
 * NodeSource > Host > Node
 * 
 * 
 * 
 * 
 * @author mschnoor
 *
 */
public class TreeView implements NodesListener, NodeSelectedListener {

    private RMController controller = null;

    /** tree view */
    private TreeGrid treeGrid = null;
    /** tree data */
    private Tree tree = null;

    /** parameter for {@link #nodesUpdated(Map)} last time it was called */
    private Map<String, NodeSource> oldNodes = null;
    /** treenodes currently held by {@link #tree} */
    private HashMap<String, TreeNode> curNodes = null;

    /** prevent event cycling */
    private boolean ignoreNodeSelectedEvent = false;

    private class TNode extends TreeNode {
        Node rmNode = null;

        public TNode(String name, Node node) {
            super(name);
            this.rmNode = node;
        }
    }

    private class THost extends TreeNode {
        Host rmHost = null;

        public THost(String name, Host h) {
            super(name);
            this.rmHost = h;
        }
    }

    private class TNS extends TreeNode {
        NodeSource rmNS = null;

        public TNS(String name, NodeSource ns) {
            super(name);
            this.rmNS = ns;
        }
    }

    TreeView(RMController controller) {
        this.controller = controller;
        this.controller.getEventDispatcher().addNodesListener(this);
        this.controller.getEventDispatcher().addNodeSelectedListener(this);
        this.curNodes = new HashMap<String, TreeNode>();
        this.oldNodes = new HashMap<String, NodeSource>();
    }

    Canvas build() {
        VLayout vl = new VLayout();

        this.treeGrid = new TreeGrid();
        treeGrid.setWidth100();
        treeGrid.setHeight100();
        treeGrid.setShowHeader(false);
        treeGrid.setSelectionType(SelectionStyle.SINGLE);

        TreeGridField field = new TreeGridField("name");
        field.setCanSort(true);
        field.setSortByDisplayField(true);

        treeGrid.setFields(field);
        treeGrid.setSortField("name");

        this.tree = new Tree();
        tree.setModelType(TreeModelType.PARENT);
        tree.setNameProperty("name");
        tree.setIdField("nodeId");

        this.treeGrid.setData(this.tree);

        this.treeGrid.addNodeClickHandler(new NodeClickHandler() {
            @Override
            public void onNodeClick(NodeClickEvent event) {
                TreeNode n = event.getNode();
                if (n instanceof TNode) {
                    TNode tn = (TNode) n;
                    ignoreNodeSelectedEvent = true;
                    TreeView.this.controller.selectNode(tn.rmNode);
                } else if (n instanceof TNS) {
                    TNS tn = (TNS) n;
                    ignoreNodeSelectedEvent = true;
                    TreeView.this.controller.selectNodeSource(tn.rmNS);
                } else if (n instanceof THost) {
                    THost tn = (THost) n;
                    ignoreNodeSelectedEvent = true;
                    TreeView.this.controller.selectHost(tn.rmHost);
                }
            }
        });

        this.treeGrid.addNodeContextClickHandler(new NodeContextClickHandler() {
            @Override
            public void onNodeContextClick(NodeContextClickEvent event) {

                final TreeNode n = event.getNode();
                if (n instanceof TNode) {
                    TNode tn = (TNode) n;
                    TreeView.this.controller.selectNode(tn.rmNode);
                } else if (n instanceof TNS) {
                    TNS tn = (TNS) n;
                    TreeView.this.controller.selectNodeSource(tn.rmNS);
                } else if (n instanceof THost) {
                    THost tn = (THost) n;
                    TreeView.this.controller.selectHost(tn.rmHost);
                }

                Menu menu = new Menu();
                menu.setShowShadow(true);
                menu.setShadowDepth(10);

                MenuItem expandItem = new MenuItem("Expand all", Images.instance.expand_16().getSafeUri()
                        .asString());
                expandItem.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(MenuItemClickEvent event) {
                        expandAll();
                    }
                });

                MenuItem collapseItem = new MenuItem("Collapse all", Images.instance.close_16().getSafeUri()
                        .asString());
                collapseItem.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(MenuItemClickEvent event) {
                        closeAll();
                    }
                });

                MenuItem removeItem = new MenuItem("Remove", RMImages.instance.node_remove_16().getSafeUri()
                        .asString());
                removeItem.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(MenuItemClickEvent event) {
                        controller.removeNodes();
                    }
                });

                MenuItem lockItem = new MenuItem("Lock", RMImages.instance.node_locked_16().getSafeUri()
                        .asString());
                lockItem.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(MenuItemClickEvent event) {
                        controller.lockNodes();
                    }
                });

                MenuItem unlockItem = new MenuItem("Unlock", RMImages.instance.node_free_16().getSafeUri()
                        .asString());
                unlockItem.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(MenuItemClickEvent event) {
                        controller.unlockNodes();
                    }
                });

                menu.setItems(expandItem, collapseItem, new MenuItemSeparator(), unlockItem, removeItem);
                treeGrid.setContextMenu(menu);
            }
        });

        vl.addMember(treeGrid);
        return vl;
    }

    /*
     * (non-Javadoc)
     * @see org.ow2.proactive_grid_cloud_portal.rm.client.Listeners.NodesListener#nodesUpdated(java.util.Map)
     */
    public void nodesUpdated(Map<String, NodeSource> nodes) {

        /* Add to _this.tree_ the nodes contained in _nodes_ but not present
         * in _oldNodes_
         */
        for (NodeSource ns : nodes.values()) {
            String nsName = ns.getSourceName();
            TNS nsTreeNode = new TNS(nsName + " <span style='color:#777;'>" + ns.getSourceDescription() +
                ", Owner: " + ns.getNodeSourceAdmin() + "</span>", ns);
            nsTreeNode.setAttribute("nodeId", nsName);
            nsTreeNode.setIcon(RMImages.instance.nodesource_16().getSafeUri().asString());

            /* NodeSources */
            NodeSource oldNs = (oldNodes != null) ? oldNodes.get(nsName) : null;
            if (oldNs == null) {
                this.tree.add(nsTreeNode, this.tree.getRoot());
                this.curNodes.put(nsName, nsTreeNode);
            }

            for (Node n : ns.getDeploying().values()) {
                String nodeUrl = n.getNodeUrl();
                TNode nodeTreeNode = new TNode(nodeUrl, n);
                nodeTreeNode.setAttribute("nodeId", nodeUrl);

                /* Deploying nodes */
                Node oldNode = (oldNs != null) ? oldNs.getDeploying().get(nodeUrl) : null;
                if (oldNode == null) {
                    this.tree.add(nodeTreeNode, this.curNodes.get(nsName));
                    this.curNodes.put(nodeUrl, nodeTreeNode);
                    nodeTreeNode.setIcon(n.getNodeState().getIcon());
                } else {
                    this.curNodes.get(nodeUrl).setIcon(n.getNodeState().getIcon());
                }
            }

            for (Host h : ns.getHosts().values()) {
                String hostName = h.getHostName();
                THost hostTreeNode = new THost(hostName, h);
                hostTreeNode.setAttribute("nodeId", h.getId());
                if (h.isVirtual()) {
                    hostTreeNode.setIcon(RMImages.instance.host_virtual_16().getSafeUri().asString());
                } else {
                    hostTreeNode.setIcon(RMImages.instance.host_16().getSafeUri().asString());
                }

                /* Hosts */
                Host oldHost = (oldNs != null) ? oldNs.getHosts().get(hostName) : null;
                if (oldHost == null) {
                    this.tree.add(hostTreeNode, this.curNodes.get(nsName));
                    this.curNodes.put(h.getId(), hostTreeNode);
                }

                for (Node n : h.getNodes().values()) {
                    String nodeUrl = n.getNodeUrl();
                    TNode nodeTreeNode = new TNode(nodeUrl, n);
                    nodeTreeNode.setAttribute("nodeId", nodeUrl);

                    /* Deployed Nodes */
                    Node oldNode = (oldHost != null) ? oldHost.getNodes().get(nodeUrl) : null;

                    if (oldNode == null) {
                        this.tree.add(nodeTreeNode, this.curNodes.get(h.getId()));
                        this.curNodes.put(nodeUrl, nodeTreeNode);
                        nodeTreeNode.setAttribute("nodeState", n.getNodeState().toString());
                        nodeTreeNode.setIcon(n.getNodeState().getIcon());
                    } else {
                        TNode curTreeNode = (TNode) curNodes.get(nodeUrl);
                        curTreeNode.setAttribute("nodeState", n.getNodeState().toString());
                        curTreeNode.rmNode = n;
                        curTreeNode.setIcon(n.getNodeState().getIcon());
                    }
                }
            }
        }

        /* Remove from _this.tree_ the nodes contained in _curNodes_ but not in _nodes_
         */
        for (Entry<String, NodeSource> oldNs : this.oldNodes.entrySet()) {
            /* Keep NodeSource */
            if (nodes.containsKey(oldNs.getKey())) {

                NodeSource newNs = nodes.get(oldNs.getKey());
                for (Entry<String, Node> oldDepl : oldNs.getValue().getDeploying().entrySet()) {
                    /* Keep deploying Node */
                    if (newNs.getDeploying().containsKey(oldDepl.getKey())) {

                    }
                    /* Deploying Node to be removed */
                    else if (curNodes.containsKey(oldDepl.getKey())) {
                        this.tree.remove(curNodes.remove(oldDepl.getKey()));
                    }
                }

                for (Entry<String, Host> oldHost : oldNs.getValue().getHosts().entrySet()) {
                    /* Keep host */
                    if (newNs.getHosts().containsKey(oldHost.getKey())) {

                        Host newHost = newNs.getHosts().get(oldHost.getKey());
                        for (Entry<String, Node> oldNode : oldHost.getValue().getNodes().entrySet()) {
                            /* Keep node */
                            if (newHost.getNodes().containsKey(oldNode.getKey())) {
                            }
                            /* Node to be removed */
                            else if (curNodes.containsKey(oldNode.getKey())) {
                                this.tree.remove(curNodes.remove(oldNode.getKey()));
                            }
                        }

                    }
                    /* Host to be removed */
                    else if (curNodes.containsKey(oldHost.getValue().getId())) {
                        this.tree.remove(curNodes.remove(oldHost.getValue().getId()));
                    }
                }

            }
            /* NodeSource to be removed */
            else if (curNodes.containsKey(oldNs.getKey())) {
                this.tree.remove(curNodes.remove(oldNs.getKey()));
            }
        }

        this.oldNodes = nodes;

        this.treeGrid.markForRedraw();
    }

    void expandAll() {
        tree.openAll();
    }

    void closeAll() {
        tree.closeAll();
    }

    private void scrollList(TreeNode tn) {
        int id = treeGrid.getRecordIndex(tn);
        if (id < 0)
            return;
        this.treeGrid.scrollToRow(id);
    }

    @Override
    public void nodeSelected(Node node) {
        if (ignoreNodeSelectedEvent) {
            ignoreNodeSelectedEvent = false;
            return;
        }

        this.treeGrid.deselectAllRecords();
        TreeNode tn = this.curNodes.get(node.getNodeUrl());
        this.treeGrid.selectRecord(tn, true);
        scrollList(tn);
    }

    @Override
    public void nodeUnselected() {
        this.treeGrid.deselectAllRecords();
    }

    @Override
    public void nodeSourceSelected(NodeSource ns) {
        if (ignoreNodeSelectedEvent) {
            ignoreNodeSelectedEvent = false;
            return;
        }

        this.treeGrid.deselectAllRecords();
        TreeNode tn = this.curNodes.get(ns.getSourceName());
        this.treeGrid.selectRecord(tn, true);
        scrollList(tn);
    }

    @Override
    public void hostSelected(Host h) {
        if (ignoreNodeSelectedEvent) {
            ignoreNodeSelectedEvent = false;
            return;
        }

        this.treeGrid.deselectAllRecords();
        TreeNode tn = this.curNodes.get(h.getId());
        this.treeGrid.selectRecord(tn, true);
        scrollList(tn);
    }
}
