package com.example.aviation.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

import java.io.IOException;

@Slf4j
public class OpenSkyResponseErrorHandler extends DefaultResponseErrorHandler {

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        HttpStatusCode statusCode = response.getStatusCode();
        String retryAfter = response.getHeaders().getFirst("X-Rate-Limit-Retry-After-Seconds");
        Long retryAfterSeconds = null;
        if (retryAfter != null) {
            try {
                retryAfterSeconds = Long.parseLong(retryAfter);
                log.warn("OpenSky API rate limit exceeded. Retry after {} seconds.", retryAfterSeconds);
            } catch (NumberFormatException e) {
                log.warn("OpenSky API rate limit exceeded. Retry-After header invalid: {}", retryAfter);
            }
        }

        String remaining = response.getHeaders().getFirst("X-Rate-Limit-Remaining");
        if (remaining != null) {
            log.info("OpenSky API credits remaining: {}", remaining);
        }

        if (statusCode.is4xxClientError() || statusCode.is5xxServerError()) {
            throw new OpenSkyApiException(
                    "OpenSky API returned " + statusCode.value(),
                    statusCode.value(),
                    retryAfterSeconds
            );
        }

        super.handleError(response);
    }
}
