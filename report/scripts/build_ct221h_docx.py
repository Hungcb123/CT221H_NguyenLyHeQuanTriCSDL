"""Build the Vietnamese CT221H report from its editable Markdown source.
Requires python-docx and Pillow. Word can update the TOC and export a PDF.
"""
from pathlib import Path
import re,json
from docx import Document
from docx.shared import Cm,Pt,Inches,RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.section import WD_SECTION_START
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.enum.table import WD_TABLE_ALIGNMENT,WD_CELL_VERTICAL_ALIGNMENT
from PIL import Image
ROOT=Path(__file__).resolve().parents[2];REPORT=ROOT/'report';ASSETS=REPORT/'CT221H_assets'
SRC=REPORT/'BaoCao_DailyMart_CT221H_TiengViet.md';OUT=REPORT/'BaoCao_DailyMart_CT221H_TiengViet.docx'
doc=Document()
styles=doc.styles
for name in ['Normal','Body Text','List Bullet','List Number','Caption','TOC Heading','Header','Footer']:
 if name in styles:
  styles[name].font.name='Times New Roman';styles[name].font.size=Pt(13)
normal=styles['Normal'];normal.paragraph_format.line_spacing=1.3;normal.paragraph_format.space_after=Pt(6);normal.paragraph_format.first_line_indent=Cm(.75)
normal.paragraph_format.widow_control=True
for name,size in [('Heading 1',16),('Heading 2',14),('Heading 3',13)]:
 st=styles[name];st.font.name='Times New Roman';st.font.size=Pt(size);st.font.color.rgb=RGBColor.from_string('172F45');st.font.bold=True
 st.paragraph_format.first_line_indent=Cm(0);st.paragraph_format.space_before=Pt(14);st.paragraph_format.space_after=Pt(8);st.paragraph_format.keep_with_next=True
styles['Heading 1'].paragraph_format.page_break_before=True
for name in ['FigureCaption','TableCaption']:
 if name not in styles:styles.add_style(name,1)
 st=styles[name];st.base_style=styles['Caption'];st.font.name='Times New Roman';st.font.size=Pt(11);st.font.italic=True;st.font.color.rgb=RGBColor.from_string('333333')
 st.paragraph_format.first_line_indent=Cm(0);st.paragraph_format.line_spacing=1.1;st.paragraph_format.space_before=Pt(5);st.paragraph_format.space_after=Pt(8);st.paragraph_format.keep_with_next=(name=='TableCaption')
if 'Code' not in styles:styles.add_style('Code',1)
st=styles['Code'];st.font.name='Consolas';st.font.size=Pt(9);st.paragraph_format.first_line_indent=Cm(0);st.paragraph_format.line_spacing=1.05;st.paragraph_format.space_after=Pt(0);st.paragraph_format.left_indent=Cm(.25);st.paragraph_format.right_indent=Cm(.15)
# Correct fonts and language in Word on Windows, including Vietnamese glyphs.
for st in styles:
 if st.type==1:
  rpr=st.element.get_or_add_rPr();rf=rpr.find(qn('w:rFonts'))
  if rf is None:rf=OxmlElement('w:rFonts');rpr.append(rf)
  face='Consolas' if st.name=='Code' else 'Times New Roman'
  for attr in ['ascii','hAnsi','eastAsia','cs']:rf.set(qn('w:'+attr),face)
  lang=OxmlElement('w:lang');lang.set(qn('w:val'),'vi-VN');rpr.append(lang)

def setup_section(section,number=None,fmt='decimal'):
 section.page_width=Cm(21);section.page_height=Cm(29.7)
 section.top_margin=Cm(2.2);section.bottom_margin=Cm(2.1);section.left_margin=Cm(3);section.right_margin=Cm(2)
 section.header_distance=Cm(1);section.footer_distance=Cm(1)
 section.header.is_linked_to_previous=False;section.footer.is_linked_to_previous=False
 if number is not None:
  pg=OxmlElement('w:pgNumType');pg.set(qn('w:start'),str(number));pg.set(qn('w:fmt'),fmt);section._sectPr.append(pg)

