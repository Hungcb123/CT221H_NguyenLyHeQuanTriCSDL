"""Check registration using a temporary account, then remove only that test account."""
import http.cookiejar
import json
import secrets
import subprocess
import urllib.error
import urllib.request
import uuid

BASE = 'http://localhost:3000'
jar = http.cookiejar.CookieJar()
client = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
token = ''
email = 'registration-test-' + uuid.uuid4().hex + '@example.test'
phone = '09' + ''.join(str(secrets.randbelow(10)) for _ in range(8))
password = secrets.token_urlsafe(16)

def call(path, method='GET', data=None, csrf=True):
    headers = {'Content-Type':'application/json'}
    if csrf:
        headers['X-Customer-CSRF'] = token
    try:
        response = client.open(urllib.request.Request(BASE+path, method=method, headers=headers,
                               data=json.dumps(data).encode() if data is not None else None), timeout=30)
        return response.status, json.loads(response.read())
    except urllib.error.HTTPError as error:
        return error.code, json.loads(error.read())

def refresh():
    global token
    code, result = call('/api/v1/customer/auth/session')
    assert code == 200, result
    token = result['data']['csrfToken']
    return result['data']['customer']

details = {'hoTen':'Khách thử đăng ký', 'email':email, 'soDienThoai':phone,
           'diaChi':'Cần Thơ', 'password':password}
try:
    assert refresh() is None
    assert call('/api/v1/customer/auth/register','POST',details,False)[0] == 403
    for field, value in [('password','short'),('email','invalid'),('soDienThoai','123'),('hoTen',' '),('diaChi','a'*256)]:
        code, response = call('/api/v1/customer/auth/register','POST',dict(details, **{field:value}))
        assert code == 400, (field, response)
    previous_cookie = [c.value for c in jar if c.name == 'JSESSIONID']
    code, result = call('/api/v1/customer/auth/register','POST',details)
    assert code == 200 and result['success'], result
    customer_id = result['data']['customer']['maKH']
    assert result['data']['customer']['email'] == email
    assert [c.value for c in jar if c.name == 'JSESSIONID'] != previous_cookie
    assert refresh()['maKH'] == customer_id
    code, wallet = call('/api/v1/wallets/'+str(customer_id))
    assert code == 200 and wallet['data']['soDuVi'] == 0, wallet
    code, cart = call('/api/v1/cart/'+str(customer_id))
    assert code == 200 and cart['data']['items'] == [], cart
    assert call('/api/v1/customer/auth/logout','POST')[0] == 200
    assert refresh() is None
    assert call('/api/v1/customer/auth/register','POST',dict(details,email=email.upper()))[0] == 409
    assert call('/api/v1/customer/auth/register','POST',dict(details,email='other-'+email))[0] == 409
    code, result = call('/api/v1/customer/auth/login','POST',{'email':email,'password':password})
    assert code == 200 and result['data']['customer']['maKH'] == customer_id, result
    assert call('/api/v1/orders/customer/'+str(customer_id))[1]['data'] == []
    assert call('/api/v1/customer/auth/logout','POST')[0] == 200
    print('Registration, validation, duplicates, auto-login, new empty cart/zero wallet, re-login: PASS')
finally:
    # UUID-derived email is generated above, never accepted from external input.
    sql = f"""SET XACT_ABORT ON;
BEGIN TRANSACTION;
DECLARE @Id INT=(SELECT MaKH FROM KhachHang WHERE Email='{email}');
IF @Id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM DonHang WHERE MaKH=@Id)
BEGIN
    DELETE FROM ChiTietGioHang WHERE MaGioHang IN(SELECT MaGioHang FROM GioHang WHERE MaKH=@Id);
    DELETE FROM GioHang WHERE MaKH=@Id;
    DELETE FROM KhachHangDangNhap WHERE MaKH=@Id;
    DELETE FROM KhachHang WHERE MaKH=@Id;
END;
COMMIT TRANSACTION;
"""
    completed = subprocess.run(['docker','exec','freshgrocery-sqlserver','/opt/mssql-tools18/bin/sqlcmd',
                  '-S','localhost','-U','sa','-P','StrongPassword123!','-C','-b','-d','FreshGroceryDB','-Q',sql],
                  capture_output=True, text=True)
    if completed.returncode:
        raise RuntimeError('Could not clean up the temporary registration test account: '+completed.stdout+completed.stderr)
