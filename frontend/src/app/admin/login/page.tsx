"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { LockKeyhole } from "lucide-react";
import { adminError, adminLogin } from "@/services/admin-api";
import { useAdminAuth } from "@/components/admin/AdminShell";
import { Notice } from "@/components/admin/AdminUI";

export default function AdminLoginPage() {
  const { session, refresh } = useAdminAuth();
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);
  useEffect(() => {
    if (session?.authenticated) router.replace("/admin");
  }, [session, router]);
  return (
    <main className="admin-login">
      <section>
        <Link href="/" className="admin-brand">
          DailyMart
        </Link>
        <h1>Đăng nhập quản trị</h1>
        <p className="ad-help">
          Quản lý cửa hàng, theo dõi tồn kho và vận hành AI.
        </p>
        <form
          onSubmit={async (e) => {
            e.preventDefault();
            setError("");
            setPending(true);
            try {
              await adminLogin(username.trim(), password);
              setPassword("");
              await refresh();
              router.replace("/admin");
            } catch (failure) {
              setError(adminError(failure));
            } finally {
              setPending(false);
            }
          }}
        >
          <Notice error={error} />
          <label className="ad-field">
            Tên đăng nhập
            <input
              className="ad-input"
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              autoFocus
            />
          </label>
          <label className="ad-field">
            Mật khẩu
            <input
              type="password"
              className="ad-input"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </label>
          <button className="ad-btn" disabled={pending}>
            <LockKeyhole size={17} aria-hidden="true" />
            {pending ? "Đang xác thực…" : "Đăng nhập"}
          </button>
          <Link href="/" className="ad-help">
            Quay lại cửa hàng
          </Link>
        </form>
      </section>
    </main>
  );
}
