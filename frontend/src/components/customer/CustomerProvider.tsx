"use client";
import { createContext, useContext, useEffect, useState } from "react";
import { customerAuth, customerSession, type Customer, type Registration } from "@/services/customer-session";
type State = { customer: Customer | null; loading: boolean; login: (email:string,password:string)=>Promise<void>; register:(details:Registration)=>Promise<void>; logout:()=>Promise<void> };
const Context = createContext<State | null>(null);
export function CustomerProvider({children}:{children:React.ReactNode}) {
  const [customer,setCustomer]=useState<Customer|null>(null);
  const [loading,setLoading]=useState(true);
  useEffect(()=>{ void customerSession().then(s=>setCustomer(s.customer)).catch(()=>setCustomer(null)).finally(()=>setLoading(false)); },[]);
  return <Context.Provider value={{customer,loading,
    login:async(email,password)=>{ const state=await customerAuth("login",{email,password});setCustomer(state.customer); },
    register:async details=>{const state=await customerAuth("register",details);setCustomer(state.customer);},
    logout:async()=>{ await customerAuth("logout");setCustomer(null); },
  }}>{children}</Context.Provider>;
}
export function useCustomer() { const state=useContext(Context); if(!state)throw new Error("Missing CustomerProvider"); return state; }
