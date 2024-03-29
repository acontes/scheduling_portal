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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.ow2.proactive_grid_cloud_portal.common.client.Controller;
import org.ow2.proactive_grid_cloud_portal.common.client.Images;
import org.ow2.proactive_grid_cloud_portal.common.client.LoginPage;
import org.ow2.proactive_grid_cloud_portal.common.client.Model.StatHistory;
import org.ow2.proactive_grid_cloud_portal.common.client.Model.StatHistory.Range;
import org.ow2.proactive_grid_cloud_portal.common.client.Settings;
import org.ow2.proactive_grid_cloud_portal.common.shared.Config;
import org.ow2.proactive_grid_cloud_portal.rm.client.NodeSource.Host;
import org.ow2.proactive_grid_cloud_portal.rm.client.NodeSource.Host.Node;
import org.ow2.proactive_grid_cloud_portal.rm.client.PluginDescriptor.Field;
import org.ow2.proactive_grid_cloud_portal.rm.shared.RMConfig;

import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;


/**
 * Logic that interacts between the remote RM and the local Model
 * <p>
 * The Controller can be accessed statically by the client to ensure
 * coherent modification of the Model data:
 * <ul><li>views submit actions to the Controller,
 * <li>the Controller performs the actions,
 * <li>the Controller updates new Data to the Model,
 * <li>the view displays what it reads from the Model.
 * </code>
 *
 *
 * @author mschnoor
 */
public class RMController extends Controller implements UncaughtExceptionHandler {

    static final String SESSION_SETTING = "pa.rm.session";
    static final String LOGIN_SETTING = "pa.rm.login";
    static final String LOCAL_SESSION_COOKIE = "pa.rm.local_session";

    @Override
    public String getLoginSettingKey() {
        return LOGIN_SETTING;
    }

    @Override
    public String getLogo32Url() {
        return RMImages.instance.logo_32().getSafeUri().asString();
    }

    @Override
    public String getLogo350Url() {
        return RMImages.instance.logo_350().getSafeUri().asString();
    }

    /** if this is different than LOCAL_SESSION cookie, we need to disconnect */
    private String localSessionNum;

    /** periodically updates the local state */
    private Timer updater = null;
    /** periodically fetches runtime stats */
    private Timer statsUpdater = null;

    /** remote gwt service */
    private RMServiceAsync rm = null;
    /** stores client data */
    private RMModelImpl model = null;

    /** shown when not logged in */
    private LoginPage loginPage = null;
    /** shown when logged in */
    private RMPage rmPage = null;

    /** result of the latest call to {@link RMServiceAsync#getStatHistory(String, String, AsyncCallback)} */
    private Request statHistReq = null;
    /** system.currenttimemillis of last StatHistory call */
    private long lastStatHistReq = 0;

    /**
     * Default constructor
     * 
     * @param rm rm server
     */
    RMController(RMServiceAsync rm) {
        this.rm = rm;
        this.model = new RMModelImpl();

        this.init();
    }

    /**
     * Call this once upon creation
     */
    private void init() {
        final String session = Settings.get().getSetting(SESSION_SETTING);

        if (session != null) {
            final Label wait = new Label("Rebinding session...");
            wait.setIcon("loading.gif");
            wait.setMargin(20);
            wait.draw();

            this.rm.getState(session, new AsyncCallback<String>() {
                public void onSuccess(String result) {
                    if (result.startsWith("you are not connected")) {
                        wait.destroy();
                        Settings.get().clearSetting(SESSION_SETTING);
                        RMController.this.loginPage = new LoginPage(RMController.this, null);
                    } else {
                        wait.destroy();
                        login(session, Settings.get().getSetting(LOGIN_SETTING));
                        model.logMessage("Rebound session " + session);
                    }
                }

                public void onFailure(Throwable caught) {
                    wait.destroy();
                    Settings.get().clearSetting(SESSION_SETTING);
                    RMController.this.loginPage = new LoginPage(RMController.this, null);
                }
            });
        } else {
            this.loginPage = new LoginPage(this, null);
        }
    }

