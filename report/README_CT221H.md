# Báo cáo DailyMart – CT221H

Tệp chính: BaoCao_DailyMart_CT221H_TiengViet.docx. Mục lục được cập nhật bằng Microsoft Word; bản PDF cùng tên được xuất bằng LibreOffice để kiểm tra bố cục.

Giảng viên: TS. Cù Vĩnh Lộc.
Sinh viên theo thứ tự: Cao Tường Hưng (B2303873), Trang Minh Chánh (B2303864), Nguyễn Châu Nhựt Cần (B2303863).

Báo cáo có 9 chương và 4 phụ lục. View là thiết kế bổ sung chưa tích hợp khởi tạo; SQL nằm tại CT221H_assets/views_de_xuat.sql. Số liệu AI mô phỏng và benchmark lịch sử có ghi nguồn và giới hạn; ảnh SePay/xóa mềm sử dụng API giả lập.

## Chỉnh sửa

Bản Markdown cùng tên chứa nội dung chính, tài liệu tham khảo và phụ lục A. Script build_ct221h_docx.py bổ sung bìa, mục lục, danh mục từ viết tắt và phụ lục B–D.

Dựng hình: python3 report/scripts/build_ct221h_figures.py (cần Graphviz/dot, matplotlib, pandas).
Dựng Word: python3 report/scripts/build_ct221h_docx.py (cần python-docx và Pillow).
Cập nhật mục lục và xuất PDF: chạy report/scripts/render_ct221h_word.ps1 -SkipPdf bằng PowerShell trên Windows có Microsoft Word. Script mở một phiên Word riêng và chỉ sửa tệp báo cáo mới; đóng tệp đích trước khi chạy.
Kiểm tra: python3 report/scripts/validate_ct221h_report.py.

CT221H_assets lưu hình, SQL bổ sung và kết quả kiểm tra. source_state.json ghi commit nền và ngày đối chiếu; báo cáo còn bao gồm working tree, không khẳng định toàn bộ thay đổi đã được commit.
