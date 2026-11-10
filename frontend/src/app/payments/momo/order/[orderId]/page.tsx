import {Suspense} from "react";
import {MoMoResult} from "@/components/customer/MoMoResult";
async function Detail({params}:{params:Promise<{orderId:string}>}){const {orderId}=await params;return <MoMoResult orderNumber={Number(orderId)}/>;}
export default function MoMoOrderPage({params}:{params:Promise<{orderId:string}>}){return <Suspense fallback={<main className="p-8">Đang tải thanh toán…</main>}><Detail params={params}/></Suspense>;}