def field(p,instruction,placeholder=''):
 r=p.add_run();b=OxmlElement('w:fldChar');b.set(qn('w:fldCharType'),'begin');r._r.append(b)
 r=p.add_run();i=OxmlElement('w:instrText');i.set(qn('xml:space'),'preserve');i.text=instruction;r._r.append(i)
 r=p.add_run();sep=OxmlElement('w:fldChar');sep.set(qn('w:fldCharType'),'separate');r._r.append(sep)
 if placeholder:p.add_run(placeholder)
 r=p.add_run();e=OxmlElement('w:fldChar');e.set(qn('w:fldCharType'),'end');r._r.append(e)

def page_header(section,label):
 p=section.header.paragraphs[0];p.paragraph_format.first_line_indent=Cm(0);p.paragraph_format.space_after=Pt(0)
 r=p.add_run(label);r.font.size=Pt(9);r.font.color.rgb=RGBColor.from_string('666666')
 p=section.footer.paragraphs[0];p.alignment=WD_ALIGN_PARAGRAPH.CENTER;p.paragraph_format.first_line_indent=Cm(0)
 field(p,' PAGE ')
 for r in p.runs:r.font.size=Pt(11)

def text_runs(p,text):
 for i,part in enumerate(re.split(r'(\*\*.*?\*\*|`[^`]+`)',text)):
  if part.startswith('**') and part.endswith('**'):p.add_run(part[2:-2]).bold=True
  elif part.startswith('`') and part.endswith('`'):
   r=p.add_run(part[1:-1]);r.font.name='Consolas';r.font.size=Pt(10.5)
  else:p.add_run(part)

def para(text,style=None):
 p=doc.add_paragraph(style=style);text_runs(p,text)
 if not style:p.alignment=WD_ALIGN_PARAGRAPH.JUSTIFY
 return p

def title(text):
 p=doc.add_paragraph();p.paragraph_format.first_line_indent=Cm(0);p.paragraph_format.space_before=Pt(12);p.paragraph_format.space_after=Pt(12)
 p.alignment=WD_ALIGN_PARAGRAPH.CENTER;r=p.add_run(text);r.bold=True;r.font.size=Pt(16)
 return p

def table(headers,rows,small=False):
 t=doc.add_table(rows=1,cols=len(headers));t.alignment=WD_TABLE_ALIGNMENT.CENTER;t.style='Table Grid';t.autofit=False
 # Make relative space reflect prose and identifier columns.
 n=len(headers);widths=([4.6,11.4] if n==2 else [3.4,5.8,6.8] if n==3 else [3.5,4.7,3.9,3.9] if n==4 else [16/n]*n)
 for c,w in zip(t.columns,widths):c.width=Cm(w)
 for c,h,w in zip(t.rows[0].cells,headers,widths):c.width=Cm(w);c.text=h
 repeat=OxmlElement('w:tblHeader');repeat.set(qn('w:val'),'true');t.rows[0]._tr.get_or_add_trPr().append(repeat)
 for row in rows:
  cells=t.add_row().cells
  for c,val,w in zip(cells,row,widths):c.width=Cm(w);c.text=str(val)
 for ri,row in enumerate(t.rows):
  trpr=row._tr.get_or_add_trPr();nosplit=OxmlElement('w:cantSplit');trpr.append(nosplit)
  for cell in row.cells:
   cell.vertical_alignment=WD_CELL_VERTICAL_ALIGNMENT.CENTER
   tcpr=cell._tc.get_or_add_tcPr();margins=OxmlElement('w:tcMar')
   for side in ['top','left','bottom','right']:
    node=OxmlElement('w:'+side);node.set(qn('w:w'),'75');node.set(qn('w:type'),'dxa');margins.append(node)
   tcpr.append(margins)
   if ri==0:
    shade=OxmlElement('w:shd');shade.set(qn('w:fill'),'E8EEF3');tcpr.append(shade)
   for p in cell.paragraphs:
    p.paragraph_format.first_line_indent=Cm(0);p.paragraph_format.space_after=Pt(2);p.paragraph_format.space_before=Pt(2);p.paragraph_format.line_spacing=1.08;p.paragraph_format.keep_with_next=(ri==0)
    for r in p.runs:r.font.size=Pt(9.5 if small else 10.5);r.font.bold=(ri==0)
 doc.add_paragraph().paragraph_format.space_after=Pt(0)
 return t

