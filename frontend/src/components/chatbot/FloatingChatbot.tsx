"use client";

import React, { useState, useRef, useEffect } from "react";
import {
  ChefHat,
  X,
  Send,
  Sparkles,
  ShoppingCart,
  ArrowRight,
  Cpu,
  RefreshCw,
  UtensilsCrossed,
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
}

interface FloatingChatbotProps {
  onCartUpdated: () => void;
  onNavigateCheckout: () => void;
}

export const FloatingChatbot: React.FC<FloatingChatbotProps> = ({
  onCartUpdated,
  onNavigateCheckout,
}) => {
  const [isOpen, setIsOpen] = useState(false);
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
  const nextMessageId = () => String(++messageSequence.current);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    if (isOpen) {
      scrollToBottom();
    }
  }, [messages, isOpen]);

  // Pre-configured smart culinary prompts
  const promptChips = [
    "Nấu canh chua cá lóc cho 4 người 🍲",
    "Thịt ba rọi kho tàu trứng gà 🥩",
    "Nguyên liệu eat-clean giảm mỡ 🥗",
  ];

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
        customerId: 1,
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
      };

      setMessages((prev) => [...prev, botMsg]);

      // If items were added via text command
      if (response.cartAdded) {
        onCartUpdated();
      }
    } catch {
      setMessages((prev) => [
        ...prev,
        {
          id: nextMessageId(),
          sender: "bot",
          text: "Xin lỗi, hiện tại đang có gián đoạn kết nối với MCP Server. Bạn vui lòng thử lại nhé!",
        },
      ]);
    } finally {
      setIsLoading(false);
    }
  };

  // Add all recipe ingredients to cart
  const handleAddRecipeToCart = async (recipe: RecipeDetailDto) => {
    if (isLoading || !recipe.duNguyenLieuTrongKho) return;
    setIsLoading(true);
    let added = 0;
    try {
      for (const item of recipe.nguyenLieuList) {
        await apiService.addToCart({ customerId: 1, productId: item.maSP, quantity: Math.max(1, Math.ceil(item.soLuongDinhLuong)) });
        added += 1;
      }
      setMessages((previous) => [...previous.map((message) => message.recipe?.maCongThuc === recipe.maCongThuc ? { ...message, cartAdded: true } : message), { id: nextMessageId(), sender: "bot", text: `Đã thêm nguyên liệu cho món "${recipe.tenMonAn}" vào giỏ hàng. Bạn có thể kiểm tra giỏ trước khi thanh toán.`, cartAdded: true }]);
    } catch (cause) {
      setMessages((previous) => [...previous, { id: nextMessageId(), sender: "bot", text: `Đã thêm ${added}/${recipe.nguyenLieuList.length} nguyên liệu. ${cause instanceof Error ? cause.message : "Vui lòng thử lại."} Hãy kiểm tra giỏ trước khi thêm tiếp.` }]);
    } finally {
      onCartUpdated();
      setIsLoading(false);
    }
  };

  return (
    <>
      {/* Floating Action Button (FAB) */}
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
          title="Trợ lý Ẩm Thực AI (REQ-AI-03)"
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

      {/* Floating Chat Drawer Window */}
      {isOpen && (
        <div id="culinary-chat" role="region" aria-label="Trợ lý ẩm thực" className="fixed bottom-24 right-4 sm:right-6 z-50 w-[calc(100vw-2rem)] sm:w-[420px] max-h-[calc(100dvh-7rem)] h-[640px] bg-white rounded-2xl border border-stone-200 shadow-2xl flex flex-col overflow-hidden animate-in fade-in slide-in-from-bottom-5 duration-200">
          {/* Header */}
          <div className="bg-[#08783C] text-white p-4 flex items-center justify-between shadow-xs">
            <div className="flex items-center space-x-3">
              <div className="w-10 h-10 rounded-full bg-white/10 flex items-center justify-center border border-white/20">
                <ChefHat className="w-5 h-5 text-amber-300" />
              </div>
              <div>
                <div className="font-extrabold text-sm flex items-center gap-1.5 font-serif">
                  <span>Bếp Trưởng AI - DailyMart</span>
                  <span className="bg-amber-400/20 text-amber-200 text-[10px] px-1.5 py-0.2 rounded font-sans font-semibold">
                    MCP Tools
                  </span>
                </div>
                <div className="text-[11px] text-amber-100 flex items-center gap-1">
                  <span className="w-2 h-2 rounded-full bg-emerald-400 inline-block" />
                  <span>Sẵn sàng tra cứu công thức & kiểm kho</span>
                </div>
              </div>
            </div>

            <button
              aria-label="Đóng trò chuyện"
              onClick={() => setIsOpen(false)}
              className="p-1 rounded-lg text-white/80 hover:bg-white/10 hover:text-white"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Quick Prompts Bar */}
          <div className="p-2.5 bg-stone-50 border-b border-stone-200 flex items-center gap-1.5 overflow-x-auto no-scrollbar text-xs">
            <span className="text-[11px] font-bold text-stone-500 uppercase shrink-0">Gợi ý:</span>
            {promptChips.map((chip, idx) => (
              <button
                key={idx}
                onClick={() => handleSendMessage(chip)}
                disabled={isLoading}
                className="shrink-0 bg-white border border-stone-200 hover:border-[#08783C] hover:text-[#08783C] px-2.5 py-1 rounded-full text-stone-700 transition-colors shadow-2xs"
              >
                {chip}
              </button>
            ))}
          </div>

          {/* Messages Container */}
          <div role="log" aria-live="polite" className="flex-1 min-h-0 p-4 overflow-y-auto space-y-4 bg-gradient-to-b from-stone-50/50 to-white text-xs sm:text-sm">
            {messages.map((msg) => (
              <div
                key={msg.id}
                className={`flex flex-col ${msg.sender === "user" ? "items-end" : "items-start"}`}
              >
                {/* Sender badge / Tool Invocation Indicator */}
                {msg.mcpTool && (
                  <div className="mb-1 flex items-center gap-1 text-[10px] text-amber-800 bg-amber-50 border border-amber-200 px-2 py-0.5 rounded-full font-mono">
                    <Cpu className="w-3 h-3 text-amber-600" />
                    <span>MCP Tool kích hoạt: {msg.mcpTool}</span>
                  </div>
                )}

                {/* Message Bubble */}
                <div
                  className={`max-w-[85%] rounded-2xl p-3 sm:p-3.5 leading-relaxed shadow-2xs ${
                    msg.sender === "user"
                      ? "bg-[#08783C] text-white rounded-tr-none"
                      : "bg-white border border-stone-200 text-stone-800 rounded-tl-none"
                  }`}
                >
                  <p>{msg.text}</p>
                </div>

                {/* Embedded Recipe Card */}
                {msg.recipe && (
                  <div className="mt-2.5 w-full max-w-[95%] bg-[#FDF8EE] border border-amber-200 rounded-xl p-3 shadow-xs">
                    <div className="flex items-center justify-between border-b border-amber-200/60 pb-2 mb-2">
                      <div className="flex items-center space-x-1.5 font-bold text-stone-900 text-sm">
                        <UtensilsCrossed className="w-4 h-4 text-[#08783C]" />
                        <span>{msg.recipe.tenMonAn}</span>
                      </div>
                      <span className="text-[11px] font-semibold text-amber-900 bg-amber-100 px-2 py-0.5 rounded-full">
                        Khẩu phần: {msg.recipe.khauPhan} người
                      </span>
                    </div>

                    <p className="text-[11px] text-stone-600 mb-2 italic">&quot;{msg.recipe.moTa}&quot;</p>

                    {/* Ingredients List with Inventory Check */}
                    <div className="space-y-1.5 my-2.5">
                      <div className="text-[11px] font-bold text-stone-700 uppercase tracking-wider">
                        Nguyên liệu tươi sống trong kho:
                      </div>
                      {msg.recipe.nguyenLieuList.map((item, i) => (
                        <div
                          key={i}
                          className="flex items-center justify-between text-xs bg-white/80 p-2 rounded border border-amber-200/40"
                        >
                          <div className="flex items-center space-x-2">
                            <span className="w-2 h-2 rounded-full bg-emerald-500" />
                            <span className="font-medium text-stone-800">{item.tenSP}</span>
                            <span className="text-stone-400 text-[11px]">
                              ({item.soLuongDinhLuong} {item.donViTinh})
                            </span>
                          </div>
                          <div className="text-right">
                            <div className="font-bold text-[#B91C1C]">
                              {item.thanhTien.toLocaleString("vi-VN")}đ
                            </div>
                            <div className="text-[10px] text-emerald-700">
                              Tồn: {item.tonKhoHienTai} {item.donViTinh}
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>

                    {/* Total & Action */}
                    <div className="pt-2 border-t border-amber-200/60 flex items-center justify-between">
                      <div>
                        <div className="text-[10px] text-stone-500 uppercase font-semibold">Tổng nguyên liệu:</div>
                        <div className="text-sm font-extrabold text-[#B91C1C]">
                          {msg.recipe.tongTienUocTinh.toLocaleString("vi-VN")} VND
                        </div>
                      </div>

                      <button
                        disabled={isLoading || !msg.canAddToCart || !msg.recipe.duNguyenLieuTrongKho || msg.cartAdded}
                        onClick={() => handleAddRecipeToCart(msg.recipe!)}
                        className="px-3.5 py-1.5 bg-[#08783C] hover:bg-[#056331] text-white text-xs font-bold rounded-lg flex items-center gap-1.5 shadow-xs transition-colors"
                      >
                        <ShoppingCart className="w-3.5 h-3.5" />
                        <span>Bỏ vào giỏ ngay</span>
                      </button>
                    </div>
                  </div>
                )}

                {/* If cart was added successfully */}
                {msg.cartAdded && (
                  <div className="mt-2 w-full flex items-center justify-end">
                    <button
                      onClick={onNavigateCheckout}
                      className="inline-flex items-center space-x-1.5 text-xs font-bold text-white bg-emerald-700 hover:bg-emerald-800 px-3.5 py-1.5 rounded-lg shadow-xs transition-colors"
                    >
                      <span>Dẫn đi thanh toán ngay</span>
                      <ArrowRight className="w-3.5 h-3.5" />
                    </button>
                  </div>
                )}
              </div>
            ))}

            {isLoading && (
              <div className="flex items-center space-x-2 text-stone-500 text-xs py-2">
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
            className="p-3 bg-white border-t border-stone-200 flex items-center gap-2"
          >
            <input
              type="text"
              value={inputMessage}
              onChange={(e) => setInputMessage(e.target.value)}
              placeholder="Hỏi món ăn: 'Nấu canh chua cá lóc cho 4 người'..."
              aria-label="Tin nhắn cho trợ lý ẩm thực"
              maxLength={2000}
              className="min-w-0 flex-1 h-11 px-3.5 bg-stone-50 border border-stone-300 rounded-full text-base focus:outline-hidden focus:border-[#08783C] focus:bg-white transition-all"
            />
            <button
              type="submit"
              disabled={isLoading || !inputMessage.trim()}
              className="w-10 h-10 rounded-full bg-[#08783C] hover:bg-[#056331] disabled:opacity-40 text-white flex items-center justify-center transition-colors shrink-0 shadow-xs"
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
