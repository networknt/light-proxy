package com.networknt.proxy;

import com.networknt.config.Config;
import com.networknt.server.HandlerProvider;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;


public class ProxyHandlerProvider implements HandlerProvider {
    static final String CONFIG_NAME = "proxy";
    static ProxyConfig config =
         (ProxyConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ProxyConfig.class);

    public HttpHandler getHandler() {
        List<String> hosts = Arrays.asList(config.getHosts().split(","));
        LoadBalancingProxyClient loadBalancer = new LoadBalancingProxyClient()
                .setConnectionsPerThread(config.getConnectionsPerThread());
        hosts.forEach(handlingConsumerWrapper(host -> loadBalancer.addHost(new URI(host)), URISyntaxException.class));
        return new ProxyHandler(loadBalancer, config.getMaxRequestTime(), ResponseCodeHandler.HANDLE_404);
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
                    System.err.println(
                            "Exception occured : " + exCast.getMessage());
                } catch (ClassCastException ccEx) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }
}
