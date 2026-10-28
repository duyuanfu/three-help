package com.bangbangbang.controller;

import com.bangbangbang.common.ApiResult;
import com.bangbangbang.service.UploadService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件上传：小程序 uni.uploadFile → 这里
 * form-data 字段名统一用 file，kind 可选 avatar / image / voice
 */
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping
    public ApiResult<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                                 @RequestParam(value = "kind", required = false) String kind) {
        return ApiResult.ok(uploadService.store(file, kind));
    }
}
