"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
} from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  LayoutDashboard,
  Package,
  FolderTree,
  ReceiptText,
  Tags,
  Bell,
  BrainCircuit,
  Gauge,
  Users,
  LogOut,
  ArrowRight,
  LoaderCircle,
} from "lucide-react";
import {
  AdminSession,
  adminError,
  adminLogout,
  getAdminSession,
} from "@/services/admin-api";

const AuthContext = createContext<{
  session: AdminSession | null;
  refresh: () => Promise<void>;
}>({ session: null, refresh: async () => {} });
export const useAdminAuth = () => useContext(AuthContext);
const links = [
  ["/admin", "Tổng quan", LayoutDashboard],
  ["/admin/products", "Sản phẩm", Package],
  ["/admin/categories", "Danh mục", FolderTree],
  ["/admin/orders", "Đơn hàng", ReceiptText],
  ["/admin/promotions", "Khuyến mãi", Tags],
  ["/admin/customers", "Khách hàng", Users],
  ["/admin/alerts", "Cảnh báo", Bell],
  ["/admin/ai", "AI & dự báo", BrainCircuit],
  ["/admin/benchmark", "Hiệu năng", Gauge],
] as const;

export function AdminShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [session, setSession] = useState<AdminSession | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const refresh = useCallback(async () => {
    setError("");
    try {
      setSession(await getAdminSession());
    } catch (e) {
      setError(adminError(e));
    } finally {
      setLoading(false);
    }
  }, []);
  useEffect(() => {
    let active = true;
    getAdminSession()
      .then((result) => {
        if (active) setSession(result);
      })
      .catch((e) => {
        if (active) setError(adminError(e));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);
  useEffect(() => {
    const unauthorized = () => {
      setSession(null);
      router.replace("/admin/login");
    };
    window.addEventListener("admin:unauthorized", unauthorized);
    return () => window.removeEventListener("admin:unauthorized", unauthorized);
  }, [router]);
  useEffect(() => {
    if (
      !loading &&
      !error &&
      !session?.authenticated &&
      pathname !== "/admin/login"
    )
      router.replace("/admin/login");
  }, [loading, error, session, pathname, router]);
  useEffect(() => {
    if (!loading && session?.authenticated) {
      document.querySelector('.admin-sidebar nav a[aria-current="page"]')?.scrollIntoView({ block: "nearest", inline: "center" });
    }
  }, [loading, session?.authenticated, pathname]);
  if (pathname === "/admin/login")
    return (
      <AuthContext.Provider value={{ session, refresh }}>
        {children}
      </AuthContext.Provider>
    );
  if (loading || (!session?.authenticated && !error))
    return (
      <main className="admin-gate">
        <LoaderCircle className="animate-spin" aria-hidden="true" />
        <p role="status">Đang kiểm tra phiên quản trị…</p>
      </main>
    );
  if (error)
    return (
      <main className="admin-gate">
        <p role="alert">{error}</p>
        <button className="ad-btn" onClick={() => void refresh()}>
          Thử lại
        </button>
        <Link href="/">Về cửa hàng</Link>
      </main>
    );
  return (
    <AuthContext.Provider value={{ session, refresh }}>
      <div className="admin-shell">
        <aside className="admin-sidebar">
          <Link href="/" className="admin-brand" aria-label="DailyMart, mở cửa hàng">
            <span>
              DailyMart<span className="admin-brand-subtitle">Quản trị cửa hàng</span>
            </span>
            <ArrowRight size={18} aria-hidden="true" />
          </Link>
          <nav aria-label="Điều hướng quản trị">
            {links.map(([href, label, Icon]) => (
              <Link
                key={href}
                href={href}
                className={pathname === href ? "active" : ""}
                aria-current={pathname === href ? "page" : undefined}
              >
                <Icon size={18} aria-hidden="true" />
                {label}
              </Link>
            ))}
          </nav>
        </aside>
        <div className="admin-workspace">
          <header className="admin-topbar">
            <span>Không gian quản trị</span>
            <div className="admin-topbar-actions">
              <span className="ad-tag">{session?.username}</span>
              <button
                type="button"
                className="ad-btn secondary"
                onClick={async () => {
                  try {
                    await adminLogout();
                    setSession(null);
                    router.replace("/admin/login");
                  } catch (e) {
                    setError(adminError(e));
                  }
                }}
              >
                <LogOut size={17} aria-hidden="true" />
                Đăng xuất
              </button>
            </div>
          </header>
          <main className="admin-content">{children}</main>
        </div>
      </div>
    </AuthContext.Provider>
  );
}