    @Override
    public void login(final String sessionId, final String login) {
        rm.getVersion(new AsyncCallback<String>() {
            public void onSuccess(String result) {
                JSONObject obj = JSONParser.parseStrict(result).isObject();
                String rmVer = obj.get("rm").isString().stringValue();
                String restVer = obj.get("rest").isString().stringValue();
                Config.get().set(RMConfig.RM_VERSION, rmVer);
                Config.get().set(RMConfig.REST_VERSION, restVer);

                __login(sessionId, login);
            }

            public void onFailure(Throwable caught) {
                String msg = getJsonErrorMessage(caught);
                model.logImportantMessage("Failed to get REST server version: " + msg);
            }
        });
    }

    private void __login(String sessionId, String login) {
        model.setLoggedIn(true);
        model.setLogin(login);
        model.setSessionId(sessionId);

        if (this.loginPage != null) {
            this.loginPage.destroy();
            this.loginPage = null;
        }
        this.rmPage = new RMPage(this);
        this.fetchRMMonitoring();
        this.startTimer();

        Settings.get().setSetting(SESSION_SETTING, sessionId);
        if (login != null) {
            Settings.get().setSetting(LOGIN_SETTING, login);
        } else {
            Settings.get().clearSetting(LOGIN_SETTING);
        }

        String lstr = "";
        if (login != null) {
            lstr += " as " + login;
        }

        // this cookie is reset to a random int on every login:
        // if another session in another tab has a different localSessionNUm
        // than the one in the domain cookie, then we exit
        this.localSessionNum = "" + System.currentTimeMillis() + "_" + Random.nextInt();
        Cookies.setCookie(LOCAL_SESSION_COOKIE, this.localSessionNum);

        model.logMessage("Connected to " + Config.get().getRestUrl() + lstr + " (sessionId=" +
            model.getSessionId() + ")");
    }

    /** 
     * Perform server logout,
     * updates the page accordingly
     */
    void logout() {
        if (!model.isLoggedIn())
            return;

        Settings.get().clearSetting(SESSION_SETTING);
        rm.logout(model.getSessionId(), new AsyncCallback<Void>() {

            public void onFailure(Throwable caught) {
            }

            public void onSuccess(Void result) {
            }

        });

        model.setLoggedIn(false);
        teardown(null);
    }

    /**
     * Start the timer that will fetch new node states periodically
     */
    private void startTimer() {
        if (this.updater != null)
            throw new IllegalStateException("Updated is running");

        this.updater = new Timer() {
            @Override
            public void run() {

                if (!localSessionNum.equals(Cookies.getCookie(LOCAL_SESSION_COOKIE))) {
                    teardown("Duplicate session detected!<br>"
                        + "Another tab or window in this browser is accessing this page.");
                }
                fetchRMMonitoring();

            }
        };
        this.updater.scheduleRepeating(RMConfig.get().getClientRefreshTime());

        this.statsUpdater = new Timer() {
            @Override
            public void run() {
                fetchStatHistory();
            }
        };
        this.statsUpdater.scheduleRepeating(RMConfig.get().getStatisticsRefreshTime());
    }

