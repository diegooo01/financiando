package com.ando.financiando.controller;

import com.ando.financiando.service.WhatsappService;
import com.twilio.twiml.MessagingResponse;
import com.twilio.twiml.messaging.Message;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook/whatsapp")
public class WhatsappController {

    private final WhatsappService whatsappService;

    public WhatsappController(WhatsappService whatsappService) {
        this.whatsappService = whatsappService;
    }

    @PostMapping(produces = MediaType.APPLICATION_XML_VALUE)
    public String receiveMessage(
            @RequestParam("Body") String body,
            @RequestParam("From") String from
    ) {
        String replyText = whatsappService.buildReply(body, from);

        Message message = new Message.Builder()
                .body(new com.twilio.twiml.messaging.Body.Builder(replyText).build())
                .build();

        MessagingResponse twiml = new MessagingResponse.Builder()
                .message(message)
                .build();

        return twiml.toXml();
    }
}