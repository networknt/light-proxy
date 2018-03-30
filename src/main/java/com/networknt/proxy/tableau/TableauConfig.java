package com.networknt.proxy.tableau;

/**
 * Config class for TableauAuthHandler
 *
 * @author Steve Hu
 */
public class TableauConfig {
    boolean enabled;
    String serverUrl;
    String serverPath;
    long tokenRenewBeforeExpired;
    long expiredRefreshRetryDelay;
    long earlyRefreshRetryDelay;
    String tableauUsername;
    String tableauContentUrl;

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

    public long getTokenRenewBeforeExpired() {
        return tokenRenewBeforeExpired;
    }

    public void setTokenRenewBeforeExpired(long tokenRenewBeforeExpired) {
        this.tokenRenewBeforeExpired = tokenRenewBeforeExpired;
    }

    public long getExpiredRefreshRetryDelay() {
        return expiredRefreshRetryDelay;
    }

    public void setExpiredRefreshRetryDelay(long expiredRefreshRetryDelay) {
        this.expiredRefreshRetryDelay = expiredRefreshRetryDelay;
    }

    public long getEarlyRefreshRetryDelay() {
        return earlyRefreshRetryDelay;
    }

    public void setEarlyRefreshRetryDelay(long earlyRefreshRetryDelay) {
        this.earlyRefreshRetryDelay = earlyRefreshRetryDelay;
    }

    public String getTableauUsername() {
        return tableauUsername;
    }

    public void setTableauUsername(String tableauUsername) {
        this.tableauUsername = tableauUsername;
    }

    public String getTableauContentUrl() {
        return tableauContentUrl;
    }

    public void setTableauContentUrl(String tableauContentUrl) {
        this.tableauContentUrl = tableauContentUrl;
    }
}
