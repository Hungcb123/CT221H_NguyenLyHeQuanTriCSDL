from pathlib import Path
from zipfile import ZipFile
import json,re
from lxml import etree
ROOT=Path(__file__).resolve().parents[2];R=ROOT/'report';docx=R/'BaoCao_DailyMart_CT221H_TiengViet.docx'
with ZipFile(docx) as z:
 root=etree.fromstring(z.read('word/document.xml'));ns={'w':'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
 text='\n'.join(root.xpath('//w:t/text()',namespaces=ns))
 assert 'CT221H' in text and 'CT222H' not in text
 names=['Cao Tường Hưng','Trang Minh Chánh','Nguyễn Châu Nhựt Cần']
 assert [text.index(n) for n in names]==sorted(text.index(n) for n in names)
 assert 'TS. Cù Vĩnh Lộc' in text
 for w in ['B2303873','B2303864','B2303863','89,95%','5,34%','view','stored procedure','trigger','chatbot','SePay']:
  assert w.lower() in text.lower(),w
 assert not any(w in text for w in ['TODO','FIXME','[Tên sinh viên]','[MSSV]'])
 headings=root.xpath('//w:p[w:pPr/w:pStyle[@w:val="Heading1"]]//w:t/text()',namespaces=ns)
 captions=root.xpath('//w:p[w:pPr/w:pStyle[@w:val="FigureCaption"]]//w:t/text()',namespaces=ns)
 instructions=root.xpath('//w:instrText/text()',namespaces=ns)
 assert len([x for x in instructions if x.strip().startswith('TOC')])==3
 assert len(captions)==10
result={'docx_bytes':docx.stat().st_size,'chapters':len([h for h in headings if h.startswith('CHƯƠNG')]),'figures':len(captions),'team_order':names,'placeholders':0}
pdf=R/'BaoCao_DailyMart_CT221H_TiengViet.pdf'
if pdf.exists():
 import fitz
 from PIL import Image,ImageDraw
 d=fitz.open(pdf);result['pdf_pages']=len(d)
 problems=[];pages=[]
 for i,p in enumerate(d):
  txt=p.get_text();pages.append({'page':i+1,'chars':len(txt),'start':txt[:130].replace('\n',' | ')})
  if len(txt.strip())<150:problems.append({'page':i+1,'issue':'near-empty'})
  if any(w in txt.lower() for w in ['error! reference','error! bookmark','lỗi! không tìm','mục lục được cập nhật']):problems.append({'page':i+1,'issue':'field error'})
 result['pdf_problems']=problems
 (R/'CT221H_assets/page_inventory.json').write_text(json.dumps(pages,ensure_ascii=False,indent=2))
 previews=R/'CT221H_assets/preview';previews.mkdir(exist_ok=True)
 for group in range((len(d)+11)//12):
  sheet=Image.new('RGB',(960,1456),'#e3e5e8');draw=ImageDraw.Draw(sheet)
  for j in range(12):
   ix=group*12+j
   if ix>=len(d):break
   pix=d[ix].get_pixmap(matrix=fitz.Matrix(.45,.45),alpha=False)
   im=Image.frombytes('RGB',(pix.width,pix.height),pix.samples);im.thumbnail((295,330))
   x=(j%3)*320+(320-im.width)//2;y=(j//3)*364+24
   sheet.paste(im,(x,y));draw.text(((j%3)*320+12,(j//3)*364+5),f'Trang {ix+1}',fill='black')
  sheet.save(previews/f'contact_{group+1}.jpg',quality=90)
 result['contact_sheets']=(len(d)+11)//12
(R/'CT221H_assets/validation_checks.json').write_text(json.dumps(result,ensure_ascii=False,indent=2))
print(json.dumps(result,ensure_ascii=False))
