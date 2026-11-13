"""Download illustrative Wikimedia photos and write a repeatable SQL image seed.

Run from repository root: python database/download_catalog_images.py
Images are persisted in backend/.local/product-images; source URLs are recorded.
These are generic illustrations, not photographs of the actual stock/brands.
"""
from pathlib import Path
import concurrent.futures
import json
import urllib.parse
import urllib.request
import time
import html
import re

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'backend/.local/product-images'
DEST.mkdir(parents=True, exist_ok=True)
CATALOG = [
('Cá lóc tươi làm sạch', 'Striped snakehead'),
('Thịt ba rọi heo VietGAP', 'Pork belly'),
('Rau muống nước sạch', 'Ipomoea aquatica'),
('Cà chua tươi Đà Lạt', 'Tomato'),
('Trứng gà ta Ba Huân (vỉ 10)', 'Egg as food'),
('Tôm sú tươi sống', 'Penaeus monodon'),
('Bắp cải trắng Đà Lạt', 'Cabbage'),
('Chuối sứ chín tự nhiên', 'Banana'),
('Cá hồi phi lê Nauy tươi', 'Salmon as food'),
('Thịt bò bắp Úc tươi', 'Beef shank'),
('Xoài cát Hòa Lộc', 'Mango'),
('Đậu hũ non trứng', 'Tofu'),
('Cải thìa VietGAP', 'Bok choy'),
('Cải ngọt tươi', 'Choy sum'),
('Rau dền đỏ', 'Amaranthus tricolor'),
('Mồng tơi xanh', 'Basella alba'),
('Bí đỏ hồ lô', 'Butternut squash'),
('Bí đao xanh', 'Wax gourd'),
('Cà rốt Đà Lạt', 'Carrot'),
('Khoai tây vàng', 'Potato'),
('Dưa leo baby', 'Cucumber'),
('Khổ qua xanh', 'Bitter melon'),
('Nấm kim châm 150g', 'Enokitake'),
('Bông cải xanh', 'Broccoli'),
('Sườn non heo', 'Pork ribs'),
('Thịt nạc vai heo', 'Pork'),
('Thịt heo xay 500g', 'Ground pork'),
('Chân giò heo', 'Ham hock'),
('Ức gà phi lê', 'Chicken as food'),
('Đùi gà góc tư', 'Chicken as food'),
('Cánh gà tươi', 'Chicken as food'),
('Gà ta nguyên con', 'Chicken as food'),
('Thịt bò thăn', 'Beef tenderloin'),
('Trứng vịt vỉ 10 quả', 'Egg as food'),
('Cá basa cắt khúc', 'Basa (fish)'),
('Cá diêu hồng tươi', 'Tilapia'),
('Cá thu cắt lát', 'Mackerel as food'),
('Cá nục tươi', 'Decapterus maruadsi'),
('Tôm thẻ tươi', 'Whiteleg shrimp'),
('Mực ống tươi', 'Squid as food'),
('Bạch tuộc tươi', 'Octopus as food'),
('Nghêu trắng', 'Clam'),
('Sò huyết tươi', 'Tegillarca granosa'),
('Chả cá thác lác 300g', 'Fish paste'),
('Cam sành miền Tây', 'Orange (fruit)'),
('Bưởi da xanh Bến Tre', 'Pomelo'),
('Dưa hấu ruột đỏ', 'Watermelon'),
('Thanh long ruột đỏ', 'Pitaya'),
('Ổi nữ hoàng', 'Guava'),
('Dứa chín gọt sẵn', 'Pineapple'),
('Đu đủ chín', 'Papaya'),
('Chôm chôm nhãn', 'Rambutan'),
('Quýt đường', 'Mandarin orange'),
('Dừa xiêm Bến Tre', 'Coconut'),
('Nước mắm cá cơm 500ml', 'Fish sauce'),
('Dầu ăn đậu nành 1 lít', 'Soybean oil'),
('Đường trắng 1kg', 'Sugar'),
('Muối i-ốt 500g', 'Salt'),
('Gạo thơm ST25 túi 5kg', 'Rice'),
('Bún gạo khô 400g', 'Rice vermicelli'),
]

# Explicit food photographs replace article lead images showing diagrams/dishes.
FILE_OVERRIDES = {
    5: 'Egg cartons with chicken eggs 03.jpg',
    10: 'Beef shank.jpg', 21: 'Kurkkuja.jpg',
    25: 'Raw pork spareribs.jpg', 26: 'Diced Pork Shoulder (13152872373).jpg',
    27: 'Pork mince.jpg', 28: 'Pork knuckle bones.jpg',
    29: 'Chicken breast, Mittelheim.jpg', 30: 'Chicken drumsticks and legs.jpg',
    31: 'Chicken wings.jpg', 32: 'PoussinOnHand.JPG',
    33: 'Beef Tenderloin150.jpg', 34: 'Egg cartons with chicken eggs 03.jpg',
    38: 'Decapterus maruadsi.jpg', 42: 'Meretrix lyrata 01.jpg',
    44: 'Fishballs.JPG', 59: 'Rice grains (IRRI).jpg',
}

def get(url):
    for attempt in range(4):
        try:
            request = urllib.request.Request(url, headers={'User-Agent': 'DailyMart-CourseDemo/1.0 (educational image attribution)'})
            with urllib.request.urlopen(request, timeout=45) as response:
                return response.read(), response.headers.get_content_type()
        except Exception:
            if attempt == 3:
                raise
            time.sleep(2 + attempt * 2)

