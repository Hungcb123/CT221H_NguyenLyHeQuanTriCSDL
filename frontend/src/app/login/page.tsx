"use client";
import { useEffect,useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCustomer } from "@/components/customer/CustomerProvider";
export default function CustomerLoginPage(){
 const {customer,loading,login}=useCustomer();const router=useRouter();
 const [email,setEmail]=useState("");const [password,setPassword]=useState("");const [error,setError]=useState("");const [pending,setPending]=useState(false);
 const destination=()=>{const value=new URLSearchParams(window.location.search).get("next");return value?.startsWith("/")&&!value.startsWith("//")?value:"/";};
 useEffect(()=>{if(!loading&&customer)router.replace(destination());},[customer,loading,router]);
 return <main className="flex min-h-screen items-center justify-center bg-emerald-50 px-4 py-10"><form className="w-full max-w-md space-y-5 rounded-2xl border bg-white p-7 shadow-sm" onSubmit={async e=>{e.preventDefault();setPending(true);setError("");try{await login(email,password);router.replace(destination());}catch(e){setError(e instanceof Error?e.message:"Không đăng nhập được.");}finally{setPending(false);}}}>
 <Link href="/" className="text-2xl font-extrabold text-emerald-800">DailyMart</Link><h1 className="text-2xl font-bold">Đăng nhập khách hàng</h1><p className="text-sm text-stone-600">Theo dõi đơn hàng và mua sắm bằng tài khoản của bạn.</p>
 {error&&<p role="alert" className="rounded-lg bg-red-50 p-3 text-red-800">{error}</p>}
 <label className="block font-medium">Email<input type="email" autoComplete="username" required value={email} onChange={e=>setEmail(e.target.value)} className="mt-2 min-h-12 w-full rounded-lg border px-3"/></label>
 <label className="block font-medium">Mật khẩu<input type="password" autoComplete="current-password" required value={password} onChange={e=>setPassword(e.target.value)} className="mt-2 min-h-12 w-full rounded-lg border px-3"/></label>
 <button disabled={pending||loading} className="min-h-12 w-full rounded-lg bg-emerald-800 font-bold text-white disabled:opacity-50">{pending?"Đang đăng nhập…":"Đăng nhập"}</button>
 <p className="text-center text-sm text-stone-600">Chưa có tài khoản? <Link href="/register" onClick={event=>{const next=new URLSearchParams(window.location.search).get("next");if(next?.startsWith("/")&&!next.startsWith("//")){event.preventDefault();router.push(`/register?next=${encodeURIComponent(next)}`);}}} className="font-semibold text-emerald-800 underline">Đăng ký ngay</Link></p>
 <Link href="/" className="block text-center text-sm text-emerald-800 underline">Tiếp tục xem sản phẩm</Link>
 </form></main>;
}