def caption(text,kind):
 p=para(text,style='FigureCaption' if kind=='figure' else 'TableCaption')
 p.alignment=WD_ALIGN_PARAGRAPH.CENTER if kind=='figure' else WD_ALIGN_PARAGRAPH.LEFT
 return p

def figure(path,label):
 path=REPORT/path
 if not path.exists():raise FileNotFoundError(path)
 w,h=Image.open(path).size
 width=min(16,15.2*w/h);height=width*h/w
 p=doc.add_paragraph();p.alignment=WD_ALIGN_PARAGRAPH.CENTER;p.paragraph_format.first_line_indent=Cm(0);p.paragraph_format.keep_with_next=True;p.paragraph_format.space_after=Pt(0)
 p.add_run().add_picture(str(path),width=Cm(width),height=Cm(height));caption(label,'figure')

def parse(md):
 lines=md.splitlines();i=0
 while i<len(lines):
  line=lines[i].strip()
  if not line:i+=1;continue
  if line.startswith('```'):
   i+=1
   while i<len(lines) and not lines[i].startswith('```'):
    p=para(lines[i] or ' ',style='Code');p.paragraph_format.keep_with_next=False
    i+=1
   i+=1;continue
  if line.startswith('@Table '):caption(line[7:],'table');i+=1;continue
  if line.startswith('|'):
   rows=[]
   while i<len(lines) and lines[i].strip().startswith('|'):
    vals=[x.strip() for x in lines[i].strip().strip('|').split('|')]
    if not all(re.fullmatch(r':?-+:?',x.replace(' ','')) for x in vals):rows.append(vals)
    i+=1
   table(rows[0],rows[1:]);continue
  if line.startswith('!['):
   m=re.match(r'!\[(.*?)\]\((.*?)\)',line);figure(Path(m[2]),m[1]);i+=1;continue
  if line.startswith('#'):
   count=len(line)-len(line.lstrip('#'));doc.add_heading(line[count:].strip(),min(count,3));i+=1;continue
  if line.startswith('- '):para(line[2:],style='List Bullet');i+=1;continue
  group=[line];i+=1
  while i<len(lines) and lines[i].strip() and not lines[i].startswith(('#','|','```','![','@Table ','- ')):
   group.append(lines[i].strip());i+=1
  p=para(' '.join(group))
  if group[0].startswith('['):p.paragraph_format.first_line_indent=Cm(0);p.paragraph_format.line_spacing=1.1;p.paragraph_format.space_after=Pt(9)

# Cover follows the institution and team of the previous report, with current course data.
cover=doc.sections[0];setup_section(cover)
for s,size,bold in [('ĐẠI HỌC CẦN THƠ',15,True),('TRƯỜNG CÔNG NGHỆ THÔNG TIN VÀ TRUYỀN THÔNG',13,True),('KHOA CÔNG NGHỆ PHẦN MỀM',13,True)]:
 p=para(s);p.alignment=WD_ALIGN_PARAGRAPH.CENTER;p.paragraph_format.first_line_indent=Cm(0);p.paragraph_format.space_after=Pt(5)
 for r in p.runs:r.font.size=Pt(size);r.bold=bold
p=doc.add_paragraph();p.paragraph_format.space_after=Pt(30)
for s,size in [('BÁO CÁO ĐỒ ÁN',20),('CT221H – NGUYÊN LÝ HỆ QUẢN TRỊ',16),('CƠ SỞ DỮ LIỆU',16)]:
 p=title(s);p.paragraph_format.space_after=Pt(5)
 for r in p.runs:r.font.size=Pt(size)
doc.add_paragraph()
for s in ['DAILYMART','HỆ THỐNG QUẢN LÝ VÀ BÁN','THỰC PHẨM TƯƠI SỐNG','TÍCH HỢP TÌM KIẾM NGỮ NGHĨA,','DỰ BÁO NHU CẦU VÀ CHATBOT']:
 p=title(s);p.paragraph_format.space_before=Pt(3);p.paragraph_format.space_after=Pt(3)
 for r in p.runs:r.font.size=Pt(17 if s=='DAILYMART' else 15)
