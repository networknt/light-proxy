package com.networknt.proxy.tableau;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.client.Http2Client;
import com.networknt.common.DecryptUtil;
import com.networknt.config.Config;
import com.networknt.exception.ApiException;
import com.networknt.exception.ClientException;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.status.Status;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * One of our customers has Tableau server running in-house with rest service enabled. The only way
 * to connect to the server is through the login process to get a token which expires within 240 minutes.
 * For light-proxy to work with the server, we need to handle the login and monitor the token expiration
 * in this tableau. https://onlinehelp.tableau.com/current/api/rest_api/en-us/REST/rest_api_concepts_auth.htm
 *
 * @author Steve Hu
 *
 */
public class TableauAuthHandler implements MiddlewareHandler {
    private static final Logger logger = LoggerFactory.getLogger(TableauAuthHandler.class);
    private static final String TABLEAU_CONFIG_NAME = "tableau";
    private static final String SECRET_CONFIG_NAME = "secret";
    private static final String FAIL_TO_GET_TABLEAU_TOKEN = "ERR11300";
    private static final HttpString TABLEAU_TOKEN = new HttpString("X-Tableau-Auth");
    private static final TableauConfig config =
            (TableauConfig) Config.getInstance().getJsonObjectConfig(TABLEAU_CONFIG_NAME, TableauConfig.class);
    private static final Map<String, Object> secretConfig;

    private volatile HttpHandler next;

    // cached tableau token for this proxy
    private String token;
    // token expire time
    private long expire;
    // indicate if a renew token thread is running
    private volatile boolean renewing = false;
    // timeout period for retry after the cached token is expired
    private volatile long expiredRetryTimeout;
    // timeout period for retry before the cached token is expired
    private volatile long earlyRetryTimeout;
    // lock object for sync between threads
    private final Object lock = new Object();

    static {
        Map<String, Object> secretMap = Config.getInstance().getJsonMapConfig(SECRET_CONFIG_NAME);
        if(secretMap != null) {
            secretConfig = DecryptUtil.decryptMap(secretMap);
        } else {
            throw new ExceptionInInitializerError("Could not locate secret.yml");
        }
    }

    public TableauAuthHandler() {

    }

    /**
     * Get the credentials from tableau config and send a request to Tableau server to get the token.
     * The token will be cached and expiration time is monitored with renew on request before expiration.
     * If the cached token is already expired upon incoming request, then the current thread is blocked
     * until login is completed with a new token retrieved.
     *
     * @param httpServerExchange http exchange
     * @throws Exception exception
     */
    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
        checkTokenExpired();
        httpServerExchange.getRequestHeaders().put(TABLEAU_TOKEN, token);
        httpServerExchange.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
        next.handleRequest(httpServerExchange);
    }

    private void checkTokenExpired() throws ClientException, ApiException {

        long tokenRenewBeforeExpired = config.getTokenRenewBeforeExpired();
        long expiredRefreshRetryDelay = config.getExpiredRefreshRetryDelay();
        long earlyRefreshRetryDelay = config.getEarlyRefreshRetryDelay();
        boolean isInRenewWindow = expire - System.currentTimeMillis() < tokenRenewBeforeExpired;
        if(logger.isTraceEnabled()) logger.trace("isInRenewWindow = " + isInRenewWindow);

        if(isInRenewWindow) {
            if(expire <= System.currentTimeMillis()) {
                if(logger.isTraceEnabled()) logger.trace("In renew window and token is expired.");
                // block other request here to prevent using expired token.
                synchronized (TableauAuthHandler.class) {
                    if(expire <= System.currentTimeMillis()) {
                        if(logger.isTraceEnabled()) logger.trace("Within the synch block, check if the current request need to renew token");
                        if(!renewing || System.currentTimeMillis() > expiredRetryTimeout) {
                            // if there is no other request is renewing or the renewing flag is true but renewTimeout is passed
                            renewing = true;
                            expiredRetryTimeout = System.currentTimeMillis() + expiredRefreshRetryDelay;
                            if(logger.isTraceEnabled()) logger.trace("Current request is renewing token synchronously as token is expired already");
                            getToken();
                            renewing = false;
                        } else {
                            if(logger.isTraceEnabled()) logger.trace("Circuit breaker is tripped and not timeout yet!");
                            // reject all waiting requests by thrown an exception.
                            throw new ApiException(new Status(FAIL_TO_GET_TABLEAU_TOKEN));
                        }
                    }
                }
            } else {
                // Not expired yet, try to renew async but let requests use the old token.
                if(logger.isTraceEnabled()) logger.trace("In renew window but token is not expired yet.");
                synchronized (TableauAuthHandler.class) {
                    if(expire > System.currentTimeMillis()) {
                        if(!renewing || System.currentTimeMillis() > earlyRetryTimeout) {
                            renewing = true;
                            earlyRetryTimeout = System.currentTimeMillis() + earlyRefreshRetryDelay;
                            if(logger.isTraceEnabled()) logger.trace("Retrieve token async is called while token is not expired yet");
                            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                            executor.schedule(() -> {
                                try {
                                    getToken();
                                    renewing = false;
                                    logger.trace("Async get token is completed.");
                                } catch (Exception e) {
                                    logger.error("Async retrieve token error", e);
                                    // swallow the exception here as it is on a best effort basis.
                                }
                            }, 50, TimeUnit.MILLISECONDS);
                            executor.shutdown();
                        }
                    }
                }
            }
        }
        if(logger.isTraceEnabled()) logger.trace("Check token is done!");
    }

    private void getToken() throws ClientException {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            // use HTTP 1.1 connection as I don't think Tableau supports HTTP 2.0
            connection = client.connect(new URI(config.getServerUrl()), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            final String requestBody = getRequestBody();
            ClientRequest request = new ClientRequest().setPath(config.getServerPath()).setMethod(Methods.POST);
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
            connection.sendRequest(request, client.createClientCallback(reference, latch, requestBody));
            latch.await();
            int statusCode = reference.get().getResponseCode();
            if(logger.isDebugEnabled()) logger.debug("statusCode = " + statusCode);
            if(statusCode == StatusCodes.OK) {
                String responseBody = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
                if(logger.isDebugEnabled()) logger.debug("responseBody = " + responseBody);
                Map<String, Object> responseMap = Config.getInstance().getMapper().readValue(responseBody, new TypeReference<Map<String, Object>>() {});
                Map<String, Object> credentials = (Map<String, Object>)responseMap.get("credentials");
                token = (String)credentials.get("token");
                expire = System.currentTimeMillis();
            }
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
    }

    private String getRequestBody() throws IOException {
        Map<String, Object> site = new HashMap<>();
        site.put("contentUrl", config.getTableauContentUrl());
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("name", config.getTableauUsername());
        credentials.put("password", secretConfig.get("tableauPassword"));
        credentials.put("site", site);
        Map<String, Object> request = new HashMap<>();
        request.put("credentials", credentials);
        return Config.getInstance().getMapper().writeValueAsString(request);
    }

    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(HttpHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(TableauAuthHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(TABLEAU_CONFIG_NAME), null);
    }
}
