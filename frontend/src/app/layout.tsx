import type { Metadata } from "next";
import "./globals.css";
import { CustomerProvider } from "@/components/customer/CustomerProvider";

export const metadata: Metadata = {
  title: "DailyMart - Bách Hóa Thực Phẩm Tươi Sống | CT221H",
  description:
    "Hệ thống mua sắm thực phẩm tươi sống chuẩn VietGAP tích hợp Trợ lý Bếp Trưởng AI Chatbot và Tìm kiếm Ngữ nghĩa Vector ONNX/Qdrant.",
  keywords: [
    "DailyMart",
    "Bách hóa tươi sống",
    "Thực phẩm VietGAP",
    "AI Chatbot ẩm thực",
    "CT221H Nguyên lý HQT CSDL",
  ],
  authors: [{ name: "CT221H Project Team" }],
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="vi" className="h-full antialiased">
      <body className="min-h-full flex flex-col font-sans"><CustomerProvider>{children}</CustomerProvider></body>
    </html>
  );
}
