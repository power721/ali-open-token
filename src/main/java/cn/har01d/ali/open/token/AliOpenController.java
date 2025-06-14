package cn.har01d.ali.open.token;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/alipan")
public class AliOpenController {
    private static Logger logger = LoggerFactory.getLogger(AliOpenController.class);
    private static final String ACCESS_TOKEN_URL = "https://openapi.alipan.com/oauth/access_token";
    private final RestTemplate restTemplate;

    @Value("${ali_client_id}")
    private String clientId;

    @Value("${ali_client_secret}")
    private String clientSecret;

    @Value("${ali_callback}")
    private String redirectUri;

    public AliOpenController(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    @GetMapping("/authorize")
    public String authorize() {
        String url = "https://www.alipan.com/o/oauth/authorize?client_id=" + clientId +
                "&redirect_uri=" + URLEncoder.encode(redirectUri) +
                "&scope=user:base,file:all:read,file:all:write" +
                "&response_type=code&relogin=true&state=" + UUID.randomUUID();
        return "redirect:" + url;
    }

    @ResponseBody
    @PostMapping("/access_token")
    public AliAccessToken accessToken(@RequestBody AliAccessToken request) {
        logger.debug("accessToken request: {}", request);
        Map<String, String> body = new HashMap<>();
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("grant_type", "refresh_token");
        body.put("refresh_token", request.getRefreshToken());
        AliAccessToken response = restTemplate.postForObject(ACCESS_TOKEN_URL, body, AliAccessToken.class);
        logger.debug("get open token response: {}", response);
        return response;
    }

    @GetMapping("/callback")
    public ModelAndView callback(String code, String state) {
        if (code == null || code.isEmpty()) {
            throw new RuntimeException("code is required");
        }

        logger.info("code: {}  state: {}", code, state);
        Map<String, String> body = new HashMap<>();
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("grant_type", "authorization_code");
        body.put("code", code);

        Map<String, Object> model = new HashMap<>();
        model.put("error", false);
        model.put("message", "");
        model.put("token", "");

        try {
            Map<String, Object> response = restTemplate.postForObject(ACCESS_TOKEN_URL, body, Map.class);
            logger.debug("get open token response: {}", response);
            model.put("token", response.get("refresh_token"));
        } catch (Exception e) {
            logger.warn("Get token error:", e);
            model.put("error", true);
            model.put("message", e.getMessage());
        }
        return new ModelAndView("token", model);
    }
}
