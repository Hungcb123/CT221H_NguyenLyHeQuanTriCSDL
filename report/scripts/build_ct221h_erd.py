"""Render both schema diagrams from the report's SQL sources, with every column."""
from pathlib import Path
from html import escape
import ast
import json
import os
import re
import subprocess

ROOT = Path(__file__).resolve().parents[2]
SOURCES = [ROOT/'database/01_init_schema.sql', ROOT/'backend/src/main/java/com/ctu/freshgrocery/customer/CustomerAuthController.java', ROOT/'backend/src/main/resources/db/sepay.sql']
GROUPS = {
    'erd_core': ['DanhMuc', 'SanPham', 'ChiTietGioHang', 'KhachHangDangNhap', 'KhachHang', 'GioHang', 'LichSuGiaoDichVi', 'DonHang', 'ChiTietDonHang'],
    'erd_extended': ['CongThucMonAn', 'NguyenLieuMonAn', 'DuBaoNhapHang', 'ChuongTrinhKhuyenMai', 'ChiTietKhuyenMai', 'ThanhToanSePay', 'NhatKyCanhBao', 'ThongBaoKhachHang', 'SePayWebhook'],
}
# Declared FK pairs, including the FK added after CREATE TABLE.
FKS = [
 ('DanhMuc','MaDanhMuc','SanPham','MaDanhMuc'), ('SanPham','MaSP','SanPham','MaSPCha'),
 ('KhachHang','MaKH','KhachHangDangNhap','MaKH'), ('KhachHang','MaKH','GioHang','MaKH'),
 ('GioHang','MaGioHang','ChiTietGioHang','MaGioHang'), ('SanPham','MaSP','ChiTietGioHang','MaSP'),
 ('KhachHang','MaKH','DonHang','MaKH'), ('DonHang','MaDH','ChiTietDonHang','MaDH'),
 ('SanPham','MaSP','ChiTietDonHang','MaSP'), ('ChuongTrinhKhuyenMai','MaKM','ChiTietDonHang','MaKM'),
 ('KhachHang','MaKH','LichSuGiaoDichVi','MaKH'), ('DonHang','MaDH','LichSuGiaoDichVi','MaDH'),
 ('SanPham','MaSP','DuBaoNhapHang','MaSP'), ('CongThucMonAn','MaCongThuc','NguyenLieuMonAn','MaCongThuc'),
 ('SanPham','MaSP','NguyenLieuMonAn','MaSP'), ('ChuongTrinhKhuyenMai','MaKM','ChiTietKhuyenMai','MaKM'),
 ('SanPham','MaSP','ChiTietKhuyenMai','MaSP'), ('DonHang','MaDH','ThanhToanSePay','MaDH'),
 ('KhachHang','MaKH','ThanhToanSePay','MaKH'),
]


def schema():
    # Reuse the report parser without executing its document-building statements.
    source = ROOT/'report/scripts/build_ct221h_docx.py'
    tree = ast.parse(source.read_text())
    funcs = [n for n in tree.body if isinstance(n, ast.FunctionDef) and n.name in {'tables_in','columns'}]
    scope = {'re': re}
    exec(compile(ast.Module(body=funcs, type_ignores=[]), str(source), 'exec'), scope)
    tables = {}
    for path in SOURCES:
        for name, body in scope['tables_in'](path.read_text()):
            cols = []
            composite = set()
            for match in re.finditer(r'PRIMARY\s+KEY\s*\(([^)]+)\)', body, re.I):
                composite.update(x.strip() for x in match.group(1).split(','))
            for col, typ, rules in scope['columns'](body):
                flags = []
                if 'Khóa chính' in rules or col in composite: flags.append('PK')
                if any(child == name and key == col for _, _, child, key in FKS): flags.append('FK')
                if 'Duy nhất' in rules: flags.append('unique')
                if 'Cho phép NULL' in rules: flags.append('NULL')
                cols.append({'name': col, 'type': typ, 'flags': flags})
            tables[name] = cols
    # ALTER ADD columns missing from original CREATE TABLE declarations.
    for table, col, typ, flags, source in [
        ('LichSuGiaoDichVi','MaYeuCauNap','UNIQUEIDENTIFIER',['NULL'], SOURCES[0]),
        ('DonHang','PhuongThucThanhToan','VARCHAR(20)',[], SOURCES[2]),
    ]:
        assert re.search(r'ADD\s+'+col+r'\s+', source.read_text(), re.I)
        if col not in {c['name'] for c in tables[table]}:
            tables[table].append({'name': col, 'type': typ, 'flags': flags})
    assert set(tables) == set(sum(GROUPS.values(), [])) and len(tables) == 18
    return tables


