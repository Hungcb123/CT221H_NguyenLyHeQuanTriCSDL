"use client";
import { useState } from "react";
import {
  BenchmarkResult,
  adminError,
  adminRequest,
} from "@/services/admin-api";
import { AdminTitle, Confirm, Notice } from "@/components/admin/AdminUI";
export default function BenchmarkPage() {
  const [result, setResult] = useState<BenchmarkResult | null>(null);
  const [count, setCount] = useState(1000);
  const [action, setAction] = useState<
    "generate" | "enable" | "disable" | null
  >(null);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  async function compare() {
    setPending(true);
    setError("");
    setMessage("");
    try {
      setResult(
        await adminRequest<BenchmarkResult>("/api/v1/admin/benchmark/compare", {
          method: "POST",
        }),
      );
    } catch (e) {
      setError(adminError(e));
    } finally {
      setPending(false);
    }
  }
  async function mutate() {
    if (!action) return;
    setPending(true);
    setError("");
    setMessage("");
    try {
      const data = await adminRequest<Record<string, unknown>>(
        action === "generate"
          ? `/api/v1/benchmark/generate-data?count=${count}`
          : `/api/v1/benchmark/indexes/toggle?enable=${action === "enable"}`,
        { method: "POST" },
      );
      if (data.status === "ERROR")
        throw new Error(String(data.message || "Thao tác không thành công."));
      setMessage(
        String(data.message || "Đã thực hiện thao tác cơ sở dữ liệu."),
      );
      setAction(null);
      setResult(null);
    } catch (e) {
      setError(adminError(e));
    } finally {
      setPending(false);
    }
  }
  return (
    <>
      <AdminTitle
        title="Đo hiệu năng chỉ mục"
        description="So sánh hai truy vấn trên các đơn được gắn nguồn benchmark trong SQL Server đang chạy."
      />
      <Notice error={error} message={message} />
      {action && (
        <Confirm
          title={
            action === "generate"
              ? `Thêm ${count.toLocaleString("vi-VN")} đơn benchmark?`
              : action === "enable"
                ? "Tạo chỉ mục bao phủ?"
                : "Gỡ chỉ mục bao phủ?"
          }
          description={
            action === "generate"
              ? "Dữ liệu sẽ được ghi vào bảng đơn hàng thật, không có chi tiết sản phẩm. Dashboard tách các đơn này khỏi doanh thu bán hàng. Thao tác này không tự hoàn tác."
              : "Thay đổi cấu trúc chỉ mục dùng cho bài đo trên cơ sở dữ liệu hiện tại; có thể ảnh hưởng thời gian truy vấn."
          }
          pending={pending}
          onCancel={() => setAction(null)}
          onConfirm={() => void mutate()}
        />
      )}
      <section className="ad-panel">
        <h2>Chạy đối chứng</h2>
        <dl className="ad-guides">
          <div className="ad-guide">
            <dt>Cách đo</dt>
            <dd>Chạy hai truy vấn tuần tự trên cùng dữ liệu, đo thời gian tại ứng dụng. Lệnh so sánh bảo đảm chỉ mục bao phủ tồn tại.</dd>
          </div>
          <div className="ad-guide">
            <dt>Cách đọc kết quả</dt>
            <dd>Bộ đệm, tải máy và thứ tự chạy có thể ảnh hưởng thời gian. Xem kế hoạch thực thi để phân biệt Scan và Seek, dùng STATISTICS IO để xem lượt đọc.</dd>
          </div>
        </dl>
        <div className="ad-actions">
          <button
            className="ad-btn"
            disabled={pending || !!action}
            onClick={() => void compare()}
          >
            {pending ? "Đang xử lý…" : "Chạy so sánh"}
          </button>
        </div>
      </section>
      {result && (
        <section className="ad-panel">
          <h2>Kết quả đo vừa chạy</h2>
          <p className="ad-help">
            {result.benchmarkOrdersInDatabase.toLocaleString("vi-VN")} đơn benchmark trên tổng{" "}
            {result.totalOrdersInDatabase.toLocaleString("vi-VN")} đơn trong cơ sở dữ liệu.
          </p>
          <div className="ad-table-wrap">
            <table className="ad-table">
              <thead>
                <tr>
                  <th>Kịch bản</th>
                  <th>Thời gian</th>
                  <th>Số dòng kết quả</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>{result.withoutIndex.accessMethod}</td>
                  <td>{result.withoutIndex.executionTimeMs} ms</td>
                  <td>{result.withoutIndex.recordsReturned}</td>
                </tr>
                <tr>
                  <td>{result.withCoveringIndex.accessMethod}</td>
                  <td>{result.withCoveringIndex.executionTimeMs} ms</td>
                  <td>{result.withCoveringIndex.recordsReturned}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div className="ad-actions">
            <span className="ad-tag">
              Tỷ số thời gian: {result.speedupFactor}
            </span>
            <span className="ad-tag">
              Giảm thời gian: {result.timeReductionPercentage}
            </span>
          </div>
          <p className="ad-help" style={{ marginTop: 16 }}>
            {result.academicEvaluation}
          </p>
        </section>
      )}
      <section className="ad-panel">
        <h2>Chuẩn bị dữ liệu & chỉ mục</h2>
        <p className="ad-help">
          Các thao tác dưới đây thay đổi cơ sở dữ liệu và cần xác nhận.
        </p>
        <div className="ad-actions">
          <select
            className="ad-select"
            style={{ width: "auto" }}
            aria-label="Số đơn benchmark cần thêm"
            value={count}
            disabled={pending || !!action}
            onChange={(e) => setCount(Number(e.target.value))}
          >
            {[1000, 10000, 50000, 100000, 500000, 1000000].map((n) => (
              <option key={n} value={n}>
                {n.toLocaleString("vi-VN")} đơn
              </option>
            ))}
          </select>
          <button
            className="ad-btn secondary"
            disabled={pending || !!action}
            onClick={() => setAction("generate")}
          >
            Sinh dữ liệu…
          </button>
          <button
            className="ad-btn secondary"
            disabled={pending || !!action}
            onClick={() => setAction("enable")}
          >
            Tạo chỉ mục…
          </button>
          <button
            className="ad-btn secondary"
            disabled={pending || !!action}
            onClick={() => setAction("disable")}
          >
            Gỡ chỉ mục…
          </button>
        </div>
      </section>
    </>
  );
}
