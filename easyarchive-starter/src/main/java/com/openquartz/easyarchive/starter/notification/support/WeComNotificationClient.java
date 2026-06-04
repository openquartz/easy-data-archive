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
public class WeComNotificationClient implements NotificationClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public NotificationChannelEnum getChannel() {
        return NotificationChannelEnum.WECOM;
    }

    @Override
    public void send(String webhookUrl, String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> payload = new HashMap<>();
        payload.put("msgtype", "markdown");
        Map<String, String> markdown = new HashMap<>();
        markdown.put("content", message);
        payload.put("markdown", markdown);
        restTemplate.postForEntity(webhookUrl, new HttpEntity<>(payload, headers), String.class);
    }
}
