package org.ofbiz.core.entity.transaction;

/**
 * Created by IntelliJ IDEA.
 * User: hbarney
 * Date: Feb 10, 2006
 * Time: 10:32:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class JNDIConnectionDetails {
    protected String connectionName = null;
    protected String serverName = null;

    public JNDIConnectionDetails() {

    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerName() {
        return serverName;
    }

    public boolean detailsSet() {
        return ((connectionName != null) && (serverName != null));
    }
}
