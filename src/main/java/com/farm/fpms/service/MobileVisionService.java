package com.farm.fpms.service;

import com.farm.fpms.domain.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;

@Service
public class MobileVisionService {

    private static final long MAX_IMAGE_BYTES = 10L * 1024L * 1024L;

    private final AiProviderService aiProviderService;
    private final AiGateway aiGateway;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MobileVisionService(AiProviderService aiProviderService, AiGateway aiGateway, JdbcTemplate jdbcTemplate) {
        this.aiProviderService = aiProviderService;
        this.aiGateway = aiGateway;
        this.jdbcTemplate = jdbcTemplate;
    }

    public VisionRecognitionResult recognize(MultipartFile image) {
        validate(image);
        AiProvider provider = aiProviderService.requireProvider("VISION");
        long start = System.nanoTime();
        try {
            byte[] imageBytes = image.getBytes();
            String contentType = image.getContentType() == null ? "image/jpeg" : image.getContentType();
            String dataUrl = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
            VisionRecognitionResult result = aiGateway.recognize(provider, dataUrl);
            aiProviderService.logCall(provider.getId(), "VISION", "手机端拍照识别：" + image.getOriginalFilename(),
                    true, Duration.ofNanos(System.nanoTime() - start), null);
            persistRecognizeLog(imageBytes, result);
            return result;
        } catch (IOException ex) {
            throw new BusinessException("图片读取失败，请重新上传");
        } catch (RuntimeException ex) {
            aiProviderService.logCall(provider.getId(), "VISION", "手机端拍照识别：" + image.getOriginalFilename(),
                    false, Duration.ofNanos(System.nanoTime() - start), ex.getClass().getSimpleName());
            throw ex;
        }
    }

    private void persistRecognizeLog(byte[] imageBytes, VisionRecognitionResult result) {
        try {
            String hash = sha256(imageBytes);
            String json = objectMapper.writeValueAsString(result);
            jdbcTemplate.update("insert into ai_recognize_log(image_hash, crop_name, result_json) values(?,?,?)",
                    hash, result.getName(), json);
        } catch (Exception ignored) {}
    }

    private String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception ex) {
            return "unknown";
        }
    }

    private void validate(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BusinessException("请先拍照或选择图片");
        }
        if (image.getSize() > MAX_IMAGE_BYTES) {
            throw new BusinessException("图片不能超过 10MB");
        }
        String contentType = image.getContentType() == null ? "" : image.getContentType().toLowerCase();
        if (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/webp")) {
            throw new BusinessException("仅支持 JPG、PNG、WebP 图片");
        }
    }
}
