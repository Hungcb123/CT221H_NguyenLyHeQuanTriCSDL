USE FreshGroceryDB;
GO
-- Recipe data is editable in SQL; chatbot routing contains no dish names or IDs.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
DECLARE @Recipes TABLE (Name NVARCHAR(150), Description NVARCHAR(500), Instructions NVARCHAR(MAX));
INSERT INTO @Recipes VALUES
(N'Bông cải xào thịt bò',N'Món xào thịt bò và bông cải xanh cho bữa cơm gia đình.',N'Rửa bông cải, cắt nhỏ; thái bò mỏng. Xào bò vừa chín, cho bông cải vào xào chín và nêm vừa ăn.'),
(N'Canh bí đỏ sườn non',N'Canh bí đỏ mềm và sườn non.',N'Rửa sạch sườn, chần rồi rửa lại. Hầm sườn, cho bí đỏ cắt miếng vào nấu chín mềm, nêm vừa ăn.'),
(N'Ức gà áp chảo bông cải',N'Ức gà áp chảo ăn kèm bông cải hấp.',N'Làm sạch ức gà, ướp vừa ăn; áp chảo cho chín hoàn toàn. Hấp bông cải chín và dùng cùng gà.'),
(N'Mực xào dưa leo',N'Mực ống xào cùng dưa leo giòn.',N'Làm sạch mực, cắt miếng. Rửa dưa leo, cắt lát. Xào mực chín rồi cho dưa leo vào đảo nhanh, nêm vừa ăn.'),
(N'Canh cải ngọt thịt heo',N'Canh cải ngọt với thịt heo xay.',N'Rửa cải ngọt, cắt đoạn. Nấu thịt heo xay chín trong nước, cho cải vào nấu chín, nêm vừa ăn.'),
(N'Bí đao nấu tôm',N'Canh bí đao với tôm thẻ.',N'Làm sạch tôm, gọt bí đao và cắt miếng. Nấu tôm chín, cho bí vào nấu chín mềm, nêm vừa ăn.'),
(N'Cá diêu hồng hấp',N'Cá diêu hồng hấp dùng cùng rau cải thìa.',N'Làm sạch cá, ướp vừa ăn và hấp chín hoàn toàn. Rửa cải thìa, hấp chín ăn cùng cá.'),
(N'Nghêu hấp',N'Nghêu hấp đơn giản cho bữa ăn gia đình.',N'Ngâm rửa nghêu sạch cát, bỏ con chết. Hấp chín, bỏ những con không mở vỏ. Nêm theo khẩu vị.');
INSERT INTO CongThucMonAn (TenMonAn,MoTa,KhauPhanMacDinh,HuongDanNau,NgayTao)
SELECT r.Name,r.Description,4,r.Instructions,GETDATE() FROM @Recipes r
WHERE NOT EXISTS (SELECT 1 FROM CongThucMonAn ct WHERE ct.TenMonAn=r.Name);

DECLARE @Ingredients TABLE (Recipe NVARCHAR(150), Product NVARCHAR(200), Qty DECIMAL(10,2));
INSERT INTO @Ingredients VALUES
(N'Bông cải xào thịt bò',N'Bông cải xanh',1),(N'Bông cải xào thịt bò',N'Thịt bò thăn',1),
(N'Canh bí đỏ sườn non',N'Bí đỏ hồ lô',1),(N'Canh bí đỏ sườn non',N'Sườn non heo',1),
(N'Ức gà áp chảo bông cải',N'Ức gà phi lê',1),(N'Ức gà áp chảo bông cải',N'Bông cải xanh',1),
(N'Mực xào dưa leo',N'Mực ống tươi',1),(N'Mực xào dưa leo',N'Dưa leo baby',1),
(N'Canh cải ngọt thịt heo',N'Cải ngọt tươi',2),(N'Canh cải ngọt thịt heo',N'Thịt heo xay 500g',1),
(N'Bí đao nấu tôm',N'Bí đao xanh',1),(N'Bí đao nấu tôm',N'Tôm thẻ tươi',1),
(N'Cá diêu hồng hấp',N'Cá diêu hồng tươi',1),(N'Cá diêu hồng hấp',N'Cải thìa VietGAP',2),
(N'Nghêu hấp',N'Nghêu trắng',1);
INSERT INTO NguyenLieuMonAn (MaCongThuc,MaSP,SoLuongDinhLuong,DonViTinh,BatBuoc)
SELECT ct.MaCongThuc,sp.MaSP,i.Qty,sp.DonViTinh,1
FROM @Ingredients i JOIN CongThucMonAn ct ON ct.TenMonAn=i.Recipe JOIN SanPham sp ON sp.TenSP=i.Product
WHERE NOT EXISTS(SELECT 1 FROM NguyenLieuMonAn n WHERE n.MaCongThuc=ct.MaCongThuc AND n.MaSP=sp.MaSP);
COMMIT TRANSACTION;
GO
