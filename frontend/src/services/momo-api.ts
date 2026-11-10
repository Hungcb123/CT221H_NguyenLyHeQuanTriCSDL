import {customerHeaders} from "./customer-session";

export interface MoMoConfig {available:boolean;sandbox:true;message:string}
export interface MoMoPayment {paymentId:string;maDH:number;amount:number;status:"CREATING"|"PENDING"|"PAID"|"FAILED"|"REVIEW";sandbox:true;payUrl:string|null;message:string}
async function request<T>(path:string,body?:unknown):Promise<T>{
  const response=await fetch(`/api/v1/payments/momo${path}`,{
    cache:"no-store",method:body===undefined?"GET":"POST",
    headers:body===undefined?undefined:{"Content-Type":"application/json",...await customerHeaders()},
    body:body===undefined?undefined:JSON.stringify(body),
  });
  const result=await response.json().catch(()=>null);
  if(!response.ok||!result?.success)throw new Error(result?.message||"Chưa xác nhận được kết quả MoMo. Vui lòng kiểm tra đơn trước khi thanh toán lại.");
  return result.data;
}
export const momoApi={
  config:()=>request<MoMoConfig>("/config"),
  create:(address:string,note:string,idempotencyKey:string)=>request<MoMoPayment>("",{address,note,idempotencyKey}),
  get:(id:string)=>request<MoMoPayment>(`/${encodeURIComponent(id)}`),
  refresh:(id:string)=>request<MoMoPayment>(`/${encodeURIComponent(id)}/refresh`,{}),
  forOrder:(order:number)=>request<MoMoPayment|Record<string,never>>(`/order/${order}`),
};

export function isMoMoSandboxUrl(value:string):boolean {
  try {const url=new URL(value);return url.protocol==="https:"&&url.hostname==="test-payment.momo.vn"&&!url.username&&!url.password&&(!url.port||url.port==="443");}
  catch{return false;}
}
