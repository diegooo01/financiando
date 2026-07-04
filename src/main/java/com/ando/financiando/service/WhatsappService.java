package com.ando.financiando.service;

import org.springframework.stereotype.Service;

@Service
public class WhatsappService {

    public String buildReply(String incomingMessage, String from) {
        if (incomingMessage == null || incomingMessage.isBlank()) {
            return "No entendí tu mensaje. Escribe algo como: almuerzo 20";
        }

        String cleaned = incomingMessage.trim();

        return "Recibí tu mensaje: \"" + cleaned + "\". "
                + "Pronto podré registrar tus gastos automáticamente.";
    }
}