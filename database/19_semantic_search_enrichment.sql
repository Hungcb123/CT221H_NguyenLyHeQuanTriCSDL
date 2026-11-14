-- =========================================================================
-- FILE: 19_semantic_search_enrichment.sql
-- Bổ sung mặt hàng thiết yếu còn thiếu, làm giàu ngữ cảnh ẩm thực/dinh dưỡng
-- và công thức món ăn phục vụ Semantic Search & AI Chatbot.
-- Chạy lại an toàn (idempotent), giữ nguyên giá/tồn kho/xóa mềm hiện có.
-- =========================================================================

USE FreshGroceryDB;
GO

SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET XACT_ABORT ON;

BEGIN TRY
BEGIN TRANSACTION;

-- 1. BỔ SUNG 23 MẶT HÀNG TƯƠI SỐNG & GIA VỊ THIẾT YẾU CÒN THIẾU
INSERT INTO dbo.SanPham
    (TenSP, MoTa, HinhAnh, Gia, DonViTinh, SoLuongTon, HanSuDung,
     NhietDoBaoQuan, MaDanhMuc, TrangThaiXoa, NgayTao, NgayCapNhat)
SELECT
    v.TenSP,
    v.MoTa,
    v.HinhAnh,
    v.Gia,
    v.DonViTinh,
    v.SoLuongTon,
    DATEADD(DAY, v.SoNgayBaoQuan, GETDATE()),
    v.NhietDoBaoQuan,
    v.MaDanhMuc,
    0,
    GETDATE(),
    GETDATE()
