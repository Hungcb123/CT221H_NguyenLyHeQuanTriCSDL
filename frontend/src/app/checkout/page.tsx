"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import {useRouter} from "next/navigation";
import {useCustomer} from "@/components/customer/CustomerProvider";
import { CartItemThumbnail } from "@/components/cart/CartItemThumbnail";
import { ArrowLeft, ArrowRight, CheckCircle2, MapPin, ShoppingBag, Wallet } from "lucide-react";
import { apiService } from "@/services/api";
import {momoApi,isMoMoSandboxUrl,type MoMoConfig} from "@/services/momo-api";
import type { CartResponse, CheckoutResponse } from "@/types";

const money = (value: number) => `${value.toLocaleString("vi-VN")} đ`;

export default function CheckoutPage() {
  const {customer,loading:authLoading}=useCustomer();const router=useRouter();
  const [cart, setCart] = useState<CartResponse | null>(null);
  const [balance, setBalance] = useState<number | null>(null);
  const [address, setAddress] = useState("");
  const [note, setNote] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [retry, setRetry] = useState(0);
  const [result, setResult] = useState<CheckoutResponse | null>(null);
  const submittingRef = useRef(false);
  const headingRef = useRef<HTMLHeadingElement>(null);
  const [method,setMethod]=useState<"WALLET"|"MOMO">("WALLET");
  const [momo,setMomo]=useState<MoMoConfig|null>(null);
  const momoKey=useRef<string|null>(null);
  useEffect(()=>{let cancelled=false;void momoApi.config().then(value=>{if(!cancelled)setMomo(value);}).catch(()=>{if(!cancelled)setMomo({available:false,sandbox:true,message:"Chưa kết nối được MoMo Sandbox."});});return()=>{cancelled=true;};},[]);

  useEffect(() => {
    if(authLoading)return;
    if(!customer){router.replace("/login?next=/checkout");return;}
    let cancelled = false;
    Promise.all([apiService.getCart(customer.maKH), apiService.getWallet(customer.maKH)])
      .then(([nextCart, wallet]) => { if (!cancelled) { setCart(nextCart); setBalance(wallet.soDuVi); } })
      .catch((cause) => { if (!cancelled) setError(cause instanceof Error ? cause.message : "Không tải được thông tin thanh toán."); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [retry, customer, authLoading, router]);

  useEffect(() => { if (result) headingRef.current?.focus(); }, [result]);

  const insufficient = balance !== null && cart !== null && balance < cart.tongTien;
  const unavailable = cart?.danhSachMon.some((item) => item.conDuHang === false);
  const canPay = !loading && !!cart?.danhSachMon.length && !unavailable
    && (method==="MOMO"?!!momo?.available:balance!==null&&!insufficient);

  async function checkout(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submittingRef.current || !canPay || !address.trim() || !customer) return;
    submittingRef.current = true;
    setSubmitting(true);
    setError(null);
    try {
      if(method==="MOMO"){
        const storageKey=`momo-checkout:${customer.maKH}`;
        if(!momoKey.current){momoKey.current=sessionStorage.getItem(storageKey)||crypto.randomUUID();sessionStorage.setItem(storageKey,momoKey.current);}
        const payment=await momoApi.create(address.trim(),note.trim(),momoKey.current);
        sessionStorage.removeItem(storageKey);momoKey.current=null;
        if(payment.payUrl && isMoMoSandboxUrl(payment.payUrl))window.location.assign(payment.payUrl);
        else router.push(`/payments/momo/return?orderId=${encodeURIComponent(payment.paymentId)}`);
        return;
      }
      setResult(await apiService.checkout(address, note, customer.maKH));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Chưa xác nhận được thanh toán. Vui lòng kiểm tra đơn hàng trước khi thử lại.");
      // Refresh stock and wallet after a rejected transaction, keeping the delivery fields.
      const refreshed = await Promise.allSettled([apiService.getCart(customer.maKH), apiService.getWallet(customer.maKH)]);
      if (refreshed[0].status === "fulfilled") setCart(refreshed[0].value);
      if (refreshed[1].status === "fulfilled") setBalance(refreshed[1].value.soDuVi);
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  }

  return (
    <main className="min-h-screen bg-stone-50 px-4 py-8 text-stone-900 sm:px-6">
      <div className="mx-auto max-w-4xl">
        <Link href="/" className="customer-action mb-6"><ArrowLeft aria-hidden="true" />Tiếp tục mua sắm</Link>
        {result ? (
          <section className="rounded-xl bg-white p-6 text-center shadow-sm sm:p-10">
            <CheckCircle2 className="mx-auto mb-4 h-14 w-14 text-emerald-700" aria-hidden="true" />
            <h1 ref={headingRef} tabIndex={-1} className="text-2xl font-bold">Thanh toán thành công</h1>
            <p className="mt-2 text-stone-600">Đơn hàng #{result.maDH} đã được xác nhận.</p>
            <dl className="mx-auto my-6 max-w-md space-y-3 text-left">
              <div className="flex justify-between gap-4"><dt>Tổng thanh toán</dt><dd className="font-bold">{money(result.tongTien)}</dd></div>
              <div className="flex justify-between gap-4"><dt>Số dư ví còn lại</dt><dd className="font-bold text-emerald-800">{money(result.soDuConLai)}</dd></div>
            </dl>
            <div className="flex flex-wrap justify-center gap-3">
              <Link href={`/orders/${result.maDH}`} className="customer-action customer-action-primary">Xem chi tiết và theo dõi đơn hàng <ArrowRight aria-hidden="true" /></Link>
              <Link href="/" className="customer-action"><ShoppingBag aria-hidden="true" />Tiếp tục đi chợ</Link>
            </div>
          </section>
        ) : (
          <>
            <h1 className="mb-6 text-2xl font-bold">Thanh toán đơn hàng</h1>
            {error && <div role="alert" className="mb-5 rounded-lg border border-red-200 bg-red-50 p-4 text-red-800">
              <p>{error}</p>
              {!submitting && <button type="button" className="mt-2 min-h-11 font-semibold underline" onClick={() => { setLoading(true); setError(null); setRetry((value) => value + 1); }}>Tải lại giỏ hàng và số dư</button>}
            </div>}
            {loading ? <p role="status">Đang tải giỏ hàng và số dư ví…</p> : (
              <form onSubmit={checkout} className="grid gap-6 md:grid-cols-3">
                <div className="space-y-6 md:col-span-2">
                  <section className="space-y-4 rounded-xl border border-stone-200 bg-white p-5 sm:p-6">
                    <h2 className="flex items-center gap-2 text-lg font-bold"><MapPin className="h-5 w-5 text-emerald-800" />Thông tin giao hàng</h2>
                    <div><label htmlFor="delivery-address" className="mb-2 block font-medium">Địa chỉ nhận hàng <span className="text-red-700">*</span></label>
                      <input id="delivery-address" autoComplete="street-address" required maxLength={255} value={address} disabled={submitting} onChange={(event) => setAddress(event.target.value)} placeholder="Số nhà, đường, phường/xã, tỉnh/thành phố" className="min-h-11 w-full rounded-lg border border-stone-300 px-3 text-base" /></div>
                    <div><label htmlFor="delivery-note" className="mb-2 block font-medium">Ghi chú giao hàng</label>
                      <textarea id="delivery-note" maxLength={255} rows={3} value={note} disabled={submitting} onChange={(event) => setNote(event.target.value)} className="w-full rounded-lg border border-stone-300 p-3 text-base" placeholder="Hướng dẫn giao hàng (nếu có)" /></div>
                  </section>
                  <section className="rounded-xl border border-stone-200 bg-white p-5 sm:p-6">
                    <fieldset disabled={submitting} className="mb-5 space-y-3">
                      <legend className="mb-3 text-lg font-bold">Phương thức thanh toán</legend>
                      <label className={`flex cursor-pointer items-start gap-3 rounded-lg border p-3 ${method==="WALLET"?"border-emerald-700 bg-emerald-50":"border-stone-200"}`}><input type="radio" name="payment-method" checked={method==="WALLET"} onChange={()=>setMethod("WALLET")} className="mt-1"/><span><strong className="block">Ví DailyMart</strong><span className="text-sm text-stone-600">Thanh toán bằng số dư ví trong cửa hàng.</span></span></label>
                      <label className={`flex items-start gap-3 rounded-lg border p-3 ${method==="MOMO"?"border-pink-700 bg-pink-50":"border-stone-200"} ${momo?.available?"cursor-pointer":"opacity-60"}`}><input type="radio" name="payment-method" checked={method==="MOMO"} disabled={!momo?.available} onChange={()=>setMethod("MOMO")} className="mt-1"/><span><strong className="block text-pink-800">MoMo Sandbox <span className="text-xs font-medium">· Thử nghiệm</span></strong><span className="text-sm text-stone-600">{momo?.message||"Đang kiểm tra kết nối MoMo…"}</span></span></label>
                    </fieldset>
                    {method==="WALLET"?<>
                    <h2 className="mb-3 flex items-center gap-2 text-lg font-bold"><Wallet className="h-5 w-5 text-emerald-800" />Ví DailyMart</h2>
                    <p>Số dư khả dụng: <strong className="text-emerald-800">{balance === null ? "Chưa tải được" : money(balance)}</strong></p>
                    {insufficient && <p role="status" className="mt-3 text-red-800">Số dư ví chưa đủ. Liên hệ quản trị viên để bổ sung tiền demo.</p>}
                    <Link href="/wallet" className="customer-action mt-3">Xem ví và giao dịch</Link>
                    </>:<p className="rounded-lg bg-pink-50 p-3 text-sm text-pink-900">Bạn sẽ chuyển sang cổng MoMo thử nghiệm. Dùng ứng dụng và tài khoản MoMo Test; không sử dụng tiền thật. Đơn chỉ được xác nhận sau khi máy chủ nhận kết quả hợp lệ.</p>}
                  </section>
                </div>
                <section className="self-start rounded-xl border border-stone-200 bg-white p-5">
                  <h2 className="mb-4 flex items-center gap-2 text-lg font-bold"><ShoppingBag className="h-5 w-5 text-emerald-800" />Đơn hàng của bạn</h2>
                  {!cart?.danhSachMon.length ? <p className="text-stone-600">Giỏ hàng đang trống. Hãy chọn sản phẩm trước khi thanh toán.</p> : (
                    <ul className="max-h-80 space-y-4 overflow-y-auto">{cart.danhSachMon.map((item) => <li key={item.maSP} className="flex items-start gap-3 text-sm">
                      <CartItemThumbnail item={item} />
                      <div className="min-w-0 flex-1">
                        <p className="break-words font-semibold">{item.tenSP}</p>
                        <div className="mt-1 flex flex-wrap justify-between gap-x-3 text-stone-600"><span>{item.soLuong} × {item.donViTinh}</span><span>{money(item.thanhTien)}</span></div>
                        {item.conDuHang === false && <p className="mt-1 text-red-800">Không đủ hàng. Vui lòng điều chỉnh giỏ hàng.</p>}
                      </div>
                    </li>)}</ul>
                  )}
                  <div className="my-5 flex flex-wrap justify-between gap-2 border-t border-stone-200 pt-4 font-bold"><span>Tổng thanh toán</span><span className="text-red-800">{money(cart?.tongTien ?? 0)}</span></div>
                  <button type="submit" disabled={!canPay || submitting || !address.trim()} className="min-h-12 w-full rounded-lg bg-emerald-800 px-3 py-3 font-bold text-white hover:bg-emerald-900 disabled:cursor-not-allowed disabled:opacity-50">{submitting ? "Đang xử lý…" : method==="MOMO"?"Thanh toán MoMo Sandbox":"Xác nhận thanh toán ví"}</button>
                </section>
              </form>
            )}
          </>
        )}
      </div>
    </main>
  );
}
