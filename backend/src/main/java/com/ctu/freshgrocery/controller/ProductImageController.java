package com.ctu.freshgrocery.controller;

import com.ctu.freshgrocery.dto.ApiResponse;
import com.ctu.freshgrocery.exception.AppException;
import com.ctu.freshgrocery.model.Product;
import com.ctu.freshgrocery.repository.ProductRepository;
import com.ctu.freshgrocery.service.SemanticSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@RestController
public class ProductImageController {
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private final Path directory;
    private final ProductRepository productRepository;
    private final SemanticSearchService semanticSearchService;

    public ProductImageController(@Value("${ADMIN_CREDENTIALS_DIR:.local}") String localDirectory) {
        this(localDirectory, null, null);
    }

    @Autowired
    public ProductImageController(
            @Value("${ADMIN_CREDENTIALS_DIR:.local}") String localDirectory,
            ProductRepository productRepository,
            SemanticSearchService semanticSearchService) {
        this.directory = Path.of(localDirectory).resolve("product-images").toAbsolutePath().normalize();
        this.productRepository = productRepository;
        this.semanticSearchService = semanticSearchService;
    }

    public record Base64ImageUploadRequest(String data, Integer productId) {}

    @PostMapping(value = "/api/v1/admin/product-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "productId", required = false) Integer productId) throws IOException {
        if (file.isEmpty() || file.getSize() > MAX_IMAGE_BYTES) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Ảnh phải có dữ liệu và không quá 10 MB.");
        }
        return storeValidatedImage(file.getBytes(), productId);
    }

    @PostMapping(value = "/api/v1/admin/product-images", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<?> uploadBase64(@RequestBody Base64ImageUploadRequest request) throws IOException {
        if (request == null || request.data() == null || request.data().isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Không nhận được dữ liệu ảnh tải lên.");
        }
        String payload = request.data().trim();
        int commaIdx = payload.indexOf(',');
        if (payload.startsWith("data:") && commaIdx > 0) {
            payload = payload.substring(commaIdx + 1);
        }
        byte[] bytes;
        try {
            bytes = Base64.getMimeDecoder().decode(payload);
        } catch (IllegalArgumentException e) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Dữ liệu ảnh tải lên không hợp lệ.");
        }
        if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Ảnh phải có dữ liệu và không quá 10 MB.");
        }
        return storeValidatedImage(bytes, request.productId());
    }

    private ApiResponse<?> storeValidatedImage(byte[] bytes, Integer productId) throws IOException {
        String extension;
        if (bytes.length > 12 && bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8 && bytes[2] == (byte) 0xff) {
            extension = "jpg";
        } else if (bytes.length > 8 && Arrays.equals(Arrays.copyOf(bytes, 8),
                new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10})) {
            extension = "png";
        } else if (bytes.length > 12
                && new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) {
            extension = "webp";
        } else {
            throw new AppException(HttpStatus.BAD_REQUEST, "Chỉ hỗ trợ ảnh JPG, PNG hoặc WebP.");
        }

        // Decode metadata without allocating a full bitmap, to reject malformed/oversized images.
        if (!extension.equals("webp")) {
            try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new AppException(HttpStatus.BAD_REQUEST, "Tệp ảnh không hợp lệ.");
                var reader = readers.next();
                try {
                    reader.setInput(input);
                    long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                    if (pixels <= 0 || pixels > 40_000_000) {
                        throw new AppException(HttpStatus.BAD_REQUEST, "Kích thước ảnh quá lớn.");
                    }
                } finally {
                    reader.dispose();
                }
            }
        }

        Files.createDirectories(directory);
        String filename = UUID.randomUUID() + "." + extension;
        Files.write(directory.resolve(filename), bytes, StandardOpenOption.CREATE_NEW);
        String url = "/api/v1/product-images/" + filename;

        boolean savedToProduct = false;
        if (productId != null && productRepository != null) {
            Optional<Product> found = productRepository.findById(productId);
            if (found.isPresent()) {
                Product p = found.get();
                p.setHinhAnh(url);
                p.setNgayCapNhat(LocalDateTime.now());
                Product saved = productRepository.save(p);
                savedToProduct = true;
                if (semanticSearchService != null && saved.getMaSPCha() == null) {
                    semanticSearchService.syncProduct(saved);
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", url);
        result.put("savedToProduct", savedToProduct);
        return ApiResponse.ok(
                savedToProduct ? "Đã tải ảnh lên và lưu trực tiếp vào sản phẩm." : "Đã tải ảnh lên máy chủ.",
                result);
    }

    @GetMapping("/api/v1/product-images/{filename}")
    public ResponseEntity<Resource> image(@PathVariable String filename) {
        if (!filename.matches("[a-zA-Z0-9_-]+\\.(jpg|png|webp)")) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy ảnh.");
        }
        Path path = directory.resolve(filename);
        if (!Files.isRegularFile(path)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy ảnh.");
        }
        MediaType type = filename.endsWith(".png") ? MediaType.IMAGE_PNG
                : filename.endsWith(".webp") ? MediaType.parseMediaType("image/webp") : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(type)
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)))
                .body(new FileSystemResource(path));
    }
}