FROM (VALUES
    -- Danh mục 1: Rau củ quả tươi sạch
    (N'Cần tây Đà Lạt',
     N'Cần tây cọng lớn giòn mát từ Đà Lạt, chuyên ép nước thanh lọc cơ thể detox giảm cân giảm mỡ hoặc xào thịt bò, xào mực. Tên gọi khác: rau cần tây, celery.',
     N'/api/v1/product-images/semantic-01.jpg', 35000, N'bó', 45, 5, N'Ngăn mát 4 - 8°C', 1),
    (N'Xà lách lô lô xanh',
     N'Rau xà lách lá giòn ngọt tự nhiên, dùng trộn salad giảm cân eat clean, ăn sống cuốn thịt nướng, gỏi cuốn tôm thịt hoặc ăn sáng. Tên gọi khác: rau xà lách, rau sống, lettuce.',
     N'/api/v1/product-images/semantic-02.jpg', 22000, N'bó', 60, 4, N'Ngăn mát 4 - 8°C', 1),
    (N'Cải thảo Đà Lạt',
     N'Cải thảo búp chắc ngọt mát, thích hợp nhúng lẩu hải sản, lẩu bò, nấu canh thịt bằm hoặc muối kim chi. Tên gọi khác: bắp cải thảo, cải cuốn, napa cabbage.',
     N'/api/v1/product-images/semantic-03.png', 24000, N'bắp', 50, 7, N'Ngăn mát 4 - 8°C', 1),
    (N'Khoai lang mật Đà Lạt',
     N'Khoai lang mật dẻo ngọt giàu chất xơ tinh bột chậm, phù hợp luộc hoặc nướng cho bữa sáng nhanh gọn, thực đơn giảm cân eat clean và tập gym. Tên gọi khác: khoai lang, sweet potato.',
     N'/api/v1/product-images/semantic-04.jpg', 38000, N'kg', 65, 14, N'Nơi khô thoáng mát', 1),
    (N'Nấm đùi gà tươi',
     N'Nấm đùi gà thân dày giòn ngọt giàu đạm thực vật, chuyên nhúng lẩu, xào dầu hào, nướng BBQ hoặc kho tiêu chay thanh đạm. Tên gọi khác: nấm đùi gà, king oyster mushroom.',
     N'/api/v1/product-images/semantic-05.jpg', 36000, N'gói', 55, 7, N'Ngăn mát 2 - 5°C', 1),
    (N'Nấm rơm tươi miền Tây',
     N'Nấm rơm búp tròn ngọt tự nhiên, chuyên nấu cháo, kho tộ chay, nấu lẩu hoặc xào rau củ. Tên gọi khác: nấm rơm, straw mushroom.',
     N'/api/v1/product-images/semantic-06.jpg', 45000, N'khay', 40, 3, N'Ngăn mát 4 - 8°C', 1),
    (N'Giá đỗ sạch ủ mầm',
     N'Giá đỗ trắng giòn không hóa chất, dùng nấu canh chua miền Tây, ăn kèm phở bò, gỏi cuốn hoặc xào thịt bò. Tên gọi khác: giá sống, giá đỗ, bean sprouts.',
     N'/api/v1/product-images/semantic-07.jpg', 10000, N'túi', 75, 3, N'Ngăn mát 4 - 8°C', 1),
    (N'Bạc hà nấu canh chua',
     N'Bạc hà tước vỏ giòn xốp thấm nước dùng, nguyên liệu đặc trưng nấu canh chua cá lóc miền Tây và canh bún. Tên gọi khác: dọc mùng, bạc hà canh chua.',
     N'/api/v1/product-images/semantic-08.jpg', 12000, N'bó', 50, 4, N'Ngăn mát 4 - 8°C', 1),
    (N'Ớt chuông Đà Lạt',
     N'Ớt chuông giòn ngọt giàu vitamin C không cay, chuyên xào bò lúc lắc, xào mực chua ngọt, nướng BBQ hoặc trộn salad giảm cân. Tên gọi khác: ớt ngọt, bell pepper.',
     N'/api/v1/product-images/semantic-09.jpg', 55000, N'kg', 45, 7, N'Ngăn mát 4 - 8°C', 1),
    (N'Đậu hũ trắng truyền thống',
     N'Đậu hũ trắng mềm béo giàu protein thực vật, thích hợp chiên sả ớt, nhồi thịt sốt cà chua, kho nấm chay thanh đạm hoặc nhúng lẩu. Tên gọi khác: đậu phụ trắng, tàu hũ, tofu.',
     N'/api/v1/product-images/semantic-10.jpg', 12000, N'hộp', 65, 5, N'Ngăn mát 2 - 5°C', 1),

    -- Danh mục 4: Trái cây tươi nhiệt đới
    (N'Táo xanh Mỹ tươi giòn',
     N'Táo xanh vị chua thanh giòn mát giàu vitamin C và chất xơ, chuyên ép nước cùng cần tây thanh lọc cơ thể detox giảm cân giảm mỡ hoặc trộn salad. Tên gọi khác: táo xanh, green apple.',
     N'/api/v1/product-images/semantic-11.jpg', 75000, N'kg', 45, 14, N'Ngăn mát 2 - 6°C', 4),
    (N'Bơ sáp Đắk Lắk',
     N'Bơ sáp ruột vàng dẻo béo giàu chất béo tốt và năng lượng, phù hợp xay sinh tố, ăn sáng nhanh gọn, trộn salad eat clean và tập gym. Tên gọi khác: quả bơ, trái bơ, avocado.',
     N'/api/v1/product-images/semantic-12.jpg', 62000, N'kg', 40, 5, N'Nhiệt độ phòng thoáng mát', 4),
    (N'Chanh tươi không hạt',
     N'Chanh tươi mọng nước thơm vỏ giàu vitamin C tăng đề kháng, dùng pha nước chanh detox giải khát, vắt ăn phở bò, pha nước chấm hải sản hoặc trộn gỏi. Tên gọi khác: trái chanh, chanh giấy, lime.',
     N'/api/v1/product-images/semantic-13.jpg', 28000, N'kg', 70, 10, N'Ngăn mát 4 - 8°C', 4),

    -- Danh mục 3: Thủy hải sản tươi sống
    (N'Cua biển Cà Mau',
     N'Cua biển thịt chắc ngọt tự nhiên giàu đạm và canxi, thích hợp hấp bia sả, rang me, nấu lẩu hải sản chua cay hoặc nấu cháo cua bổ dưỡng. Tên gọi khác: cua thịt, cua Cà Mau, crab.',
     N'/api/v1/product-images/semantic-14.jpg', 340000, N'kg', 18, 2, N'Bảo quản mát 4 - 8°C', 3),

    -- Danh mục 5: Gia vị & Đồ khô bách hóa
    (N'Tỏi ta Lý Sơn 250g',
     N'Tỏi ta tép nhỏ thơm nồng tinh dầu tự nhiên, gia vị thiết yếu để phi thơm xào rau muống, ướp thịt kho tàu, bò lúc lắc và pha nước mắm chua ngọt. Tên gọi khác: tỏi khô, củ tỏi, garlic.',
     N'/api/v1/product-images/semantic-15.jpg', 32000, N'túi', 85, 60, N'Nơi khô ráo, thoáng mát', 5),
    (N'Hành tím Vĩnh Châu 250g',
     N'Hành tím thơm dịu ngọt củ, dùng làm hành phi giòn, ướp thịt kho tàu, cá kho tộ miền Tây và ướp đồ nướng. Tên gọi khác: hành khô, củ hành tím, shallot.',
     N'/api/v1/product-images/semantic-16.jpg', 22000, N'túi', 85, 60, N'Nơi khô ráo, thoáng mát', 5),
    (N'Gừng tươi rửa sạch 250g',
     N'Gừng tươi thơm ấm vị cay nhẹ, dùng hấp mực, hấp cá khử mùi tanh, kho gà, nấu nước dùng phở bò hoặc pha trà gừng ấm bụng giải cảm. Tên gọi khác: củ gừng, ginger.',
     N'/api/v1/product-images/semantic-17.jpg', 16000, N'túi', 75, 20, N'Nơi khô ráo, thoáng mát', 5),
    (N'Sả cây tươi 300g',
     N'Sả cây tươi thơm nồng tinh dầu, dùng làm nghêu hấp sả, nấu lẩu hải sản chua cay, ướp gà nướng sả ớt hoặc đậu hũ chiên sả. Tên gọi khác: cây sả, củ sả, lemongrass.',
     N'/api/v1/product-images/semantic-18.jpg', 12000, N'bó', 80, 10, N'Ngăn mát 4 - 8°C', 5),
    (N'Ớt hiểm đỏ tươi 100g',
     N'Ớt hiểm cay thơm tự nhiên, dùng kho cá kho tộ, nêm canh chua miền Tây, nấu lẩu Thái hoặc dầm nước mắm tỏi ớt. Tên gọi khác: ớt chỉ thiên, ớt tươi, ớt cay, chili.',
     N'/api/v1/product-images/semantic-19.jpg', 12000, N'túi', 80, 7, N'Ngăn mát 4 - 8°C', 5),
    (N'Me vắt nấu canh chua 200g',
     N'Me vắt chua thanh tự nhiên tách hạt, nguyên liệu cốt lõi tạo vị chua dịu cho canh chua cá lóc miền Tây, lẩu hải sản chua cay hoặc sốt rang me. Tên gọi khác: me chua, cốt me, tamarind.',
     N'/api/v1/product-images/semantic-20.jpg', 18000, N'gói', 90, 180, N'Nơi khô ráo, thoáng mát', 5),
    (N'Nước tương đậu nành 500ml',
     N'Nước tương lên men tự nhiên đậm vị, dùng chấm rau củ luộc, ướp bò lúc lắc, xào mì hoặc kho món chay thanh đạm. Tên gọi khác: xì dầu, soy sauce.',
     N'/api/v1/product-images/semantic-21.jpg', 26000, N'chai', 95, 365, N'Nơi khô ráo, thoáng mát', 5),
    (N'Hạt nêm thịt thăn 400g',
     N'Hạt nêm ngọt thanh từ thịt thăn và xương ống, dùng nêm canh, xào, kho cho bữa cơm gia đình đậm đà. Tên gọi khác: bột nêm, gia vị nêm nấu ăn.',
     N'/api/v1/product-images/semantic-22.jpg', 36000, N'gói', 95, 365, N'Nơi khô ráo, thoáng mát', 5),
    (N'Tiêu đen xay Phú Quốc 100g',
     N'Hạt tiêu đen Phú Quốc xay thơm cay nồng, dùng ướp cá basa kho tiêu, cá kho tộ, bò lúc lắc, thịt kho tàu và rắc cháo nóng. Tên gọi khác: hạt tiêu, tiêu xay, black pepper.',
     N'/api/v1/product-images/semantic-23.jpg', 34000, N'hũ', 80, 365, N'Nơi khô ráo, thoáng mát', 5)
) AS v(TenSP, MoTa, HinhAnh, Gia, DonViTinh, SoLuongTon, SoNgayBaoQuan, NhietDoBaoQuan, MaDanhMuc)
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.SanPham sp WITH (UPDLOCK, HOLDLOCK)
    WHERE sp.TenSP = v.TenSP AND sp.MaSPCha IS NULL
);

