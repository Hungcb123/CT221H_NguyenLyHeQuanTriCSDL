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
  const {customer} = useCustomer();
  const router = useRouter();
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
  const lastRecipe = useRef<number | undefined>(undefined);
  const [promptChips,setPromptChips] = useState<string[]>([]);
  useEffect(()=>{let cancelled=false;void fetch("/api/v1/ai/chatbot/recipes",{cache:"no-store"}).then(r=>r.json()).then(j=>{if(!cancelled&&j.success)setPromptChips((j.data as RecipeDetailDto[]).filter(r=>r.duNguyenLieuTrongKho).slice(0,4).map(r=>`Nấu ${r.tenMonAn}`));}).catch(()=>{});return()=>{cancelled=true;};},[]);
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
      if(response.recipeSuggested) lastRecipe.current=response.recipeSuggested.maCongThuc;
      else if(response.suggestedRecipes?.length) lastRecipe.current=undefined;

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
          text: "Chưa kết nối được trợ lý. Bạn vui lòng thử lại nhé!",
        },
      ]);
    } finally {
      setIsLoading(false);
    }
  };

  // Add all recipe ingredients to cart
  const handleAddRecipeToCart = async (recipe: RecipeDetailDto) => {
    if (!customer) { router.push("/login?next=/"); return; }
    if (isLoading || !recipe.duNguyenLieuTrongKho) return;
    setIsLoading(true);
    try {
      await apiService.addRecipeToCart(recipe.maCongThuc, recipe.khauPhan, customer.maKH);
      setMessages((previous) => [...previous.map((message) => message.recipe?.maCongThuc === recipe.maCongThuc ? { ...message, cartAdded: true } : message), { id: nextMessageId(), sender: "bot", text: `Đã thêm nguyên liệu cho món "${recipe.tenMonAn}" vào giỏ hàng. Bạn có thể kiểm tra giỏ trước khi thanh toán.`, cartAdded: true }]);
    } catch (cause) {
      setMessages((previous) => [...previous, { id: nextMessageId(), sender: "bot", text: `Chưa thêm nguyên liệu vào giỏ. ${cause instanceof Error ? cause.message : "Vui lòng thử lại."}` }]);
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
                <div className="flex items-center gap-1.5 text-base font-bold">
                  <span>Bếp Trưởng AI - DailyMart</span>
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
                {/* Message Bubble */}
                <div
                  className={`max-w-[85%] rounded-2xl p-3 sm:p-3.5 leading-relaxed shadow-2xs ${
                    msg.sender === "user"
                      ? "bg-[#08783C] text-white rounded-tr-none"
                      : "bg-white border border-stone-200 text-stone-800 rounded-tl-none"
                  }`}
                >
                  {msg.sender === "user" ? <p className="whitespace-pre-wrap break-words">{msg.text}</p> : (
                    <div className="min-w-0 break-words [&_p]:mb-3 [&_p:last-child]:mb-0 [&_ul]:list-disc [&_ul]:pl-5 [&_ol]:list-decimal [&_ol]:pl-5 [&_li]:my-1 [&_strong]:font-semibold">
                      <ReactMarkdown remarkPlugins={[remarkGfm]} skipHtml>{msg.text}</ReactMarkdown>
                    </div>
                  )}
                </div>

                {/* Embedded Recipe Card */}
                {!!msg.suggestions?.length && <div className="mt-2 flex w-full flex-col gap-2">{msg.suggestions.map(recipe=><button key={recipe.maCongThuc} disabled={isLoading} onClick={()=>handleSendMessage(`Nấu ${recipe.tenMonAn}`)} className="rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-left text-sm text-emerald-900 hover:bg-emerald-100"><strong>{recipe.tenMonAn}</strong><span className="mt-1 block text-xs">Đủ nguyên liệu · {recipe.tongTienUocTinh.toLocaleString("vi-VN")} đ / {recipe.khauPhan} người</span></button>)}</div>}
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
                            <span className={`w-2 h-2 shrink-0 rounded-full ${item.conHang ? "bg-emerald-500" : "bg-red-500"}`} />
                            <span className="font-medium text-stone-800">{item.tenSP}</span>
                            {item.thayTheCho && <span className="text-[10px] text-emerald-800">Lựa chọn còn hàng</span>}
                            <span className="text-stone-400 text-[11px]">
                              ({item.soLuongDinhLuong} {item.donViTinh})
                            </span>
                          </div>
                          <div className="text-right">
                            <div className="font-bold text-[#B91C1C]">
                              {item.thanhTien.toLocaleString("vi-VN")}đ
                            </div>
                            <div className={`text-[10px] ${item.conHang ? "text-emerald-700" : "text-red-700"}`}>
                              {item.conHang ? `Tồn: ${item.tonKhoHienTai} ${item.donViTinh}` : item.lyDoKhongKhaDung || "Không thể mua"}
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
                        className="px-3.5 py-1.5 bg-[#08783C] hover:bg-[#056331] disabled:opacity-50 disabled:cursor-not-allowed text-white text-xs font-bold rounded-lg flex items-center gap-1.5 shadow-xs transition-colors"
                      >
                        <ShoppingCart className="w-3.5 h-3.5" />
                        <span>{msg.cartAdded ? "Đã thêm vào giỏ" : !msg.recipe.duNguyenLieuTrongKho ? "Chưa đủ nguyên liệu" : "Bỏ vào giỏ ngay"}</span>
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
