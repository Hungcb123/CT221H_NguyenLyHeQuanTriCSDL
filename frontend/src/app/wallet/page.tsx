"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, Wallet } from "lucide-react";
import { useCustomer } from "@/components/customer/CustomerProvider";
import { apiService } from "@/services/api";
import type { WalletTransaction } from "@/types";

const money = (value: number) => value.toLocaleString("vi-VN", { style: "currency", currency: "VND" });
const transactionLabels: Record<string, string> = { NAP_TIEN: "Nạp ví", THANH_TOAN: "Thanh toán", HOAN_TIEN: "Hoàn tiền" };

export default function WalletPage() {
  const { customer, loading } = useCustomer();
  const router = useRouter();
  const [wallet, setWallet] = useState<{ customerId: number; balance: number; transactions: WalletTransaction[] } | null>(null);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    if (loading) return;
    if (!customer) {
      router.replace("/login?next=/wallet");
      return;
    }
    let active = true;
    const customerId = customer.maKH;
    Promise.all([apiService.getWallet(customerId), apiService.getWalletTransactions(customerId)])
      .then(([summary, transactions]) => {
        if (active) setWallet({ customerId, balance: summary.soDuVi, transactions });
      })
      .catch((failure) => {
        if (active) setError(failure instanceof Error ? failure.message : "Không tải được ví.");
      });
    return () => { active = false; };
  }, [customer, loading, retry, router]);

  const current = customer && wallet?.customerId === customer.maKH ? wallet : null;
  return (
    <main className="min-h-screen bg-stone-50 px-4 py-8 sm:px-6">
      <div className="mx-auto max-w-3xl space-y-5">
        <nav className="flex flex-wrap gap-3" aria-label="Điều hướng ví">
          <Link href="/" className="customer-action"><ArrowLeft aria-hidden="true" />Về cửa hàng</Link>
          <Link href="/checkout" className="customer-action">Đến thanh toán</Link>
        </nav>
        <section className="rounded-xl border border-emerald-200 bg-emerald-50 p-5">
          <h1 className="flex items-center gap-2 text-xl font-bold"><Wallet className="size-5" aria-hidden="true" />Ví DailyMart</h1>
          <p className="mt-3 text-sm text-emerald-900">Số dư khả dụng</p>
          <p className="mt-1 text-2xl font-bold tabular-nums">{current ? money(current.balance) : "—"}</p>
        </section>
        {error && <div role="alert" className="rounded-xl bg-red-50 p-4 text-red-800">
          <p>{error}</p>
          <button type="button" className="customer-action mt-3" onClick={() => { setError(""); setRetry(value => value + 1); }}>Thử lại</button>
        </div>}
        {!current && !error && <p role="status">Đang tải ví…</p>}
        {current && <section className="rounded-xl border border-stone-200 bg-white p-5">
          <h2 className="text-lg font-bold">Lịch sử giao dịch</h2>
          {current.transactions.length === 0 ? <p className="mt-3 text-stone-600">Chưa có giao dịch.</p> : (
            <ul className="mt-3 divide-y divide-stone-200">
              {current.transactions.map((entry) => <li key={entry.maGiaoDich} className="flex flex-wrap justify-between gap-3 py-4">
                <div className="min-w-0">
                  <p className="font-semibold">{transactionLabels[entry.loaiGiaoDich] ?? entry.loaiGiaoDich}</p>
                  <p className="mt-1 break-words text-sm text-stone-700">{entry.noiDung}</p>
                  <p className="mt-1 text-sm text-stone-600">{new Date(entry.ngayGiaoDich).toLocaleString("vi-VN")}</p>
                  {entry.maDH && <Link className="mt-2 inline-block text-sm font-semibold text-emerald-800 underline" href={`/orders/${entry.maDH}`}>Đơn #{entry.maDH}</Link>}
                </div>
                <div className="tabular-nums">
                  <p className={entry.soTienThayDoi >= 0 ? "font-bold text-emerald-800" : "font-bold text-red-800"}>
                    {entry.soTienThayDoi > 0 ? "+" : ""}{money(entry.soTienThayDoi)}
                  </p>
                  <p className="mt-1 text-sm text-stone-600">Còn {money(entry.soDuSau)}</p>
                </div>
              </li>)}
            </ul>
          )}
        </section>}
      </div>
    </main>
  );
}
