package com.ielts.gateway.routing;

import com.ielts.gateway.config.RoutingProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Enumeration;

@RestController
public class ProxyController {

    private final WebClient webClient;
    private final RoutingProperties routing;

    public ProxyController(WebClient webClient, RoutingProperties routing) {
        this.webClient = webClient;
        this.routing = routing;
    }

    @RequestMapping({"/auth/**", "/data/**"})
    public Mono<ResponseEntity<String>> proxyToDataService(HttpServletRequest request,
                                                            @RequestBody(required = false) String body) {
        return proxy(routing.getDataService(), request, body);
    }

    @RequestMapping("/writing/**")
    public Mono<ResponseEntity<String>> proxyToWritingService(HttpServletRequest request,
                                                              @RequestBody(required = false) String body) {
        return proxy(routing.getWritingService(), request, body);
    }

    @RequestMapping("/speaking/**")
    public Mono<ResponseEntity<String>> proxyToSpeakingService(HttpServletRequest request,
                                                               @RequestBody(required = false) String body) {
        return proxy(routing.getSpeakingService(), request, body);
    }

    private Mono<ResponseEntity<String>> proxy(String baseUrl, HttpServletRequest request, String body) {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String targetUrl = baseUrl + path + (query != null ? "?" + query : "");

        WebClient.RequestBodySpec spec = webClient.method(HttpMethod.valueOf(request.getMethod()))
                .uri(targetUrl)
                .headers(headers -> copyHeaders(request, headers));

        WebClient.RequestHeadersSpec<?> requestSpec = (body != null)
                ? spec.bodyValue(body)
                : spec;

        return requestSpec.retrieve()
                .toEntity(String.class)
                .onErrorResume(e -> Mono.just(ResponseEntity.status(502).body("{\"code\":502,\"message\":\"Service unavailable\"}")));
    }

    private void copyHeaders(HttpServletRequest request, HttpHeaders headers) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if (!name.equalsIgnoreCase("host") && !name.equalsIgnoreCase("content-length")) {
                headers.add(name, request.getHeader(name));
            }
        }
    }
}