-- 2. LÀM GIÀU MÔ TẢ ẨM THỰC & DINH DƯỠNG CHO CÁC SẢN PHẨM HIỆN CÓ (CHỈ NỐI MỘT LẦN)
DECLARE @Enrich TABLE (TenSP NVARCHAR(200), Extra NVARCHAR(500));
INSERT INTO @Enrich (TenSP, Extra) VALUES
(N'Cá lóc tươi làm sạch', N'Nguyên liệu chính nấu canh chua cá lóc miền Tây, cá lóc kho tộ, nấu cháo cá hoặc hấp bầu; giàu protein ít béo.'),
(N'Thịt ba rọi heo VietGAP', N'Nguyên liệu nấu thịt kho tàu trứng, luộc cuốn bánh tráng gỏi cuốn tôm thịt hoặc nướng BBQ.'),
(N'Rau muống nước sạch', N'Dùng xào tỏi, luộc chấm nước tương, nấu canh chua miền Tây hoặc nhúng lẩu.'),
(N'Cà chua tươi Đà Lạt', N'Chuyên dùng làm sốt cà chua, nấu canh chua miền Tây, xào trứng, hầm bò hoặc trộn salad đẹp da.'),
(N'Trứng gà ta Ba Huân (vỉ 10)', N'Thực phẩm giàu protein tiện lợi cho bữa sáng nhanh gọn, chiên ốp la, kho tàu hoặc tập gym eat clean.'),
(N'Tôm sú tươi sống', N'Hải sản giàu protein và canxi; chuyên hấp bia, nướng muối ớt, nấu lẩu hải sản chua cay hoặc cuốn gỏi cuốn tôm thịt.'),
(N'Bắp cải trắng Đà Lạt', N'Rau ăn kèm món luộc, xào tỏi, nhúng lẩu hoặc nấu canh thịt bằm thanh đạm.'),
(N'Chuối sứ chín tự nhiên', N'Giàu kali và năng lượng cho bữa sáng nhanh gọn, người tập gym, ăn kiêng hoặc tráng miệng.'),
(N'Cá hồi phi lê Nauy tươi', N'Thực phẩm giàu protein và Omega-3 hàng đầu cho người tập gym, giảm cân eat clean, làm sốt bơ chanh hoặc nấu cháo.'),
(N'Thịt bò bắp Úc tươi', N'Giàu protein và sắt; chuyên nấu phở bò, nhúng lẩu bò, bò kho hoặc hầm rau củ.'),
(N'Xoài cát Hòa Lộc', N'Trái cây nhiệt đới giàu vitamin C, ăn tươi tráng miệng hoặc xay sinh tố giải nhiệt.'),
(N'Đậu hũ non trứng', N'Giàu đạm thực vật thanh đạm, dùng nấu canh rong biển, hấp trứng, nhúng lẩu hoặc thực đơn chay.'),
(N'Ức gà phi lê', N'Thực phẩm giàu protein ít béo cho người tập gym, giảm cân, eat clean và bữa ăn lành mạnh.'),
(N'Thịt bò thăn', N'Giàu protein mềm ngọt; chuyên làm bò lúc lắc ớt chuông, bít tết áp chảo, nấu phở bò tái hoặc xào bông cải.'),
(N'Cá basa cắt khúc', N'Thịt cá béo mềm chuyên nấu cá kho tộ miền Tây, kho tiêu hoặc nấu canh chua.'),
(N'Cá diêu hồng tươi', N'Chuyên nấu lẩu hải sản chua cay, canh chua cá diêu hồng, chưng tương hoặc hấp hành gừng.'),
(N'Cá thu cắt lát', N'Giàu Omega-3 và protein, chuyên làm cá thu sốt cà chua, kho thơm hoặc áp chảo.'),
(N'Tôm thẻ tươi', N'Giàu đạm, chuyên làm gỏi cuốn tôm thịt, rim mặn ngọt, nấu canh bầu bí hoặc nhúng lẩu hải sản.'),
(N'Mực ống tươi', N'Hải sản giàu protein ít mỡ, chuyên hấp gừng sả, xào chua ngọt, nướng sa tế hoặc nhúng lẩu hải sản.'),
(N'Bạch tuộc tươi', N'Chuyên nhúng lẩu hải sản chua cay, nướng sa tế BBQ hoặc xào cay. Tên gọi khác: octopus.'),
(N'Nghêu trắng', N'Nguyên liệu nấu lẩu hải sản, nghêu hấp sả ớt hoặc canh chua thanh mát. Tên gọi khác: ngao trắng, con ngao, clam.'),
(N'Bông cải xanh', N'Giàu chất xơ, vitamin C và khoáng chất cho thực đơn eat clean giảm cân, tập gym, xào thịt bò hoặc luộc. Tên gọi khác: súp lơ xanh, broccoli.'),
(N'Sò huyết tươi', N'Giàu sắt bổ máu, chuyên làm sò huyết rang me, cháy tỏi hoặc nấu cháo hải sản.'),
(N'Cam sành miền Tây', N'Trái cây giàu vitamin C tăng sức đề kháng, ép nước cam tươi giải khát thanh lọc.'),
(N'Bưởi da xanh Bến Tre', N'Trái cây giàu vitamin C ít calo hỗ trợ giảm cân detox, ăn tráng miệng hoặc trộn gỏi tôm thịt.'),
(N'Dưa hấu ruột đỏ', N'Trái cây mọng nước ép nước giải khát thanh nhiệt hoặc tráng miệng.'),
(N'Thanh long ruột đỏ', N'Trái cây giàu chất xơ và vitamin C tốt cho tiêu hóa, ăn tươi hoặc xay sinh tố.'),
(N'Ổi nữ hoàng', N'Trái cây rất giàu vitamin C ít đường hỗ trợ giảm cân eat clean, ăn tươi hoặc ép nước detox.'),
(N'Dứa chín gọt sẵn', N'Nguyên liệu nấu canh chua miền Tây, kho cá, xào chua ngọt hoặc ép nước detox giảm cân giàu vitamin C.'),
(N'Quýt đường', N'Trái cây giàu vitamin C ngọt thanh tăng đề kháng, tráng miệng tiện lợi.'),
(N'Dừa xiêm Bến Tre', N'Nước dừa tươi giải khát bù khoáng, dùng kho thịt kho tàu mềm ngon chuẩn miền Tây.'),
(N'Bún gạo khô 400g', N'Dùng làm bún ăn sáng nhanh gọn, cuốn gỏi cuốn tôm thịt hoặc bún xào.'),
(N'Dưa leo baby', N'Ít calo thanh mát, dùng ép nước detox giảm cân, ăn kèm cơm sườn hoặc trộn salad.');

