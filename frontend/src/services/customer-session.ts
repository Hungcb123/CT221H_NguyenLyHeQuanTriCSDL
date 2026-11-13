export interface Customer { maKH: number; hoTen: string; email: string; diaChi?: string }
export interface CustomerSession { customer: Customer | null; csrfToken: string }
export interface Registration { hoTen: string; email: string; soDienThoai: string; diaChi: string; password: string }
let session: CustomerSession | null = null;

export function customerDestination(): string {
  const next = new URLSearchParams(window.location.search).get("next");
  if (!next?.startsWith("/") || next.startsWith("//")) return "/";
  try {
    const destination = new URL(next, window.location.origin);
    if (destination.origin !== window.location.origin || destination.pathname.startsWith("//")) return "/";
    return `${destination.pathname}${destination.search}${destination.hash}`;
  } catch {
    return "/";
  }
}

export async function customerSession(): Promise<CustomerSession> {
  const response = await fetch("/api/v1/customer/auth/session", { cache: "no-store" });
  const json = await response.json();
  if (!response.ok || !json.success) throw new Error(json.message || "Không tải được tài khoản.");
  session = json.data;
  return json.data;
}
export async function customerHeaders(): Promise<Record<string,string>> {
  const state = session ?? await customerSession();
  return { "X-Customer-CSRF": state.csrfToken };
}
export async function customerAuth(action: "login" | "logout" | "register", body?: {email: string; password: string} | Registration): Promise<CustomerSession> {
  const response = await fetch(`/api/v1/customer/auth/${action}`, {
    method: "POST", headers: { "Content-Type": "application/json", ...await customerHeaders() },
    body: body ? JSON.stringify(body) : undefined,
  });
  const json = await response.json();
  if (!response.ok || !json.success) { session = null; throw new Error(json.message || "Không cập nhật được tài khoản."); }
  session = json.data;
  return json.data;
}
