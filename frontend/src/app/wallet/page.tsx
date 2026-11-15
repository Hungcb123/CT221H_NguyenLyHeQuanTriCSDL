"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, RefreshCw, ShoppingBag, Wallet } from "lucide-react";
import { useCustomer } from "@/components/customer/CustomerProvider";
import { AccountMenu } from "@/components/customer/AccountMenu";
import { apiService } from "@/services/api";
import type { WalletTransaction } from "@/types";

const money = (value: number) => `${value.toLocaleString("vi-VN")} đ`;
const transactionNames: Record<string, string> = {
  NAP_TIEN: "Nạp tiền",
  THANH_TOAN: "Thanh toán đơn hàng",
  HOAN_TIEN: "Hoàn tiền",
};

function WalletView({ customerId }: { customerId: number }) {
  const [snapshot, setSnapshot] = useState<{
    balance: number;
    transactions: WalletTransaction[];
  } | null>(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(true);
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    let cancelled = false;
    let latestRequest = 0;
    const load = async () => {
      const requestId = ++latestRequest;
      try {
        const [wallet, transactions] = await Promise.all([
          apiService.getWallet(customerId),
          apiService.getWalletTransactions(customerId),
        ]);
        if (!cancelled && requestId === latestRequest) {
          setSnapshot({ balance: wallet.soDuVi, transactions });
          setError("");
        }
      } catch (cause) {
        if (!cancelled && requestId === latestRequest)
          setError(
            cause instanceof Error
              ? cause.message
              : "Không tải được thông tin ví.",
          );
      } finally {
        if (!cancelled && requestId === latestRequest) setBusy(false);
      }
    };
    void load();
    window.addEventListener("focus", load);
    return () => {
      cancelled = true;
      window.removeEventListener("focus", load);
    };
  }, [customerId, retry]);

  return (
    <div className="mx-auto max-w-4xl space-y-6 px-4 py-8">
      <div className="flex flex-wrap gap-3">
        <Link href="/" className="customer-action">
          <ArrowLeft aria-hidden="true" />
          Tiếp tục mua sắm
        </Link>
        <Link href="/checkout" className="customer-action">
          <ShoppingBag aria-hidden="true" />
          Đến thanh toán
        </Link>
      </div>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="flex items-center gap-3 text-2xl font-bold">
          <Wallet aria-hidden="true" />
          Ví DailyMart
        </h1>
        <button
          type="button"
          className="customer-action"
          disabled={busy}
          onClick={() => {
            setBusy(true);
            setError("");
            setRetry((value) => value + 1);
          }}
        >
          <RefreshCw aria-hidden="true" />
          Cập nhật ví
        </button>
      </div>
      {busy && <p role="status">Đang tải thông tin ví…</p>}
      {error && (
        <p
          role="alert"
          className="rounded-lg border border-red-200 bg-red-50 p-4 text-red-800"
        >
          {error} Bấm Cập nhật ví để thử lại.
        </p>
      )}
      {!busy && !error && snapshot && (
        <>
          <section className="rounded-2xl bg-emerald-900 p-6 text-white sm:p-8">
            <p className="text-emerald-100">Số dư khả dụng</p>
            <p className="mt-2 break-words text-3xl font-bold sm:text-4xl">
              {money(snapshot.balance)}
            </p>
            <p className="mt-4 text-sm text-emerald-100">
              Dùng số dư ví để thanh toán đơn hàng tại DailyMart.
            </p>
          </section>
          <p className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-950">
            Để nạp ví demo, nhờ quản trị viên vào mục Khách hàng → Nạp ví demo.
            Tiền nạp và tiền hoàn sẽ được ghi nhận trong lịch sử bên dưới.
          </p>
          <section className="rounded-xl border border-stone-200 bg-white p-5 sm:p-6">
            <h2 className="text-lg font-bold">Lịch sử giao dịch</h2>
            {snapshot.transactions.length === 0 ? (
              <p className="mt-4 text-stone-600">Ví chưa có giao dịch nào.</p>
            ) : (
              <ul className="mt-4 divide-y divide-stone-200">
                {snapshot.transactions.map((transaction) => (
                  <li
                    key={transaction.maGiaoDich}
                    className="grid grid-cols-[minmax(0,1fr)_auto] gap-3 py-4"
                  >
                    <div className="min-w-0 space-y-1">
                      <p className="font-semibold">
                        {transactionNames[transaction.loaiGiaoDich] ||
                          transaction.loaiGiaoDich}
                      </p>
                      <p className="text-sm text-stone-600">
                        {new Date(transaction.ngayGiaoDich).toLocaleString(
                          "vi-VN",
                        )}
                      </p>
                      <p className="break-words text-sm text-stone-600">
                        {transaction.noiDung}
                      </p>
                      {transaction.maDH != null && (
                        <Link
                          href={`/orders/${transaction.maDH}`}
                          className="inline-block min-h-11 pt-2 text-sm font-semibold text-emerald-800 underline"
                        >
                          Xem đơn hàng #{transaction.maDH}
                        </Link>
                      )}
                    </div>
                    <div className="space-y-1 text-right">
                      <p
                        className={`font-bold ${transaction.soTienThayDoi >= 0 ? "text-emerald-800" : "text-red-800"}`}
                      >
                        {transaction.soTienThayDoi > 0 ? "+" : ""}
                        {money(transaction.soTienThayDoi)}
                      </p>
                      <p className="text-sm text-stone-600">
                        Số dư sau: {money(transaction.soDuSau)}
                      </p>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}
    </div>
  );
}

export default function WalletPage() {
  const { customer, loading } = useCustomer();
  const router = useRouter();
  useEffect(() => {
    if (!loading && !customer) router.replace("/login?next=/wallet");
  }, [customer, loading, router]);
  if (loading || !customer)
    return (
      <main className="p-8" role="status">
        Đang kiểm tra tài khoản…
      </main>
    );
  return (
    <main className="min-h-screen bg-stone-50 text-stone-900">
      <header className="flex items-center justify-between bg-emerald-900 px-5 py-4 text-white">
        <Link href="/" className="text-2xl font-extrabold">
          DailyMart
        </Link>
        <AccountMenu />
      </header>
      <WalletView key={customer.maKH} customerId={customer.maKH} />
    </main>
  );
}
