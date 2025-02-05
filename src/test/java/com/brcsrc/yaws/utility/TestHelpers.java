package com.brcsrc.yaws.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public class TestHelpers {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> HttpEntity<String> createTestEntity(T object) throws  JsonProcessingException {
        String jsonBody = objectMapper.writeValueAsString(object);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(jsonBody, headers);
    }
}
