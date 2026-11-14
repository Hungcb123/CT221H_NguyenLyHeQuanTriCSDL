import {Suspense} from "react";
import {SePayResult} from "@/components/customer/SePayResult";
export default function SePayReturnPage(){return <Suspense fallback={<main className="p-8">Đang tải thanh toán…</main>}><SePayResult/></Suspense>;}
