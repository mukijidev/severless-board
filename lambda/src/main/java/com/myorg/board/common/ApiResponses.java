package com.myorg.board.common;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public final class ApiResponses {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ApiResponses() {}

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static APIGatewayProxyResponseEvent json(int status, Object body) {
        try {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(status)
                    .withHeaders(Map.of(
                            "Content-Type", "application/json",
                            "Access-Control-Allow-Origin", "*"))
                    .withBody(MAPPER.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize response", e);
        }
    }

    public static APIGatewayProxyResponseEvent error(int status, String message) {
        return json(status, Map.of("message", message));
    }

    public static APIGatewayProxyResponseEvent noContent() {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(204)
                .withHeaders(Map.of("Access-Control-Allow-Origin", "*"));
    }
}