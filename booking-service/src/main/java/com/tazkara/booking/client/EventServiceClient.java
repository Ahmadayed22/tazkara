package com.tazkara.booking.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tazkara.booking.client.EventClientDtos.EventSnapshot;
import com.tazkara.booking.client.EventClientDtos.RemoteError;
import com.tazkara.booking.client.EventClientDtos.ReservationResult;
import com.tazkara.booking.client.EventClientDtos.ReserveTicketsRequest;
import com.tazkara.booking.config.BookingProperties;
import com.tazkara.booking.exception.BookingErrorCode;
import com.tazkara.common.error.DomainException;
import com.tazkara.common.error.ErrorCode;
import com.tazkara.common.security.InternalApiKeyFilter;
import com.tazkara.common.security.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Synchronous REST client for the Event Service internal API. Each call carries the shared internal API key.
 *   4xx business errors (TICKETS_UNAVAILABLE, EVENT_NOT_BOOKABLE...) are passed on with the same code and status.
 *   Timeouts, connection errors and 5xx become EVENT_SERVICE_UNAVAILABLE (503); for a reservation this means
 *       "outcome unknown", which the caller compensates.
 */
@Component
public class EventServiceClient {

    private static final Logger log = LoggerFactory.getLogger(EventServiceClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public EventServiceClient(RestClient.Builder builder, BookingProperties properties,
                              SecurityProperties securityProperties, ObjectMapper objectMapper) {
        BookingProperties.EventService settings = properties.eventService();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) settings.connectTimeout().toMillis());
        requestFactory.setReadTimeout((int) settings.readTimeout().toMillis());
        this.restClient = builder
                .baseUrl(settings.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(InternalApiKeyFilter.HEADER, securityProperties.internalApiKey())
                .build();
        this.objectMapper = objectMapper;
    }

    public EventSnapshot getEvent(Long eventId) {
        return call(() -> restClient.get()
                .uri("/internal/events/{eventId}", eventId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw translate(response);
                })
                .body(EventSnapshot.class));
    }

    public ReservationResult reserve(Long eventId, String bookingReference, Map<Long, Integer> quantities) {
        List<ReserveTicketsRequest.Line> lines = quantities.entrySet().stream()
                .map(entry -> new ReserveTicketsRequest.Line(entry.getKey(), entry.getValue()))
                .toList();
        return call(() -> restClient.post()
                .uri("/internal/events/{eventId}/reservations", eventId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ReserveTicketsRequest(bookingReference, lines))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw translate(response);
                })
                .body(ReservationResult.class));
    }

    private <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (DomainException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            log.warn("Event Service unreachable: {}", ex.getMessage());
            throw unavailable();
        } catch (RestClientException ex) {
            log.warn("Unexpected Event Service response: {}", ex.getMessage());
            throw unavailable();
        }
    }

    private DomainException translate(ClientHttpResponse response) throws IOException {
        HttpStatus status = HttpStatus.resolve(response.getStatusCode().value());
        if (status == null || status.is5xxServerError()
                || status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
            // 401/403 here means the internal API key is misconfigured: an infrastructure problem, not the user's.
            log.error("Event Service answered {}", response.getStatusCode());
            return unavailable();
        }
        RemoteError error = readError(response);
        return new DomainException(new RemoteErrorCode(error.code(), status), error.message());
    }

    private RemoteError readError(ClientHttpResponse response) {
        try {
            RemoteError error = objectMapper.readValue(response.getBody(), RemoteError.class);
            if (error.code() != null) {
                return error;
            }
        } catch (IOException ex) {
            log.warn("Could not parse Event Service error body: {}", ex.getMessage());
        }
        return new RemoteError("EVENT_SERVICE_ERROR", "The Event Service rejected the request");
    }

    private static DomainException unavailable() {
        return new DomainException(BookingErrorCode.EVENT_SERVICE_UNAVAILABLE,
                "The Event Service is temporarily unavailable, please retry");
    }

    /** An error code received from the Event Service, passed on unchanged to our client. */
    record RemoteErrorCode(String code, HttpStatus status) implements ErrorCode {
    }
}
