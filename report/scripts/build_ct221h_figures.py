from pathlib import Path
import subprocess,shutil
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import pandas as pd
ROOT=Path(__file__).resolve().parents[2]; OUT=ROOT/'report/CT221H_assets';OUT.mkdir(exist_ok=True)
def graph(name,body,landscape=True):
 source='digraph G { graph [rankdir='+('LR' if landscape else 'TB')+', bgcolor="white", pad="0.25", nodesep="0.35", ranksep="0.5"]; node [shape=box,style="rounded,filled",fillcolor="#F1F5F8",color="#334155",fontname="DejaVu Sans",fontsize=11,margin="0.16,0.12"]; edge [color="#64748B",fontname="DejaVu Sans",fontsize=9,arrowsize=0.65];'+body+'}'
 (OUT/(name+'.dot')).write_text(source)
 subprocess.run(['dot','-Tpng','-Gdpi=180',str(OUT/(name+'.dot')),'-o',str(OUT/(name+'.png'))],check=True)
graph('architecture','''
 user [label="Khách hàng / Quản trị viên"];
 web [label="Next.js + React\nGiao diện web"];
 api [label="Spring Boot REST API\nXác thực · Nghiệp vụ · JDBC/JPA"];
 sql [label="SQL Server 2022\nBảng · SP · Trigger · Chỉ mục"];
 vector [label="Qdrant\nChỉ mục vector 384 chiều"];
 onnx [label="ONNX Runtime Java\nEmbedding + Dự báo MLP"];
 sepay [label="SePay\nThông báo giao dịch ngân hàng"];
 user -> web;web -> api [label="HTTP / JSON"];
 api -> sql;api -> vector;api -> onnx;sepay -> api [label="Webhook có xác thực"];
''',False)
graph('erd_core','''
 node [shape=record,style="filled",fillcolor="#F8FAFC"];
 cat [label="{DanhMuc|PK MaDanhMuc}"];
 sp [label="{SanPham|PK MaSP|FK MaDanhMuc|FK MaSPCha}"];
 kh [label="{KhachHang|PK MaKH}"];
 gh [label="{GioHang|PK MaGioHang|FK MaKH UNIQUE}"];
 ctgh [label="{ChiTietGioHang|PK/FK MaGioHang|PK/FK MaSP}"];
 dh [label="{DonHang|PK MaDH|FK MaKH}"];
 ctdh [label="{ChiTietDonHang|PK/FK MaDH|PK/FK MaSP|FK MaKM}"];
 vi [label="{LichSuGiaoDichVi|PK MaGiaoDich|FK MaKH, MaDH}"];
 cat->sp [label="1–N"];kh->gh [label="1–0..1"];gh->ctgh [label="1–N"];sp->ctgh [label="1–N"];
 kh->dh [label="1–N"];dh->ctdh [label="1–N"];sp->ctdh [label="1–N"];kh->vi [label="1–N"];dh->vi [label="1–N"];
''',False)
graph('erd_extended','''
 node [shape=record,style="filled",fillcolor="#F8FAFC"];
 sp [label="{SanPham|PK MaSP}"];
 km [label="{ChuongTrinhKhuyenMai|PK MaKM}"];
 ctkm [label="{ChiTietKhuyenMai|PK, FK MaKM + MaSP}"];
 recipe [label="{CongThucMonAn|PK MaCongThuc}"];
 ing [label="{NguyenLieuMonAn|PK, FK MaCongThuc + MaSP}"];
 forecast [label="{DuBaoNhapHang|PK MaDuBao · FK MaSP}"];
 dh [label="{DonHang|PK MaDH}"];
 pay [label="{ThanhToanSePay|PK PaymentId · FK MaDH UNIQUE|FK MaKH · IdempotencyKey}"];
 evt [label="{SePayWebhook|PK TransactionId|Không có FK thanh toán}"];
 km->ctkm [label="1–N"];sp->ctkm [label="1–N"];recipe->ing [label="1–N"];sp->ing [label="1–N"];sp->forecast [label="1–N"];dh->pay [label="1–0..1"];evt->pay [style=dashed,label="không có FK trực tiếp",constraint=false];forecast->dh [style=invis];ing->evt [style=invis];
''',False)
graph('wallet_flow','''
 a [label="Xác thực khách hàng\nKiểm tra CSRF / chủ sở hữu"];
 b [label="BEGIN TRANSACTION\nKhóa ví + sản phẩm + suất ưu đãi"];
 c [label="Kiểm tra giỏ, tồn kho, hạn dùng\nTính giá tại server + kiểm tra số dư"];
 d [label="Trừ kho / ví · Tạo đơn PAID\nGhi chi tiết + sổ cái · Xóa giỏ"];
 e [label="COMMIT\nTrả kết quả từ database"];
 f [label="ROLLBACK\nKhông ghi nhận giao dịch dở dang",fillcolor="#FFF4F0"];
 a->b->c->d->e;c->f [label="vi phạm nghiệp vụ"];d->f [label="lỗi SQL"];
''',False)
graph('search_flow','''
 q [label="Truy vấn tiếng Việt"];
 bm [label="BM25 tên + mô tả\nk1 = 1,2; b = 0,75"];
 emb [label="ONNX MiniLM\nWordPiece → pooling → L2"];
 vec [label="Qdrant cosine\nXếp hạng vector"];
 rrf [label="RRF (k = 60)\nTrọng số 0,5 / 0,5"];
 policy [label="Đối chiếu loại hàng + bằng chứng\nNgưỡng 0,60 / chênh lệch 0,05"];
 out [label="products: kết quả chính\nrelatedProducts: tham khảo"];
 fall [label="KEYWORD_FALLBACK\nBM25 trọng số 1"];
 q->bm;q->emb;emb->vec;bm->rrf;vec->rrf;rrf->policy->out;emb->fall [label="lỗi ONNX/Qdrant"];fall->policy;
''',False)
graph('forecast_flow','''
 history [label="Lịch sử đơn / chi tiết\nCác ngày trước ngày dự báo"];
 feat [label="10 đặc trưng\nLịch · Giá · Mã SP · Lag · Rolling"];
 scale [label="StandardScaler\nmean / scale từ tệp cấu hình"];
 model [label="ONNX MLP\n10 → 64 → 32 → 1"];
 rule [label="Nhu cầu không âm\nNhập thêm: max(0, ceil(1,1 × dự báo) − tồn)\nRủi ro: THẤP / TRUNG BÌNH / CAO"];
 db [label="sp_LuuKetQuaDuBaoNhapHang\nDuBaoNhapHang + cảnh báo"];
 ui [label="Trang quản trị AI\nGiả lập không lưu / vận hành có lưu"];
 history->feat->scale->model->rule;rule->db;rule->ui;
''',False)
graph('chatbot_flow','''
 q [label="Tin nhắn + công thức đã chọn"];
 route [label="Nhận diện ý định bằng từ khóa / regex\nĐiều phối công cụ trong Java"];
 recipe [label="Tra công thức\nĐối chiếu nguyên liệu SQL"];
 search [label="Tìm sản phẩm hybrid\nTra khuyến mãi đang hiệu lực"];
 add [label="Thêm nguyên liệu\nPhiên đăng nhập + CSRF\nSP kiểm tra lại toàn bộ"];
 reply [label="Câu trả lời theo mẫu\nCông thức · Giá · Tồn · Liên kết checkout"];
 q->route;route->recipe;route->search;route->add;recipe->reply;search->reply;add->reply;
''',False)
plt.rcParams.update({'font.family':'DejaVu Sans','font.size':10})
df=pd.read_csv(ROOT/'benchmark-evidence/measurements.csv')
fig,axes=plt.subplots(1,3,figsize=(11,3.7))
for ax,col,title in zip(axes,['logical_reads','cpu_ms','elapsed_ms'],['Đọc trang logic / lượt','CPU trung vị (ms)','Thời gian trung vị (ms)']):
 vals=[df[df['mode']==mode][col].median() for mode in ['SCAN','COVERING']]
 bars=ax.bar(['Quét bảng','Chỉ mục bao phủ'],vals,color=['#64748B','#245478'],width=.58)
 for bar,val in zip(bars,vals):ax.text(bar.get_x()+bar.get_width()/2,val*1.025,f'{val:,.0f}'.replace(',','.'),ha='center',va='bottom')
 ax.set_title(title,pad=16);ax.set_ylim(0,max(vals)*1.2);ax.spines[['top','right']].set_visible(False);ax.grid(axis='y',alpha=.15);ax.set_axisbelow(True);ax.tick_params(axis='x',labelsize=9)
fig.tight_layout();fig.savefig(OUT/'benchmark.png',dpi=200,bbox_inches='tight');plt.close(fig)
for source,name in [('/tmp/sepay-soft-delete-1440.png','ui_soft_delete.png'),('/tmp/sepay-payment-1440.png','ui_sepay.png')]:
 if Path(source).exists():shutil.copy2(source,OUT/name)
print('Generated diagrams and benchmark chart:',OUT)
