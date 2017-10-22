package com.networknt.proxy;

import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Since release 1.5.0, light-proxy will support both http and https to the
 * downstream servers. This test case will cover the scenario that https is
 * used. As we only have one default proxy.yml in test folder, we will manually
 * construct a config file to test https connections.
 *
 * @author Steve Hu
 */
public class ProxyHttpsTest {
    static final Logger logger = LoggerFactory.getLogger(ProxyHttpTest.class);

    static Undertow server1 = null;
    static Undertow server2 = null;
    static Undertow server3 = null;
    @ClassRule
    public static TestServer server = TestServer.getInstance();

    static final boolean enableHttp2 = server.getServerConfig().isEnableHttp2();
    static final boolean enableHttps = server.getServerConfig().isEnableHttps();
    static final int httpPort = server.getServerConfig().getHttpPort();
    static final int httpsPort = server.getServerConfig().getHttpsPort();
    static final String url = enableHttp2 || enableHttps ? "https://localhost:" + httpsPort : "http://localhost:" + httpPort;
    static final String homeDir = System.getProperty("user.home");

    @BeforeClass
    public static void setUp() {
        Map<String, Object> map = new HashMap<>();
        map.put("http2Enabled", true);
        map.put("httpsEnabled", true);
        map.put("hosts", "https://localhost:8081,https://localhost:8082,https://localhost:8083");
        map.put("connectionsPerThread", 20);
        map.put("maxRequestTime", 10000);
        try {
            DumperOptions options = new DumperOptions();
            options.setAllowReadOnlyProperties(true);
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setIndent(4);

            Yaml yaml = new Yaml(options);
            FileWriter writer = new FileWriter(homeDir + "/proxy.yml");
            yaml.dump(map, writer);
            // Add home directory to the classpath of the system class loader.
            addURL(new File(homeDir).toURI().toURL());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(server1 == null) {
            logger.info("starting server1");
            server1 = Undertow.builder()
                    .addHttpListener(8081, "localhost")
                    .setHandler(new HttpHandler() {
                        @Override
                        public void handleRequest(HttpServerExchange exchange) throws Exception {
                            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                            exchange.getResponseSender().send("Server1");
                        }
                    })
                    .build();

            server1.start();
        }

        if(server2 == null) {
            logger.info("starting server2");
            server2 = Undertow.builder()
                    .addHttpListener(8082, "localhost")
                    .setHandler(new HttpHandler() {
                        @Override
                        public void handleRequest(HttpServerExchange exchange) throws Exception {
                            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                            exchange.getResponseSender().send("Server2");
                        }
                    })
                    .build();

            server2.start();
        }

        if(server3 == null) {
            logger.info("starting server3");
            server3 = Undertow.builder()
                    .addHttpListener(8083, "localhost")
                    .setHandler(new HttpHandler() {
                        @Override
                        public void handleRequest(HttpServerExchange exchange) throws Exception {
                            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                            exchange.getResponseSender().send("Server3");
                        }
                    })
                    .build();

            server3.start();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if(server1 != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {

            }
            server1.stop();
            logger.info("The server1 is stopped.");
        }
        if(server2 != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {

            }
            server2.stop();
            logger.info("The server2 is stopped.");
        }
        if(server3 != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {

            }
            server3.stop();
            logger.info("The server3 is stopped.");
        }

        // Remove the test.json from home directory
        File test = new File(homeDir + "/proxy.yml");
        test.delete();
    }


    @Test
    public void testGet() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(10);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, enableHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final List<AtomicReference<ClientResponse>> references = new CopyOnWriteArrayList<>();
        try {
            connection.getIoThread().execute(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 10; i++) {
                        AtomicReference<ClientResponse> reference = new AtomicReference<>();
                        references.add(i, reference);
                        final ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath("/");
                        connection.sendRequest(request, client.createClientCallback(reference, latch));
                    }
                }

            });

            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        for (final AtomicReference<ClientResponse> reference : references) {
            Assert.assertTrue(reference.get().getAttachment(Http2Client.RESPONSE_BODY).contains("Server"));
            System.out.println(reference.get().getAttachment(Http2Client.RESPONSE_BODY));
        }
    }

    public static void addURL(URL url) throws Exception {
        URLClassLoader classLoader
                = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class clazz= URLClassLoader.class;

        // Use reflection
        Method method= clazz.getDeclaredMethod("addURL", URL.class);
        method.setAccessible(true);
        method.invoke(classLoader, url);
    }

}