    /**
     * Perform the server call to fetch RRD history statistics
     */
    private void fetchStatHistory() {
        String range = "";
        String[] sources = new String[] { "BusyNodesCount", "FreeNodesCount", "DownNodesCount",
                "AvailableNodesCount", "AverageActivity" };
        long updateFreq = Range.YEAR_1.getUpdateFrequency();
        boolean changedRange = false;
        for (String src : sources) {
            if (model.getStatHistory(src) != null &&
                !model.getStatHistory(src).range.equals(model.getRequestedStatHistoryRange(src))) {
                changedRange = true;
            }

            Range r = model.getRequestedStatHistoryRange(src);
            range += r.getChar();
            if (r.getUpdateFrequency() < updateFreq)
                updateFreq = r.getUpdateFrequency();
        }

        final long now = System.currentTimeMillis();
        final long dt = now - this.lastStatHistReq;

        // do not update stats every 5sec if the graphed range is large
        if (dt > updateFreq * 1000 || changedRange) {
            this.lastStatHistReq = now;

            this.statHistReq = rm.getStatHistory(model.getSessionId(), range, new AsyncCallback<String>() {
                @Override
                public void onSuccess(String result) {

                    JSONValue val = RMController.this.parseJSON(result);
                    JSONObject obj = val.isObject();

                    HashMap<String, StatHistory> stats = new HashMap<String, StatHistory>();
                    for (String source : obj.keySet()) {
                        JSONArray arr = obj.get(source).isArray();

                        ArrayList<Double> values = new ArrayList<Double>();
                        for (int i = 0; i < arr.size(); i++) {
                            JSONValue dval = arr.get(i);
                            if (dval.isNumber() != null) {
                                values.add(dval.isNumber().doubleValue());
                            } else if (i < arr.size() - 1) {
                                values.add(Double.NaN);
                            }

                        }
                        StatHistory st = new StatHistory(source, values, model
                                .getRequestedStatHistoryRange(source));
                        stats.put(source, st);
                    }
                    model.setStatHistory(stats);
                    model.logMessage("Updated Statistics History in " + (System.currentTimeMillis() - now) +
                        "ms");
                }

                @Override
                public void onFailure(Throwable caught) {
                    if (getJsonErrorCode(caught) == 401) {
                        teardown("You have been disconnected from the server.");
                    } else {
                        error("Failed to fetch Statistics History: " + getJsonErrorMessage(caught));
                    }
                }
            });
        }

        /*
         * max nodes from RRD on RM 
         * not used right now, uncomment if needed
         * 
        List<String> attrs = new ArrayList<String>();
        attrs.add("MaxFreeNodes");
        attrs.add("MaxBusyNodes");
        attrs.add("MaxDownNodes");
        // attrs.add("MaxTotalNodes"); // for some reason there is no Max Total Nodes...

        rm.getMBeanInfo(model.getSessionId(),
        		"ProActiveResourceManager:name=RuntimeData", attrs,
        		new AsyncCallback<String>() {

        			@Override
        			public void onFailure(Throwable caught) {
        				error("Failed to get MBean Info: "
        						+ getJsonErrorMessage(caught));

        			}

        			@Override
        			public void onSuccess(String result) {
        				JSONArray arr = JSONParser.parseStrict(result)
        						.isArray();
        				for (int i = 0; i < arr.size(); i++) {
        					String name = arr.get(i).isObject().get("name")
        							.isString().stringValue();
        					int value = (int) arr.get(i).isObject()
        							.get("value").isNumber().doubleValue();
        					if (name.equals("MaxFreeNodes")) {
        						model.setMaxNumFree(value);
        					} else if (name.equals("MaxBusyNodes")) {
        						model.setMaxNumBusy(value);
        					} else if (name.equals("MaxDownNodes")) {
        						model.setMaxNumDown(value);
        					}
        				}

        			}
        		});
         */
    }

    /**
     * Change the requested history range for a given set of sources,
     * store it in the model, perform statistic fetch
     *  
     * @param r range to set
     * @param source source names
     */
    public void setRuntimeRRDRange(Range r, String... source) {
        for (String src : source) {
            model.setRequestedStatHistoryRange(src, r);
        }

        if (statHistReq != null && statHistReq.isPending())
            this.statHistReq.cancel();
        fetchStatHistory();
    }

    /**
     * Perform the server call to fetch current nodes states,
     * store it on the model, notify listeners
     */
    private void fetchRMMonitoring() {
        final long t = System.currentTimeMillis();

        rm.getMonitoring(model.getSessionId(), new AsyncCallback<String>() {
            public void onSuccess(String result) {
                if (!model.isLoggedIn())
                    return;

                HashMap<String, NodeSource> nodes = parseRMMonitoring(result);
                model.setNodes(nodes);
                model.logMessage("Fetched " + nodes.size() + " node sources in " +
                    (System.currentTimeMillis() - t) + "ms");
            }

            public void onFailure(Throwable caught) {
                if (getJsonErrorCode(caught) == 401) {
                    teardown("You have been disconnected from the server.");
                } else {
                    error("Failed to fetch RM State: " + getJsonErrorMessage(caught));
                }
            }
        });
    }

