import { Suspense } from "react";
import { CustomerOrders } from "@/components/customer/CustomerOrders";

export default function OrdersPage() {
  return (
    <Suspense fallback={<main className="p-8">Đang tải đơn hàng…</main>}>
      <CustomerOrders />
    </Suspense>
  );
}
