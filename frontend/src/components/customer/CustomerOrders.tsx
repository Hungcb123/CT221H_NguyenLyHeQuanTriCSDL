"use client";
import Link from "next/link";
import { ArrowLeft, ArrowRight, CheckCircle2, RefreshCw, ShoppingBag } from "lucide-react";
import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useCustomer } from "./CustomerProvider";
import { AccountMenu } from "./AccountMenu";

type Order = {
  maDH: number;
  ngayTao: string;
  tongTien: number;
  trangThai: string;
  phuongThucThanhToan?: string;
  diaChiGiaoHang: string;
  ghiChu?: string;
  items: {
    maSP: number;
    tenSP: string;
    tenLoai?: string | null;
    donViTinh?: string | null;
    soLuong: number;
    donGia: number;
    thanhTien: number;
  }[];
};

const money = (value: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(value);

const status: Record<string, string> = {
  PAID: "Đã thanh toán",
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  PROCESSING: "Đang chuẩn bị",
  SHIPPING: "Đang giao hàng",
  DELIVERED: "Đã giao hàng",
  CANCELLED: "Đã hủy",
  REFUNDED: "Đã hoàn tiền",
};

export function CustomerOrders({ orderId }: { orderId?: string }) {
  const { customer, loading } = useCustomer();
  const router = useRouter();
  const searchParams = useSearchParams();
  const justPlaced = Boolean(orderId && searchParams.get("justPlaced") === "1");
  const [orders, setOrders] = useState<Order[]>([]);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(true);
  const [retry, setRetry] = useState(0);
  const [page, setPage] = useState(1);

  useEffect(() => {
    let cancelled = false;
    if (loading) return;
    if (!customer) {
      router.replace(
        `/login?next=${encodeURIComponent(orderId ? `/orders/${orderId}` : "/orders")}`,
      );
      return;
    }
    void fetch(
      orderId
        ? `/api/v1/orders/${encodeURIComponent(orderId)}`
        : `/api/v1/orders/customer/${customer.maKH}?page=${page}&size=20`,
      { cache: "no-store" },
    )
      .then(async (r) => {
        const j = await r.json();
        if (!r.ok || !j.success) throw new Error(j.message || "Không tải được đơn hàng.");
        return orderId ? [j.data] : j.data;
      })
      .then((data) => {
        if (!cancelled) setOrders(data);
      })
      .catch((e) => {
        if (!cancelled) setError(e.message);
      })
      .finally(() => {
        if (!cancelled) setBusy(false);
      });
    return () => {
      cancelled = true;
    };
  }, [customer, loading, orderId, retry, router, page]);

  if (!customer) return <main className="p-8">Đang kiểm tra tài khoản…</main>;

  return (
    <main className="min-h-screen bg-stone-50 text-stone-900">
      <header className="flex items-center justify-between bg-emerald-900 px-5 py-4 text-white">
        <Link href="/" className="text-2xl font-extrabold">
          DailyMart
        </Link>
        <AccountMenu />
      </header>
      <div className="mx-auto max-w-4xl space-y-5 px-4 py-8">
        <div className="flex flex-wrap items-center gap-3">
          <Link href={orderId ? "/orders" : "/"} className="customer-action">
            <ArrowLeft aria-hidden="true" />
            {orderId ? "Tất cả đơn hàng" : "Tiếp tục mua sắm"}
          </Link>
          {orderId && (
            <Link href="/" className="customer-action">
              <ShoppingBag aria-hidden="true" />
              Tiếp tục đi chợ
            </Link>
          )}
        </div>

        {justPlaced && (
          <section
            role="status"
            className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-emerald-200 bg-emerald-50 p-5 text-emerald-950 shadow-xs"
          >
            <div className="flex items-center gap-3">
              <CheckCircle2 className="h-8 w-8 shrink-0 text-emerald-700" aria-hidden="true" />
              <div>
                <p className="text-lg font-bold">Đặt hàng thành công!</p>
                <p className="text-sm text-emerald-900">
                  Đơn hàng #{orderId} của bạn đã được ghi nhận thành công.
                </p>
              </div>
            </div>
            <Link href="/" className="customer-action customer-action-primary">
              <ShoppingBag aria-hidden="true" />
              Tiếp tục đi chợ
            </Link>
          </section>
        )}

        <div className="flex flex-wrap items-center justify-between gap-3">
          <h1 className="text-2xl font-bold">
            {orderId ? `Đơn hàng #${orderId}` : "Đơn hàng của tôi"}
          </h1>
          <button
            type="button"
            disabled={busy}
            onClick={() => {
              setBusy(true);
              setError("");
              setRetry((v) => v + 1);
            }}
            className="customer-action"
          >
            <RefreshCw aria-hidden="true" />
            Cập nhật trạng thái
          </button>
        </div>
        {error && (
          <p role="alert" className="rounded-lg bg-red-50 p-4 text-red-800">
            {error}
          </p>
        )}
        {(busy || loading) && <p role="status">Đang tải đơn hàng…</p>}
        {!busy && !error && orders.length === 0 && (
          <section className="rounded-xl border bg-white p-8 text-center">
            <p>Bạn chưa có đơn hàng nào.</p>
            <Link href="/" className="customer-action mt-4">
              <ShoppingBag aria-hidden="true" />
              Khám phá sản phẩm
            </Link>
          </section>
        )}
        {!busy &&
          orders.map((order) => (
            <article
              key={order.maDH}
              className="space-y-4 rounded-xl border border-stone-200 bg-white p-5 sm:p-7"
            >
              <div className="flex flex-wrap items-center justify-between gap-3">
                <h2 className="font-bold">Đơn #{order.maDH}</h2>
                <span className="rounded-full bg-emerald-50 px-3 py-1 text-sm font-semibold text-emerald-800">
                  {status[order.trangThai] || order.trangThai}
                </span>
              </div>
              <p className="text-sm text-stone-600">
                Đặt lúc {new Date(order.ngayTao).toLocaleString("vi-VN")}
              </p>
              {order.phuongThucThanhToan === "COD" && (
                <div className="rounded-lg bg-emerald-50 p-3 text-sm text-emerald-900">
                  <strong>Thanh toán khi nhận hàng (COD)</strong>
                  <p>
                    {order.trangThai === "DELIVERED"
                      ? "Đã giao hàng và thu tiền."
                      : order.trangThai === "CANCELLED"
                        ? "Đơn đã hủy, không cần thanh toán."
                        : "Thanh toán cho người giao hàng khi nhận được đơn."}
                  </p>
                </div>
              )}
              {order.phuongThucThanhToan === "WALLET" && (
                <div className="rounded-lg bg-emerald-50 p-3 text-sm text-emerald-900">
                  <strong>Thanh toán bằng ví DailyMart</strong>
                  <p>
                    {order.trangThai === "CANCELLED" || order.trangThai === "REFUNDED"
                      ? "Xem lịch sử ví để kiểm tra khoản hoàn tiền của đơn hàng."
                      : "Đơn hàng đã được thanh toán bằng số dư ví."}
                  </p>
                  <Link href="/wallet" className="mt-2 inline-block font-semibold underline">
                    Xem ví và lịch sử giao dịch
                  </Link>
                </div>
              )}
              {order.phuongThucThanhToan === "SEPAY" && (
                <div className="rounded-lg bg-emerald-50 p-3 text-sm text-emerald-900">
                  <strong>SePay · Chuyển khoản ngân hàng</strong>
                  <Link
                    href={`/payments/sepay/order/${order.maDH}`}
                    className="mt-2 block font-semibold underline"
                  >
                    {order.trangThai === "PENDING"
                      ? "Quét QR / kiểm tra thanh toán"
                      : "Xem thanh toán SePay"}
                  </Link>
                </div>
              )}
              {order.phuongThucThanhToan === "MOMO_SANDBOX" && (
                <div className="rounded-lg bg-pink-50 p-3 text-sm text-pink-900">
                  <strong>MoMo Sandbox — đơn thử nghiệm</strong>
                  <p>Không có tiền thật được thanh toán.</p>
                  <Link
                    href={`/payments/momo/order/${order.maDH}`}
                    className="mt-2 inline-block font-semibold underline"
                  >
                    {order.trangThai === "PENDING"
                      ? "Tiếp tục / kiểm tra thanh toán"
                      : "Xem kết quả MoMo"}
                  </Link>
                </div>
              )}
              <ul className="divide-y">
                {order.items?.map((item) => (
                  <li key={item.maSP} className="flex items-start justify-between gap-4 py-3">
                    <div>
                      <p className="font-medium">
                        {item.tenSP}
                        {item.tenLoai && !item.tenSP.includes(item.tenLoai) ? ` (${item.tenLoai})` : ""}
                      </p>
                      <p className="text-sm text-stone-500">
                        {item.soLuong} {item.donViTinh || ""} × {money(item.donGia)}
                      </p>
                    </div>
                    <strong className="shrink-0 text-sm">{money(item.thanhTien)}</strong>
                  </li>
                ))}
              </ul>
              <div className="flex justify-between border-t pt-4 font-bold">
                <span>Tổng thanh toán</span>
                <span className="text-red-800">{money(order.tongTien)}</span>
              </div>
              {orderId ? (
                <>
                  <p className="text-sm">
                    <strong>Giao đến:</strong> {order.diaChiGiaoHang}
                  </p>
                  {order.ghiChu && (
                    <p className="text-sm">
                      <strong>Ghi chú:</strong> {order.ghiChu}
                    </p>
                  )}
                </>
              ) : (
                <Link
                  href={`/orders/${order.maDH}`}
                  className="customer-action customer-action-primary"
                >
                  Xem chi tiết đơn hàng <ArrowRight aria-hidden="true" />
                </Link>
              )}
            </article>
          ))}
        {!orderId && !busy && !error && (
          <div className="flex flex-wrap items-center justify-between gap-2">
            <button
              type="button"
              disabled={page === 1}
              onClick={() => {
                setBusy(true);
                setPage((p) => p - 1);
              }}
              className="customer-action"
            >
              Trang trước
            </button>
            <span>Trang {page}</span>
            <button
              type="button"
              disabled={orders.length < 20}
              onClick={() => {
                setBusy(true);
                setPage((p) => p + 1);
              }}
              className="customer-action"
            >
              Trang sau
            </button>
          </div>
        )}
      </div>
    </main>
  );
}
