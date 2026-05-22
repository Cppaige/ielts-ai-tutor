package com.ielts.gateway.routing;

import com.ielts.gateway.config.RoutingProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Enumeration;

@RestController
public class ProxyController {

    private final RestClient restClient;
    private final RoutingProperties routing;

    public ProxyController(RestClient.Builder restClientBuilder, RoutingProperties routing) {
        this.restClient = restClientBuilder.build();
        this.routing = routing;
    }

    @RequestMapping({"/auth/**", "/data/**", "/api/auth/**", "/api/data/**"})
    public ResponseEntity<String> proxyToDataService(HttpServletRequest request,
                                                     @RequestBody(required = false) String body) {
        return proxy(routing.getDataService(), request, body);
    }

    @RequestMapping({"/writing/**", "/api/writing/**"})
    public ResponseEntity<String> proxyToWritingService(HttpServletRequest request,
                                                        @RequestBody(required = false) String body) {
        return proxy(routing.getWritingService(), request, body);
    }

    @RequestMapping({"/speaking/**", "/api/speaking/**"})
    public ResponseEntity<String> proxyToSpeakingService(HttpServletRequest request,
                                                         @RequestBody(required = false) String body) {
        return proxy(routing.getSpeakingService(), request, body);
    }

    private ResponseEntity<String> proxy(String baseUrl, HttpServletRequest request, String body) {
        // 去掉 /api 前缀再转发，保持下游服务路径不变
        String path = request.getRequestURI().replaceFirst("^/api", "");
        String query = request.getQueryString();
        String targetUrl = baseUrl + path + (query != null ? "?" + query : "");

        try {
            RestClient.RequestBodySpec spec = restClient.method(HttpMethod.valueOf(request.getMethod()))
                    .uri(targetUrl)
                    .headers(headers -> copyHeaders(request, headers));

            RestClient.RequestHeadersSpec<?> requestSpec = (body != null)
                    ? spec.body(body)
                    : spec;

            return requestSpec.retrieve().toEntity(String.class);
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(502)
                    .body("{\"code\":502,\"message\":\"Service unavailable\"}");
        }
    }

    private void copyHeaders(HttpServletRequest request, org.springframework.http.HttpHeaders headers) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if (!name.equalsIgnoreCase("host") && !name.equalsIgnoreCase("content-length")) {
                headers.add(name, request.getHeader(name));
            }
        }
    }
}