UPDATE sp
SET sp.MoTa = CASE
        WHEN NULLIF(sp.MoTa, N'') IS NULL THEN e.Extra
        ELSE sp.MoTa + N' ' + e.Extra
    END,
    sp.NgayCapNhat = GETDATE()
FROM dbo.SanPham sp
JOIN @Enrich e ON sp.TenSP = e.TenSP AND sp.MaSPCha IS NULL AND sp.TrangThaiXoa = 0
WHERE CHARINDEX(e.Extra, ISNULL(sp.MoTa, N'')) = 0;

-- 3. BỔ SUNG CÔNG THỨC MÓN ĂN PHỔ BIẾN CHO CHATBOT & SEMANTIC SEARCH
DECLARE @NewRecipes TABLE (Name NVARCHAR(150), Description NVARCHAR(500), Instructions NVARCHAR(MAX));
INSERT INTO @NewRecipes VALUES
(N'Lẩu hải sản chua cay',
 N'Nồi lẩu hải sản chua cay đậm đà từ tôm sú, mực ống, nghêu trắng cùng nấm kim châm, cải thảo và sả me thơm lừng.',
 N'1. Phi thơm sả, tỏi, ớt rồi nấu nước dùng chua cay với me vắt và cà chua, dứa. 2. Bày tôm sú, mực ống, nghêu trắng ra đĩa. 3. Khi ăn nhúng hải sản cùng nấm kim châm và cải thảo tươi.'),
