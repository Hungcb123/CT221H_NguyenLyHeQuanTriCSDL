-- DEMO ONLY: explicitly simulate new batches for expired, unchanged seed products.
-- Not run by init_db.sh. Preserves stock, prices, customer data and order history.
-- Only matches the original 60 seed names/descriptions, not admin-created products/types.
USE FreshGroceryDB;
GO
SET XACT_ABORT ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
DECLARE @Changed TABLE (MaSP INT, TenSP NVARCHAR(200), HanCu DATETIME2, HanMoi DATETIME2);
BEGIN TRANSACTION;
UPDATE p SET HanSuDung = DATEADD(DAY, demo.DaysFresh, GETDATE()), NgayCapNhat = GETDATE()
OUTPUT inserted.MaSP, inserted.TenSP, deleted.HanSuDung, inserted.HanSuDung INTO @Changed
FROM dbo.SanPham p
JOIN (VALUES
    (N'Cải thìa VietGAP', N'Cải thìa xanh giòn, dùng xào tỏi hoặc nấu canh.', 4),
    (N'Cải ngọt tươi', N'Cải ngọt non, phù hợp nấu canh thịt bằm.', 3),
    (N'Rau dền đỏ', N'Rau dền lá mềm, nấu canh tôm hoặc thịt.', 3),
    (N'Mồng tơi xanh', N'Mồng tơi tươi, dùng nấu canh cua hoặc canh tôm.', 3),
    (N'Bí đỏ hồ lô', N'Bí đỏ thịt dẻo, dùng nấu súp, canh hoặc cháo.', 14),
    (N'Bí đao xanh', N'Bí đao tươi, nấu canh xương hoặc hấp thịt.', 10),
    (N'Cà rốt Đà Lạt', N'Cà rốt giòn ngọt, dùng hầm xương hoặc làm salad.', 10),
    (N'Khoai tây vàng', N'Khoai tây bở thơm, chiên, nghiền hoặc hầm bò.', 14),
    (N'Dưa leo baby', N'Dưa leo giòn, dùng ăn kèm hoặc trộn salad.', 6),
    (N'Khổ qua xanh', N'Khổ qua tươi, dùng nhồi thịt hoặc xào trứng.', 5),
    (N'Nấm kim châm 150g', N'Nấm kim châm đóng gói, dùng nhúng lẩu hoặc cuộn bò.', 7),
    (N'Bông cải xanh', N'Bông cải xanh tươi, hấp hoặc xào thịt bò.', 5),
    (N'Sườn non heo', N'Sườn non chặt miếng, nấu canh hoặc rim chua ngọt.', 3),
    (N'Thịt nạc vai heo', N'Nạc vai mềm, dùng xay, kho hoặc nướng.', 3),
    (N'Thịt heo xay 500g', N'Thịt heo xay đóng khay, dùng nấu canh hoặc làm chả.', 2),
    (N'Chân giò heo', N'Chân giò cắt khoanh, hầm đậu hoặc nấu bún.', 3),
    (N'Ức gà phi lê', N'Ức gà bỏ da, dùng áp chảo hoặc trộn salad.', 3),
    (N'Đùi gà góc tư', N'Đùi gà tươi, thích hợp chiên nước mắm hoặc nướng.', 3),
    (N'Cánh gà tươi', N'Cánh gà làm sạch, chiên giòn hoặc rim mặn ngọt.', 3),
    (N'Gà ta nguyên con', N'Gà ta làm sạch, dùng luộc, hấp hoặc nấu cháo.', 2),
    (N'Thịt bò thăn', N'Thăn bò mềm, dùng xào rau củ hoặc áp chảo.', 3),
    (N'Trứng vịt vỉ 10 quả', N'Trứng vịt tươi, dùng kho thịt hoặc làm bánh.', 18),
    (N'Cá basa cắt khúc', N'Cá basa làm sạch, dùng kho tiêu hoặc chiên.', 2),
    (N'Cá diêu hồng tươi', N'Cá diêu hồng nguyên con làm sạch, hấp hoặc nấu lẩu.', 2),
    (N'Cá thu cắt lát', N'Cá thu thịt chắc, dùng sốt cà hoặc kho thơm.', 2),
    (N'Cá nục tươi', N'Cá nục biển, kho cà chua hoặc hấp cuốn bánh tráng.', 2),
    (N'Tôm thẻ tươi', N'Tôm thẻ làm sạch, hấp, rang hoặc nấu canh.', 2),
    (N'Mực ống tươi', N'Mực ống thịt giòn, hấp gừng hoặc xào chua ngọt.', 2),
    (N'Bạch tuộc tươi', N'Bạch tuộc làm sạch, nướng sa tế hoặc nhúng lẩu.', 2),
    (N'Nghêu trắng', N'Nghêu tươi, dùng hấp sả hoặc nấu canh chua.', 2),
    (N'Sò huyết tươi', N'Sò huyết tươi, dùng rang me hoặc nấu cháo.', 2),
    (N'Chả cá thác lác 300g', N'Chả cá quết sẵn, dùng chiên hoặc nấu canh.', 3),
    (N'Cam sành miền Tây', N'Cam sành mọng nước, dùng vắt nước hoặc ăn tươi.', 7),
    (N'Bưởi da xanh Bến Tre', N'Bưởi da xanh múi hồng, vị ngọt thanh.', 14),
    (N'Dưa hấu ruột đỏ', N'Dưa hấu ruột đỏ mọng nước, dùng tráng miệng.', 7),
    (N'Thanh long ruột đỏ', N'Thanh long ruột đỏ ngọt dịu, ăn tươi hoặc xay sinh tố.', 6),
    (N'Ổi nữ hoàng', N'Ổi giòn ít hạt, dùng ăn tươi hoặc ép nước.', 6),
    (N'Dứa chín gọt sẵn', N'Dứa chín gọt sạch mắt, dùng ăn tươi hoặc nấu canh.', 3),
    (N'Đu đủ chín', N'Đu đủ ruột đỏ mềm ngọt, dùng ăn tươi.', 4),
    (N'Chôm chôm nhãn', N'Chôm chôm thịt giòn, ngọt thơm, dễ tách hạt.', 4),
    (N'Quýt đường', N'Quýt vỏ mỏng dễ bóc, vị ngọt thanh.', 7),
    (N'Dừa xiêm Bến Tre', N'Dừa xiêm nước ngọt, dùng uống hoặc kho thịt.', 7),
    (N'Nước mắm cá cơm 500ml', N'Nước mắm cá cơm dùng nêm món kho và pha nước chấm.', 365),
    (N'Dầu ăn đậu nành 1 lít', N'Dầu đậu nành dùng chiên, xào các món gia đình.', 270),
    (N'Đường trắng 1kg', N'Đường trắng đóng gói, dùng nấu chè và nêm món ăn.', 365),
    (N'Muối i-ốt 500g', N'Muối i-ốt hạt mịn dùng nêm nếm hằng ngày.', 365),
    (N'Gạo thơm ST25 túi 5kg', N'Gạo thơm hạt dài, cơm dẻo, dùng cho bữa ăn gia đình.', 180),
    (N'Bún gạo khô 400g', N'Bún gạo khô dùng xào hoặc nấu nước dùng.', 180),
    (N'Cá lóc tươi làm sạch', N'Cá lóc đồng làm sạch mang và ruột, cắt khúc sẵn sàng chế biến món canh chua', 2),
    (N'Thịt ba rọi heo VietGAP', N'Thịt ba chỉ heo tươi mềm, tỷ lệ nạc mỡ cân đối, thích hợp kho tàu hoặc luộc', 3),
    (N'Rau muống nước sạch', N'Rau muống non ngọn xanh giòn, trồng thủy canh không hóa chất', 2),
    (N'Cà chua tươi Đà Lạt', N'Cà chua chín mọng đỏ tự nhiên, giàu vitamin C và khoáng chất', 5),
    (N'Trứng gà ta Ba Huân (vỉ 10)', N'Trứng gà tươi sạch tiệt trùng UV, lòng đỏ béo ngậy', 20),
    (N'Tôm sú tươi sống', N'Tôm sú sinh thái Cà Mau thịt chắc ngọt tự nhiên', 2),
    (N'Bắp cải trắng Đà Lạt', N'Bắp cải cuộn chặt búp, giòn ngọt, thích hợp xào hoặc nấu canh thịt bằm', 7),
    (N'Chuối sứ chín tự nhiên', N'Chuối sứ Bến Tre dẻo thơm, ngọt dịu tráng miệng', 4),
    (N'Cá hồi phi lê Nauy tươi', N'Thịt cá hồi màu cam tươi đẹp mắt, giàu Omega-3, ăn sống Sashimi hoặc áp chảo', 2),
    (N'Thịt bò bắp Úc tươi', N'Bắp bò giòn sần sật có vân hoa đẹp mắt, thích hợp nhúng lẩu hoặc hầm tiêu', 4),
    (N'Xoài cát Hòa Lộc', N'Xoài cát Tiền Giang vỏ vàng mịn, vị ngọt đậm đà hương thơm đặc trưng', 5),
    (N'Đậu hũ non trứng', N'Đậu hũ non mềm mịn béo ngậy, thích hợp nấu canh rong biển hoặc chiên giòn', 10)
) demo(TenSP, MoTa, DaysFresh) ON p.TenSP = demo.TenSP AND p.MoTa = demo.MoTa
WHERE p.MaSPCha IS NULL AND p.TrangThaiXoa = 0 AND p.HanSuDung <= GETDATE();
COMMIT TRANSACTION;
SELECT * FROM @Changed ORDER BY MaSP;
GO
