"use client";

import React, { useState, useRef, useEffect } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { useCustomer } from "@/components/customer/CustomerProvider";
import { useRouter } from "next/navigation";
import {
  ChefHat,
  X,
  Send,
  Sparkles,
  ShoppingCart,
  ArrowRight,
  RefreshCw,
  UtensilsCrossed,
  Maximize2,
  Minimize2,
} from "lucide-react";
import { apiService } from "@/services/api";
import { ChatMessageResponse, RecipeDetailDto } from "@/types";

interface Message {
  id: string;
  sender: "user" | "bot";
  text: string;
  mcpTool?: string;
  recipe?: RecipeDetailDto;
  canAddToCart?: boolean;
  cartAdded?: boolean;
  suggestions?: RecipeDetailDto[];
}

interface FloatingChatbotProps {
  onCartUpdated: () => void;
  onNavigateCheckout: () => void;
}

export const FloatingChatbot: React.FC<FloatingChatbotProps> = ({
  onCartUpdated,
  onNavigateCheckout,
}) => {
  const { customer } = useCustomer();
  const router = useRouter();
  const [isOpen, setIsOpen] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);
  const [inputMessage, setInputMessage] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [messages, setMessages] = useState<Message[]>([
    {
      id: "intro-1",
      sender: "bot",
      text: "Xin chào! Tôi là Bếp Trưởng AI của DailyMart 🍲. Bạn đang có ý định nấu món gì cho gia đình hôm nay? Tôi có thể tư vấn công thức chuẩn vị, kiểm tra tồn kho nguyên liệu tươi trong kho và giúp bạn bỏ tất cả vào giỏ hàng chỉ trong 1 chạm!",
    },
  ]);

  const messageSequence = useRef(0);
  const lastRecipe = useRef<number | undefined>(undefined);
  const [promptChips, setPromptChips] = useState<string[]>([]);

  useEffect(() => {
    let cancelled = false;
    void fetch("/api/v1/ai/chatbot/recipes", { cache: "no-store" })
      .then((r) => r.json())
      .then((j) => {
        if (!cancelled && j.success) {
          setPromptChips(
            (j.data as RecipeDetailDto[])
              .filter((r) => r.duNguyenLieuTrongKho)
              .slice(0, 6)
              .map((r) => `Nấu ${r.tenMonAn}`)
          );
        }
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, []);

  const nextMessageId = () => String(++messageSequence.current);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    if (isOpen) {
      scrollToBottom();
    }
  }, [messages, isOpen, isExpanded]);

  useEffect(() => {
    if (!isOpen) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        if (isExpanded) setIsExpanded(false);
        else setIsOpen(false);
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [isOpen, isExpanded]);

  const handleSendMessage = async (textToSend?: string) => {
    const text = textToSend || inputMessage.trim();
    if (!text || isLoading) return;

    const userMsg: Message = {
      id: nextMessageId(),
      sender: "user",
      text,
    };

    setMessages((prev) => [...prev, userMsg]);
    if (!textToSend) setInputMessage("");
    setIsLoading(true);

    try {
      const response: ChatMessageResponse = await apiService.chatWithBot({
        customerId: customer?.maKH,
        recipeId: lastRecipe.current,
        message: text,
      });

      const botMsg: Message = {
        id: nextMessageId(),
        sender: "bot",
        text: response.reply,
        mcpTool: response.mcpToolInvoked,
        recipe: response.recipeSuggested,
        canAddToCart: response.canAddToCart,
        cartAdded: response.cartAdded,
        suggestions: response.suggestedRecipes,
      };
      if (response.recipeSuggested) lastRecipe.current = response.recipeSuggested.maCongThuc;
      else if (response.suggestedRecipes?.length) lastRecipe.current = undefined;

      setMessages((prev) => [...prev, botMsg]);

      if (response.cartAdded) {
        onCartUpdated();
      }
    } catch {
      setMessages((prev) => [
        ...prev,
        {
          id: nextMessageId(),
          sender: "bot",
          text: "Chưa kết nối được trợ lý. Bạn vui lòng thử lại nhé!",
        },
      ]);
    } finally {
      setIsLoading(false);
    }
  };

  // Add all recipe ingredients to cart
  const handleAddRecipeToCart = async (recipe: RecipeDetailDto) => {
    if (!customer) {
      router.push("/login?next=/");
      return;
    }
    if (isLoading || !recipe.duNguyenLieuTrongKho) return;
    setIsLoading(true);
    try {
      await apiService.addRecipeToCart(recipe.maCongThuc, recipe.khauPhan, customer.maKH);
      setMessages((previous) => [
        ...previous.map((message) =>
          message.recipe?.maCongThuc === recipe.maCongThuc ? { ...message, cartAdded: true } : message
        ),
        {
          id: nextMessageId(),
          sender: "bot",
          text: `Đã thêm đầy đủ nguyên liệu món "${recipe.tenMonAn}" vào giỏ hàng. Bạn có thể kiểm tra giỏ trước khi thanh toán.`,
          cartAdded: true,
        },
      ]);
    } catch (cause) {
      setMessages((previous) => [
        ...previous,
        {
          id: nextMessageId(),
          sender: "bot",
          text: `Chưa thêm nguyên liệu vào giỏ. ${
            cause instanceof Error ? cause.message : "Vui lòng thử lại."
          }`,
        },
      ]);
    } finally {
      onCartUpdated();
      setIsLoading(false);
    }
  };

  const visibleChips = isExpanded ? promptChips : promptChips.slice(0, 4);

  return (
    <>
      {/* Backdrop when expanded */}
      {isOpen && isExpanded && (
        <div
          aria-hidden="true"
          onClick={() => setIsExpanded(false)}
          className="fixed inset-0 z-40 bg-stone-950/30 backdrop-blur-[1px] transition-opacity"
        />
      )}

      {/* Floating Action Button (FAB) */}
      {!(isOpen && isExpanded) && (
        <div className="fixed bottom-5 right-5 z-50 flex items-center sm:bottom-6 sm:right-6">
          {!isOpen && (
            <div className="mr-3 hidden sm:flex items-center bg-white border border-[#08783C]/20 shadow-lg rounded-full px-3 py-1.5 text-xs font-bold text-[#08783C]">
              <Sparkles className="w-3.5 h-3.5 text-amber-500 mr-1.5" />
              <span>Hỏi Bếp Trưởng AI 🍲</span>
            </div>
          )}

          <button
            onClick={() => setIsOpen(!isOpen)}
            className={`w-14 h-14 rounded-full flex items-center justify-center shadow-xl transition-all duration-300 relative ${
              isOpen
                ? "bg-stone-800 text-white rotate-90"
                : "bg-[#08783C] text-white hover:scale-105 ring-4 ring-amber-400/30"
            }`}
            aria-label={isOpen ? "Đóng trợ lý ẩm thực" : "Mở trợ lý ẩm thực"}
            aria-expanded={isOpen}
            aria-controls="culinary-chat"
            title="Trợ lý Ẩm Thực AI"
          >
            {isOpen ? (
              <X className="w-6 h-6" />
            ) : (
              <>
                <ChefHat className="w-7 h-7" />
                <span className="absolute -top-1 -right-1 w-3.5 h-3.5 bg-emerald-500 rounded-full border-2 border-white animate-pulse" />
              </>
            )}
          </button>
        </div>
      )}

      {/* Floating Chat Drawer Window */}
      {isOpen && (
        <div
          id="culinary-chat"
          role="region"
          aria-label="Trợ lý ẩm thực"
          className={`fixed z-50 bg-white rounded-2xl border border-stone-200 shadow-2xl flex flex-col overflow-hidden transition-all duration-200 ${
            isExpanded
              ? "inset-3 sm:inset-5 lg:inset-y-5 lg:right-6 lg:left-auto lg:w-[min(900px,calc(100vw-3rem))]"
              : "bottom-24 right-4 sm:right-6 w-[calc(100vw-2rem)] sm:w-[460px] h-[min(680px,calc(100dvh-7rem))]"
          }`}
        >
          {/* Header */}
          <div className="bg-[#08783C] text-white px-4 py-3.5 flex items-center justify-between gap-2 shadow-xs">
            <div className="flex items-center space-x-3 min-w-0">
              <div className="w-10 h-10 shrink-0 rounded-full bg-white/10 flex items-center justify-center border border-white/20">
                <ChefHat className="w-5 h-5 text-amber-300" />
              </div>
              <div className="min-w-0">
                <div className="flex items-center gap-1.5 text-base font-bold truncate">
                  <span>Bếp Trưởng AI - DailyMart</span>
                </div>
                <div className="text-[11px] text-amber-100 flex items-center gap-1">
                  <span className="w-2 h-2 rounded-full bg-emerald-400 inline-block shrink-0" />
                  <span className="truncate">Sẵn sàng tra cứu công thức & kiểm kho</span>
                </div>
              </div>
            </div>

            <div className="flex items-center gap-1.5 shrink-0">
              <button
                type="button"
                aria-label={isExpanded ? "Thu nhỏ khung trò chuyện" : "Mở rộng khung trò chuyện"}
                title={isExpanded ? "Thu nhỏ" : "Mở rộng khung chat"}
                onClick={() => setIsExpanded((prev) => !prev)}
                className="inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-white/10 text-xs font-semibold text-white hover:bg-white/20 transition-colors"
              >
                {isExpanded ? (
                  <>
                    <Minimize2 className="w-4 h-4" />
                    <span className="hidden sm:inline">Thu nhỏ</span>
                  </>
                ) : (
                  <>
                    <Maximize2 className="w-4 h-4" />
                    <span className="hidden sm:inline">Mở rộng</span>
                  </>
                )}
              </button>
              <button
                type="button"
                aria-label="Đóng trò chuyện"
                title="Đóng"
                onClick={() => setIsOpen(false)}
                className="p-1.5 rounded-lg text-white/85 hover:bg-white/15 hover:text-white transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
          </div>

          {/* Quick Prompts Bar */}
          <div
            className={`px-3 py-2.5 bg-stone-50 border-b border-stone-200 flex items-center gap-1.5 text-xs ${
              isExpanded
                ? "flex-wrap"
                : "overflow-x-auto [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
            }`}
          >
            <span className="text-[11px] font-bold text-stone-500 uppercase shrink-0 mr-0.5">
              Gợi ý:
            </span>
            {visibleChips.map((chip, idx) => (
              <button
                key={idx}
                type="button"
                onClick={() => handleSendMessage(chip)}
                disabled={isLoading}
                className="shrink-0 bg-white border border-stone-200 hover:border-[#08783C] hover:text-[#08783C] px-2.5 py-1 rounded-full text-stone-700 transition-colors shadow-2xs"
              >
                {chip}
              </button>
            ))}
          </div>

          {/* Messages Container */}
          <div
            role="log"
            aria-live="polite"
            className={`flex-1 min-h-0 overflow-y-auto space-y-4 bg-gradient-to-b from-stone-50/50 to-white ${
              isExpanded ? "p-5 sm:p-6 text-sm sm:text-base" : "p-4 text-xs sm:text-sm"
            }`}
          >
            {messages.map((msg) => (
              <div
                key={msg.id}
                className={`flex flex-col ${msg.sender === "user" ? "items-end" : "items-start"}`}
              >
                {/* Message Bubble */}
                <div
                  className={`${
                    isExpanded ? "max-w-[82%]" : "max-w-[88%]"
                  } rounded-2xl p-3 sm:p-4 leading-relaxed shadow-2xs ${
                    msg.sender === "user"
                      ? "bg-[#08783C] text-white rounded-tr-none"
                      : "bg-white border border-stone-200 text-stone-800 rounded-tl-none"
                  }`}
                >
                  {msg.sender === "user" ? (
                    <p className="whitespace-pre-wrap break-words">{msg.text}</p>
                  ) : (
                    <div className="min-w-0 break-words [&_p]:mb-3 [&_p:last-child]:mb-0 [&_ul]:list-disc [&_ul]:pl-5 [&_ol]:list-decimal [&_ol]:pl-5 [&_li]:my-1 [&_strong]:font-semibold">
                      <ReactMarkdown remarkPlugins={[remarkGfm]} skipHtml>
                        {msg.text}
                      </ReactMarkdown>
                    </div>
                  )}
                </div>

                {/* Suggested Recipes List */}
                {!!msg.suggestions?.length && (
                  <div
                    className={`mt-2.5 grid w-full gap-2 ${
                      isExpanded ? "grid-cols-1 md:grid-cols-2" : "grid-cols-1"
                    }`}
                  >
                    {msg.suggestions.map((recipe) => (
                      <button
                        key={recipe.maCongThuc}
                        type="button"
                        disabled={isLoading}
                        onClick={() => handleSendMessage(`Nấu ${recipe.tenMonAn}`)}
                        className="rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-left text-sm text-emerald-900 hover:bg-emerald-100 transition-colors"
                      >
                        <strong>{recipe.tenMonAn}</strong>
                        <span className="mt-1 block text-xs">
                          Đủ nguyên liệu · {recipe.tongTienUocTinh.toLocaleString("vi-VN")} đ /{" "}
                          {recipe.khauPhan} người
                        </span>
                      </button>
                    ))}
                  </div>
                )}

                {/* Embedded Recipe Card */}
                {msg.recipe && (
                  <div
                    className={`mt-2.5 w-full ${
                      isExpanded ? "max-w-full p-4 sm:p-5" : "max-w-[97%] p-3.5"
                    } bg-[#FDF8EE] border border-amber-200 rounded-xl shadow-xs`}
                  >
                    <div className="flex flex-wrap items-center justify-between gap-2 border-b border-amber-200/60 pb-2.5 mb-2.5">
                      <div className="flex items-center space-x-2 font-bold text-stone-900 text-sm sm:text-base">
                        <UtensilsCrossed className="w-4 h-4 text-[#08783C] shrink-0" />
                        <span>{msg.recipe.tenMonAn}</span>
                      </div>
                      <span className="text-xs font-semibold text-amber-900 bg-amber-100 px-2.5 py-0.5 rounded-full">
                        Khẩu phần: {msg.recipe.khauPhan} người
                      </span>
                    </div>

                    <p className="text-xs text-stone-600 mb-3 italic">
                      &quot;{msg.recipe.moTa}&quot;
                    </p>

                    {/* Ingredients List with Inventory Check */}
                    <div className="my-2.5">
                      <div className="text-[11px] font-bold text-stone-700 uppercase tracking-wider mb-2">
                        Nguyên liệu tươi sống trong kho ({msg.recipe.nguyenLieuList.length} món):
                      </div>
                      <div
                        className={`grid gap-2 ${
                          isExpanded ? "grid-cols-1 md:grid-cols-2" : "grid-cols-1"
                        }`}
                      >
                        {msg.recipe.nguyenLieuList.map((item, i) => (
                          <div
                            key={i}
                            className="flex items-center justify-between gap-2 text-xs sm:text-[13px] bg-white/90 px-3 py-2 rounded-lg border border-amber-200/50"
                          >
                            <div className="flex items-center gap-2 min-w-0">
                              <span
                                className={`w-2 h-2 shrink-0 rounded-full ${
                                  item.conHang ? "bg-emerald-500" : "bg-red-500"
                                }`}
                              />
                              <div className="min-w-0">
                                <span className="font-medium text-stone-800">{item.tenSP}</span>
                                {item.thayTheCho && (
                                  <span className="ml-1.5 text-[10px] text-emerald-800">
                                    (Lựa chọn còn hàng)
                                  </span>
                                )}
                                <span className="ml-1 text-stone-500 text-[11px]">
                                  ({item.soLuongDinhLuong} {item.donViTinh})
                                </span>
                              </div>
                            </div>
                            <div className="text-right shrink-0">
                              <div className="font-bold text-[#B91C1C]">
                                {item.thanhTien.toLocaleString("vi-VN")}đ
                              </div>
                              <div
                                className={`text-[10px] ${
                                  item.conHang ? "text-emerald-700" : "text-red-700"
                                }`}
                              >
                                {item.conHang
                                  ? `Tồn: ${item.tonKhoHienTai} ${item.donViTinh}`
                                  : item.lyDoKhongKhaDung || "Không thể mua"}
                              </div>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>

                    {/* Total & Action */}
                    <div className="pt-3 mt-1 border-t border-amber-200/60 flex flex-wrap items-center justify-between gap-3">
                      <div>
                        <div className="text-[11px] text-stone-500 uppercase font-semibold">
                          Tổng nguyên liệu:
                        </div>
                        <div className="text-base font-extrabold text-[#B91C1C]">
                          {msg.recipe.tongTienUocTinh.toLocaleString("vi-VN")} VND
                        </div>
                      </div>

                      <button
                        type="button"
                        disabled={
                          isLoading ||
                          !msg.canAddToCart ||
                          !msg.recipe.duNguyenLieuTrongKho ||
                          msg.cartAdded
                        }
                        onClick={() => handleAddRecipeToCart(msg.recipe!)}
                        className="px-4 py-2 bg-[#08783C] hover:bg-[#056331] disabled:opacity-50 disabled:cursor-not-allowed text-white text-xs sm:text-sm font-bold rounded-lg flex items-center gap-1.5 shadow-xs transition-colors"
                      >
                        <ShoppingCart className="w-4 h-4" />
                        <span>
                          {msg.cartAdded
                            ? "Đã thêm vào giỏ"
                            : !msg.recipe.duNguyenLieuTrongKho
                              ? "Chưa đủ nguyên liệu"
                              : "Bỏ vào giỏ ngay"}
                        </span>
                      </button>
                    </div>
                  </div>
                )}

                {/* If cart was added successfully */}
                {msg.cartAdded && (
                  <div className="mt-2 w-full flex items-center justify-end">
                    <button
                      type="button"
                      onClick={onNavigateCheckout}
                      className="inline-flex items-center space-x-1.5 text-xs sm:text-sm font-bold text-white bg-emerald-700 hover:bg-emerald-800 px-3.5 py-2 rounded-lg shadow-xs transition-colors"
                    >
                      <span>Dẫn đi thanh toán ngay</span>
                      <ArrowRight className="w-4 h-4" />
                    </button>
                  </div>
                )}
              </div>
            ))}

            {isLoading && (
              <div className="flex items-center space-x-2 text-stone-500 text-xs sm:text-sm py-2">
                <RefreshCw className="w-4 h-4 animate-spin text-[#08783C]" />
                <span>Bếp Trưởng AI đang suy luận & kiểm kho...</span>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Input Bar */}
          <form
            onSubmit={(e) => {
              e.preventDefault();
              handleSendMessage();
            }}
            className="p-3 sm:p-3.5 bg-white border-t border-stone-200 flex items-center gap-2.5"
          >
            <input
              type="text"
              value={inputMessage}
              onChange={(e) => setInputMessage(e.target.value)}
              placeholder="Hỏi món ăn: 'Nấu canh chua cá lóc cho 4 người'..."
              aria-label="Tin nhắn cho trợ lý ẩm thực"
              maxLength={2000}
              className="min-w-0 flex-1 h-11 px-4 bg-stone-50 border border-stone-300 rounded-full text-sm sm:text-base focus:outline-hidden focus:border-[#08783C] focus:bg-white transition-all"
            />
            <button
              type="submit"
              disabled={isLoading || !inputMessage.trim()}
              className="w-11 h-11 rounded-full bg-[#08783C] hover:bg-[#056331] disabled:opacity-40 text-white flex items-center justify-center transition-colors shrink-0 shadow-xs"
              title="Gửi tin nhắn"
            >
              <Send className="w-4 h-4" />
            </button>
          </form>
        </div>
      )}
    </>
  );
};
