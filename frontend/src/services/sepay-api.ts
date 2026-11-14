import {customerHeaders} from "./customer-session";

export interface SePayConfig {available:boolean;message:string}
export interface SePayPayment {paymentId:string;maDH:number;amount:number;status:"CREATING"|"PENDING"|"PAID"|"FAILED"|"REVIEW";qrUrl:string|null;bankCode:string;accountNumber:string;accountName:string;transferContent:string;message:string}
async function request<T>(path:string,body?:unknown):Promise<T>{
  const response=await fetch(`/api/v1/payments/sepay${path}`,{
    cache:"no-store",method:body===undefined?"GET":"POST",
    headers:body===undefined?undefined:{"Content-Type":"application/json",...await customerHeaders()},
    body:body===undefined?undefined:JSON.stringify(body),
  });
  const result=await response.json().catch(()=>null);
  if(!response.ok||!result?.success)throw new Error(result?.message||"Chưa xác nhận được kết quả SePay. Vui lòng kiểm tra đơn trước khi thanh toán lại.");
  return result.data;
}
export const sepayApi={
  config:()=>request<SePayConfig>("/config"),
  create:(address:string,note:string,idempotencyKey:string)=>request<SePayPayment>("",{address,note,idempotencyKey}),
  get:(id:string)=>request<SePayPayment>(`/${encodeURIComponent(id)}`),
  refresh:(id:string)=>request<SePayPayment>(`/${encodeURIComponent(id)}/refresh`,{}),
  forOrder:(order:number)=>request<SePayPayment|Record<string,never>>(`/order/${order}`),
};
