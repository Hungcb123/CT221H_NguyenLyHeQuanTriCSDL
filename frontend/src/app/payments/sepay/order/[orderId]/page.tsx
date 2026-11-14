import {Suspense} from "react";
import {SePayResult} from "@/components/customer/SePayResult";
async function Detail({params}:{params:Promise<{orderId:string}>}){const {orderId}=await params;return <SePayResult orderNumber={Number(orderId)}/>;}
export default function SePayOrderPage({params}:{params:Promise<{orderId:string}>}){return <Suspense fallback={<main className="p-8">Đang tải thanh toán…</main>}><Detail params={params}/></Suspense>;}