doc.add_paragraph()
p=para('Giảng viên hướng dẫn: TS. Cù Vĩnh Lộc');p.paragraph_format.first_line_indent=Cm(0);p.alignment=WD_ALIGN_PARAGRAPH.CENTER
p=para('Nhóm sinh viên thực hiện');p.alignment=WD_ALIGN_PARAGRAPH.CENTER;p.paragraph_format.first_line_indent=Cm(0)
for name,code in [('Cao Tường Hưng','B2303873'),('Trang Minh Chánh','B2303864'),('Nguyễn Châu Nhựt Cần','B2303863')]:
 p=para(f'{name}  –  {code}');p.alignment=WD_ALIGN_PARAGRAPH.CENTER;p.paragraph_format.first_line_indent=Cm(0);p.paragraph_format.space_after=Pt(4)
 for r in p.runs:r.bold=True
p=doc.add_paragraph();p.paragraph_format.space_after=Pt(15)
p=para('Cần Thơ, tháng 11 năm 2026');p.alignment=WD_ALIGN_PARAGRAPH.CENTER;p.paragraph_format.first_line_indent=Cm(0)
# No decorative page borders.

front=doc.add_section(WD_SECTION_START.NEW_PAGE);setup_section(front,1,'lowerRoman')
for old in front._sectPr.findall(qn('w:pgBorders')):front._sectPr.remove(old)
page_header(front,'CT221H – Nguyên lý hệ quản trị cơ sở dữ liệu | DailyMart')
md=SRC.read_text();abstract,body=md.split('# CHƯƠNG 1.',1)
parse(abstract)
# Unnumbered front matter titles excluded from main TOC.
for heading,instruction in [('MỤC LỤC',' TOC \\o "1-2" \\h \\z \\u '),('DANH MỤC HÌNH',' TOC \\t "FigureCaption,1" \\h \\z '),('DANH MỤC BẢNG',' TOC \\t "TableCaption,1" \\h \\z ')]:
 doc.add_page_break();title(heading);p=doc.add_paragraph();p.paragraph_format.first_line_indent=Cm(0);field(p,instruction,'Mục lục được cập nhật khi hoàn tất dàn trang bằng Microsoft Word.')
doc.add_page_break();title('DANH MỤC TỪ VIẾT TẮT')
table(['Từ viết tắt','Diễn giải'],[
 ('ACID','Atomicity, Consistency, Isolation, Durability – các thuộc tính giao dịch'),('API','Application Programming Interface – giao diện lập trình ứng dụng'),('BM25','Hàm xếp hạng văn bản dựa trên tần suất từ và độ dài tài liệu'),('CRUD','Create, Read, Update, Delete – thêm, đọc, sửa, xóa'),('CSRF','Cross-Site Request Forgery – giả mạo yêu cầu liên trang'),('CSDL','Cơ sở dữ liệu'),('DTO','Data Transfer Object – đối tượng truyền dữ liệu'),('ERD','Entity–Relationship Diagram – sơ đồ thực thể, quan hệ'),('JDBC / JPA','Cơ chế kết nối và truy cập dữ liệu từ Java'),('LLM','Large Language Model – mô hình ngôn ngữ lớn'),('MAE / RMSE','Sai số tuyệt đối trung bình / căn sai số bình phương trung bình'),('MCP','Model Context Protocol; tên thành phần công cụ trong dự án chưa đồng nghĩa kiểm chứng chuẩn đầy đủ'),('MLP','Multilayer Perceptron – mạng nơ-ron truyền thẳng nhiều lớp'),('ONNX','Open Neural Network Exchange – định dạng trao đổi mô hình'),('QA','Quality Assurance – bảo đảm và kiểm tra chất lượng'),('RRF','Reciprocal Rank Fusion – gộp nghịch đảo thứ hạng'),('SP','Stored Procedure – thủ tục lưu'),('WAPE','Weighted Absolute Percentage Error – tổng sai số tuyệt đối / tổng nhãn')])
