"""Local integration checks; leaves the cart contents unchanged."""
import urllib.request
import urllib.error
import http.cookiejar
import json

BASE = 'http://localhost:3000'
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
token = ''

def request(path, method='GET', body=None, authenticated=True):
    headers = {'Content-Type': 'application/json'}
    if authenticated:
        headers['X-Customer-CSRF'] = token
    try:
        response = opener.open(urllib.request.Request(BASE+path, method=method, headers=headers,
                    data=json.dumps(body).encode() if body is not None else None), timeout=45)
        raw = response.read()
        return response.status, json.loads(raw)
    except urllib.error.HTTPError as error:
        return error.code, json.loads(error.read())

def session():
    global token
    status, payload = request('/api/v1/customer/auth/session')
    assert status == 200, payload
    token = payload['data']['csrfToken']
    return payload['data']['customer']

def login(email):
    global token
    session()
    status, payload = request('/api/v1/customer/auth/login', 'POST', {'email':email,'password':'DailyMart123!'})
    assert status == 200, payload
    token = payload['data']['csrfToken']
    return payload['data']['customer']

session()
assert request('/api/v1/orders/customer/1')[0] == 401
assert request('/api/v1/customer/auth/login', 'POST', {'email':'nguyenvanan@ctu.edu.vn','password':'DailyMart123!'}, False)[0] == 403
assert request('/api/v1/customer/auth/login', 'POST', {'email':'nguyenvanan@ctu.edu.vn','password':'wrong'})[0] == 401
customer = login('nguyenvanan@ctu.edu.vn')
assert customer['maKH'] == 1, customer
status, payload = request('/api/v1/orders/customer/1?page=1&size=20')
assert status == 200 and isinstance(payload['data'], list), payload
assert all(order['maKH'] == 1 for order in payload['data'])
assert all('Benchmark CT221H' not in (order.get('ghiChu') or '') for order in payload['data'])
assert request('/api/v1/orders/customer/2')[0] == 403
assert request('/api/v1/cart/2')[0] == 403
assert request('/api/v1/wallets/2')[0] == 403
assert request('/api/v1/customer/auth/logout','POST')[0] == 200
assert session() is None
assert request('/api/v1/orders/customer/1')[0] == 401
print('Login, CSRF, customer ownership, order list and logout: PASS')

customer = login('levancuong@ctu.edu.vn')
status, cart = request('/api/v1/cart/3')
assert status == 200, cart
old = {i['maSP']:i['soLuong'] for i in cart['data']['items']}
_, recipes = request('/api/v1/ai/chatbot/recipes')
available = [r for r in recipes['data'] if r['duNguyenLieuTrongKho']]
assert available, recipes
recipe = next(r for r in available if not any(i['maSP'] in old for i in r['nguyenLieuList']))
status, chat = request('/api/v1/ai/chatbot/chat','POST',{'message':'Nấu '+recipe['tenMonAn']})
assert status == 200 and chat['data']['recipeSuggested']['maCongThuc'] == recipe['maCongThuc'], chat
assert chat['data']['canAddToCart']
# Request more servings than the demo inventory can supply.
status, unavailable = request('/api/v1/ai/chatbot/chat','POST',{'message':'Nấu canh chua cá lóc cho 1000000 người'})
assert status == 200 and unavailable['data']['suggestedRecipes'], unavailable
assert unavailable['data'].get('recipeSuggested') is None
assert all(r['duNguyenLieuTrongKho'] for r in unavailable['data']['suggestedRecipes'])
try:
    status, result = request(f"/api/v1/ai/chatbot/recipes/{recipe['maCongThuc']}/cart",'POST',{'customerId':3,'servings':recipe['khauPhan']})
    assert status == 200, result
    _, cart = request('/api/v1/cart/3')
    current = {i['maSP']:i['soLuong'] for i in cart['data']['items']}
    assert all(i['maSP'] in current for i in recipe['nguyenLieuList'])
finally:
    for item in recipe['nguyenLieuList']:
        assert request('/api/v1/cart/items','DELETE',{'maKH':3,'maSP':item['maSP']})[0] == 200
_, cart = request('/api/v1/cart/3')
assert old == {i['maSP']:i['soLuong'] for i in cart['data']['items']}
assert request('/api/v1/customer/auth/logout','POST')[0] == 200
print('Live available recipes, feasible alternatives and atomic recipe cart (cart restored): PASS')