(N'Phở bò truyền thống',
 N'Tô phở bò nóng hổi thơm gừng nướng, thịt bò bắp và bò thăn mềm ngọt ăn kèm giá đỗ, hành lá, ngò gai và húng quế.',
 N'1. Nướng thơm gừng và hành tím nấu nước dùng bò đậm vị. 2. Chần bánh phở/bún, xếp thịt bò bắp và bò thăn thái mỏng lên trên. 3. Chan nước dùng sôi, thêm hành lá, ngò gai, húng quế, giá đỗ và vắt chanh tươi.'),
(N'Bò lúc lắc ớt chuông',
 N'Thịt bò thăn cắt vuông áp chảo lửa lớn mềm mọng cùng ớt chuông Đà Lạt và tỏi thơm.',
 N'1. Thịt bò thăn cắt khối vuông 2cm, ướp tỏi băm, nước tương, tiêu đen 15 phút. 2. Ớt chuông và cà chua cắt miếng vuông. 3. Áp chảo bò lửa lớn nhanh tay cho xém cạnh rồi đảo cùng ớt chuông.'),
(N'Cá lóc kho tộ miền Tây',
 N'Cá lóc đồng kho tộ đậm đà màu cánh gián cay thơm tiêu đen, ớt hiểm và hành lá.',
 N'1. Cá lóc cắt khúc ướp nước mắm cá cơm, đường, hành tím, tỏi, tiêu đen 20 phút. 2. Thắng nước màu trong tộ, xếp cá vào kho lửa nhỏ cùng nước dừa tươi. 3. Khi nước kho sánh kẹo, thêm ớt hiểm và hành lá.'),
