package com.ando.financiando.controller;

import com.ando.financiando.service.TwilioSignatureValidator;
import com.ando.financiando.service.WhatsappService;
import com.twilio.twiml.MessagingResponse;
import com.twilio.twiml.messaging.Message;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook/whatsapp")
public class WhatsappController {

    private final WhatsappService whatsappService;
    private final TwilioSignatureValidator signatureValidator;

    public WhatsappController(WhatsappService whatsappService,
                              TwilioSignatureValidator signatureValidator) {
        this.whatsappService = whatsappService;
        this.signatureValidator = signatureValidator;
    }

    @PostMapping(produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> receiveMessage(
            @RequestParam("Body") String body,
            @RequestParam("From") String from,
            HttpServletRequest request
    ) {
        if (!signatureValidator.isValid(request, request.getParameterMap())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String replyText = whatsappService.buildReply(body, from);

        Message message = new Message.Builder()
                .body(new com.twilio.twiml.messaging.Body.Builder(replyText).build())
                .build();

        MessagingResponse twiml = new MessagingResponse.Builder()
                .message(message)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(twiml.toXml());
    }
}