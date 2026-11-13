import {CustomerOrders} from "@/components/customer/CustomerOrders";
import {Suspense} from "react";
async function OrderDetail({params}:{params:Promise<{orderId:string}>}){const {orderId}=await params;return <CustomerOrders orderId={orderId}/>;}
export default function OrderPage({params}:{params:Promise<{orderId:string}>}){return <Suspense fallback={<main className="p-8">Đang tải đơn hàng…</main>}><OrderDetail params={params}/></Suspense>;}
