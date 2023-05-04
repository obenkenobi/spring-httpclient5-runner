package com.example.clientserver;

import com.newrelic.api.agent.NewRelic;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer;
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.StatusLine;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@RestController
public class ClientController {

    @GetMapping("/stream")
    public List<String> callData() {

        try (CloseableHttpAsyncClient client = HttpAsyncClients.createDefault()) {

            final HttpHost target = new HttpHost("localhost", 8081);

            client.start();

            List<String> queryParams = List.of("", "john", "jerry", "todd");
            ArrayList<Future<SimpleHttpResponse>> responsesFutures = new ArrayList<>(queryParams.size());
            ArrayList<String> responses = new ArrayList<>(queryParams.size());
            for(String name: queryParams) {
                final SimpleHttpRequest request = SimpleRequestBuilder.get()
                        .setHttpHost(target)
                        .setPath("/hello")
                        .addParameter("name", name)
                        .build();

                var future = client.execute(SimpleRequestProducer.create(request),
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

                        });
                responsesFutures.add(future);
            }
            for (Future<SimpleHttpResponse> future: responsesFutures) {
                responses.add(future.get().getBodyText());
            }
            return responses;

        } catch (IOException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

}
