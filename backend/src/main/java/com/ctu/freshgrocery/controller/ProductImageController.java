package com.ctu.freshgrocery.controller;

import com.ctu.freshgrocery.dto.ApiResponse;
import com.ctu.freshgrocery.exception.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.nio.file.*;
import java.io.*;
import java.util.*;

@RestController
public class ProductImageController {
    private final Path directory;

    public ProductImageController(@Value("${ADMIN_CREDENTIALS_DIR:.local}") String localDirectory) {
        directory = Path.of(localDirectory).resolve("product-images").toAbsolutePath().normalize();
    }

    @PostMapping("/api/v1/admin/product-images")
    public ApiResponse<?> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024)
            throw new AppException(HttpStatus.BAD_REQUEST, "Ảnh phải có dữ liệu và không quá 5 MB.");
        byte[] bytes = file.getBytes();
        String extension;
        if (bytes.length > 12 && bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8 && bytes[2] == (byte) 0xff) extension = "jpg";
        else if (bytes.length > 8 && Arrays.equals(Arrays.copyOf(bytes, 8), new byte[]{(byte)137,80,78,71,13,10,26,10})) extension = "png";
        else if (bytes.length > 12 && new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
                && new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP")) extension = "webp";
        else throw new AppException(HttpStatus.BAD_REQUEST, "Chỉ hỗ trợ ảnh JPG, PNG hoặc WebP.");
        // Decode metadata without allocating a full bitmap, to reject malformed/oversized images.
        if (!extension.equals("webp")) {
            try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new AppException(HttpStatus.BAD_REQUEST, "Tệp ảnh không hợp lệ.");
                var reader = readers.next();
                try {
                    reader.setInput(input);
                    long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                    if (pixels <= 0 || pixels > 40_000_000) throw new AppException(HttpStatus.BAD_REQUEST, "Kích thước ảnh quá lớn.");
                } finally { reader.dispose(); }
            }
        }
        Files.createDirectories(directory);
        String filename = UUID.randomUUID() + "." + extension;
        Files.write(directory.resolve(filename), bytes, StandardOpenOption.CREATE_NEW);
        return ApiResponse.ok("Đã tải ảnh lên", Map.of("url", "/api/v1/product-images/" + filename));
    }

    @GetMapping("/api/v1/product-images/{filename}")
    public ResponseEntity<Resource> image(@PathVariable String filename) {
        if (!filename.matches("[a-zA-Z0-9_-]+\\.(jpg|png|webp)"))
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy ảnh.");
        Path path = directory.resolve(filename);
        if (!Files.isRegularFile(path)) throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy ảnh.");
        MediaType type = filename.endsWith(".png") ? MediaType.IMAGE_PNG
                : filename.endsWith(".webp") ? MediaType.parseMediaType("image/webp") : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type).header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(30))).body(new FileSystemResource(path));
    }
}
