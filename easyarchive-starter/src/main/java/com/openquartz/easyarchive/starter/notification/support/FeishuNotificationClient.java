package com.openquartz.easyarchive.starter.notification.support;

import com.openquartz.easyarchive.starter.model.enums.NotificationChannelEnum;
import com.openquartz.easyarchive.starter.notification.NotificationClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class FeishuNotificationClient implements NotificationClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public NotificationChannelEnum getChannel() {
        return NotificationChannelEnum.FEISHU;
    }

    @Override
    public void send(String webhookUrl, String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> payload = new HashMap<>();
        payload.put("msg_type", "text");
        Map<String, String> content = new HashMap<>();
        content.put("text", message);
        payload.put("content", content);
        restTemplate.postForEntity(webhookUrl, new HttpEntity<>(payload, headers), String.class);
    }
}
