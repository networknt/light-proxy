package com.networknt.proxy;

/**
 * Config class for reverse proxy.
 *
 * @author Steve Hu
 */
public class ProxyConfig {
    boolean enabled;
    boolean http2Enabled;
    boolean httpsEnabled;
    String hosts;
    int connectionsPerThread;
    int maxRequestTime;

    public ProxyConfig() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isHttp2Enabled() {
        return http2Enabled;
    }

    public void setHttp2Enabled(boolean http2Enabled) {
        this.http2Enabled = http2Enabled;
    }

    public boolean isHttpsEnabled() {
        return httpsEnabled;
    }

    public void setHttpsEnabled(boolean httpsEnabled) {
        this.httpsEnabled = httpsEnabled;
    }

    public String getHosts() {
        return hosts;
    }

    public void setHosts(String hosts) {
        this.hosts = hosts;
    }

    public int getConnectionsPerThread() {
        return connectionsPerThread;
    }

    public void setConnectionsPerThread(int connectionsPerThread) {
        this.connectionsPerThread = connectionsPerThread;
    }

    public int getMaxRequestTime() {
        return maxRequestTime;
    }

    public void setMaxRequestTime(int maxRequestTime) {
        this.maxRequestTime = maxRequestTime;
    }
}