titles = list(dict.fromkeys(title for _, title in CATALOG))
pages = {}
for offset in range(0, len(titles), 25):
    params = {'action': 'query', 'format': 'json', 'redirects': 1, 'prop': 'pageimages',
              'piprop': 'thumbnail|name', 'pithumbsize': 640, 'titles': '|'.join(titles[offset:offset+25])}
    data = json.loads(get('https://en.wikipedia.org/w/api.php?' + urllib.parse.urlencode(params))[0])
    fetched = {p['title']: p for p in data['query']['pages'].values()}
    redirects = {p['from']: p['to'] for p in data['query'].get('redirects', [])}
    for title in titles[offset:offset+25]:
        pages[title] = fetched.get(redirects.get(title, title), {})

params = {'action': 'query', 'format': 'json', 'prop': 'imageinfo',
          'iiprop': 'url|extmetadata', 'iiurlwidth': 640,
          'titles': '|'.join('File:' + title for title in set(FILE_OVERRIDES.values()))}
override_data = json.loads(get('https://commons.wikimedia.org/w/api.php?' + urllib.parse.urlencode(params))[0])
overrides = {p['title']: p['imageinfo'][0] for p in override_data['query']['pages'].values() if p.get('imageinfo')}

def download(entry):
    index, (name, title) = entry
    page = pages[title]
    override = FILE_OVERRIDES.get(index+1)
    metadata = overrides.get('File:' + override) if override else None
    if metadata:
        page = {'thumbnail': {'source': metadata.get('thumburl', metadata['url'])}, 'pageimage': override}
    if not page.get('thumbnail'):
        return {'name': name, 'error': 'No thumbnail', 'title': title}
    url = page['thumbnail']['source']
    content, kind = get(url)
    extension = {'image/jpeg': 'jpg', 'image/png': 'png', 'image/webp': 'webp'}.get(kind)
    if not extension:
        return {'name': name, 'error': kind, 'title': title}
    filename = f'demo-{index+1:02}.{extension}'
    (DEST / filename).write_bytes(content)
    return {'name': name, 'filename': filename, 'source': url,
            'sourcePage': 'https://commons.wikimedia.org/wiki/File:' + urllib.parse.quote(page['pageimage']),
            'article': title, 'metadata': metadata.get('extmetadata', {}) if metadata else {}}

with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
    results = list(pool.map(download, enumerate(CATALOG)))
for offset in range(0, len(results), 30):
    batch = [r for r in results[offset:offset+30] if 'sourcePage' in r]
    params = {'action': 'query', 'format': 'json', 'prop': 'imageinfo', 'iiprop': 'extmetadata',
              'titles': '|'.join(urllib.parse.unquote(r['sourcePage'].split('/wiki/')[1]) for r in batch)}
    info = json.loads(get('https://commons.wikimedia.org/w/api.php?' + urllib.parse.urlencode(params))[0])
    by_title = {p['title'].replace('_', ' ').casefold(): p.get('imageinfo', [{}])[0].get('extmetadata', {}) for p in info['query']['pages'].values()}
    for row in batch:
        row['metadata'] = by_title.get(urllib.parse.unquote(row['sourcePage'].split('/wiki/')[1]).replace('_', ' ').casefold(), {})
(ROOT / 'database/catalog-image-sources.json').write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding='utf-8')
credits = ['<!doctype html><html lang="vi"><meta charset="utf-8"><title>Nguồn ảnh sản phẩm</title>',
           '<style>body{font:16px system-ui;max-width:960px;margin:40px auto;padding:20px;line-height:1.6}li{margin:20px 0}img{max-width:120px;max-height:90px}</style>',
           '<a href="/">← Về cửa hàng</a><h1>Nguồn ảnh sản phẩm minh họa</h1>',
           '<p>Ảnh minh họa nguyên liệu cho dữ liệu mẫu; không thể hiện chính xác nhãn hiệu, quy cách hoặc lô hàng thực tế. Ảnh từ Wikimedia Commons, giữ nguyên nội dung và thu nhỏ để hiển thị.</p><ul>']
for row in results:
    if 'filename' not in row:
        continue
    meta = row.get('metadata', {})
    artist = html.escape(html.unescape(re.sub('<[^>]+>', '', meta.get('Artist', {}).get('value', ''))))
    license_name = meta.get('LicenseShortName', {}).get('value', 'Xem giấy phép tại nguồn')
    credits.append('<li><strong>' + html.escape(row['name']) + '</strong><br>'
                   + artist + ' — ' + html.escape(license_name) + ' · <a href="'
                   + html.escape(row['sourcePage'], quote=True) + '">Nguồn &amp; giấy phép ảnh</a></li>')
credits.append('</ul></html>')
(ROOT / 'frontend/public/product-image-credits.html').write_text('\n'.join(credits), encoding='utf-8')
sql = ['-- Generic illustrative photos; attribution: catalog-image-sources.json', 'USE FreshGroceryDB;', 'GO']
for row in results:
    if 'filename' in row:
        name = row['name'].replace("'", "''")
        sql.append(f"UPDATE dbo.SanPham SET HinhAnh = N'/api/v1/product-images/{row['filename']}' WHERE TenSP = N'{name}' AND (HinhAnh IS NULL OR HinhAnh = N'' OR HinhAnh LIKE N'/api/v1/product-images/demo-%');")
(ROOT / 'database/08_catalog_images.sql').write_text('\n'.join(sql)+'\nGO\n', encoding='utf-8')
print(json.dumps({'downloaded': sum('filename' in row for row in results), 'errors': [r for r in results if 'error' in r]}, ensure_ascii=True))