    /**
     * Parse the node state JSON string
     * 
     * @param json the "rm/monitoring" json result
     * @return a POJO representation
     */
    private HashMap<String, NodeSource> parseRMMonitoring(String json) {

        JSONObject obj = this.parseJSON(json).isObject();
        HashMap<String, NodeSource> ns = new HashMap<String, NodeSource>();

        JSONArray nodesources = obj.get("nodeSource").isArray();
        for (int i = 0; i < nodesources.size(); i++) {
            JSONObject nsObj = nodesources.get(i).isObject();

            String sourceName = nsObj.get("sourceName").isString().stringValue();
            String sourceDescription = "";
            JSONString js = (nsObj.get("sourceDescription")).isString();
            if (js != null)
                sourceDescription = js.stringValue();
            String nodeSourceAdmin = nsObj.get("nodeSourceAdmin").isString().stringValue();

            ns.put(sourceName, new NodeSource(sourceName, sourceDescription, nodeSourceAdmin));
        }

        int numDeploying = 0;
        int numLost = 0;
        int numConfiguring = 0;
        int numFree = 0;
        int numLocked = 0;
        int numBusy = 0;
        int numDown = 0;
        int numToBeRemoved = 0;

        JSONArray nodes = obj.get("nodesEvents").isArray();
        for (int i = 0; i < nodes.size(); i++) {
            try {
                JSONObject nodeObj = nodes.get(i).isObject();

                String hostName = nodeObj.get("hostName").isString().stringValue();
                String nss = nodeObj.get("nodeSource").isString().stringValue();

                String nodeUrl = nodeObj.get("nodeUrl").isString().stringValue();
                String nodeState = nodeObj.get("nodeState").isString().stringValue();
                String nodeInfo = nodeObj.get("nodeInfo").isString().stringValue();
                String timeStampFormatted = nodeObj.get("timeStampFormatted").isString().stringValue();
                long timeStamp = Math.round(nodeObj.get("timeStamp").isNumber().doubleValue());
                String nodeProvider = nodeObj.get("nodeProvider").isString().stringValue();
                String nodeOwner = "";
                JSONString nodeOwnerStr = nodeObj.get("nodeOwner").isString();
                if (nodeOwnerStr != null)
                    nodeOwner = nodeOwnerStr.stringValue();
                String vmName = "";
                JSONString vmStr = nodeObj.get("vmname").isString();
                if (vmStr != null)
                    vmName = vmStr.stringValue();
                String description = "";
                JSONString descStr = nodeObj.get("nodeInfo").isString();
                if (descStr != null)
                    description = descStr.stringValue();

                String defaultJMXUrl = "";
                JSONString jmxStr = nodeObj.get("defaultJMXUrl").isString();
                if (jmxStr != null) {
                    defaultJMXUrl = jmxStr.stringValue();
                }
                String proactiveJMXUrl = "";
                JSONString paJmx = nodeObj.get("proactiveJMXUrl").isString();
                if (paJmx != null) {
                    proactiveJMXUrl = paJmx.stringValue();
                }

                Node n = new Node(nodeUrl, nodeState, nodeInfo, timeStamp, timeStampFormatted, nodeProvider,
                    nodeOwner, nss, hostName, vmName, description, defaultJMXUrl, proactiveJMXUrl);

                // deploying node
                if (hostName == null || hostName.length() == 0) {
                    ns.get(nss).getDeploying().put(nodeUrl, n);
                }
                // already deployed node
                else {

                    Host host = ns.get(nss).getHosts().get(hostName);
                    if (host == null) {
                        host = new Host(hostName, nss);
                        ns.get(nss).getHosts().put(hostName, host);
                    }
                    host.getNodes().put(nodeUrl, n);
                    if (nodeUrl.toLowerCase().contains("virt-")) {
                        host.setVirtual(true);
                    }
                }

                switch (n.getNodeState()) {
                    case BUSY:
                        numBusy++;
                        break;
                    case CONFIGURING:
                        numConfiguring++;
                        break;
                    case DEPLOYING:
                        numDeploying++;
                        break;
                    case DOWN:
                        numDown++;
                        break;
                    case FREE:
                        numFree++;
                        break;
                    case LOCKED:
                        numLocked++;
                        break;
                    case LOST:
                        numLost++;
                        break;
                    case TO_BE_REMOVED:
                        numToBeRemoved++;
                        break;
                }
            } catch (Throwable t) {
                System.out.println("Failed to parse node : ");
                System.out.println(nodes.get(i).toString());
                t.printStackTrace();

                model.logCriticalMessage(t.getClass().getName() + ": " + t.getMessage() + " for input: " +
                    nodes.get(i).toString());
            }
        }

        model.setNumBusy(numBusy);
        model.setNumConfiguring(numConfiguring);
        model.setNumDeploying(numDeploying);
        model.setNumDown(numDown);
        model.setNumFree(numFree);
        model.setNumLocked(numLocked);
        model.setNumLost(numLost);
        model.setNumToBeRemoved(numToBeRemoved);

        int numPhysical = 0;
        int numVirtual = 0;
        for (NodeSource nos : ns.values()) {
            for (Host h : nos.getHosts().values()) {
                if (h.isVirtual()) {
                    numVirtual++;
                } else {
                    numPhysical++;
                }
            }
        }

        model.setNumPhysicalHosts(numPhysical);
        model.setNumVirtualHosts(numVirtual);

        return ns;
    }

