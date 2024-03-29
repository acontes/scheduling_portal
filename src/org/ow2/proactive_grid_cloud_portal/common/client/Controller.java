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
package org.ow2.proactive_grid_cloud_portal.common.client;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;


public abstract class Controller {

    /**
     * @return locally stored data
     */
    public abstract Model getModel();

    /**
     * @return event dispatcher, used to register new listeners
     */
    public abstract EventDispatcher getEventDispatcher();

    /**
     * login has been succesfully performed,
     * this method sets the pages & model accordingly.
     * This does NOT perform a server login call
     * 
     * @param sessionId
     * @param login can be null
     */
    public abstract void login(String sessionId, String login);

    /**
     * @return key of the 'login' setting in the local store, used to pre-fill
     *  the login page
     */
    public abstract String getLoginSettingKey();

    /**
     * @return URL of the small application logo
     */
    public abstract String getLogo32Url();

    /**
     * @return URL of the large application logo
     */
    public abstract String getLogo350Url();

    /**
     * @param throwable a serialized JSON Exception
     * @return the value of the 'errorMessage' key
     */
    public static String getJsonErrorMessage(Throwable throwable) {
        String msg = throwable.getMessage();
        return getJsonErrorMessage(msg);
    }

    /**
     * @param throwable a serialized JSON Exception
     * @return the value of the 'httpErrorCode' key, or -1
     */
    public static int getJsonErrorCode(Throwable throwable) {
        String msg = throwable.getMessage();
        return getJsonErrorCode(msg);
    }

    /**
     * @param str String representation of a serialized JSON Exception
     * @return the value of the 'errorMessage' key
     */
    public static String getJsonErrorMessage(String str) {
        try {
            JSONObject exc = JSONParser.parseStrict(str).isObject();
            if (exc != null && exc.containsKey("errorMessage")) {
                JSONValue val = exc.get("errorMessage");
                if (val == null || val.isString() == null) {
                    return "<no reason>";
                } else {
                    return val.isString().stringValue();
                }
            }
        } catch (Exception e) {
            if (str != null) {
                return str;
            } else {
                return "<no reason>";
            }
        }
        return null;
    }

    /**
     * @param str String representation of a serialized JSON Exception
     * @return the value of the 'httpErrorCode' key, or -1
     */
    public static int getJsonErrorCode(String str) {
        try {
            JSONObject exc = JSONParser.parseStrict(str).isObject();
            if (exc != null && exc.containsKey("httpErrorCode")) {
                JSONValue val = exc.get("httpErrorCode");
                if (val == null || val.isNumber() == null) {
                    return -1;
                } else {
                    return (int) val.isNumber().doubleValue();
                }
            }
        } catch (Exception e) {
            return -1;
        }
        return -1;
    }

    /**
     * Parse a JSON string
     * <p>
     * If the input is not valid JSON or parsing fails for some reason,
     * the exception will be logged in the UI but not thrown.
     * 
     * @param jsonStr a valid JSON string
     * @return a java representation of the JSON object hierarchy,
     *     or a JSONObject representing {} if parsing fails
     */
    public JSONValue parseJSON(String jsonStr) {
        try {
            JSONValue val = JSONParser.parseStrict(jsonStr);
            return val;
        } catch (Throwable t) {
            // only shows up in eclipse dev mode
            t.printStackTrace();

            this.getModel().logCriticalMessage(
                    "JSON Parser failed " + t.getClass().getName() + ": " + t.getLocalizedMessage());
            this.getModel().logCriticalMessage("input was: " + jsonStr);
            return new JSONObject();
        }
    }

}