(N'Gỏi cuốn tôm thịt thanh mát',
 N'Món gỏi cuốn tươi ngon ít dầu mỡ với tôm sú, thịt ba rọi luộc, bún, xà lách và húng quế.',
 N'1. Luộc chín tôm sú bóc vỏ chẻ đôi; thịt ba rọi luộc thái lát mỏng. 2. Trụng bún gạo khô để ráo, rửa sạch xà lách, giá đỗ và húng quế. 3. Cuốn các nguyên liệu và chấm nước mắm tỏi ớt chua ngọt.'),
(N'Nước ép cần tây táo xanh detox',
 N'Thức uống xanh thanh lọc cơ thể, hỗ trợ giảm cân đẹp da từ cần tây Đà Lạt, táo xanh, dưa leo và chanh tươi.',
 N'1. Rửa sạch cần tây Đà Lạt, táo xanh và dưa leo, cắt khúc vừa máy ép. 2. Ép lấy nước cốt nguyên chất, vắt thêm vài giọt chanh tươi không hạt và thưởng thức lạnh.');

INSERT INTO dbo.CongThucMonAn (TenMonAn, MoTa, KhauPhanMacDinh, HuongDanNau, NgayTao)
SELECT r.Name, r.Description, 4, r.Instructions, GETDATE()
FROM @NewRecipes r
WHERE NOT EXISTS (SELECT 1 FROM dbo.CongThucMonAn ct WHERE ct.TenMonAn = r.Name);