    /**
     * Fetch and store NS Infrastructure and Policy creation parameters
     * store it in the model
     * @param success call this when it's done
     * @param failure call this if it fails
     */
    public void fetchSupportedInfrastructuresAndPolicies(final Runnable success, final Runnable failure) {
        rm.getInfrastructures(model.getSessionId(), new AsyncCallback<String>() {

            public void onFailure(Throwable caught) {
                String msg = getJsonErrorMessage(caught);
                SC.warn("Failed to fetch supported infrastructures:<br>" + msg);
                failure.run();
            }

            public void onSuccess(String result) {
                model.setSupportedInfrastructures(parsePluginDescriptors(result));

                rm.getPolicies(model.getSessionId(), new AsyncCallback<String>() {

                    public void onFailure(Throwable caught) {
                        String msg = getJsonErrorMessage(caught);
                        SC.warn("Failed to fetch supported policies:<br>" + msg);
                        failure.run();
                    }

                    public void onSuccess(String result) {
                        model.setSupportedPolicies(parsePluginDescriptors(result));
                        success.run();
                    }
                });
            }
        });
    }

    private HashMap<String, PluginDescriptor> parsePluginDescriptors(String json) {
        JSONArray arr = this.parseJSON(json).isArray();
        HashMap<String, PluginDescriptor> plugins = new HashMap<String, PluginDescriptor>();

        for (int i = 0; i < arr.size(); i++) {
            JSONObject p = arr.get(i).isObject();

            String pluginName = p.get("pluginName").isString().stringValue();
            String pluginDescription = p.get("pluginDescription").isString().stringValue();
            PluginDescriptor desc = new PluginDescriptor(pluginName, pluginDescription);

            JSONArray fields = p.get("configurableFields").isArray();
            for (int j = 0; j < fields.size(); j++) {
                JSONObject field = fields.get(j).isObject();

                String name = field.get("name").isString().stringValue();
                String value = field.get("value").isString().stringValue();

                JSONObject meta = field.get("meta").isObject();
                String metaType = meta.get("type").isString().stringValue();
                String descr = meta.get("description").isString().stringValue();

                boolean pass = false, cred = false, file = false;
                if (metaType.equalsIgnoreCase("password"))
                    pass = true;
                else if (metaType.equalsIgnoreCase("fileBrowser"))
                    file = true;
                else if (metaType.equalsIgnoreCase("credential"))
                    cred = true;

                Field f = new PluginDescriptor.Field(name, value, descr, pass, cred, file);

                desc.getConfigurableFields().add(f);
            }

            plugins.put(pluginName, desc);
        }

        return plugins;
    }

    /**
     * Unlock selected node/host/nodesource
     */
    public void unlockNodes() {
        unlockNodes(getSelectedNodesUrls());
    }

    /**
     * lock selected node/host/nodesource
     */
    public void lockNodes() {
        lockNodes(getSelectedNodesUrls());
    }

