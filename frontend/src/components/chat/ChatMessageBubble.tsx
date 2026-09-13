import type { ChatMessageResponse, BotMessageDto } from "./chat.types";

interface ChatMessageBubbleProps {
  message: ChatMessageResponse;
  customerId: number;
  isAdmin: boolean;
  isCompleted: boolean;
  isLatest?: boolean;
  onSendBotOption: (label: string) => void;
}

export function ChatMessageBubble({ message: m, customerId, isAdmin, isCompleted, isLatest = false, onSendBotOption }: ChatMessageBubbleProps) {
  const isMe = m.memberId === customerId;
  const isBot = m.senderEmail === "bot@system.com";
  const isOtherAdmin = !isMe && !isBot && !isAdmin;
  const isOtherCustomer = !isMe && !isBot && isAdmin;
  
  const isSenderAdmin = isAdmin ? (m.memberId === customerId) : (!isBot && m.memberId !== customerId);
  const isSenderCustomer = isAdmin ? (!isBot && m.memberId !== customerId) : (m.memberId === customerId);

  let botData: BotMessageDto | null = null;
  if (m.messageType === "BUTTON") {
    try {
      botData = JSON.parse(m.content) as BotMessageDto;
    } catch (e) {}
  }

  let bgColor = "rgba(255,255,255,0.05)";
  let borderColor = "1px solid rgba(255,255,255,0.1)";
  let textColor = "#f0e0d8";

  if (isBot) {
    bgColor = "rgba(15,0,0,0.85)";
    borderColor = "1px solid rgba(204,34,0,0.4)";
    textColor = "#f0e0d8";
  } else if (isSenderAdmin) {
    bgColor = "#064e3b";
    borderColor = "1px solid #047857";
    textColor = "#ecfdf5";
  } else if (isSenderCustomer) {
    bgColor = "#8b1a08";
    borderColor = "1px solid #7f1d1d";
    textColor = "#fef2f2";
  }

  const handleOptionClick = (label: string) => {
    if (isAdmin) {
      alert("관리자는 챗봇을 조작할 수 없습니다.");
      return;
    }
    if (!isCompleted) {
      onSendBotOption(label);
    }
  };

  return (
    <div className={`flex ${isMe ? "justify-end" : "justify-start"}`}>
      <div 
        className="max-w-[85%] break-keep rounded px-3 py-2 text-sm shadow-md"
        style={{ 
          background: bgColor, 
          color: textColor,
          border: borderColor,
          borderBottomRightRadius: isMe ? 0 : "0.375rem",
          borderBottomLeftRadius: !isMe ? 0 : "0.375rem"
        }}
      >
        {isOtherAdmin && <div className="text-[10px] mb-1 font-bold" style={{ color: "#10b981", fontFamily: "Share Tech Mono" }}>CODE GREEN</div>}
        {isOtherCustomer && <div className="text-[10px] mb-1 font-bold" style={{ color: "#a08070", fontFamily: "Share Tech Mono" }}>CUSTOMER</div>}
        {isBot && <div className="text-[10px] mb-1 font-bold" style={{ color: "#ff4422", fontFamily: "Share Tech Mono" }}>[ SYSTEM BOT ]</div>}
        
        {botData ? (
          <>
            <div className="whitespace-pre-wrap leading-relaxed">{botData.text}</div>
            
            {botData.products && botData.products.length > 0 && (
              <div className="mt-3 flex flex-col gap-2">
                {botData.products.map((p, pIdx) => (
                  <div key={pIdx} className="text-xs p-2 rounded flex justify-between" style={{ background: "rgba(0,0,0,0.5)", border: "1px solid rgba(255,68,34,0.2)" }}>
                    <span style={{ color: "#f0e0d8" }}>{p.name}</span>
                    <span style={{ color: "#ff6644", fontFamily: "Share Tech Mono" }}>${p.price}</span>
                  </div>
                ))}
              </div>
            )}
            
            {botData.options && botData.options.length > 0 && (
              <div className="mt-3 flex flex-col gap-2">
                {botData.options.map((opt, oIdx) => {
                  const isOptionDisabled = isAdmin || !isLatest;
                  return (
                    <button
                      key={oIdx}
                      disabled={isOptionDisabled}
                      onClick={() => handleOptionClick(opt.label)}
                      className={`text-xs py-1.5 px-3 rounded border text-center transition-colors ${isOptionDisabled ? "opacity-50 cursor-not-allowed" : "hover:bg-[#cc2200] hover:text-white"}`}
                      style={{ borderColor: "rgba(204,34,0,0.7)", color: "#ff6644", background: "rgba(0,0,0,0.5)" }}
                    >
                      {opt.label}
                    </button>
                  );
                })}
              </div>
            )}
          </>
        ) : (
          <div className="whitespace-pre-wrap leading-relaxed">{m.content}</div>
        )}
        
        <div className="text-[9px] text-right mt-1" style={{ color: isMe ? "rgba(255,255,255,0.7)" : "#a08070" }}>
          {new Date(m.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
        </div>
      </div>
    </div>
  );
}