def node(name, cols):
    rows = [f'<TR><TD WIDTH="160" ALIGN="CENTER" BGCOLOR="black"><FONT COLOR="white"><B>{escape(name)}</B></FONT></TD></TR>']
    for col in cols:
        label = col['name']
        if col['flags']: label += ' ('+', '.join(col['flags'])+')'
        rows.append(f'<TR><TD WIDTH="160" PORT="{col["name"]}" ALIGN="LEFT">{escape(label)}</TD></TR>')
    return f'{name} [label=<<TABLE BORDER="1" CELLBORDER="0" CELLSPACING="0" CELLPADDING="3">'+''.join(rows)+'</TABLE>>];'


def build_erd(out=None):
    out = out or ROOT/'report/CT221H_assets'
    out.mkdir(parents=True, exist_ok=True)
    tables = schema()
    for group, names in GROUPS.items():
        lines = ['digraph G {', 'graph [rankdir=LR, bgcolor="white", pad="0.25", nodesep="0.5", ranksep="0.65", splines=polyline, outputorder=edgesfirst];',
                 'node [shape=plain, fontname="Arial", fontsize=12];',
                 'edge [color="black", penwidth=0.75, fontname="Arial", fontsize=10, arrowsize=0.6];']
        lines += [node(name, tables[name]) for name in names]
        if group == 'erd_core':
            ranks = [['DanhMuc','KhachHangDangNhap','LichSuGiaoDichVi'],['SanPham','KhachHang','DonHang'],['ChiTietGioHang','GioHang','ChiTietDonHang']]
        else:
            ranks = [['CongThucMonAn','ChuongTrinhKhuyenMai','NhatKyCanhBao'],['NguyenLieuMonAn','ChiTietKhuyenMai','ThongBaoKhachHang'],['DuBaoNhapHang','ThanhToanSePay','SePayWebhook']]
        for rank in ranks:
            lines.append('{rank=same; '+';'.join(rank)+';}')
            lines += [f'{a}->{b} [style=invis, weight=30];' for a,b in zip(rank, rank[1:])]
        for parent, parentkey, child, childkey in FKS:
            if child not in names or parent not in names: continue
            assert parentkey in {c['name'] for c in tables[parent]}
            assert childkey in {c['name'] for c in tables[child]}
            maximum = '1' if 'unique' in next(c['flags'] for c in tables[child] if c['name']==childkey) or child=='KhachHangDangNhap' else 'N'
            # Attach to the entity border, avoiding lines through its attributes.
            if parent == child:
                lines.append(f'{parent}:n->{child}:n [xlabel="cha / loại", constraint=false];')
            else:
                lines.append(f'{parent}->{child} [xlabel="1 : 0..{maximum}", constraint=false];')
        # Keep grid columns ordered without allowing FK arrows to distort the layout.
        lines += [f'{a[0]}->{b[0]} [style=invis, weight=50];' for a,b in zip(ranks,ranks[1:])]
        lines.append('}')
        dot = out/(group+'.dot'); dot.write_text('\n'.join(lines))
        for fmt in ['svg','png']:
            subprocess.run([os.environ.get('DOT_BINARY','dot'), '-T'+fmt, '-Gdpi=240', str(dot), '-o', str(out/(group+'.'+fmt))], check=True)
    manifest = {'sources':[str(p.relative_to(ROOT)) for p in SOURCES], 'table_count':len(tables), 'column_count':sum(map(len,tables.values())), 'tables':tables, 'groups':GROUPS}
    (out/'erd_schema_manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2))
    print(f'Generated 2 diagrams: {len(tables)} tables, {manifest["column_count"]} columns, including ALTER ADD columns.')


if __name__ == '__main__':
    build_erd()