-- Bổ sung thêm nguyên liệu canh chua miền Tây vào công thức số 1 nếu có
DECLARE @RecipeIngredients TABLE (Recipe NVARCHAR(150), Product NVARCHAR(200), Qty DECIMAL(10,2));
INSERT INTO @RecipeIngredients VALUES
(N'Canh chua cá lóc miền Tây', N'Đậu bắp tươi', 1),
(N'Canh chua cá lóc miền Tây', N'Dứa chín gọt sẵn', 1),
(N'Canh chua cá lóc miền Tây', N'Bạc hà nấu canh chua', 1),
(N'Canh chua cá lóc miền Tây', N'Giá đỗ sạch ủ mầm', 1),
(N'Canh chua cá lóc miền Tây', N'Me vắt nấu canh chua 200g', 1),
(N'Canh chua cá lóc miền Tây', N'Rau ngổ tươi', 1),
(N'Lẩu hải sản chua cay', N'Tôm sú tươi sống', 1),
(N'Lẩu hải sản chua cay', N'Mực ống tươi', 1),
(N'Lẩu hải sản chua cay', N'Nghêu trắng', 1),
(N'Lẩu hải sản chua cay', N'Nấm kim châm 150g', 2),
(N'Lẩu hải sản chua cay', N'Cải thảo Đà Lạt', 1),
(N'Lẩu hải sản chua cay', N'Sả cây tươi 300g', 1),
(N'Lẩu hải sản chua cay', N'Me vắt nấu canh chua 200g', 1),
(N'Phở bò truyền thống', N'Thịt bò bắp Úc tươi', 1),
(N'Phở bò truyền thống', N'Thịt bò thăn', 1),
(N'Phở bò truyền thống', N'Giá đỗ sạch ủ mầm', 1),
(N'Phở bò truyền thống', N'Hành lá tươi', 1),
(N'Phở bò truyền thống', N'Ngò gai tươi', 1),
(N'Phở bò truyền thống', N'Húng quế tươi', 1),
(N'Phở bò truyền thống', N'Gừng tươi rửa sạch 250g', 1),
(N'Bò lúc lắc ớt chuông', N'Thịt bò thăn', 1),
(N'Bò lúc lắc ớt chuông', N'Ớt chuông Đà Lạt', 1),
(N'Bò lúc lắc ớt chuông', N'Tỏi ta Lý Sơn 250g', 1),
(N'Bò lúc lắc ớt chuông', N'Nước tương đậu nành 500ml', 1),
(N'Cá lóc kho tộ miền Tây', N'Cá lóc tươi làm sạch', 1),
(N'Cá lóc kho tộ miền Tây', N'Nước mắm cá cơm 500ml', 1),
(N'Cá lóc kho tộ miền Tây', N'Tiêu đen xay Phú Quốc 100g', 1),
(N'Cá lóc kho tộ miền Tây', N'Ớt hiểm đỏ tươi 100g', 1),
(N'Cá lóc kho tộ miền Tây', N'Hành tím Vĩnh Châu 250g', 1),
(N'Gỏi cuốn tôm thịt thanh mát', N'Tôm sú tươi sống', 1),
(N'Gỏi cuốn tôm thịt thanh mát', N'Thịt ba rọi heo VietGAP', 1),
(N'Gỏi cuốn tôm thịt thanh mát', N'Bún gạo khô 400g', 1),
(N'Gỏi cuốn tôm thịt thanh mát', N'Xà lách lô lô xanh', 1),
(N'Gỏi cuốn tôm thịt thanh mát', N'Húng quế tươi', 1),
(N'Nước ép cần tây táo xanh detox', N'Cần tây Đà Lạt', 2),
(N'Nước ép cần tây táo xanh detox', N'Táo xanh Mỹ tươi giòn', 1),
(N'Nước ép cần tây táo xanh detox', N'Dưa leo baby', 1),
(N'Nước ép cần tây táo xanh detox', N'Chanh tươi không hạt', 1);

INSERT INTO dbo.NguyenLieuMonAn (MaCongThuc, MaSP, SoLuongDinhLuong, DonViTinh, BatBuoc)
SELECT ct.MaCongThuc, sp.MaSP, ri.Qty, sp.DonViTinh, 1
FROM @RecipeIngredients ri
JOIN dbo.CongThucMonAn ct ON ct.TenMonAn = ri.Recipe
JOIN dbo.SanPham sp ON sp.TenSP = ri.Product AND sp.MaSPCha IS NULL AND sp.TrangThaiXoa = 0
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.NguyenLieuMonAn nl
    WHERE nl.MaCongThuc = ct.MaCongThuc AND nl.MaSP = sp.MaSP
);

COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
