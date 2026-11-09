"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { User, LogOut, Package, Wallet } from "lucide-react";
import { useCustomer } from "./CustomerProvider";
import { useState } from "react";
export function AccountMenu() {
  const {customer,loading,logout}=useCustomer();const router=useRouter();
  const [open,setOpen]=useState(false);const [pending,setPending]=useState(false);const [error,setError]=useState("");
  if(loading)return <span className="text-xs">Đang tải…</span>;
  if(!customer)return <Link href="/login" className="flex min-h-11 items-center gap-2 text-sm"><User size={18}/>Đăng nhập</Link>;
  return <div className="relative">
    <button onClick={()=>setOpen(!open)} aria-expanded={open} aria-controls="customer-menu" className="flex min-h-11 items-center gap-2 rounded-lg px-2 text-sm hover:bg-white/10"><User size={18}/><span className="max-w-40 truncate">{customer.hoTen}</span></button>
    {open&&<><button aria-label="Đóng menu tài khoản" className="fixed inset-0 z-40 cursor-default" onClick={()=>setOpen(false)}/><div id="customer-menu" className="absolute right-0 z-50 mt-2 w-64 rounded-xl border border-stone-200 bg-white p-2 text-stone-900 shadow-xl">
      <p className="border-b p-3 text-xs text-stone-500">{customer.email}</p>
      <Link href="/orders" onClick={()=>setOpen(false)} className="flex min-h-12 items-center gap-2 rounded-lg px-3 hover:bg-emerald-50"><Package size={18}/>Đơn hàng của tôi</Link>
      <Link href="/wallet" onClick={()=>setOpen(false)} className="flex min-h-12 items-center gap-2 rounded-lg px-3 hover:bg-emerald-50"><Wallet size={18} aria-hidden="true"/>Ví DailyMart</Link>
      <button disabled={pending} className="flex min-h-12 w-full items-center gap-2 rounded-lg px-3 text-red-800 hover:bg-red-50 disabled:opacity-50" onClick={async()=>{setPending(true);setError("");try{await logout();setOpen(false);router.push("/");router.refresh();}catch(e){setError(e instanceof Error?e.message:"Không đăng xuất được.");}finally{setPending(false);}}}><LogOut size={18}/>{pending?"Đang đăng xuất…":"Đăng xuất"}</button>
      {error&&<p role="alert" className="p-2 text-sm text-red-700">{error}</p>}
    </div></>}
  </div>;
}
