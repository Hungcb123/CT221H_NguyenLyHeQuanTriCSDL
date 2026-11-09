"use client";

import type { ReactNode } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ShieldCheck, ShoppingBasket, UserRound } from "lucide-react";
import { customerDestination } from "@/services/customer-session";
import styles from "./LoginLayout.module.css";

const roles = [
  { id: "customer", label: "Khách hàng", href: "/login", Icon: UserRound },
  { id: "admin", label: "Quản trị", href: "/admin/login", Icon: ShieldCheck },
] as const;

export function LoginLayout({ role, title, description, children, footer }: {
  role: (typeof roles)[number]["id"];
  title: string;
  description: string;
  children: ReactNode;
  footer: ReactNode;
}) {
  const router = useRouter();

  return (
    <main className={styles.page}>
      <div className={styles.card}>
        <div className={styles.brandPanel}>
          <Link href="/" className={styles.brand}>
            <span className={styles.brandMark}><ShoppingBasket size={20} aria-hidden="true" /></span>
            DailyMart
          </Link>
          <div className={styles.illustration} aria-hidden="true">
            <ShoppingBasket strokeWidth={1.3} />
          </div>
        </div>
        <section className={styles.content} aria-labelledby="login-title">
          <nav className={styles.roles} aria-label="Loại tài khoản đăng nhập">
            {roles.map(({ id, label, href, Icon }) => (
              <Link
                key={id}
                href={href}
                aria-current={role === id ? "page" : undefined}
                onNavigate={(event) => {
                  const next = customerDestination();
                  if (next !== "/") {
                    event.preventDefault();
                    router.push(`${href}?next=${encodeURIComponent(next)}`);
                  }
                }}
              >
                <Icon size={17} aria-hidden="true" />
                {label}
              </Link>
            ))}
          </nav>
          <header className={styles.heading}>
            <h1 id="login-title">{title}</h1>
            <p>{description}</p>
          </header>
          {children}
          <footer className={styles.footer}>{footer}</footer>
        </section>
      </div>
    </main>
  );
}
