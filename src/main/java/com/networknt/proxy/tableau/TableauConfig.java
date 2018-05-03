package com.networknt.proxy.tableau;

/**
 * Config class for TableauCacheAuthHandler
 *
 * @author Steve Hu
 */
public class TableauConfig {
    boolean enabled;
    String serverUrl;
    String serverPath;
    String tableauUsername;

    public TableauConfig() {

    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getServerPath() {
        return serverPath;
    }

    public void setServerPath(String serverPath) {
        this.serverPath = serverPath;
    }

    public String getTableauUsername() {
        return tableauUsername;
    }

    public void setTableauUsername(String tableauUsername) {
        this.tableauUsername = tableauUsername;
    }
}
