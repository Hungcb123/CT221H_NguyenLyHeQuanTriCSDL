"use client";
import { useState } from "react";
import {
  AIStatus,
  AdminPage,
  AdminProduct,
  ForecastResult,
  adminApi,
  adminError,
  adminRequest,
  money,
} from "@/services/admin-api";
import type { HybridSearchResponse, SemanticProductResponse } from "@/types";
import { useAdminResource } from "@/components/admin/useAdminResource";
import { AdminTitle, Confirm, Empty, Notice } from "@/components/admin/AdminUI";
function SearchRows({ rows }: { rows: SemanticProductResponse[] }) {
  return (
    <div className="ad-table-wrap">
      <table className="ad-table">
        <thead>
          <tr>
            <th>Sản phẩm</th>
            <th>Giá</th>
            <th>Vector</th>
            <th>BM25</th>
            <th>Điểm xếp hạng</th>
            <th>Khớp</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((p) => (
            <tr key={p.maSP}>
              <td>
                {p.tenSP}
                <small>#{p.maSP}</small>
              </td>
              <td className="number">{money(p.gia)}</td>
              <td>{p.similarityScore?.toFixed(4) ?? "—"}</td>
              <td>{p.keywordScore?.toFixed(4) ?? "—"}</td>
              <td>{p.rankingScore?.toFixed(6) ?? "—"}</td>
              <td>{p.matchType}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {!rows.length && <Empty>Không có kết quả.</Empty>}
    </div>
  );
}
function ForecastRows({ rows }: { rows: ForecastResult[] }) {
  return (
    <div className="ad-table-wrap">
      <table className="ad-table">
        <thead>
          <tr>
            <th>Sản phẩm / ngày dự báo</th>
            <th>Tiêu thụ dự báo</th>
            <th>Tồn kho</th>
            <th>Đề xuất nhập</th>
            <th>Rủi ro</th>
            <th>Suy luận</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={`${r.maDuBao}-${i}`}>
              <td>
                {r.tenSP || `#${r.maSP}`}
                <small>{new Date(r.ngayDuBao).toLocaleDateString("vi-VN")}</small>
              </td>
              <td>{r.soLuongDuBaoTieuThu}</td>
              <td>{r.soLuongTonHienTai}</td>
              <td>{r.soLuongDeXuatNhap}</td>
              <td>
                <span
                  className={`ad-tag ${r.ruiRoHuHong === "CAO" ? "bad" : ""}`}
                >
                  {r.ruiRoHuHong}
                </span>
              </td>
              <td>{r.doTreInferenceMs ?? "—"} ms</td>
            </tr>
          ))}
        </tbody>
      </table>
      {!rows.length && <Empty>Chưa có dự báo.</Empty>}
    </div>
  );
}
export default function AIPage() {
  const status = useAdminResource<AIStatus>("/api/v1/admin/ai/status");
  const products = useAdminResource<AdminPage<AdminProduct>>(
    "/api/v1/admin/products?size=100",
  );
  const [query, setQuery] = useState("");
  const [search, setSearch] = useState<HybridSearchResponse | null>(null);
  const [product, setProduct] = useState("");
  const [forecasts, setForecasts] = useState<ForecastResult[]>([]);
  const [history, setHistory] = useState<ForecastResult[] | null>(null);
  const [customStock, setCustomStock] = useState("80");
  const [customExpiry, setCustomExpiry] = useState("1");
  const [customLag1, setCustomLag1] = useState("30");
  const [customLag7, setCustomLag7] = useState("32");
  const [customMean7, setCustomMean7] = useState("31");
  const [customStd7, setCustomStd7] = useState("3.5");
  const [pending, setPending] = useState("");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [batch, setBatch] = useState(false);
  async function run(kind: string, work: () => Promise<void>) {
    setPending(kind);
    setError("");
    setMessage("");
    try {
      await work();
    } catch (e) {
      setError(adminError(e));
    } finally {
      setPending("");
    }
  }
  return (
    <>
      <AdminTitle
        title="AI & dự báo nhập hàng"
        description="Vận hành các mô hình đang cài đặt: embedding cho hybrid search và mô hình ONNX dự báo nhu cầu."
      >
        <button
          className="ad-btn secondary"
          onClick={status.refresh}
          disabled={!!pending}
        >
          Kiểm tra lại
        </button>
      </AdminTitle>
      <Notice
        error={error || status.error || products.error}
        message={message}
      />
      <section className="ad-panel">
        <h2>Trạng thái hệ thống</h2>
        {status.data ? (
          <>
            <dl className="ad-status-list">
              <dt>Embedding</dt>
              <dd>
                <span
                  className={`ad-tag ${status.data.embedding.ready ? "" : "bad"}`}
                >
                  {status.data.embedding.ready ? "Sẵn sàng" : "Chưa sẵn sàng"}
                </span>{" "}
                {status.data.embedding.name} · {status.data.embedding.dimension}{" "}
                chiều
              </dd>
              <dt>Qdrant</dt>
              <dd>
                <span
                  className={`ad-tag ${status.data.qdrant.status === "ONLINE" ? "" : "bad"}`}
                >
                  {status.data.qdrant.status}
                </span>{" "}
                {status.data.qdrant.pointsCount} vector ·{" "}
                {status.data.qdrant.collectionName} ·{" "}
                {status.data.qdrant.distanceMetric}
              </dd>
              <dt>Dự báo</dt>
              <dd>
                <span
                  className={`ad-tag ${status.data.forecast.ready ? "" : "bad"}`}
                >
                  {status.data.forecast.ready ? "Sẵn sàng" : "Chưa sẵn sàng"}
                </span>{" "}
                {status.data.forecast.name}
              </dd>
            </dl>
            {status.data.forecast.evaluation && (
              <>
                <h3>Chỉ số thực nghiệm mô hình dự báo (Temporal Holdout 80/20 · {status.data.forecast.evaluation.catalogSize} sản phẩm)</h3>
                <dl className="ad-stats" style={{ marginBottom: 16 }}>
                  <div>
                    <dt>F1 quyết định nhập hàng</dt>
                    <dd>{status.data.forecast.evaluation.restockF1Percent}%</dd>
                  </div>
                  <div>
                    <dt>Accuracy nhập hàng</dt>
                    <dd>{status.data.forecast.evaluation.restockAccuracyPercent}%</dd>
                  </div>
                  <div>
                    <dt>Macro F1 phân loại rủi ro</dt>
                    <dd>{status.data.forecast.evaluation.spoilageRiskMacroF1Percent}%</dd>
                  </div>
                  <div>
                    <dt>Độ chính xác sản lượng (1−WAPE)</dt>
                    <dd>{status.data.forecast.evaluation.volumeAccuracyPercent}%</dd>
                  </div>
                  <div>
                    <dt>Hệ số R² / MAE / RMSE</dt>
                    <dd style={{ fontSize: 18 }}>
                      {status.data.forecast.evaluation.r2} · {status.data.forecast.evaluation.mae} · {status.data.forecast.evaluation.rmse}
                    </dd>
                  </div>
                </dl>
              </>
            )}
            <p className="ad-help">
              Khi embedding hoặc Qdrant lỗi, tìm kiếm dùng BM25 và trả chế độ
              KEYWORD_FALLBACK. Lỗi mô hình dự báo được báo lỗi; không tạo dự
              báo giả.
            </p>
          </>
        ) : (
          <Empty>
            {status.loading
              ? "Đang kiểm tra…"
              : "Chưa đọc được trạng thái. Bấm Kiểm tra lại để thử lại."}
          </Empty>
        )}
        <div className="ad-actions">
          <button
            className="ad-btn secondary"
            disabled={!!pending || !status.data?.embedding.ready}
            onClick={() =>
              void run("sync", async () => {
                const result = await adminRequest<{
                  message?: string;
                  totalProcessed: number;
                  totalSynced: number;
                }>("/api/v1/search/sync", { method: "POST" });
                setMessage(
                  result.message ||
                    `Đồng bộ hoàn tất: ${result.totalSynced}/${result.totalProcessed} sản phẩm.`,
                );
                status.refresh();
              })
            }
          >
            {pending === "sync" ? "Đang đồng bộ…" : "Đồng bộ vector sản phẩm"}
          </button>
        </div>
      </section>
      <section className="ad-panel">
        <h2>Thử hybrid search</h2>
        <p className="ad-help">
          Tên hàng chính xác được ưu tiên, sau đó kết hợp BM25 và vector bằng
          RRF. Điểm bên dưới dùng để xếp hạng, không phải xác suất đúng.
        </p>
        <form
          className="ad-toolbar"
          style={{ marginTop: 16 }}
          onSubmit={(e) => {
            e.preventDefault();
            void run("search", async () => {
              setSearch(null);
              setSearch(await adminApi.search(query.trim()));
            });
          }}
        >
          <input
            className="ad-input"
            aria-label="Truy vấn hybrid search"
            placeholder="Ví dụ: thịt bò, nguyên liệu nấu canh"
            required
            maxLength={500}
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <button className="ad-btn" disabled={!!pending || !query.trim()}>
            {pending === "search" ? "Đang tìm…" : "Thử tìm kiếm"}
          </button>
        </form>
        {search && (
          <>
            <p className="ad-help">
              Chế độ: {search.mode} · {search.rankingMethod} · k={search.rrfK} ·
              trọng số vector {search.semanticWeight}, BM25{" "}
              {search.keywordWeight}
            </p>
            <h3>Kết quả phù hợp</h3>
            <SearchRows rows={search.products} />
            <h3>Gợi ý thay thế</h3>
            <p className="ad-help">
              Tách riêng khỏi kết quả chính, đặc biệt khi cửa hàng không có mặt
              hàng được yêu cầu.
            </p>
            <SearchRows rows={search.relatedProducts} />
          </>
        )}
      </section>
      {batch && (
        <Confirm
          title="Dự báo toàn bộ sản phẩm?"
          description="Chạy mô hình với lịch sử bán hàng thực tế và lưu từng kết quả vào cơ sở dữ liệu. Đây là dự báo tham khảo cho kế hoạch nhập hàng."
          pending={!!pending}
          onCancel={() => setBatch(false)}
          onConfirm={() =>
            void run("batch", async () => {
              const result = await adminRequest<{
                danhSachDuBao: ForecastResult[];
                tongSoMatHang: number;
              }>("/api/v1/ai/forecast/run-batch", { method: "POST" });
              setForecasts(result.danhSachDuBao);
              setHistory(null);
              setBatch(false);
              setMessage(`Đã dự báo ${result.tongSoMatHang} sản phẩm.`);
            })
          }
        />
      )}
      <section className="ad-panel">
        <h2>Dự báo nhu cầu & đề xuất nhập</h2>
        <p className="ad-help">
          Đầu vào lấy từ doanh số SQL trong 7 ngày trước ngày dự báo; ngày không
          bán ghi nhận 0. Dữ liệu bán hàng ít có thể làm dự báo kém tin cậy. Mô
          hình chưa hiệu chỉnh độ tin cậy nên không hiển thị phần trăm chính
          xác.
        </p>
        <div className="ad-toolbar" style={{ marginTop: 16 }}>
          <select
            className="ad-select"
            aria-label="Sản phẩm dự báo"
            value={product}
            onChange={(e) => {
              setProduct(e.target.value);
              setHistory(null);
            }}
          >
            <option value="">Chọn sản phẩm</option>
            {products.data?.items.map((p) => (
              <option key={p.maSP} value={p.maSP}>
                #{p.maSP} · {p.tenSP}
              </option>
            ))}
          </select>
          <button
            className="ad-btn"
            disabled={!!pending || !product || !status.data?.forecast.ready}
            onClick={() =>
              void run("forecast", async () => {
                const result = await adminRequest<ForecastResult>(
                  `/api/v1/admin/ai/forecast/${product}`,
                  { method: "POST" },
                );
                setForecasts([result]);
                setHistory(null);
                setMessage("Đã chạy mô hình và lưu dự báo.");
              })
            }
          >
            {pending === "forecast" ? "Đang dự báo…" : "Dự báo & lưu"}
          </button>
          <button
            className="ad-btn secondary"
            disabled={!!pending || !product}
            onClick={() =>
              void run("history", async () => {
                setHistory(await adminApi.forecasts(Number(product)));
              })
            }
          >
            Xem lịch sử
          </button>
          <button
            className="ad-btn secondary"
            disabled={!!pending || !status.data?.forecast.ready}
            onClick={() => setBatch(true)}
          >
            Dự báo toàn bộ…
          </button>
        </div>
        {(products.data?.totalElements ?? 0) > 100 && (
          <p className="ad-help">
            Đang hiển thị 100 sản phẩm mới nhất; dự báo toàn bộ vẫn xử lý toàn
            catalog.
          </p>
        )}
        <details style={{ marginTop: 12, marginBottom: 16 }}>
          <summary>Giả lập kịch bản dự báo tùy chỉnh (kiểm thử rủi ro tồn kho & cận hạn)</summary>
          <form
            className="ad-form-grid"
            style={{ marginTop: 14 }}
            onSubmit={(e) => {
              e.preventDefault();
              if (!product) return;
              void run("custom-forecast", async () => {
                const result = await adminRequest<ForecastResult>(
                  "/api/v1/ai/forecast/predict-custom",
                  {
                    method: "POST",
                    body: JSON.stringify({
                      maSP: Number(product),
                      currentStock: Number(customStock),
                      daysUntilExpiry: Number(customExpiry),
                      lag1: Number(customLag1),
                      lag7: Number(customLag7),
                      rollingMean7: Number(customMean7),
                      rollingStd7: Number(customStd7),
                    }),
                  },
                );
                setForecasts([result]);
                setHistory(null);
                setMessage(
                  result.ruiRoHuHong === "CAO"
                    ? "Đã chạy kịch bản giả lập: phát hiện rủi ro CAO và tự động ghi cảnh báo kho."
                    : "Đã chạy kịch bản dự báo giả lập và lưu kết quả.",
                );
              });
            }}
          >
            <label className="ad-field">
              Tồn kho giả lập
              <input
                type="number"
                min={0}
                className="ad-input"
                value={customStock}
                onChange={(e) => setCustomStock(e.target.value)}
                required
              />
            </label>
            <label className="ad-field">
              Số ngày còn hạn (HSD)
              <input
                type="number"
                min={0}
                className="ad-input"
                value={customExpiry}
                onChange={(e) => setCustomExpiry(e.target.value)}
                required
              />
            </label>
            <label className="ad-field">
              Bán hôm qua (Lag_1)
              <input
                type="number"
                step="0.1"
                min={0}
                className="ad-input"
                value={customLag1}
                onChange={(e) => setCustomLag1(e.target.value)}
                required
              />
            </label>
            <label className="ad-field">
              Bán 7 ngày trước (Lag_7)
              <input
                type="number"
                step="0.1"
                min={0}
                className="ad-input"
                value={customLag7}
                onChange={(e) => setCustomLag7(e.target.value)}
                required
              />
            </label>
            <label className="ad-field">
              Trung bình 7 ngày (Rolling_Mean_7)
              <input
                type="number"
                step="0.1"
                min={0}
                className="ad-input"
                value={customMean7}
                onChange={(e) => setCustomMean7(e.target.value)}
                required
              />
            </label>
            <label className="ad-field">
              Độ lệch chuẩn 7 ngày (Rolling_Std_7)
              <input
                type="number"
                step="0.1"
                min={0}
                className="ad-input"
                value={customStd7}
                onChange={(e) => setCustomStd7(e.target.value)}
                required
              />
            </label>
            <div className="ad-span-full ad-actions" style={{ marginTop: 4 }}>
              <button
                type="submit"
                className="ad-btn secondary"
                disabled={!!pending || !product || !status.data?.forecast.ready}
              >
                {pending === "custom-forecast"
                  ? "Đang chạy giả lập…"
                  : "Chạy dự báo kịch bản giả lập"}
              </button>
            </div>
          </form>
        </details>
        {!!forecasts.length && (
          <>
            <h3>Kết quả vừa chạy</h3>
            <ForecastRows rows={forecasts} />
            {forecasts.length === 1 && forecasts[0].featuresUsed && (
              <details style={{ marginTop: 16 }}>
                <summary>Đầu vào thực tế của lần dự báo</summary>
                <pre className="ad-json">
                  {JSON.stringify(forecasts[0].featuresUsed, null, 2)}
                </pre>
              </details>
            )}
          </>
        )}
        {history && (
          <>
            <h3>Lịch sử đã lưu</h3>
            <ForecastRows rows={history} />
          </>
        )}
      </section>
    </>
  );
}
