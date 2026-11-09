"use client";

import {useEffect, useState} from "react";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import {useRouter} from "next/navigation";
import {useCustomer} from "@/components/customer/CustomerProvider";
import {customerDestination} from "@/services/customer-session";

export default function RegisterPage() {
  const {customer, loading, register} = useCustomer();
  const router = useRouter();
  const [hoTen, setHoTen] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [address, setAddress] = useState("");
  const [password, setPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => {if (!loading && customer) router.replace(customerDestination());}, [customer, loading, router]);

  return <main className="flex min-h-screen items-center justify-center bg-emerald-50 px-4 py-10">
    <form className="w-full max-w-lg space-y-5 rounded-2xl border border-stone-200 bg-white p-6 shadow-sm sm:p-8" onSubmit={async event => {
      event.preventDefault();
      if(pending) return;
      setError("");
      if(password !== confirmation) {setError("Mật khẩu xác nhận chưa khớp."); return;}
      if(new TextEncoder().encode(password).length > 72) {setError("Mật khẩu quá dài. Hãy dùng tối đa 72 byte UTF-8."); return;}
      setPending(true);
      try {
        await register({hoTen: hoTen.trim(), email: email.trim(), soDienThoai: phone.trim(), diaChi: address.trim(), password});
        router.replace(customerDestination());
      } catch(cause) {setError(cause instanceof Error ? cause.message : "Không tạo được tài khoản. Vui lòng thử lại.");}
      finally {setPending(false);}
    }}>
      <Link href="/" className="text-2xl font-extrabold text-emerald-800">DailyMart</Link>
      <div><h1 className="text-2xl font-bold text-stone-900">Tạo tài khoản</h1><p className="mt-2 text-sm text-stone-600">Đăng ký để mua sắm và theo dõi đơn hàng của bạn.</p></div>
      {error && <p role="alert" className="rounded-lg bg-red-50 p-3 text-sm text-red-800">{error}</p>}
      <fieldset disabled={pending || loading} className="space-y-4 disabled:opacity-60">
        <label className="block font-medium">Họ và tên<input autoComplete="name" required minLength={2} maxLength={100} value={hoTen} onChange={e=>setHoTen(e.target.value)} className="mt-2 min-h-12 w-full rounded-lg border border-stone-300 px-3"/></label>
        <label className="block font-medium">Email<input type="email" autoComplete="email" required maxLength={100} value={email} onChange={e=>setEmail(e.target.value)} className="mt-2 min-h-12 w-full rounded-lg border border-stone-300 px-3"/></label>
        <label className="block font-medium">Số điện thoại<input type="tel" autoComplete="tel-national" inputMode="tel" required pattern="0[0-9]{9}" title="10 chữ số, bắt đầu bằng 0" maxLength={10} value={phone} onChange={e=>setPhone(e.target.value)} placeholder="Ví dụ: 0912345678" className="mt-2 min-h-12 w-full rounded-lg border border-stone-300 px-3"/></label>
        <label className="block font-medium">Địa chỉ <span className="text-sm font-normal text-stone-500">(không bắt buộc)</span><input autoComplete="street-address" maxLength={255} value={address} onChange={e=>setAddress(e.target.value)} className="mt-2 min-h-12 w-full rounded-lg border border-stone-300 px-3"/></label>
        <label className="block font-medium">Mật khẩu<input type="password" autoComplete="new-password" required minLength={8} maxLength={72} value={password} onChange={e=>setPassword(e.target.value)} aria-describedby="password-help" className="mt-2 min-h-12 w-full rounded-lg border border-stone-300 px-3"/><span id="password-help" className="mt-1 block text-xs font-normal text-stone-500">Ít nhất 8 ký tự.</span></label>
        <label className="block font-medium">Nhập lại mật khẩu<input type="password" autoComplete="new-password" required minLength={8} maxLength={72} value={confirmation} onChange={e=>setConfirmation(e.target.value)} className="mt-2 min-h-12 w-full rounded-lg border border-stone-300 px-3"/></label>
        <button className="min-h-12 w-full rounded-lg bg-emerald-800 px-4 font-bold text-white hover:bg-emerald-900">{pending?"Đang tạo tài khoản…":"Đăng ký"}</button>
      </fieldset>
      <p className="text-center text-sm text-stone-600">Đã có tài khoản? <Link href="/login" onClick={event=>{const next=customerDestination();if(next!=="/"){event.preventDefault();router.push(`/login?next=${encodeURIComponent(next)}`);}}} className="font-semibold text-emerald-800 underline">Đăng nhập</Link></p>
      <div className="text-center"><Link href="/" className="customer-action"><ArrowLeft aria-hidden="true" />Tiếp tục xem sản phẩm</Link></div>
    </form>
  </main>;
}
