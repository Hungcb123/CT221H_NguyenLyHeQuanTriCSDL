import {Suspense} from "react";
import {MoMoResult} from "@/components/customer/MoMoResult";
export default function MoMoReturnPage(){return <Suspense fallback={<main className="p-8">Đang tải thanh toán…</main>}><MoMoResult/></Suspense>;}
