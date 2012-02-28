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

import org.ow2.proactive_grid_cloud_portal.rm.client.RMController;

import com.google.gwt.visualization.client.visualizations.corechart.AxisOptions;

/**
 * Shows the total CPU usage in percentage.
 */
public class CpuUsageAreaChart extends MBeanTimeAreaChart {
	
	public CpuUsageAreaChart(RMController controller, String jmxServerUrl) {
		super(controller, jmxServerUrl, "sigar:Type=LoadAverage", "LastMinute", "Cpu Usage");
		AxisOptions vAxis = AxisOptions.create();
		vAxis.setTitle("Percentage");
		vAxis.setMaxValue(100);
		loadOpts.setVAxisOptions(vAxis);		
	}
	
	protected Long formatNumber(String attr, double value) {
		if (attr.equals("LastMinute")) {
			return (long)(value * 100);
		}
		return (long)value;
	}	
}