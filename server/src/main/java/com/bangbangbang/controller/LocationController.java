package com.bangbangbang.controller;

import com.bangbangbang.common.ApiResult;
import com.bangbangbang.common.BizException;
import com.bangbangbang.common.UserContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 位置服务：把经纬度反查成城市/区县，供首页「自动定位」显示。
 *
 * 依赖腾讯位置服务 WebService API 的 key（免费申请，个人主体可用），
 * 通过环境变量 TENCENT_MAP_KEY 注入；未配置时接口返回业务提示，
 * 前端据此退回「手动选择位置」（uni.chooseLocation，无需 key）。
 */
@RestController
@RequestMapping("/api/location")
public class LocationController {

    private static final String REVERSE_URL = "https://apis.map.qq.com/ws/geocoder/v1/";

    private final String tencentKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public LocationController(@Value("${app.map.tencent-key:}") String tencentKey,
                              ObjectMapper objectMapper) {
        this.tencentKey = tencentKey;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/reverse")
    public ApiResult<Map<String, Object>> reverse(@RequestParam double lat, @RequestParam double lng) {
        // 定位只对登录用户开放，避免被当成公开的免费逆地理服务
        UserContext.require();

        if (tencentKey == null || tencentKey.isBlank()) {
            throw BizException.of("未配置地图服务密钥");
        }
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw BizException.of("坐标不合法");
        }

        String url = REVERSE_URL + "?location=" + lat + "," + lng
                + "&key=" + tencentKey + "&get_poi=0";

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            if (root.path("status").asInt(-1) != 0) {
                String message = root.path("message").asText("逆地理编码失败");
                throw BizException.of("定位失败：" + message);
            }

            JsonNode component = root.path("result").path("address_component");
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("city", component.path("city").asText(""));
            data.put("district", component.path("district").asText(""));
            data.put("province", component.path("province").asText(""));
            data.put("address", root.path("result").path("address").asText(""));
            return ApiResult.ok(data);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw BizException.of("定位服务暂时不可用");
        }
    }
}
