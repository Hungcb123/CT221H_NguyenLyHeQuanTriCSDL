"""Build the seven-chapter report from Markdown while preserving reference DOCX parts.

Run with the bundled Python runtime. Refresh fields with Word on a temporary copy;
the source reference is never modified. Render outputs are QA artifacts.
"""
from pathlib import Path
from copy import deepcopy
from zipfile import ZipFile, ZIP_DEFLATED
import hashlib, json, re, argparse
from lxml import etree
from docx import Document
from docx.shared import Cm, Pt
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from PIL import Image, ImageDraw, ImageFont

ROOT=Path(__file__).resolve().parents[2]
REPORT=ROOT/'report'
ASSETS=REPORT/'CT221H_7Chuong_assets'
SOURCE=REPORT/'BaoCao_DailyMart_CT221H_7Chuong.md'
OUTPUT=REPORT/'BaoCao_DailyMart_CT221H_7Chuong.docx'
REF=Path(r'C:/Users/nhutc/Downloads/BaoCao_DailyMart_CT221H_TiengViet_DaRasoat.docx')
EXPECTED_HASH='7b438de51021c87f132fff9c5ba0bd5655a69e72d81c9d77ef7f04993569a9ae'
NS={'w':'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
FONT=Path('C:/Windows/Fonts/arial.ttf')
ASSETS.mkdir(exist_ok=True)

def font(size=32,bold=False):
    return ImageFont.truetype(str(FONT.with_name('arialbd.ttf') if bold else FONT),size)

def diagram(name,w,h,boxes,edges):
    image=Image.new('RGB',(w,h),'white');d=ImageDraw.Draw(image)
    for x1,y1,x2,y2 in edges:
        d.line((x1,y1,x2,y2),fill='#526477',width=5)
        import math
        a=math.atan2(y2-y1,x2-x1);length=17
        d.polygon([(x2,y2),(x2-length*math.cos(a-.45),y2-length*math.sin(a-.45)),(x2-length*math.cos(a+.45),y2-length*math.sin(a+.45))],fill='#526477')
    for x,y,bw,bh,title,body in boxes:
        d.rounded_rectangle((x,y,x+bw,y+bh),radius=14,fill='#F1F5F8',outline='#44576B',width=3)
        lines=[(title,font(33,True))]+[(line,font(29)) for line in body.split('\n') if line]
        lineh=44;yy=y+(bh-lineh*len(lines))/2
        for text,f in lines:
            left,top,right,bottom=d.textbbox((0,0),text,font=f)
            d.text((x+(bw-(right-left))/2,yy),text,font=f,fill='#172F45');yy+=lineh
    image.save(ASSETS/(name+'.png'))

def figures():
    diagram('transaction',1500,1080,[
      (80,25,960,145,'Xác thực và kiểm tra phương thức','Phiên khách hàng · Chủ sở hữu giỏ · COD hoặc WALLET'),
      (80,225,960,145,'BEGIN TRANSACTION','Khóa ví → Sản phẩm → Suất khuyến mãi'),
      (80,425,960,145,'Kiểm tra và tính tiền tại SQL','Tồn · Hạn dùng · Cha–con · Giá · Số dư'),
      (80,625,960,170,'Ghi dữ liệu nghiệp vụ','Kho · Đơn · Chi tiết · Suất ưu đãi · Giỏ\nWALLET: thêm cập nhật ví và sổ cái'),
      (80,865,960,145,'COMMIT','Trả mã đơn đã ghi nhận'),
      (1120,425,340,370,'ROLLBACK','Lỗi nghiệp vụ\nhoặc lỗi SQL\nHoàn tác cả\nnhật ký trigger')
    ],[(560,170,560,225),(560,370,560,425),(560,570,560,625),(560,795,560,865),(1040,498,1120,498),(1040,710,1120,710)])
    diagram('trigger',1600,760,[
      (440,20,720,145,'AFTER UPDATE trên SanPham','JOIN inserted với deleted theo MaSP'),
      (30,245,480,210,'Nhật ký trước và sau','So sánh JSON các trường\nGhi khi dữ liệu nghiệp vụ đổi'),
      (560,245,480,210,'Cảnh báo tồn và giá','Tồn dương → hết hàng\nGiá mới < 0,5× hoặc > 2×'),
      (1090,245,480,210,'Cảnh báo cận hạn','DATEDIFF(day) <= 1\nVà còn tồn'),
      (360,570,880,145,'NhatKyCanhBao','Cùng giao dịch với câu UPDATE kích hoạt')
    ],[(800,165,270,245),(800,165,800,245),(800,165,1330,245),(270,455,800,570),(800,455,800,570),(1330,455,800,570)])
    diagram('index',1500,760,[
      (180,30,1140,145,'Truy vấn báo cáo','WHERE thời gian, trạng thái, nguồn · GROUP BY MaKH'),
      (60,285,650,190,'Cột khóa','NgayTao → TrangThai\nHỗ trợ truy cập theo khoảng thời gian'),
      (790,285,650,190,'Cột INCLUDE','MaKH · TongTien · NguonDon\nBao phủ dữ liệu cần đọc và lọc'),
      (180,590,1140,145,'Kiểm chứng bằng kế hoạch thực thi và IO','Index hint không tự bảo đảm Seek hay elapsed thấp hơn')
    ],[(750,175,385,285),(750,175,1115,285),(385,475,750,590),(1115,475,750,590)])
    diagram('forecast',1500,1150,[
      (180,15,1140,145,'SQL bán hàng thực của 7 ngày trước','SALE + PAID/DELIVERED · Ngày không bán = 0'),
      (180,225,1140,145,'Tạo 10 đặc trưng và kiểm tra phạm vi','Mã SKU · Mức bán · Độ biến động · Giá và lag'),
      (40,460,650,190,'ONNX MLP','Mã và đầu vào phù hợp\nChuẩn hóa theo mean và scale'),
      (810,460,650,190,'SMA-7','SKU mới, bán thưa, không đều\nHoặc đầu vào ngoài phạm vi'),
      (180,750,1140,145,'Nhu cầu không âm → Đề xuất nhập và rủi ro','max(0, ceil(1,10 × dự báo) − tồn)'),
      (180,970,1140,145,'Stored procedure lưu kết quả','Lưu thuật toán và lý do · CAO: ghi cảnh báo')
    ],[(750,160,750,225),(750,370,365,460),(750,370,1135,460),(365,650,750,750),(1135,650,750,750),(750,895,750,970)])

def field(p,instruction):
    for typ in ['begin','separate','end']:
        r=p.add_run()
        if typ=='separate':
            it=OxmlElement('w:instrText');it.set(qn('xml:space'),'preserve');it.text=instruction;r._r.append(it);r=p.add_run()
        ch=OxmlElement('w:fldChar');ch.set(qn('w:fldCharType'),typ);r._r.append(ch)

def softwrap(text):
    # Preserve visible identifiers while letting Word wrap long source paths in narrow cells.
    return re.sub(r'([a-z0-9])([A-Z])',r'\1'+ '\u200b' +r'\2',text).replace('_','_\u200b').replace('/','/\u200b')

def create():
    assert hashlib.sha256(REF.read_bytes()).hexdigest()==EXPECTED_HASH,'Reference changed'
    figures()
    doc=Document(REF);body=doc._element.body
    old=list(body);cover=deepcopy(old[:22]);front_section=deepcopy(old[179]);main_section=deepcopy(old[-1])
    for e in list(body):body.remove(e)
    body.append(main_section)
    for e in cover:body.insert(len(body)-1,e)
    # Keep the original cover's positions, direct formatting and verified author metadata.
    replacements={
      'TÍCH HỢP TÌM KIẾM NGỮ NGHĨA,':'THỦ TỤC LƯU GIAO DỊCH TRIGGER',
      'DỰ BÁO NHU CẦU VÀ CHATBOT':'CHỈ MỤC VÀ CÁC PHÂN HỆ AI'}
    for p in list(body)[:22]:
        text=''.join(t.text or '' for t in p.iter(qn('w:t')))
        if text in replacements:
            texts=list(p.iter(qn('w:t')));texts[0].text=replacements[text]
            for t in texts[1:]:t.text=''
        if text=='DAILYMART':
            pp=p.find(qn('w:pPr'));style=pp.find(qn('w:pStyle'))
            if style is None:style=OxmlElement('w:pStyle');pp.insert(0,style)
            style.set(qn('w:val'),'Title')
            borders=OxmlElement('w:pBdr')
            for side in ['top','left','bottom','right','between','bar']:
                edge=OxmlElement('w:'+side);edge.set(qn('w:val'),'nil');borders.append(edge)
            pp.append(borders)
            for run in p.iter(qn('w:r')):
                rp=run.find(qn('w:rPr'))
                if rp is None:rp=OxmlElement('w:rPr');run.insert(0,rp)
                col=rp.find(qn('w:color'))
                if col is None:col=OxmlElement('w:color');rp.append(col)
                col.attrib.clear();col.set(qn('w:val'),'000000')
                faces=rp.find(qn('w:rFonts'))
                if faces is None:faces=OxmlElement('w:rFonts');rp.append(faces)
                faces.attrib.clear()
                for face in ['ascii','hAnsi','eastAsia','cs']:faces.set(qn('w:'+face),'Times New Roman')

    def runs(p,text):
        for part in re.split(r'(\*\*.*?\*\*|`[^`]+`)',text):
            r=p.add_run(part.strip('`') if part.startswith('`') else part[2:-2] if part.startswith('**') else part)
            if part.startswith('**'):r.bold=True
            elif part.startswith('`'):r.font.name='Consolas';r.font.size=Pt(10)

    def para(text,style=None):
        p=doc.add_paragraph(style=style);runs(p,text)
        if style is None:p.alignment=WD_ALIGN_PARAGRAPH.JUSTIFY
        return p

    def table(records):
        n=len(records[0]);t=doc.add_table(rows=1,cols=n);t.style='Table Grid';t.autofit=False;t.alignment=WD_TABLE_ALIGNMENT.CENTER
        widths=([5.3,10.7] if n==2 else [4.0,6.0,6.0] if n==3 else [16/n]*n)
        for col,w in zip(t.columns,widths):col.width=Cm(w)
        # Explicit grid width, margins and repeating header inherited from reference pattern.
        pr=t._tbl.tblPr;borders=OxmlElement('w:tblBorders')
        for side in ['top','left','bottom','right','insideH','insideV']:
            node=OxmlElement('w:'+side);node.set(qn('w:val'),'single');node.set(qn('w:sz'),'4');node.set(qn('w:color'),'D9D9D9');borders.append(node)
        pr.append(borders)
        for i,record in enumerate(records):
            row=t.rows[0] if i==0 else t.add_row()
            trpr=row._tr.get_or_add_trPr();trpr.append(OxmlElement('w:cantSplit'))
            if i==0:trpr.append(OxmlElement('w:tblHeader'))
            for cell,text,w in zip(row.cells,record,widths):
                cell.width=Cm(w);cell.vertical_alignment=WD_CELL_VERTICAL_ALIGNMENT.CENTER
                tcpr=cell._tc.get_or_add_tcPr();marg=OxmlElement('w:tcMar')
                for side in ['top','left','bottom','right']:
                    node=OxmlElement('w:'+side);node.set(qn('w:w'),'80');node.set(qn('w:type'),'dxa');marg.append(node)
                tcpr.append(marg)
                if i==0:
                    shade=OxmlElement('w:shd');shade.set(qn('w:fill'),'E8EEF3');tcpr.append(shade)
                p=cell.paragraphs[0];p.paragraph_format.first_line_indent=Cm(0);p.paragraph_format.line_spacing=1.08;p.paragraph_format.space_before=Pt(2);p.paragraph_format.space_after=Pt(2)
                p.paragraph_format.keep_with_next=(i<len(records)-1) if len(records)<=7 else (i<2)
                r=p.add_run(softwrap(text));r.font.size=Pt(10.5);r.bold=i==0
        spacer=doc.add_paragraph();spacer.paragraph_format.space_after=Pt(0);spacer.paragraph_format.space_before=Pt(0);spacer.paragraph_format.line_spacing=0.5;spacer.add_run().font.size=Pt(2)
        return t

    def title(text):
        p=para(text);p.alignment=WD_ALIGN_PARAGRAPH.CENTER;p.paragraph_format.first_line_indent=Cm(0);p.paragraph_format.page_break_before=True;p.paragraph_format.keep_with_next=True
        for r in p.runs:r.bold=True;r.font.size=Pt(16)
        return p

    def parse(md):
        lines=md.splitlines();i=0
        while i<len(lines):
            line=lines[i].strip()
            if not line:i+=1;continue
            if line.startswith('```'):
                i+=1
                while i<len(lines) and not lines[i].startswith('```'):
                    p=para(lines[i] or ' ','Code');p.paragraph_format.keep_with_next=False;i+=1
                i+=1;continue
            if line.startswith('@Table '):
                p=para(line[7:],'TableCaption');p.paragraph_format.keep_with_next=True;i+=1;continue
            if line.startswith('|'):
                records=[]
                while i<len(lines) and lines[i].strip().startswith('|'):
                    record=[v.strip() for v in lines[i].strip().strip('|').split('|')]
                    if not all(re.fullmatch(':?-+:?',v.replace(' ','')) for v in record):records.append(record)
                    i+=1
                table(records);continue
            if line.startswith('!['):
                label,path=re.match(r'!\[(.*?)\]\((.*?)\)',line).groups();p=doc.add_paragraph();p.alignment=WD_ALIGN_PARAGRAPH.CENTER;p.paragraph_format.first_line_indent=Cm(0);p.paragraph_format.keep_with_next=True;p.paragraph_format.space_after=Pt(0)
                image=REPORT/path;w,h=Image.open(image).size;width=min(16,13.5*w/h)
                p.add_run().add_picture(str(image),width=Cm(width));cap=para(label,'FigureCaption');cap.alignment=WD_ALIGN_PARAGRAPH.CENTER;i+=1;continue
            if line.startswith('#'):
                level=len(line)-len(line.lstrip('#'));doc.add_heading(line[level:].strip(),min(level,3));i+=1;continue
            text=[line];i+=1
            while i<len(lines) and lines[i].strip() and not lines[i].startswith(('#','|','```','![','@Table ')):
                text.append(lines[i].strip());i+=1
            p=para(' '.join(text))
            if text[0].startswith('['):
                p.paragraph_format.first_line_indent=Cm(0);p.paragraph_format.line_spacing=1.1;p.paragraph_format.space_after=Pt(8)
                for r in p.runs:r.text=softwrap(r.text);r.font.size=Pt(11)

    abstract,main=SOURCE.read_text(encoding='utf8').split('# Chương 1.',1)
    parse(abstract)
    for label,instruction in [('MỤC LỤC',' TOC \\o "1-2" \\h \\z \\u '),('DANH MỤC HÌNH',' TOC \\t "FigureCaption,1" \\h \\z '),('DANH MỤC BẢNG',' TOC \\t "TableCaption,1" \\h \\z ')]:
        title(label);p=doc.add_paragraph();p.paragraph_format.first_line_indent=Cm(0);field(p,instruction)
    title('DANH MỤC TỪ VIẾT TẮT')
    table([['Từ viết tắt','Diễn giải'],['ACID','Nguyên tử, nhất quán, cô lập và bền vững của giao dịch'],['API','Giao diện lập trình ứng dụng'],['BM25','Hàm xếp hạng từ khóa dựa trên tần suất và độ dài tài liệu'],['COD','Thanh toán khi nhận hàng'],['CSRF','Giả mạo yêu cầu liên trang'],['CSDL / ERD','Cơ sở dữ liệu / Sơ đồ thực thể và quan hệ'],['JDBC / JPA','Các cơ chế truy cập dữ liệu từ Java'],['MLP / ONNX','Mạng nơ-ron nhiều lớp / Định dạng trao đổi mô hình'],['RRF','Hợp nhất thứ hạng theo nghịch đảo vị trí'],['SMA-7','Trung bình bán bảy ngày'],['SP / SKU','Thủ tục lưu / Đơn vị hàng hóa được quản lý riêng'],['MAE / RMSE','Sai số tuyệt đối trung bình / Căn sai số bình phương trung bình'],['WAPE','Tổng sai số tuyệt đối chia tổng lượng thực tế']])
    body.insert(len(body)-1,front_section)
    parse('# Chương 1.'+main)
    # New document content, old style/section/header systems and untouched package parts.
    with ZipFile(REF) as z:parts={i.filename:z.read(i.filename) for i in z.infolist()};infos={i.filename:i for i in z.infolist()}
    parts['word/document.xml']=etree.tostring(doc._element,encoding='UTF-8',xml_declaration=True,standalone=True)
    original_rels=etree.fromstring(parts['word/_rels/document.xml.rels']);ids={e.get('Id') for e in original_rels}
    for rid,rel in doc.part.rels.items():
        if rid not in ids:
            node=etree.SubElement(original_rels,'{http://schemas.openxmlformats.org/package/2006/relationships}Relationship');node.set('Id',rid);node.set('Type',rel.reltype);node.set('Target',rel.target_ref)
            if rel.is_external:node.set('TargetMode','External')
            else:parts[str(rel.target_part.partname).lstrip('/')]=rel.target_part.blob
    parts['word/_rels/document.xml.rels']=etree.tostring(original_rels,encoding='UTF-8',xml_declaration=True,standalone=True)
    settings=etree.fromstring(parts['word/settings.xml']);up=settings.find(qn('w:updateFields'))
    if up is None:up=OxmlElement('w:updateFields');settings.append(up)
    up.set(qn('w:val'),'true');parts['word/settings.xml']=etree.tostring(settings,encoding='UTF-8',xml_declaration=True,standalone=True)
    with ZipFile(OUTPUT,'w',ZIP_DEFLATED) as z:
        for name,data in parts.items():z.writestr(infos[name] if name in infos else name,data)
    allowed={'word/document.xml','word/_rels/document.xml.rels','word/settings.xml'}
    with ZipFile(REF) as a,ZipFile(OUTPUT) as b:
        unchanged=[n for n in a.namelist() if n not in allowed and a.read(n)==b.read(n)]
        assert all(a.read(n)==b.read(n) for n in a.namelist() if n not in allowed)
    print(json.dumps({'output':str(OUTPUT),'paragraphs':len(doc.paragraphs),'tables':len(doc.tables),'figures':len(doc.inline_shapes),'preservedParts':len(unchanged),'referenceSha256':EXPECTED_HASH},ensure_ascii=False))

if __name__=='__main__':create()
