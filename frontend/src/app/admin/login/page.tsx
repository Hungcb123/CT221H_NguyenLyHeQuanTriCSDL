"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, LockKeyhole } from "lucide-react";
import { adminError, adminLogin } from "@/services/admin-api";
import { useAdminAuth } from "@/components/admin/AdminShell";
import { LoginLayout } from "@/components/auth/LoginLayout";
import styles from "@/components/auth/LoginLayout.module.css";

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
    <LoginLayout
      role="admin"
      title="Đăng nhập quản trị"
      description="Quản lý cửa hàng, theo dõi tồn kho và vận hành AI."
      footer={<Link href="/" className="customer-action justify-self-center"><ArrowLeft aria-hidden="true" />Quay lại cửa hàng</Link>}
    >
      <form className={styles.form} onSubmit={async (event) => {
        event.preventDefault();
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
      }}>
        {error && <p role="alert" className={styles.error}>{error}</p>}
        <label className={styles.field}>
          Tên đăng nhập
          <input autoComplete="username" value={username} onChange={(event) => setUsername(event.target.value)} required />
        </label>
        <label className={styles.field}>
          Mật khẩu
          <input type="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} required />
        </label>
        <button type="submit" className={styles.submit} disabled={pending}>
          <LockKeyhole size={17} aria-hidden="true" />
          {pending ? "Đang xác thực…" : "Đăng nhập"}
        </button>
      </form>
    </LoginLayout>
  );
}
