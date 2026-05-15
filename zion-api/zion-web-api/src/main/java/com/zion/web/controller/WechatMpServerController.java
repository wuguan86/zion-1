package com.zion.web.controller;

import com.zion.web.service.WechatPcQrLoginService;
import com.zion.wechat.WechatMpService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.charset.StandardCharsets;

/**
 * WeChat Official Account server verification endpoint.
 */
@RestController
@RequestMapping("/wechat/mp")
@RequiredArgsConstructor
@Slf4j
public class WechatMpServerController {

    @GetMapping("/server")
    public String verify(@RequestParam String signature,
                         @RequestParam String timestamp,
                         @RequestParam String nonce,
                         @RequestParam String echostr) {
        boolean valid = wechatMpService.checkSignature(signature, timestamp, nonce);
        log.debug("[WechatMpServer] verify request: valid={}, timestamp={}, nonce={}", valid, timestamp, nonce);
        return valid ? echostr : "";
    }

    @PostMapping(value = "/server", consumes = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.ALL_VALUE})
    public String message(@RequestParam(required = false) String signature,
                          @RequestParam(required = false) String timestamp,
                          @RequestParam(required = false) String nonce,
                          @org.springframework.web.bind.annotation.RequestBody String body) {
        WechatEvent event = parseEvent(body);
        log.debug("[WechatMpServer] message received: msgType={}, event={}, eventKey={}, openIdPresent={}",
                event.msgType(), event.event(), event.eventKey(), event.fromUserName() != null && !event.fromUserName().isBlank());

        if ("event".equalsIgnoreCase(event.msgType())) {
            String scene = normalizeScene(event.event(), event.eventKey());
            if (scene != null && scene.startsWith("pc_login_")) {
                pcQrLoginService.confirmScan(scene, event.fromUserName());
            }
        }

        return "";
    }

    private WechatEvent parseEvent(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            Document document = factory.newDocumentBuilder()
                    .parse(new java.io.ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            return new WechatEvent(
                    text(document, "FromUserName"),
                    text(document, "MsgType"),
                    text(document, "Event"),
                    text(document, "EventKey")
            );
        } catch (Exception e) {
            log.warn("[WechatMpServer] failed to parse message body", e);
            return new WechatEvent(null, null, null, null);
        }
    }

    private String text(Document document, String tagName) {
        NodeList nodes = document.getElementsByTagName(tagName);
        if (nodes.getLength() == 0 || nodes.item(0) == null) {
            return null;
        }
        return nodes.item(0).getTextContent();
    }

    private String normalizeScene(String event, String eventKey) {
        if (eventKey == null || eventKey.isBlank()) {
            return null;
        }
        if ("subscribe".equalsIgnoreCase(event) && eventKey.startsWith("qrscene_")) {
            return eventKey.substring("qrscene_".length());
        }
        return eventKey;
    }

    private final WechatMpService wechatMpService;
    private final WechatPcQrLoginService pcQrLoginService;

    private record WechatEvent(String fromUserName, String msgType, String event, String eventKey) {
    }
}
