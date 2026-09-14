import { useState } from "react";
import { useChatRoom } from "./useChatRoom";
import { ChatMessageBubble } from "./ChatMessageBubble";
import { getAccessToken } from "../../api/client";

export default function ChatRoomView({ roomId, customerId, isAdmin = false }: { roomId: number; customerId: number; isAdmin?: boolean }) {
  const [input, setInput] = useState("");
  
  const {
    messages,
    isLoading,
    isCompleted,
    status,
    isError,
    showScrollBottom,
    containerRef,
    handleScroll,
    scrollToBottom,
    sendMessage
  } = useChatRoom(roomId);

  const isAdminBotMode = isAdmin && status === "BOT_MODE";
  const isInputDisabled = isCompleted || isAdminBotMode;

  const send = (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim() || isInputDisabled) return;
    sendMessage(customerId, input);
    setInput("");
  };

  return (
    <div className="flex flex-col h-full bg-[#060606] relative">
      <div 
        ref={containerRef}
        onScroll={handleScroll}
        className="flex-1 overflow-y-auto overflow-x-hidden p-4 space-y-4 custom-scrollbar"
      >
        {!isAdmin && !isCompleted && (
          <div className="flex flex-col items-center justify-center my-6 pb-6 border-b border-[rgba(204,34,0,0.2)]">
            <p className="text-[11px] text-gray-500 mb-3 text-center" style={{ fontFamily: "Noto Sans KR, sans-serif" }}>
              [ 시스템 : 본 채널을 더 이상 사용하지 않는다면<br/>아래 버튼을 눌러 통신을 종료하십시오. ]
            </p>
            <button 
              onClick={() => {
                if (window.confirm("현재 통신을 완전히 종료하시겠습니까?")) {
                  fetch(`/api/chat/rooms/${roomId}/close`, { 
                    method: "PATCH",
                    headers: {
                      Authorization: `Bearer ${getAccessToken()}`
                    }
                  });
                }
              }}
              className="text-xs font-bold px-4 py-2 rounded transition-colors uppercase tracking-widest border border-[#cc2200] text-[#ff4422] hover:bg-[#cc2200] hover:text-white"
              style={{ fontFamily: "Share Tech Mono, monospace", boxShadow: "0 0 10px rgba(204,34,0,0.1)" }}
            >
              CLOSE CHANNEL
            </button>
          </div>
        )}

        {isLoading && (
          <div className="text-center py-2 text-[#a08070] text-xs font-mono">
            Loading past communications...
          </div>
        )}
        
        {isError && (
          <div className="text-center py-3 my-2 text-xs font-mono rounded" style={{ background: "rgba(204,34,0,0.1)", border: "1px solid rgba(204,34,0,0.3)", color: "#e83010" }}>
            통신이 불안정하여 일부 대화 기록을 불러오지 못했습니다.
          </div>
        )}

        {messages.map((m, idx) => (
          <ChatMessageBubble 
            key={idx}
            message={m}
            customerId={customerId}
            isAdmin={isAdmin}
            isCompleted={isCompleted}
            isLatest={idx === messages.length - 1}
            onSendBotOption={(label) => sendMessage(customerId, label)}
          />
        ))}
      </div>

      {showScrollBottom && (
        <button
          onClick={scrollToBottom}
          className="absolute bottom-14 left-1/2 -translate-x-1/2 px-4 py-1.5 rounded-full flex items-center justify-center gap-1.5 transition-all hover:bg-black shadow-[0_4px_10px_rgba(0,0,0,0.5)] z-50 text-[10px] font-bold tracking-widest uppercase cursor-pointer"
          style={{ background: "rgba(20,0,0,0.95)", border: "1px solid rgba(255,68,34,0.4)", color: "#ff4422", fontFamily: "Share Tech Mono" }}
        >
          <span>Latest</span>
          <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="6 9 12 15 18 9"></polyline>
          </svg>
        </button>
      )}

      <form onSubmit={send} className="p-3 border-t flex gap-2" style={{ borderColor: "rgba(204,34,0,0.3)", background: "#0a0000" }}>
        <input 
          type="text" 
          value={input}
          onChange={(e) => setInput(e.target.value)}
          disabled={isInputDisabled}
          placeholder={
            isCompleted ? "상담이 완전히 종료되었습니다." 
            : isAdminBotMode ? "봇 모드에서는 채팅을 입력할 수 없습니다."
            : "메시지 전송..."
          }
          className="flex-1 bg-black border rounded px-3 py-2 text-sm text-white focus:outline-none focus:border-[#cc2200] transition-colors"
          style={{ borderColor: "rgba(204,34,0,0.25)", opacity: isInputDisabled ? 0.5 : 1 }}
        />
        <button 
          type="submit" 
          disabled={isInputDisabled || !input.trim()}
          className="px-4 py-2 font-bold rounded text-xs transition-colors hover:bg-[#e83010] disabled:hover:bg-[#333]"
          style={{ background: isInputDisabled ? "#333" : "#cc2200", color: isInputDisabled ? "#888" : "#fff", fontFamily: "Share Tech Mono" }}
        >
          SEND
        </button>
      </form>
    </div>
  );
}
