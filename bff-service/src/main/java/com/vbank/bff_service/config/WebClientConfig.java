package com.vbank.bff_service.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public ReactorClientHttpConnector downstreamClientConnector(
            @Value("${bff.downstream.connect-timeout}") Duration connectTimeout,
            @Value("${bff.downstream.response-timeout}") Duration responseTimeout
    ) {
        int connectTimeoutMillis = Math.toIntExact(connectTimeout.toMillis());

        HttpClient httpClient = HttpClient.create()
                .option(
                        ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        connectTimeoutMillis
                )
                .responseTimeout(responseTimeout);

        return new ReactorClientHttpConnector(httpClient);
    }

    @Bean
    @Qualifier("userWebClient")
    public WebClient userWebClient(
            WebClient.Builder builder,
            ReactorClientHttpConnector downstreamClientConnector,
            @Value("${services.user.base-url}") String baseUrl,
            @Value("${bff.downstream.max-in-memory-size}") int maxInMemorySize
    ) {
        return buildClient(
                builder,
                downstreamClientConnector,
                baseUrl,
                maxInMemorySize
        );
    }

    @Bean
    @Qualifier("accountWebClient")
    public WebClient accountWebClient(
            WebClient.Builder builder,
            ReactorClientHttpConnector downstreamClientConnector,
            @Value("${services.account.base-url}") String baseUrl,
            @Value("${bff.downstream.max-in-memory-size}") int maxInMemorySize
    ) {
        return buildClient(
                builder,
                downstreamClientConnector,
                baseUrl,
                maxInMemorySize
        );
    }

    @Bean
    @Qualifier("transactionWebClient")
    public WebClient transactionWebClient(
            WebClient.Builder builder,
            ReactorClientHttpConnector downstreamClientConnector,
            @Value("${services.transaction.base-url}") String baseUrl,
            @Value("${bff.downstream.max-in-memory-size}") int maxInMemorySize
    ) {
        return buildClient(
                builder,
                downstreamClientConnector,
                baseUrl,
                maxInMemorySize
        );
    }

    private WebClient buildClient(
            WebClient.Builder builder,
            ReactorClientHttpConnector connector,
            String baseUrl,
            int maxInMemorySize
    ) {
        return builder.clone()
                .baseUrl(baseUrl)
                .clientConnector(connector)
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(maxInMemorySize))
                .build();
    }
}
