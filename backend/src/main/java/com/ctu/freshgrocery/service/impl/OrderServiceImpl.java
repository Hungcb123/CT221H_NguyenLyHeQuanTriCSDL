package com.ctu.freshgrocery.service.impl;

import com.ctu.freshgrocery.dto.CheckoutRequest;
import com.ctu.freshgrocery.dto.CheckoutResponse;
import com.ctu.freshgrocery.dto.ConcurrencyTestResponse;
import com.ctu.freshgrocery.dto.OrderDetailResponse;
import com.ctu.freshgrocery.exception.AppException;
import com.ctu.freshgrocery.model.Cart;
import com.ctu.freshgrocery.model.Customer;
import com.ctu.freshgrocery.model.Order;
import com.ctu.freshgrocery.model.Product;
import com.ctu.freshgrocery.repository.CartRepository;
import com.ctu.freshgrocery.repository.CustomerRepository;
import com.ctu.freshgrocery.repository.OrderRepository;
import com.ctu.freshgrocery.repository.OrderSpRepository;
import com.ctu.freshgrocery.repository.ProductRepository;
import com.ctu.freshgrocery.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderSpRepository orderSpRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public CheckoutResponse checkout(CheckoutRequest request) {
        if (!customerRepository.existsById(request.getMaKH())) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng với ID: " + request.getMaKH());
        }

        Cart cart = cartRepository.findByMaKH(request.getMaKH())
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Khách hàng chưa có giỏ hàng để thanh toán"));

        Map<String, Object> result = orderSpRepository.executeThanhToanDonHang(
                request.getMaKH(),
                cart.getMaGioHang(),
                request.getDiaChiGiaoHang(),
                request.getGhiChu()
        );

        Integer statusCode = (Integer) result.get("StatusCode");
        String message = (String) result.get("Message");
        Integer maDH = (Integer) result.get("MaDH_Output");

        if (statusCode == null || statusCode < 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, statusCode != null ? statusCode : -99, message != null ? message : "Lỗi xử lý giao dịch thanh toán");
        }

        Order order = orderRepository.findById(maDH).orElse(null);
        BigDecimal tongTien = order != null ? order.getTongTien() : BigDecimal.ZERO;

        BigDecimal balanceAfterPayment = jdbcTemplate.queryForObject(
                "SELECT SoDuSau FROM LichSuGiaoDichVi WHERE MaDH = ? AND MaKH = ? AND LoaiGiaoDich = 'THANH_TOAN'",
                BigDecimal.class, maDH, request.getMaKH());

        return CheckoutResponse.builder()
                .maDH(maDH)
                .statusCode(statusCode)
                .message(message)
                .tongTien(tongTien)
                .soDuConLai(balanceAfterPayment)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderById(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn đơn hàng với ID: " + orderId));

        return mapToOrderDetailResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDetailResponse> getOrdersByCustomer(Integer customerId) {
        return getOrdersByCustomer(customerId, 1, 20);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDetailResponse> getOrdersByCustomer(Integer customerId, int page, int size) {
        if (!customerRepository.existsById(customerId)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng với ID: " + customerId);
        }

        List<Order> orders = orderRepository.findCustomerOrders(customerId,
                org.springframework.data.domain.PageRequest.of(Math.max(0, page-1), Math.clamp(size, 1, 100)));
        return orders.stream()
                .map(this::mapToOrderDetailResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ConcurrencyTestResponse runConcurrencyTest() {
        log.info("=== BẮT ĐẦU KỊCH BẢN THỬ NGHIỆM TRANH CHẤP ĐỒNG THỜI (ACID CONCURRENCY TEST) ===");

        // 1. Chuẩn bị sản phẩm kiểm thử Flash Sale (Tồn kho đúng = 1)
        String testProductName = "Tôm hùm Alaska Flash Sale (Tồn = 1)";
        Product testProduct = productRepository.findByTenSP(testProductName)
                .orElseGet(() -> {
                    Product p = Product.builder()
                            .tenSP(testProductName)
                            .moTa("Sản phẩm đặc biệt phục vụ kiểm thử tranh chấp đồng thời và chống overselling")
                            .gia(new BigDecimal("100000.00"))
                            .donViTinh("con")
                            .soLuongTon(1)
                            .hanSuDung(LocalDateTime.now().plusDays(7))
                            .nhietDoBaoQuan("Bảo quản lạnh -18°C")
                            .maDanhMuc(3) // Thủy hải sản
                            .trangThaiXoa(false)
                            .ngayTao(LocalDateTime.now())
                            .ngayCapNhat(LocalDateTime.now())
                            .build();
                    return productRepository.save(p);
                });

        // Đảm bảo số lượng tồn đúng = 1 và giá = 100.000 VND
        testProduct.setSoLuongTon(1);
        testProduct.setGia(new BigDecimal("100000.00"));
        testProduct.setHanSuDung(LocalDateTime.now().plusDays(7));
        testProduct.setTrangThaiXoa(false);
        testProduct = productRepository.save(testProduct);
        Integer testProductId = testProduct.getMaSP();

        // 2. Chuẩn bị 2 khách hàng thử nghiệm (Khách 1 và Khách 2)
        Customer cust1 = customerRepository.findById(1)
                .orElseThrow(() -> new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tìm thấy khách hàng 1 để test"));
        Customer cust2 = customerRepository.findById(2)
                .orElseThrow(() -> new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tìm thấy khách hàng 2 để test"));

        // Nạp thêm tiền nếu số dư không đủ 100.000 VND
        if (cust1.getSoDuVi().compareTo(new BigDecimal("100000.00")) < 0) {
            orderSpRepository.executeNapTienVi(cust1.getMaKH(), new BigDecimal("200000.00"), "Bổ sung ví test tranh chấp");
        }
        if (cust2.getSoDuVi().compareTo(new BigDecimal("100000.00")) < 0) {
            orderSpRepository.executeNapTienVi(cust2.getMaKH(), new BigDecimal("200000.00"), "Bổ sung ví test tranh chấp");
        }

        // Lấy giỏ hàng của Khách 1 và Khách 2
        Cart cart1 = cartRepository.findByMaKH(cust1.getMaKH()).orElseGet(() -> {
            Cart c = Cart.builder().maKH(cust1.getMaKH()).ngayCapNhat(LocalDateTime.now()).build();
            return cartRepository.save(c);
        });
        Cart cart2 = cartRepository.findByMaKH(cust2.getMaKH()).orElseGet(() -> {
            Cart c = Cart.builder().maKH(cust2.getMaKH()).ngayCapNhat(LocalDateTime.now()).build();
            return cartRepository.save(c);
        });

        // Dọn dẹp giỏ hàng cũ và chỉ thêm 1 sản phẩm Flash Sale vào mỗi giỏ
        jdbcTemplate.update("DELETE FROM ChiTietGioHang WHERE MaGioHang IN (?, ?)", cart1.getMaGioHang(), cart2.getMaGioHang());
        jdbcTemplate.update("INSERT INTO ChiTietGioHang (MaGioHang, MaSP, SoLuong) VALUES (?, ?, 1)", cart1.getMaGioHang(), testProductId);
        jdbcTemplate.update("INSERT INTO ChiTietGioHang (MaGioHang, MaSP, SoLuong) VALUES (?, ?, 1)", cart2.getMaGioHang(), testProductId);

        // Ghi nhận số dư ví trước khi tranh chấp
        BigDecimal balanceBefore1 = readWalletBalance(cust1.getMaKH());
        BigDecimal balanceBefore2 = readWalletBalance(cust2.getMaKH());

        // 3. Kích hoạt 2 luồng cùng nhấn thanh toán đồng thời (1 mili-giây) bằng CountDownLatch
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(2);

        final Map<String, Object>[] resHolderA = new Map[1];
        final Map<String, Object>[] resHolderB = new Map[1];

        CompletableFuture.runAsync(() -> {
            try {
                startGate.await();
                resHolderA[0] = orderSpRepository.executeThanhToanDonHang(
                        cust1.getMaKH(),
                        cart1.getMaGioHang(),
                        "Địa chỉ test luồng A",
                        "Kiểm thử tranh chấp đồng thời Luồng A",
                        "BENCHMARK"
                );
            } catch (Exception e) {
                log.error("Lỗi trong Thread A: ", e);
            } finally {
                endGate.countDown();
            }
        });

        CompletableFuture.runAsync(() -> {
            try {
                startGate.await();
                resHolderB[0] = orderSpRepository.executeThanhToanDonHang(
                        cust2.getMaKH(),
                        cart2.getMaGioHang(),
                        "Địa chỉ test luồng B",
                        "Kiểm thử tranh chấp đồng thời Luồng B",
                        "BENCHMARK"
                );
            } catch (Exception e) {
                log.error("Lỗi trong Thread B: ", e);
            } finally {
                endGate.countDown();
            }
        });

        // Bắn tín hiệu bắt đầu cho cả 2 luồng
        startGate.countDown();

        try {
            endGate.await(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Quá thời gian chờ kiểm thử tranh chấp");
        }

        // 4. Lấy số dư ví sau giao dịch và số lượng tồn kho thực tế sau đối chứng
        BigDecimal balanceAfter1 = readWalletBalance(cust1.getMaKH());
        BigDecimal balanceAfter2 = readWalletBalance(cust2.getMaKH());
        int remainingStock = jdbcTemplate.queryForObject(
                "SELECT SoLuongTon FROM SanPham WHERE MaSP = ?", Integer.class, testProductId);

        Map<String, Object> rA = resHolderA[0];
        Map<String, Object> rB = resHolderB[0];

        Integer statusA = rA != null ? (Integer) rA.get("StatusCode") : -99;
        String msgA = rA != null ? (String) rA.get("Message") : "Không có phản hồi";
        Integer orderIdA = rA != null ? (Integer) rA.get("MaDH_Output") : 0;

        Integer statusB = rB != null ? (Integer) rB.get("StatusCode") : -99;
        String msgB = rB != null ? (String) rB.get("Message") : "Không có phản hồi";
        Integer orderIdB = rB != null ? (Integer) rB.get("MaDH_Output") : 0;

        boolean successA = statusA != null && statusA > 0;
        boolean successB = statusB != null && statusB > 0;

        ConcurrencyTestResponse.ThreadResult threadResultA = ConcurrencyTestResponse.ThreadResult.builder()
                .customerName(cust1.getHoTen())
                .customerId(cust1.getMaKH())
                .statusCode(statusA)
                .message(msgA)
                .orderId(orderIdA)
                .balanceBefore(balanceBefore1)
                .balanceAfter(balanceAfter1)
                .isSuccess(successA)
                .build();

        ConcurrencyTestResponse.ThreadResult threadResultB = ConcurrencyTestResponse.ThreadResult.builder()
                .customerName(cust2.getHoTen())
                .customerId(cust2.getMaKH())
                .statusCode(statusB)
                .message(msgB)
                .orderId(orderIdB)
                .balanceBefore(balanceBefore2)
                .balanceAfter(balanceAfter2)
                .isSuccess(successB)
                .build();

        boolean isOneSuccessOneRollback = (successA && !successB) || (!successA && successB);
        boolean isStockZero = remainingStock == 0;

        String evaluation = (isOneSuccessOneRollback && isStockZero)
                ? "PASS - Đảm bảo chuẩn ACID: Khóa dòng độc quyền (UPDLOCK, ROWLOCK) và SET XACT_ABORT ON thành công 100%. Đúng 1 đơn hàng được tạo, tồn kho về đúng 0 (không âm), luồng còn lại bị Rollback an toàn và bảo toàn số dư ví."
                : "FAIL - Có hiện tượng tranh chấp sai lệch (Overselling hoặc Lost Update). Vui lòng kiểm tra lại cấu hình Transaction.";

        log.info("Kết quả Concurrency Test: {}", evaluation);

        return ConcurrencyTestResponse.builder()
                .productId(testProductId)
                .productName(testProductName)
                .timestamp(LocalDateTime.now())
                .threadA(threadResultA)
                .threadB(threadResultB)
                .remainingStock(remainingStock)
                .acidEvaluation(evaluation)
                .build();
    }

    private OrderDetailResponse mapToOrderDetailResponse(Order order) {
        List<OrderDetailResponse.OrderItemDetailDto> itemDtos = new ArrayList<>();
        if (order.getItems() != null) {
            itemDtos = order.getItems().stream()
                    .map(item -> OrderDetailResponse.OrderItemDetailDto.builder()
                            .maSP(item.getId().getMaSP())
                            .tenSP(item.getProduct() != null ? item.getProduct().getTenSP() : "Sản phẩm #" + item.getId().getMaSP())
                            .soLuong(item.getSoLuong())
                            .donGia(item.getDonGia())
                            .giaGoc(item.getGiaGoc() != null ? item.getGiaGoc() : item.getDonGia())
                            .tienGiam(item.getTienGiam() != null ? item.getTienGiam() : BigDecimal.ZERO)
                            .maKM(item.getMaKM())
                            .thanhTien(item.getDonGia().multiply(BigDecimal.valueOf(item.getSoLuong())))
                            .build())
                    .collect(Collectors.toList());
        }

        Customer cust = order.getCustomer();
        return OrderDetailResponse.builder()
                .maDH(order.getMaDH())
                .maKH(order.getMaKH())
                .tenKhachHang(cust != null ? cust.getHoTen() : "Khách hàng #" + order.getMaKH())
                .soDienThoai(cust != null ? cust.getSoDienThoai() : "")
                .diaChiGiaoHang(order.getDiaChiGiaoHang())
                .ghiChu(order.getGhiChu())
                .ngayTao(order.getNgayTao())
                .tongTien(order.getTongTien())
                .trangThai(order.getTrangThai())
                .phuongThucThanhToan(order.getPhuongThucThanhToan())
                .nguonDon(order.getNguonDon())
                .items(itemDtos)
                .build();
    }

    private BigDecimal readWalletBalance(int customerId) {
        return jdbcTemplate.queryForObject("SELECT SoDuVi FROM KhachHang WHERE MaKH = ?", BigDecimal.class, customerId);
    }
}
