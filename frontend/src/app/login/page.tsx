"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { useRouter } from "next/navigation";
import { LoginLayout } from "@/components/auth/LoginLayout";
import styles from "@/components/auth/LoginLayout.module.css";
import { useCustomer } from "@/components/customer/CustomerProvider";
import { customerDestination } from "@/services/customer-session";

export default function CustomerLoginPage() {
  const { customer, loading, login } = useCustomer();
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);

  useEffect(() => {
    if (!loading && customer) router.replace(customerDestination());
  }, [customer, loading, router]);

  return (
    <LoginLayout
      role="customer"
      title="Đăng nhập khách hàng"
      description="Theo dõi đơn hàng và mua sắm bằng tài khoản của bạn."
      footer={(
        <>
          <p>
            Chưa có tài khoản?{" "}
            <Link href="/register" onNavigate={(event) => {
              const next = customerDestination();
              if (next !== "/") {
                event.preventDefault();
                router.push(`/register?next=${encodeURIComponent(next)}`);
              }
            }}>Đăng ký ngay</Link>
          </p>
          <Link href="/" className="customer-action justify-self-center"><ArrowLeft aria-hidden="true" />Tiếp tục xem sản phẩm</Link>
        </>
      )}
    >
      <form className={styles.form} onSubmit={async (event) => {
        event.preventDefault();
        setPending(true);
        setError("");
        try {
          await login(email, password);
          router.replace(customerDestination());
        } catch (failure) {
          setError(failure instanceof Error ? failure.message : "Không đăng nhập được.");
        } finally {
          setPending(false);
        }
      }}>
        {error && <p role="alert" className={styles.error}>{error}</p>}
        <label className={styles.field}>
          Email
          <input type="email" autoComplete="username" required value={email} onChange={(event) => setEmail(event.target.value)} />
        </label>
        <label className={styles.field}>
          Mật khẩu
          <input type="password" autoComplete="current-password" required value={password} onChange={(event) => setPassword(event.target.value)} />
        </label>
        <button type="submit" className={styles.submit} disabled={pending || loading}>
          {pending ? "Đang đăng nhập…" : "Đăng nhập"}
        </button>
      </form>
    </LoginLayout>
  );
}
