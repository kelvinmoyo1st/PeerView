package com.peerview.signaling;

import java.util.Map;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class SignalingController {

    @MessageMapping("/session/{sessionId}/signal")
    @SendTo("/topic/session/{sessionId}/signal")
    public SignalMessage signal(@DestinationVariable String sessionId, SignalMessage message) {
        return message;
    }

    public record SignalMessage(String type, Map<String, Object> payload) {}
}