    private Set<String> getSelectedNodesUrls() {
        Set<String> urls = new HashSet<String>();
        if (model.getSelectedNode() != null) {
            urls.add(model.getSelectedNode().getNodeUrl());
        } else if (model.getSelectedHost() != null) {
            for (Node n : model.getSelectedHost().getNodes().values()) {
                urls.add(n.getNodeUrl());
            }
        } else if (model.getSelectedNodeSource() != null) {
            for (Host h : model.getSelectedNodeSource().getHosts().values()) {
                for (Node n : h.getNodes().values()) {
                    urls.add(n.getNodeUrl());
                }
            }
        }
        return urls;
    }

    private void lockNodes(final Set<String> nodeUrls) {
        // there's no real incentive to storing locked node states
        // here, let's just try to do what the user says, and report
        // the error if it's nonsense
        rm.lockNodes(model.getSessionId(), nodeUrls, new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable caught) {
                model.logImportantMessage("Failed to lock " + nodeUrls.size() + " nodes: " +
                    getJsonErrorMessage(caught));

            }

            @Override
            public void onSuccess(String result) {
                model.logMessage("Successfully locked " + nodeUrls.size() + " nodes");
            }
        });
    }

    private void unlockNodes(final Set<String> nodeUrls) {
        // there's no real incentive to storing locked node states
        // here, let's just try to do what the user says, and report
        // the error if it's nonsense
        rm.unlockNodes(model.getSessionId(), nodeUrls, new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable caught) {
                model.logImportantMessage("Failed to unlock " + nodeUrls.size() + " nodes: " +
                    getJsonErrorMessage(caught));

            }

            @Override
            public void onSuccess(String result) {
                model.logMessage("Successfully unlocked " + nodeUrls.size() + " nodes");
            }
        });
    }

    /**
     * Remove nodes according to the current selection:
     * if a host is selected, multiple nodes will be removed
     * if a nodesource is selected, multiple hosts will be removed
     */
    public void removeNodes() {
        String _msg = null;
        int _numNodes = 1;
        if (model.getSelectedNode() != null) {
            _msg = "Node " + model.getSelectedNode().getNodeUrl();
        } else if (model.getSelectedHost() != null) {
            _msg = "1 Node from Host " + model.getSelectedHost().getHostName() + " (ns: " +
                model.getSelectedHost().getSourceName() + ")";
            _numNodes = model.getSelectedHost().getNodes().size();
        } else if (model.getSelectedNodeSource() != null) {
            _msg = "NodeSource " + model.getSelectedNodeSource().getSourceName();
        } else {
            return;
        }

        final String msg = new String(_msg);
        final int numNodes = _numNodes;

        final AsyncCallback<String> callback = new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable caught) {
                String err = getJsonErrorMessage(caught);
                model.logImportantMessage("Failed to remove " + msg + ": " + err);
            }

            @Override
            public void onSuccess(String result) {
                if (Boolean.parseBoolean(result)) {
                    model.logMessage("Successfully removed " + msg);
                } else {
                    model.logMessage(msg + " was not removed");
                }
            }
        };

        if (model.getSelectedNode() != null) {
            confirmRemoveNode("Confirm removal of <strong>" + msg + "</strong>", new NodeRemovalCallback() {
                public void run(boolean force) {
                    rm
                            .removeNode(model.getSessionId(), model.getSelectedNode().getNodeUrl(), force,
                                    callback);
                }
            });
        } else if (model.getSelectedHost() != null) {
            final Host h = model.getSelectedHost();
            confirmRemoveNode("Confirm removal of <strong>" + numNodes + " node" +
                ((numNodes > 1) ? "s" : "") + "</strong> on <strong>host " + h.getHostName() + "</strong>",
                    new NodeRemovalCallback() {
                        public void run(boolean force) {
                            for (Node n : h.getNodes().values()) {
                                rm.removeNode(model.getSessionId(), n.getNodeUrl(), force, callback);
                            }
                        }
                    });
        } else if (model.getSelectedNodeSource() != null) {
            confirmRemoveNode("Confirm removal of <strong>" + msg + "</strong>", new NodeRemovalCallback() {
                public void run(boolean force) {
                    rm.removeNodesource(model.getSessionId(), model.getSelectedNodeSource().getSourceName(),
                            force, callback);
                }
            });
        }
    }

    private abstract class NodeRemovalCallback {
        public abstract void run(boolean force);
    }

    private void confirmRemoveNode(String message, final NodeRemovalCallback callback) {
        final Window win = new Window();
        win.setTitle("Confirm node removal");
        win.setShowMinimizeButton(false);
        win.setIsModal(true);
        win.setShowModalMask(true);
        win.setWidth(380);
        win.setHeight(150);
        win.setCanDragResize(false);
        win.setCanDragReposition(false);
        win.centerInPage();

        Label label = new Label(message);
        label.setHeight(40);

        final CheckboxItem force = new CheckboxItem("force", "Wait task completion on busy nodes");
        final DynamicForm form = new DynamicForm();
        form.setColWidths(25, "*");
        form.setItems(force);

        Canvas fill = new Canvas();
        fill.setHeight100();

        HLayout buttons = new HLayout();
        buttons.setMembersMargin(5);
        buttons.setAlign(Alignment.RIGHT);
        buttons.setHeight(25);

        IButton ok = new IButton("OK", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                callback.run(force.getValueAsBoolean());
                win.hide();
                win.destroy();
            }
        });
        ok.setIcon(Images.instance.ok_16().getSafeUri().asString());
        IButton cancel = new IButton("Cancel", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                win.hide();
                win.destroy();
            }
        });
        cancel.setIcon(Images.instance.cancel_16().getSafeUri().asString());
        buttons.setMembers(ok, cancel);

        VLayout layout = new VLayout();
        layout.setMembersMargin(5);
        layout.setMargin(5);
        layout.setMembers(label, form, fill, buttons);

        win.addItem(layout);
        win.show();
    }

    public RMServiceAsync getRMService() {
        return this.rm;
    }

    /**
     * Override user settings, rewrite cookies, refresh corresponding ui elements
     * 
     * @param refreshTime refresh time for update thread in ms
     * @param forceRefresh refresh ui even if properties did not change
     */
    public void setUserSettings(String refreshTime, boolean forceRefresh) {

        boolean refreshChanged = !refreshTime.equals("" + RMConfig.get().getClass());
        RMConfig.get().set(RMConfig.CLIENT_REFRESH_TIME, refreshTime);
        Settings.get().setSetting(RMConfig.CLIENT_REFRESH_TIME, refreshTime);

        if (refreshChanged) {
            this.stopTimer();
            this.startTimer();
        }
    }

    /**
     * Change the currently selected node
     * notify listeners
     * 
     * @param selection currently selected node
     */
    public void selectNode(Node selection) {
        this.model.setSelectedNode(selection.getNodeUrl());
    }

    /**
     * Change the currently selected node
     * notify listeners
     * 
     * @param sel currently selected host
     */
    public void selectHost(Host sel) {
        this.model.setSelectedHost(sel.getId());
    }

    /**
     * Change the currently selected ns
     * notify listeners
     * 
     * @param sel currently selected ns
     */
    public void selectNodeSource(NodeSource sel) {
        this.model.setSelectedNodeSource(sel.getSourceName());
    }

    /**
     * Issue an error message to the user and exit the schedulerView
     *
     * @param reason error message to display
     */
    private void error(String reason) {
        model.logCriticalMessage(reason);
    }

    /**
     * stop the timer that updates node states periodically
     */
    private void stopTimer() {
        if (this.updater == null)
            return;

        this.updater.cancel();
        this.updater = null;

        this.statsUpdater.cancel();
        this.statsUpdater = null;
    }

    /**
     * Shut down everything, get back to the login page
     * @param message an error message, or null
     */
    private void teardown(String message) {
        this.stopTimer();

        if (this.rmPage == null)
            return;

        this.rmPage.destroy();
        this.rmPage = null;

        this.model = new RMModelImpl();
        this.loginPage = new LoginPage(this, message);
    }

    /**
     * @return a read only view of the clients local data, which stores everything 
     * 		that was received from the server by the controller
     */
    @Override
    public RMModel getModel() {
        return this.model;
    }

    /**
     * @return the Event Dispatcher to use to register new event listeners
     */
    @Override
    public RMEventDispatcher getEventDispatcher() {
        return this.model;
    }

    public void onUncaughtException(Throwable e) {
        e.printStackTrace();
        error(e.getMessage());
    }
}
