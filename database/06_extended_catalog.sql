-- Bổ sung 48 sản phẩm mẫu. Chạy lại không thêm trùng hoặc sửa sản phẩm hiện có.
USE FreshGroceryDB;
GO
SET XACT_ABORT ON;
BEGIN TRANSACTION;

INSERT INTO dbo.SanPham
    (TenSP, MoTa, Gia, DonViTinh, SoLuongTon, HanSuDung,
     NhietDoBaoQuan, MaDanhMuc, TrangThaiXoa, NgayTao, NgayCapNhat)
SELECT s.TenSP, s.MoTa, s.Gia, s.DonViTinh, s.SoLuongTon,
       DATEADD(DAY, s.SoNgayBaoQuan, GETDATE()),
       CASE WHEN s.Nhom = N'Gia vị & Đồ khô bách hóa' THEN N'Nơi khô ráo, thoáng mát'
            WHEN s.Nhom IN (N'Thịt tươi sống mỗi ngày', N'Thủy hải sản tươi sống') THEN N'Ngăn mát 0 - 2°C'
            ELSE N'Ngăn mát 4 - 8°C' END,
       dm.MaDanhMuc, 0, GETDATE(), GETDATE()
FROM (VALUES
    (N'Cải thìa VietGAP', N'Cải thìa xanh giòn, dùng xào tỏi hoặc nấu canh.', 18000, N'bó', 70, 4, N'Rau củ quả tươi sạch'),
    (N'Cải ngọt tươi', N'Cải ngọt non, phù hợp nấu canh thịt bằm.', 15000, N'bó', 85, 3, N'Rau củ quả tươi sạch'),
    (N'Rau dền đỏ', N'Rau dền lá mềm, nấu canh tôm hoặc thịt.', 12000, N'bó', 60, 3, N'Rau củ quả tươi sạch'),
    (N'Mồng tơi xanh', N'Mồng tơi tươi, dùng nấu canh cua hoặc canh tôm.', 14000, N'bó', 65, 3, N'Rau củ quả tươi sạch'),
    (N'Bí đỏ hồ lô', N'Bí đỏ thịt dẻo, dùng nấu súp, canh hoặc cháo.', 28000, N'kg', 45, 14, N'Rau củ quả tươi sạch'),
    (N'Bí đao xanh', N'Bí đao tươi, nấu canh xương hoặc hấp thịt.', 22000, N'kg', 55, 10, N'Rau củ quả tươi sạch'),
    (N'Cà rốt Đà Lạt', N'Cà rốt giòn ngọt, dùng hầm xương hoặc làm salad.', 32000, N'kg', 60, 10, N'Rau củ quả tươi sạch'),
    (N'Khoai tây vàng', N'Khoai tây bở thơm, chiên, nghiền hoặc hầm bò.', 35000, N'kg', 70, 14, N'Rau củ quả tươi sạch'),
    (N'Dưa leo baby', N'Dưa leo giòn, dùng ăn kèm hoặc trộn salad.', 30000, N'kg', 55, 6, N'Rau củ quả tươi sạch'),
    (N'Khổ qua xanh', N'Khổ qua tươi, dùng nhồi thịt hoặc xào trứng.', 26000, N'kg', 40, 5, N'Rau củ quả tươi sạch'),
    (N'Nấm kim châm 150g', N'Nấm kim châm đóng gói, dùng nhúng lẩu hoặc cuộn bò.', 16000, N'gói', 90, 7, N'Rau củ quả tươi sạch'),
    (N'Bông cải xanh', N'Bông cải xanh tươi, hấp hoặc xào thịt bò.', 48000, N'kg', 35, 5, N'Rau củ quả tươi sạch'),
    (N'Sườn non heo', N'Sườn non chặt miếng, nấu canh hoặc rim chua ngọt.', 165000, N'kg', 35, 3, N'Thịt tươi sống mỗi ngày'),
    (N'Thịt nạc vai heo', N'Nạc vai mềm, dùng xay, kho hoặc nướng.', 125000, N'kg', 45, 3, N'Thịt tươi sống mỗi ngày'),
    (N'Thịt heo xay 500g', N'Thịt heo xay đóng khay, dùng nấu canh hoặc làm chả.', 65000, N'khay', 50, 2, N'Thịt tươi sống mỗi ngày'),
    (N'Chân giò heo', N'Chân giò cắt khoanh, hầm đậu hoặc nấu bún.', 115000, N'kg', 25, 3, N'Thịt tươi sống mỗi ngày'),
    (N'Ức gà phi lê', N'Ức gà bỏ da, dùng áp chảo hoặc trộn salad.', 90000, N'kg', 45, 3, N'Thịt tươi sống mỗi ngày'),
    (N'Đùi gà góc tư', N'Đùi gà tươi, thích hợp chiên nước mắm hoặc nướng.', 78000, N'kg', 55, 3, N'Thịt tươi sống mỗi ngày'),
    (N'Cánh gà tươi', N'Cánh gà làm sạch, chiên giòn hoặc rim mặn ngọt.', 95000, N'kg', 40, 3, N'Thịt tươi sống mỗi ngày'),
    (N'Gà ta nguyên con', N'Gà ta làm sạch, dùng luộc, hấp hoặc nấu cháo.', 185000, N'con', 20, 2, N'Thịt tươi sống mỗi ngày'),
    (N'Thịt bò thăn', N'Thăn bò mềm, dùng xào rau củ hoặc áp chảo.', 320000, N'kg', 25, 3, N'Thịt tươi sống mỗi ngày'),
    (N'Trứng vịt vỉ 10 quả', N'Trứng vịt tươi, dùng kho thịt hoặc làm bánh.', 42000, N'vỉ', 65, 18, N'Thịt tươi sống mỗi ngày'),
    (N'Cá basa cắt khúc', N'Cá basa làm sạch, dùng kho tiêu hoặc chiên.', 65000, N'kg', 40, 2, N'Thủy hải sản tươi sống'),
    (N'Cá diêu hồng tươi', N'Cá diêu hồng nguyên con làm sạch, hấp hoặc nấu lẩu.', 68000, N'kg', 35, 2, N'Thủy hải sản tươi sống'),
    (N'Cá thu cắt lát', N'Cá thu thịt chắc, dùng sốt cà hoặc kho thơm.', 185000, N'kg', 25, 2, N'Thủy hải sản tươi sống'),
    (N'Cá nục tươi', N'Cá nục biển, kho cà chua hoặc hấp cuốn bánh tráng.', 55000, N'kg', 45, 2, N'Thủy hải sản tươi sống'),
    (N'Tôm thẻ tươi', N'Tôm thẻ làm sạch, hấp, rang hoặc nấu canh.', 165000, N'kg', 35, 2, N'Thủy hải sản tươi sống'),
    (N'Mực ống tươi', N'Mực ống thịt giòn, hấp gừng hoặc xào chua ngọt.', 240000, N'kg', 25, 2, N'Thủy hải sản tươi sống'),
    (N'Bạch tuộc tươi', N'Bạch tuộc làm sạch, nướng sa tế hoặc nhúng lẩu.', 195000, N'kg', 25, 2, N'Thủy hải sản tươi sống'),
    (N'Nghêu trắng', N'Nghêu tươi, dùng hấp sả hoặc nấu canh chua.', 45000, N'kg', 50, 2, N'Thủy hải sản tươi sống'),
    (N'Sò huyết tươi', N'Sò huyết tươi, dùng rang me hoặc nấu cháo.', 135000, N'kg', 30, 2, N'Thủy hải sản tươi sống'),
    (N'Chả cá thác lác 300g', N'Chả cá quết sẵn, dùng chiên hoặc nấu canh.', 68000, N'hộp', 40, 3, N'Thủy hải sản tươi sống'),
    (N'Cam sành miền Tây', N'Cam sành mọng nước, dùng vắt nước hoặc ăn tươi.', 35000, N'kg', 80, 7, N'Trái cây tươi nhiệt đới'),
    (N'Bưởi da xanh Bến Tre', N'Bưởi da xanh múi hồng, vị ngọt thanh.', 65000, N'kg', 45, 14, N'Trái cây tươi nhiệt đới'),
    (N'Dưa hấu ruột đỏ', N'Dưa hấu ruột đỏ mọng nước, dùng tráng miệng.', 18000, N'kg', 100, 7, N'Trái cây tươi nhiệt đới'),
    (N'Thanh long ruột đỏ', N'Thanh long ruột đỏ ngọt dịu, ăn tươi hoặc xay sinh tố.', 32000, N'kg', 55, 6, N'Trái cây tươi nhiệt đới'),
    (N'Ổi nữ hoàng', N'Ổi giòn ít hạt, dùng ăn tươi hoặc ép nước.', 28000, N'kg', 60, 6, N'Trái cây tươi nhiệt đới'),
    (N'Dứa chín gọt sẵn', N'Dứa chín gọt sạch mắt, dùng ăn tươi hoặc nấu canh.', 22000, N'trái', 40, 3, N'Trái cây tươi nhiệt đới'),
    (N'Đu đủ chín', N'Đu đủ ruột đỏ mềm ngọt, dùng ăn tươi.', 25000, N'kg', 45, 4, N'Trái cây tươi nhiệt đới'),
    (N'Chôm chôm nhãn', N'Chôm chôm thịt giòn, ngọt thơm, dễ tách hạt.', 45000, N'kg', 35, 4, N'Trái cây tươi nhiệt đới'),
    (N'Quýt đường', N'Quýt vỏ mỏng dễ bóc, vị ngọt thanh.', 48000, N'kg', 50, 7, N'Trái cây tươi nhiệt đới'),
    (N'Dừa xiêm Bến Tre', N'Dừa xiêm nước ngọt, dùng uống hoặc kho thịt.', 18000, N'trái', 75, 7, N'Trái cây tươi nhiệt đới'),
    (N'Nước mắm cá cơm 500ml', N'Nước mắm cá cơm dùng nêm món kho và pha nước chấm.', 45000, N'chai', 90, 365, N'Gia vị & Đồ khô bách hóa'),
    (N'Dầu ăn đậu nành 1 lít', N'Dầu đậu nành dùng chiên, xào các món gia đình.', 52000, N'chai', 80, 270, N'Gia vị & Đồ khô bách hóa'),
    (N'Đường trắng 1kg', N'Đường trắng đóng gói, dùng nấu chè và nêm món ăn.', 26000, N'gói', 100, 365, N'Gia vị & Đồ khô bách hóa'),
    (N'Muối i-ốt 500g', N'Muối i-ốt hạt mịn dùng nêm nếm hằng ngày.', 8000, N'gói', 120, 365, N'Gia vị & Đồ khô bách hóa'),
    (N'Gạo thơm ST25 túi 5kg', N'Gạo thơm hạt dài, cơm dẻo, dùng cho bữa ăn gia đình.', 175000, N'túi', 60, 180, N'Gia vị & Đồ khô bách hóa'),
    (N'Bún gạo khô 400g', N'Bún gạo khô dùng xào hoặc nấu nước dùng.', 28000, N'gói', 75, 180, N'Gia vị & Đồ khô bách hóa')
) AS s(TenSP, MoTa, Gia, DonViTinh, SoLuongTon, SoNgayBaoQuan, Nhom)
JOIN dbo.DanhMuc dm ON dm.TenDanhMuc = s.Nhom
WHERE NOT EXISTS (SELECT 1 FROM dbo.SanPham p WITH (UPDLOCK, HOLDLOCK) WHERE p.TenSP = s.TenSP);

COMMIT TRANSACTION;
GO
