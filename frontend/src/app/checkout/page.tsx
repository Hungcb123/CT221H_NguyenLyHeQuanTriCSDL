"use client";

import { Suspense, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useCustomer } from "@/components/customer/CustomerProvider";
import { CartItemThumbnail } from "@/components/cart/CartItemThumbnail";
import { ArrowLeft, MapPin, ShoppingBag, Wallet } from "lucide-react";
import { apiService } from "@/services/api";
import { sepayApi, type SePayConfig } from "@/services/sepay-api";
import type { CartResponse } from "@/types";

const money = (value: number) => `${value.toLocaleString("vi-VN")} đ`;

function CheckoutFormView({ visitKey }: { visitKey: string }) {
  const { customer, loading: authLoading } = useCustomer();
  const router = useRouter();
  const [cart, setCart] = useState<CartResponse | null>(null);
  const [address, setAddress] = useState("");
  const [note, setNote] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [retry, setRetry] = useState(0);
  const submittingRef = useRef(false);
  const [method, setMethod] = useState<"COD" | "WALLET" | "SEPAY">("COD");
  const [wallet, setWallet] = useState<{
    customerId: number;
    balance: number | null;
    error: string | null;
  } | null>(null);
  const [sepay, setSepay] = useState<SePayConfig | null>(null);
  const sepayKey = useRef<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    void sepayApi
      .config()
      .then((value) => {
        if (!cancelled) setSepay(value);
      })
      .catch(() => {
        if (!cancelled)
          setSepay({ available: false, message: "Chưa kết nối được SePay." });
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (authLoading) return;
    if (!customer) {
      router.replace("/login?next=/checkout");
      return;
    }
    let cancelled = false;
    const fetchLatestCart = () => {
      apiService
        .getCart(customer.maKH)
        .then((nextCart) => {
          if (!cancelled) setCart(nextCart);
        })
        .catch((cause) => {
          if (!cancelled)
            setError(
              cause instanceof Error
                ? cause.message
                : "Không tải được thông tin thanh toán.",
            );
        })
        .finally(() => {
          if (!cancelled) setLoading(false);
        });
    };
    fetchLatestCart();
    const onFocus = () => fetchLatestCart();
    window.addEventListener("focus", onFocus);
    return () => {
      cancelled = true;
      window.removeEventListener("focus", onFocus);
    };
  }, [visitKey, retry, customer, authLoading, router]);

  useEffect(() => {
    if (authLoading || !customer) return;
    let cancelled = false;
    let latestRequest = 0;
    const fetchWallet = async () => {
      const requestId = ++latestRequest;
      try {
        const next = await apiService.getWallet(customer.maKH);
        if (!cancelled && requestId === latestRequest)
          setWallet({
            customerId: customer.maKH,
            balance: next.soDuVi,
            error: null,
          });
      } catch (cause) {
        if (!cancelled && requestId === latestRequest)
          setWallet({
            customerId: customer.maKH,
            balance: null,
            error:
              cause instanceof Error
                ? cause.message
                : "Không tải được số dư ví.",
          });
      }
    };
    void fetchWallet();
    window.addEventListener("focus", fetchWallet);
    return () => {
      cancelled = true;
      window.removeEventListener("focus", fetchWallet);
    };
  }, [visitKey, retry, customer, authLoading]);

  const currentWallet = wallet?.customerId === customer?.maKH ? wallet : null;
  const walletBalance = currentWallet?.balance ?? null;
  const walletEnough =
    walletBalance !== null && !!cart && walletBalance >= cart.tongTien;
  const unavailable = cart?.danhSachMon.some(
    (item) => item.conDuHang === false,
  );
  const canPay =
    !loading &&
    !!cart?.danhSachMon.length &&
    !unavailable &&
    (method === "SEPAY"
      ? !!sepay?.available && cart.tongTien >= 1000
      : method === "WALLET"
        ? walletEnough
        : true);

  async function checkout(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submittingRef.current || !canPay || !address.trim() || !customer)
      return;
    submittingRef.current = true;
    setSubmitting(true);
    setError(null);
    try {
      if (method === "SEPAY") {
        const storageKey = `sepay-checkout:${customer.maKH}`;
        if (!sepayKey.current) {
          sepayKey.current =
            sessionStorage.getItem(storageKey) || crypto.randomUUID();
          sessionStorage.setItem(storageKey, sepayKey.current);
        }
        const payment = await sepayApi.create(
          address.trim(),
          note.trim(),
          sepayKey.current,
        );
        sessionStorage.removeItem(storageKey);
        sepayKey.current = null;
        router.push(
          `/payments/sepay/return?orderId=${encodeURIComponent(payment.paymentId)}`,
        );
        return;
      }
      const createdOrder = await apiService.checkout(
        address,
        note,
        customer.maKH,
        method,
      );
      router.replace(
        `/orders/${encodeURIComponent(String(createdOrder.maDH))}?justPlaced=1`,
      );
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : "Chưa xác nhận được thanh toán. Vui lòng kiểm tra đơn hàng trước khi thử lại.",
      );
      setRetry((value) => value + 1);
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  }

  return (
    <main className="min-h-screen bg-stone-50 px-4 py-8 text-stone-900 sm:px-6">
      <div className="mx-auto max-w-4xl">
        <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
          <Link href="/" className="customer-action">
            <ArrowLeft aria-hidden="true" />
            Tiếp tục mua sắm
          </Link>
          <Link href="/wallet" className="customer-action">
            <Wallet aria-hidden="true" />
            Ví của tôi
          </Link>
        </div>
        <h1 className="mb-6 text-2xl font-bold">Thanh toán đơn hàng</h1>
        {error && (
          <div
            role="alert"
            className="mb-5 rounded-lg border border-red-200 bg-red-50 p-4 text-red-800"
          >
            <p>{error}</p>
            {!submitting && (
              <button
                type="button"
                className="mt-2 min-h-11 font-semibold underline"
                onClick={() => {
                  setLoading(true);
                  setError(null);
                  setRetry((value) => value + 1);
                }}
              >
                Tải lại giỏ hàng
              </button>
            )}
          </div>
        )}
        {loading ? (
          <p role="status">Đang tải giỏ hàng…</p>
        ) : (
          <form onSubmit={checkout} className="grid gap-6 md:grid-cols-3">
            <div className="space-y-6 md:col-span-2">
              <section className="space-y-4 rounded-xl border border-stone-200 bg-white p-5 sm:p-6">
                <h2 className="flex items-center gap-2 text-lg font-bold">
                  <MapPin className="h-5 w-5 text-emerald-800" />
                  Thông tin giao hàng
                </h2>
                <div>
                  <label
                    htmlFor="delivery-address"
                    className="mb-2 block font-medium"
                  >
                    Địa chỉ nhận hàng <span className="text-red-700">*</span>
                  </label>
                  <input
                    id="delivery-address"
                    autoComplete="street-address"
                    required
                    maxLength={255}
                    value={address}
                    disabled={submitting}
                    onChange={(event) => setAddress(event.target.value)}
                    placeholder="Số nhà, đường, phường/xã, tỉnh/thành phố"
                    className="min-h-11 w-full rounded-lg border border-stone-300 px-3 text-base"
                  />
                </div>
                <div>
                  <label
                    htmlFor="delivery-note"
                    className="mb-2 block font-medium"
                  >
                    Ghi chú giao hàng
                  </label>
                  <textarea
                    id="delivery-note"
                    maxLength={255}
                    rows={3}
                    value={note}
                    disabled={submitting}
                    onChange={(event) => setNote(event.target.value)}
                    className="w-full rounded-lg border border-stone-300 p-3 text-base"
                    placeholder="Hướng dẫn giao hàng (nếu có)"
                  />
                </div>
              </section>
              <section className="rounded-xl border border-stone-200 bg-white p-5 sm:p-6">
                <fieldset disabled={submitting} className="mb-5 space-y-3">
                  <legend className="mb-3 text-lg font-bold">
                    Phương thức thanh toán
                  </legend>
                  <label
                    className={`flex cursor-pointer items-start gap-3 rounded-lg border p-3 ${
                      method === "COD"
                        ? "border-emerald-700 bg-emerald-50"
                        : "border-stone-200"
                    }`}
                  >
                    <input
                      type="radio"
                      name="payment-method"
                      checked={method === "COD"}
                      onChange={() => setMethod("COD")}
                      className="mt-1"
                    />
                    <span>
                      <strong className="block">
                        Thanh toán khi nhận hàng (COD)
                      </strong>
                      <span className="text-sm text-stone-600">
                        Trả tiền cho người giao hàng khi nhận được đơn.
                      </span>
                    </span>
                  </label>
                  <label
                    className={`flex cursor-pointer items-start gap-3 rounded-lg border p-3 ${
                      method === "WALLET"
                        ? "border-emerald-700 bg-emerald-50"
                        : "border-stone-200"
                    }`}
                  >
                    <input
                      type="radio"
                      name="payment-method"
                      checked={method === "WALLET"}
                      onChange={() => setMethod("WALLET")}
                      className="mt-1"
                    />
                    <span>
                      <strong className="flex items-center gap-2">
                        <Wallet size={18} aria-hidden="true" /> Ví DailyMart
                      </strong>
                      <span className="block text-sm text-stone-600">
                        {walletBalance !== null
                          ? `Số dư khả dụng: ${money(walletBalance)}`
                          : currentWallet?.error || "Đang tải số dư ví…"}
                      </span>
                    </span>
                  </label>
                  <label
                    className={`flex items-start gap-3 rounded-lg border p-3 ${
                      method === "SEPAY"
                        ? "border-emerald-700 bg-emerald-50"
                        : "border-stone-200"
                    } ${sepay?.available ? "cursor-pointer" : "opacity-60"}`}
                  >
                    <input
                      type="radio"
                      name="payment-method"
                      checked={method === "SEPAY"}
                      disabled={!sepay?.available}
                      onChange={() => setMethod("SEPAY")}
                      className="mt-1"
                    />
                    <span>
                      <strong className="block text-emerald-800">
                        SePay · Chuyển khoản QR
                      </strong>
                      <span className="text-sm text-stone-600">
                        {sepay?.message || "Đang kiểm tra kết nối SePay…"}
                      </span>
                    </span>
                  </label>
                </fieldset>
                <p className="rounded-lg bg-emerald-50 p-3 text-sm text-emerald-900">
                  {method === "COD"
                    ? "Bạn chưa cần thanh toán lúc đặt hàng. Vui lòng chuẩn bị đủ số tiền để thanh toán khi nhận hàng."
                    : method === "WALLET"
                      ? "Tiền sẽ được trừ từ ví ngay khi đặt hàng thành công. Bạn có thể xem giao dịch và khoản hoàn tiền trong Ví của tôi."
                      : "Nhập địa chỉ nhận hàng rồi bấm “Tạo đơn & hiện mã QR”. Mã QR sẽ xuất hiện ở bước tiếp theo, kèm số tài khoản, số tiền và nội dung chuyển khoản. Đơn giữ hàng trong 30 phút."}
                </p>
                {method === "WALLET" && currentWallet?.error && (
                  <div role="alert" className="mt-3 text-sm text-red-800">
                    <p>{currentWallet.error}</p>
                    <button
                      type="button"
                      className="min-h-11 font-semibold underline"
                      disabled={submitting}
                      onClick={() => setRetry((value) => value + 1)}
                    >
                      Tải lại số dư ví
                    </button>
                  </div>
                )}
                {method === "WALLET" &&
                  walletBalance !== null &&
                  cart &&
                  !walletEnough && (
                    <p role="alert" className="mt-3 text-sm text-red-800">
                      Ví còn thiếu {money(cart.tongTien - walletBalance)}. Vui
                      lòng bổ sung số dư ví hoặc chọn phương thức thanh toán
                      khác.
                    </p>
                  )}
                {method === "SEPAY" && !!cart && cart.tongTien < 1000 && (
                  <p role="alert" className="mt-3 text-sm text-red-800">
                    SePay yêu cầu tổng đơn từ 1.000đ. Hãy chọn thêm sản phẩm
                    hoặc thanh toán khi nhận hàng.
                  </p>
                )}
              </section>
            </div>
            <section className="self-start rounded-xl border border-stone-200 bg-white p-5">
              <h2 className="mb-4 flex items-center gap-2 text-lg font-bold">
                <ShoppingBag className="h-5 w-5 text-emerald-800" />
                Đơn hàng của bạn
              </h2>
              {!cart?.danhSachMon.length ? (
                <p className="text-stone-600">
                  Giỏ hàng đang trống. Hãy chọn sản phẩm trước khi thanh toán.
                </p>
              ) : (
                <ul className="max-h-80 space-y-4 overflow-y-auto">
                  {cart.danhSachMon.map((item) => (
                    <li
                      key={item.maSP}
                      className="flex items-start gap-3 text-sm"
                    >
                      <CartItemThumbnail item={item} />
                      <div className="min-w-0 flex-1">
                        <p className="break-words font-semibold">
                          {item.tenSP}
                        </p>
                        <div className="mt-1 flex flex-wrap justify-between gap-x-3 text-stone-600">
                          <span>
                            {item.soLuong} × {item.donViTinh}
                          </span>
                          <span>{money(item.thanhTien)}</span>
                        </div>
                        {item.conDuHang === false && (
                          <p className="mt-1 text-red-800">
                            Không đủ hàng. Vui lòng điều chỉnh giỏ hàng.
                          </p>
                        )}
                      </div>
                    </li>
                  ))}
                </ul>
              )}
              <div className="my-5 flex flex-wrap justify-between gap-2 border-t border-stone-200 pt-4 font-bold">
                <span>Tổng thanh toán</span>
                <span className="text-red-800">
                  {money(cart?.tongTien ?? 0)}
                </span>
              </div>
              <button
                type="submit"
                disabled={!canPay || submitting || !address.trim()}
                className="min-h-12 w-full rounded-lg bg-emerald-800 px-3 py-3 font-bold text-white hover:bg-emerald-900 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {submitting
                  ? "Đang xử lý…"
                  : method === "SEPAY"
                    ? "Tạo đơn & hiện mã QR"
                    : method === "WALLET"
                      ? "Thanh toán bằng ví"
                      : "Đặt hàng · trả tiền khi nhận"}
              </button>
              {!address.trim() && (
                <p className="mt-3 text-sm text-stone-600">
                  Nhập địa chỉ nhận hàng để tiếp tục.
                </p>
              )}
            </section>
          </form>
        )}
      </div>
    </main>
  );
}

function CheckoutRouterView() {
  const searchParams = useSearchParams();
  const visitKey = searchParams.get("v") || "default";
  return <CheckoutFormView key={visitKey} visitKey={visitKey} />;
}

export default function CheckoutPage() {
  return (
    <Suspense
      fallback={
        <main className="min-h-screen bg-stone-50 px-4 py-8 text-stone-900 sm:px-6">
          <div className="mx-auto max-w-4xl">
            <p role="status">Đang tải thanh toán…</p>
          </div>
        </main>
      }
    >
      <CheckoutRouterView />
    </Suspense>
  );
}
