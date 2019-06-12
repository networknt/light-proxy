/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.proxy;

import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.handler.HandlerProvider;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Proxy Handler Provider
 * @author Steve Hu
 */
public class ProxyHandlerProvider implements HandlerProvider {
    static final String CONFIG_NAME = "proxy";
    static final Logger logger = LoggerFactory.getLogger(ProxyHandlerProvider.class);
    static ProxyConfig config = (ProxyConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ProxyConfig.class);

    @Override
    public HttpHandler getHandler() {
        List<String> hosts = Arrays.asList(config.getHosts().split(","));
        if(config.httpsEnabled) {
            LoadBalancingProxyClient loadBalancer = new LoadBalancingProxyClient()
                    .setConnectionsPerThread(config.getConnectionsPerThread());
            hosts.forEach(handlingConsumerWrapper(host -> loadBalancer.addHost(new URI(host), Http2Client.SSL), URISyntaxException.class));
            return ProxyHandler.builder()
                    .setProxyClient(loadBalancer)
                    .setMaxConnectionRetries(config.maxConnectionRetries)
                    .setMaxRequestTime(config.maxRequestTime)
                    .setReuseXForwarded(config.reuseXForwarded)
                    .setRewriteHostHeader(config.rewriteHostHeader)
                    .setNext(ResponseCodeHandler.HANDLE_404)
                    .build();
        } else {
            LoadBalancingProxyClient loadBalancer = new LoadBalancingProxyClient()
                    .setConnectionsPerThread(config.getConnectionsPerThread());
            hosts.forEach(handlingConsumerWrapper(host -> loadBalancer.addHost(new URI(host)), URISyntaxException.class));
            return ProxyHandler.builder()
                    .setProxyClient(loadBalancer)
                    .setMaxConnectionRetries(config.maxConnectionRetries)
                    .setMaxRequestTime(config.maxRequestTime)
                    .setReuseXForwarded(config.reuseXForwarded)
                    .setRewriteHostHeader(config.rewriteHostHeader)
                    .setNext(ResponseCodeHandler.HANDLE_404)
                    .build();
        }
    }

    @FunctionalInterface
    public interface ThrowingConsumer<T, E extends Exception> {
        void accept(T t) throws E;
    }
    static <T, E extends Exception> Consumer<T> handlingConsumerWrapper(
            ThrowingConsumer<T, E> throwingConsumer, Class<E> exceptionClass) {

        return i -> {
            try {
                throwingConsumer.accept(i);
            } catch (Exception ex) {
                try {
                    E exCast = exceptionClass.cast(ex);
                    logger.error("Exception occured :", ex);
                } catch (ClassCastException ccEx) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }
}
