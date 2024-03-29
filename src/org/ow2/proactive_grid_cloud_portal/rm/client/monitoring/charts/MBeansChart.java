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
package org.ow2.proactive_grid_cloud_portal.rm.client.monitoring.charts;

import java.util.Arrays;

import org.ow2.proactive_grid_cloud_portal.rm.client.RMController;
import org.ow2.proactive_grid_cloud_portal.rm.client.RMModel;
import org.ow2.proactive_grid_cloud_portal.rm.client.RMServiceAsync;

import com.google.gwt.user.client.rpc.AsyncCallback;


/**
 * Chart that retrieves information from several MBeans.
 */
public abstract class MBeansChart extends MBeanChart {

    public MBeansChart(RMController controller, String jmxServerUrl, String mbean, String[] attrs,
            String title) {
        super(controller, jmxServerUrl, mbean, attrs, title);
    }

    @Override
    public void reload() {
        final RMServiceAsync rm = controller.getRMService();
        final RMModel model = controller.getModel();
        final long t = System.currentTimeMillis();

        rm.getNodeMBeansInfo(model.getSessionId(), jmxServerUrl, mbeanName, Arrays.asList(attrs),
                new AsyncCallback<String>() {
                    public void onSuccess(String result) {
                        if (onFinish != null) {
                            onFinish.run();
                        }
                        if (!model.isLoggedIn())
                            return;

                        model.logMessage("Fetched " + mbeanName + ":" + Arrays.toString(attrs) + " in " +
                            (System.currentTimeMillis() - t) + "ms");
                        processResult(result);
                    }

                    public void onFailure(Throwable caught) {
                        if (onFinish != null) {
                            onFinish.run();
                        }
                        if (RMController.getJsonErrorCode(caught) == 401) {
                            model.logMessage("You have been disconnected from the server.");
                        } else {
                            //error("Failed to fetch RM State: " + RMController.getJsonErrorMessage(caught));
                        }
                    }
                });
    }
}
