package com.example.testserver;

import com.newrelic.api.agent.NewRelic;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer;
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.message.StatusLine;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@RestController
public class TestController {
    private static final String BASE_URL = "cat-fact.herokuapp.com";

    @GetMapping("/hello")
    String hello(@RequestParam(name="name", defaultValue = "world") String name, @RequestHeader Map<String, String> headers) throws ExecutionException, InterruptedException {
        printHeaders(name, headers);

        try(CloseableHttpAsyncClient client = HttpAsyncClients.createDefault()) {
            final HttpHost target = new HttpHost(BASE_URL);

            final SimpleHttpRequest request = SimpleRequestBuilder.get()
                    .setHttpHost(target)
                    .setPath("/facts/")
                    .build();

            client.start();

            client.execute(
                    SimpleRequestProducer.create(request),
                    SimpleResponseConsumer.create(),
                    new FutureCallback<>() {

                        @Override
                        public void completed(final SimpleHttpResponse response) {
                            System.out.println("completed callback " + NewRelic.getAgent().getTransaction());
                            System.out.println(request + "->" + new StatusLine(response));
                        }

                        @Override
                        public void failed(final Exception ex) {
                            System.out.println("failed callback " + NewRelic.getAgent().getTransaction());
                            System.out.println(request + "->" + ex);
                        }

                        @Override
                        public void cancelled() {
                            System.out.println("cancelled callback " + NewRelic.getAgent().getTransaction());
                            System.out.println(request + " cancelled");
                        }
                    }).get();
            return "Hello " + name + "!!!";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/helloSync")
    String helloSync(@RequestParam(name="name", defaultValue = "world") String name) throws IOException {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            final HttpHost target = new HttpHost(BASE_URL);
            ClassicHttpRequest httpGet = ClassicRequestBuilder.get().setHttpHost(target).setPath("/facts/").build();

            String clientRes = httpclient.execute(httpGet, response -> {
                System.out.println(response.getCode() + " " + response.getReasonPhrase());
                final HttpEntity entity = response.getEntity();
                // do something useful with the response body
                // and ensure it is fully consumed
                String text = new BufferedReader(new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8))
                        .lines().collect(Collectors.joining("\n"));
                System.out.println("Res text --> " + text);
                EntityUtils.consume(entity);
                return text;
            });
            return "Hello " + name + "!!!\n" + clientRes;
        }
    }

    private void printHeaders(String name, Map<String, String> headers) {
        List<String> lines = new ArrayList<>(3 + headers.size());
        lines.add("____________________________________________________________________________________");
        lines.add("Begin Printing Headers for name: " + name);
        System.out.println();
        for(var h: headers.entrySet()) {
            lines.add("[Log Header] | " + h.getKey() + " | " + h.getValue());
        }
        lines.add("____________________________________________________________________________________");
        System.out.println(String.join("\n", lines));
    }
}
