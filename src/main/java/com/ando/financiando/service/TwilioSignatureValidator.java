package com.ando.financiando.service;

import com.twilio.security.RequestValidator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.TreeMap;

@Component
public class TwilioSignatureValidator {

    private final RequestValidator requestValidator;

    public TwilioSignatureValidator(@Value("${twilio.auth-token}") String authToken) {
        this.requestValidator = new RequestValidator(authToken);
    }

    public boolean isValid(HttpServletRequest request, Map<String, String[]> params) {
        String signature = request.getHeader("X-Twilio-Signature");
        if (signature == null || signature.isBlank()) {
            return false;
        }

        String url = buildOriginalUrl(request);

        Map<String, String> flatParams = new TreeMap<>();
        params.forEach((key, values) -> {
            if (values != null && values.length > 0) {
                flatParams.put(key, values[0]);
            }
        });

        return requestValidator.validate(url, flatParams, signature);
    }

    private String buildOriginalUrl(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String forwardedHost = request.getHeader("X-Forwarded-Host");

        if (forwardedProto != null && forwardedHost != null) {
            return forwardedProto + "://" + forwardedHost + request.getRequestURI();
        }

        return request.getRequestURL().toString();
    }
}