main=doc.add_section(WD_SECTION_START.NEW_PAGE);setup_section(main,1)
page_header(main,'CT221H – Nguyên lý hệ quản trị cơ sở dữ liệu | Báo cáo đồ án DailyMart')
parse('# CHƯƠNG 1.'+body)

# Append detailed dictionary extracted from source declarations, never invented.
def tables_in(text):
 for m in re.finditer(r'CREATE\s+TABLE\s+(?:dbo\.)?(\w+)\s*\(',text,re.I):
  start=m.end();depth=1;i=start;quote=False
  while i<len(text) and depth:
   ch=text[i]
   if ch=="'":quote=not quote
   if not quote:
    if ch=='(':depth+=1
    elif ch==')':depth-=1
   i+=1
  yield m.group(1),text[start:i-1]
def columns(body):
 chunks=[];start=0;depth=0;quote=False
 for i,ch in enumerate(body):
  if ch=="'":quote=not quote
  if not quote:
   if ch=='(':depth+=1
   elif ch==')':depth-=1
   elif ch==',' and depth==0:chunks.append(body[start:i]);start=i+1
 chunks.append(body[start:]);result=[]
 for c in chunks:
  c=re.sub(r'--[^\n]*','',c).strip()
  m=re.match(r'(\w+)\s+((?:NVARCHAR|VARCHAR|DECIMAL|DATETIME2|UNIQUEIDENTIFIER|BIGINT|INT|BIT|DATE)(?:\([^)]*\))?)\s*(.*)',c,re.I|re.S)
  if not m:continue
  name,typ,rest=m.groups();rules=[]
  if re.search(r'PRIMARY KEY',rest,re.I):rules.append('Khóa chính')
  if 'IDENTITY' in rest:rules.append('Tự tăng')
  if re.search(r'NOT NULL',rest,re.I):rules.append('Không NULL')
  elif re.search(r'\bNULL\b',rest,re.I):rules.append('Cho phép NULL')
  if 'UNIQUE' in rest:rules.append('Duy nhất')
  if 'CHECK' in rest:rules.append('Có CHECK')
  if 'DEFAULT' in rest:rules.append('Có mặc định')
  if 'REFERENCES' in rest:rules.append('Có tham chiếu FK')
  result.append((name,typ.upper(),'; '.join(rules) or 'Xem khóa/ràng buộc cấp bảng'))
 return result
sources=[ROOT/'database/01_init_schema.sql',ROOT/'backend/src/main/java/com/ctu/freshgrocery/customer/CustomerAuthController.java',ROOT/'backend/src/main/resources/db/momo.sql',ROOT/'backend/src/main/resources/db/sepay.sql']
doc.add_heading('PHỤ LỤC B. TỪ ĐIỂN DỮ LIỆU RÚT TỪ KHAI BÁO BẢNG',1)
para('Phụ lục được trích từ các câu CREATE TABLE của schema và thành phần bổ sung. Khóa ngoại, khóa ghép và chỉ mục khai báo ở cấp bảng cần đọc cùng script gốc; các dòng dưới không thay thế toàn bộ DDL. Cột PhuongThucThanhToan của DonHang được bổ sung bằng migration thanh toán; các cột bổ sung bằng ALTER và phiên bản migration cần đối chiếu ở từng môi trường.')
count=0
for source in sources:
 for name,body_sql in tables_in(source.read_text()):
  rows=columns(body_sql)
  if not rows:continue
  count+=1;doc.add_heading(f'B.{count}. {name}',2)
  p=para('Nguồn: '+str(source.relative_to(ROOT)));p.paragraph_format.first_line_indent=Cm(0);p.paragraph_format.keep_with_next=True
  for r in p.runs:r.font.size=Pt(10)
  caption(f'Bảng B.{count}. Các cột của {name} theo khai báo gốc','table');table(['Cột','Kiểu dữ liệu','Ràng buộc khai báo trên cột'],rows,small=True)

