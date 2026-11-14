"use client";
import {useEffect,useState} from "react";
import {useRouter,useSearchParams} from "next/navigation";
import Link from "next/link";
import {CheckCircle2,Clock3,RefreshCw,TriangleAlert} from "lucide-react";
import {useCustomer} from "./CustomerProvider";
import {sepayApi,type SePayPayment} from "@/services/sepay-api";

export function SePayResult({orderNumber}:{orderNumber?:number}){
  const {customer,loading}=useCustomer();const router=useRouter();const params=useSearchParams();
  // Only use the URL's order identifier. resultCode/signature/amount in browser URLs cannot confirm payment.
  const id=params.get("orderId");const [payment,setPayment]=useState<SePayPayment|null>(null);
  const [error,setError]=useState("");const [busy,setBusy]=useState(false);
  useEffect(()=>{
    if(loading)return;
    if(!customer){router.replace(`/login?next=${encodeURIComponent(orderNumber?`/payments/sepay/order/${orderNumber}`:`/payments/sepay/return?orderId=${encodeURIComponent(id||"")}`)}`);return;}
    let cancelled=false;let timer:ReturnType<typeof setTimeout>|undefined;let cycles=0;
    async function load(){
      try{
        const result=orderNumber?await sepayApi.forOrder(orderNumber):id?await sepayApi.get(id):null;
        if(!result||!("paymentId" in result)||!result.paymentId)throw new Error("Không tìm thấy thanh toán SePay của tài khoản này.");
        const state=result as SePayPayment;
        if(cancelled)return;
        setError("");setPayment(state);
        if((state.status==="PENDING"||state.status==="CREATING")&&cycles++<600)timer=setTimeout(()=>{void load();},3000);
      }catch(cause){if(!cancelled)setError(cause instanceof Error?cause.message:"Không tải được thanh toán.");}
    }
    void load();return()=>{cancelled=true;if(timer)clearTimeout(timer);};
  },[customer,loading,id,orderNumber,router]);
  async function refresh(){if(!payment||busy)return;setBusy(true);setError("");try{setPayment(await sepayApi.refresh(payment.paymentId));}catch(cause){setError(cause instanceof Error?cause.message:"Chưa kết nối được SePay.");}finally{setBusy(false);}}
  const pending=payment?.status==="PENDING"||payment?.status==="CREATING";
  return <main className="flex min-h-screen items-center justify-center bg-stone-50 px-4 py-10"><section className="w-full max-w-xl space-y-5 rounded-2xl border bg-white p-6 text-center shadow-sm sm:p-9">
    <Link href="/" className="text-2xl font-extrabold text-emerald-800">DailyMart</Link>
    <p className="rounded-full bg-emerald-50 px-4 py-2 text-sm font-semibold text-emerald-800">SePay · Chuyển khoản ngân hàng</p>
    {payment?.status==="PAID"?<CheckCircle2 className="mx-auto h-14 w-14 text-emerald-700"/>:pending?<Clock3 className="mx-auto h-14 w-14 text-amber-600"/>:<TriangleAlert className="mx-auto h-14 w-14 text-stone-500"/>}
    <h1 className="text-2xl font-bold">{payment?.status==="PAID"?"Thanh toán thành công":pending?"Đang chờ xác nhận thanh toán":payment?.status==="FAILED"?"Thanh toán chưa thành công":payment?.status==="REVIEW"?"Giao dịch cần đối soát":"Kiểm tra thanh toán SePay"}</h1>
    {error&&<p role="alert" className="rounded-lg bg-red-50 p-3 text-sm text-red-800">{error}</p>}
    {!payment&&!error&&<p role="status">Đang đọc trạng thái từ máy chủ…</p>}
    {payment&&<><p className="text-stone-600">{payment.message}</p><dl className="space-y-2 rounded-xl bg-stone-50 p-4 text-left"><div className="flex justify-between"><dt>Đơn hàng</dt><dd className="font-semibold">#{payment.maDH}</dd></div><div className="flex justify-between"><dt>Số tiền</dt><dd className="font-bold">{payment.amount.toLocaleString("vi-VN")} đ</dd></div></dl>
      {pending&&<div className="flex flex-col gap-3">{payment.qrUrl&&<>
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img src={payment.qrUrl} alt={`QR chuyển khoản đơn ${payment.maDH}`} width={320} height={320} className="mx-auto h-auto max-w-full"/>
        <dl className="space-y-3 text-left text-sm">
          <div><dt className="text-stone-600">Ngân hàng</dt><dd className="font-semibold">{payment.bankCode}</dd></div>
          <div><dt className="text-stone-600">Chủ tài khoản</dt><dd className="font-semibold">{payment.accountName}</dd></div>
          <div><dt className="text-stone-600">Số tài khoản</dt><dd className="select-all break-all font-semibold">{payment.accountNumber}</dd></div>
          <div><dt className="text-stone-600">Nội dung chuyển khoản</dt><dd className="select-all break-all font-semibold">{payment.transferContent}</dd></div>
        </dl>
      </>}<button disabled={busy} onClick={()=>void refresh()} className="customer-action justify-center"><RefreshCw aria-hidden="true" className={busy?"animate-spin":""}/>{busy?"Đang kiểm tra…":"Tôi đã thanh toán — kiểm tra lại"}</button></div>}
      <Link href={`/orders/${payment.maDH}`} className="customer-action justify-center">Xem đơn hàng</Link>
    </>}
    <div className="flex flex-wrap justify-center gap-4 text-sm"><Link href="/orders" className="text-emerald-800 underline">Đơn hàng của tôi</Link><Link href="/" className="text-emerald-800 underline">Về cửa hàng</Link></div>
  </section></main>;
}
