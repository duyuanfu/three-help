package com.bangbangbang.service;

import com.bangbangbang.common.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传：把小程序选择的图片/语音落到服务器磁盘，返回可访问的相对地址
 *
 * 目录结构：{upload.dir}/{kind}/{yyyyMM}/{uuid}.{ext}
 * 返回地址：/uploads/{kind}/{yyyyMM}/{uuid}.{ext}
 *
 * 存相对地址而不是完整 URL：换 IP / 换域名时数据库无需迁移，
 * 前端在展示前用 API_ORIGIN 拼成完整地址。
 */
@Service
public class UploadService {

    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
    private static final Set<String> AUDIO_EXT = Set.of("mp3", "aac", "m4a", "wav", "amr", "silk");
    private static final Set<String> ALLOWED_KIND = Set.of("avatar", "image", "voice");

    /** 单文件大小上限：8MB */
    private static final long MAX_SIZE = 8L * 1024 * 1024;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public Map<String, Object> store(MultipartFile file, String kindRaw) {
        if (file == null || file.isEmpty()) {
            throw BizException.of("请选择要上传的文件");
        }
        if (file.getSize() > MAX_SIZE) {
            throw BizException.of("文件过大，请上传 8MB 以内的文件");
        }

        String kind = (kindRaw == null || !ALLOWED_KIND.contains(kindRaw)) ? "image" : kindRaw;
        String ext = resolveExtension(file);
        boolean imageKind = !"voice".equals(kind);

        if (imageKind && !IMAGE_EXT.contains(ext)) {
            throw BizException.of("仅支持 jpg / png / webp 等图片格式");
        }
        if (!imageKind && !AUDIO_EXT.contains(ext)) {
            throw BizException.of("仅支持 mp3 / aac / m4a 等音频格式");
        }

        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = Paths.get(uploadDir, kind, month).toAbsolutePath().normalize();
        Path target = dir.resolve(fileName);

        try {
            Files.createDirectories(dir);
            file.transferTo(target.toFile());
        } catch (IOException e) {
            throw BizException.of("文件保存失败：" + e.getMessage());
        }

        String url = "/uploads/" + kind + "/" + month + "/" + fileName;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", url);
        result.put("name", file.getOriginalFilename());
        result.put("size", file.getSize());
        result.put("kind", kind);
        return result;
    }

    /** 从原始文件名或 Content-Type 推断扩展名 */
    private String resolveExtension(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name != null) {
            int dot = name.lastIndexOf('.');
            if (dot > -1 && dot < name.length() - 1) {
                String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
                if (!ext.isEmpty() && ext.length() <= 5) {
                    return ext;
                }
            }
        }
        String contentType = file.getContentType();
        if (contentType != null) {
            String type = contentType.toLowerCase(Locale.ROOT);
            if (type.contains("jpeg")) return "jpg";
            if (type.contains("png")) return "png";
            if (type.contains("gif")) return "gif";
            if (type.contains("webp")) return "webp";
            if (type.contains("mp4") || type.contains("m4a")) return "m4a";
            if (type.contains("mpeg")) return "mp3";
        }
        throw BizException.of("无法识别文件类型");
    }
}