doc.add_heading('PHỤ LỤC C. SQL THIẾT KẾ VIEW BỔ SUNG',1)
para('Trạng thái: thiết kế kèm báo cáo, chưa tích hợp khởi tạo và chưa thực thi kiểm thử SQL Server. Cần chạy trên QA, kiểm tra kết quả và quyền truy cập trước khi sử dụng. View không tự bảo đảm tăng tốc, cập nhật dữ liệu hay sổ doanh thu kế toán.')
parse('```sql\n'+'\n'.join(line for line in (ASSETS/'views_de_xuat.sql').read_text().splitlines() if not line.startswith('--'))+'\n```')
doc.add_heading('PHỤ LỤC D. KỊCH BẢN TRÌNH DIỄN VÀ BẢO VỆ',1)
para('Kịch bản sau tập trung vào các yêu cầu học phần và cách giải thích dữ liệu. Những bước chưa có môi trường chạy cần trình bày là kế hoạch hoặc thiết kế, không thay ảnh mô phỏng bằng minh chứng thực thi.')
table(['Bước','Thao tác trình diễn','Điểm cần giải thích'],[
 ('1','Mở schema và ERD; chỉ ra khóa chính/ngoại','Các bảng chi tiết, nguồn đơn và dữ liệu giá lịch sử'),
 ('2','Thêm, đọc và sửa một sản phẩm QA','Request → service → stored procedure → kết quả từ SQL'),
 ('3','Sửa giá hoặc tồn rồi mở nhật ký','inserted/deleted; điều kiện cảnh báo và JSON trước–sau'),
 ('4','Xóa mềm sản phẩm rồi lọc/khôi phục','INSTEAD OF DELETE; giữ lịch sử và gỡ chỉ mục'),
 ('5','Mua giỏ đủ tiền, sau đó thử giỏ thiếu tiền','Commit/rollback; đối chiếu ví, kho, đơn và sổ cái'),
 ('6','Mở SQL thiết kế view và giải thích bộ lọc','Chưa triển khai; loại đơn demo/benchmark và giữ khách chưa mua'),
 ('7','Mở kế hoạch SCAN/SEEK và measurements.csv','Đọc logic, buffer ấm, trung vị; mục tiêu SRS chưa đạt trọn'),
 ('8','Tìm một tên hàng, một nhu cầu và mặt hàng không có','BM25/vector/RRF, products/relatedProducts và từ chối'),
 ('9','Giả lập hai mức tồn trong trang AI','10 đặc trưng, scaler, ONNX và quy tắc; giả lập không lưu'),
 ('10','Hỏi món ăn rồi thêm nguyên liệu đã chọn','Chatbot theo kịch bản, công cụ và kiểm tra lại tại database'),
 ('11','Mở luồng SePay ở môi trường test nếu đã cấu hình','Webhook xác thực; duplicate, expiry, late payment và đối soát')])
para('Khi kết thúc, nhóm cần nêu giới hạn hiện tại: view mới ở thiết kế; dữ liệu AI mô phỏng; bộ tìm kiếm nhỏ và đã biết; kiểm thử ngân hàng thật chưa thực hiện. Việc nêu đúng giới hạn giúp phân biệt nội dung đã xây dựng, minh chứng đã thu thập và hướng phát triển tiếp theo.')

props=doc.core_properties;props.title='DailyMart – Báo cáo đồ án CT221H';props.subject='Nguyên lý hệ quản trị cơ sở dữ liệu';props.author='Cao Tường Hưng; Trang Minh Chánh; Nguyễn Châu Nhựt Cần';props.keywords='CT221H, SQL Server, view, stored procedure, trigger, semantic search, ONNX, chatbot';props.comments='Bản tiếng Việt; cập nhật 14/11/2026. View là thiết kế bổ sung chưa triển khai. Số liệu đánh giá được phân loại theo nguồn.'
settings=doc.settings.element;update=OxmlElement('w:updateFields');update.set(qn('w:val'),'true');settings.append(update)
doc.save(OUT)
print(json.dumps({'docx':str(OUT),'dictionary_tables':count,'paragraphs':len(doc.paragraphs),'tables':len(doc.tables),'images':len(doc.inline_shapes),'words_markdown':len(md.split())},ensure_ascii=False))
