import { useEffect, useState, useRef, useLayoutEffect } from "react";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import type { ChatMessageResponse } from "./chat.types";

export default function ChatRoomView({ roomId, customerId }: { roomId: number; customerId: number }) {
  const [messages, setMessages] = useState<ChatMessageResponse[]>([]);
  const [page, setPage] = useState(0);
  const [isLast, setIsLast] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [input, setInput] = useState("");
  const [isCompleted, setIsCompleted] = useState(false);
  
  const stompClient = useRef<Client | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const previousScrollHeight = useRef<number>(0);
  const isFetchingHistory = useRef(false);

  useEffect(() => {
    // 1. 방 상태 확인 (COMPLETED 인지 체크)
    fetch(`/api/chat/rooms/${roomId}`)
      .then(res => res.json())
      .then(json => {
        if (json.data && json.data.status === "COMPLETED") setIsCompleted(true);
      });

    // 2. 초기 데이터(page=0) 로드
    loadMoreMessages(0, true);

    // 3. STOMP 소켓 연결
    const client = new Client({
      webSocketFactory: () => new SockJS("/ws"),
      debug: (str) => console.log(str),
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/sub/chat/room/${roomId}`, (msg) => {
          const newMsg = JSON.parse(msg.body) as ChatMessageResponse;
          setMessages(prev => [...prev, newMsg]);
          // 새 메시지가 오면 즉각(순간이동) 맨 아래로 스크롤 (카카오톡 스타일)
          setTimeout(() => {
             if (containerRef.current) {
                containerRef.current.scrollTo({
                  top: containerRef.current.scrollHeight,
                  behavior: "auto"
                });
             }
          }, 50);
        });
      }
    });

    client.activate();
    stompClient.current = client;

    return () => {
      client.deactivate();
    };
  }, [roomId]);

  const loadMoreMessages = async (pageToLoad: number, isInitial = false) => {
    if (isFetchingHistory.current) return;
    isFetchingHistory.current = true;
    setIsLoading(true);

    try {
      const res = await fetch(`/api/chat/rooms/${roomId}/messages?page=${pageToLoad}&size=20`);
      const json = await res.json();
      
      if (json.data && json.data.content) {
        // 최신순으로 넘어오므로 뒤집어서 과거->최신으로 배열
        const newMsgs = [...json.data.content].reverse(); 
        
        // 과거 데이터를 끼워넣기 전의 높이를 기억 (스크롤 보정용)
        if (!isInitial && containerRef.current) {
          previousScrollHeight.current = containerRef.current.scrollHeight;
        }

        setMessages(prev => isInitial ? newMsgs : [...newMsgs, ...prev]);
        setIsLast(json.data.last);
        setPage(pageToLoad);
      }
    } finally {
      setIsLoading(false);
      isFetchingHistory.current = false;
    }
  };

  const isInitialMount = useRef(true);

  // 과거 메시지 로드 후 스크롤 튀는 현상 방지 및 초기 로딩 시 순간이동
  useLayoutEffect(() => {
    if (containerRef.current) {
      if (isInitialMount.current && messages.length > 0) {
        // 첫 로딩 시: 브라우저가 화면을 그리기 전에 즉시 맨 아래로 스크롤 설정 (깜빡임/내려가는 액션 방지)
        containerRef.current.scrollTop = containerRef.current.scrollHeight;
        isInitialMount.current = false;
      } else if (page > 0 && previousScrollHeight.current > 0) {
        // 과거 로딩 시: 늘어난 높이만큼 스크롤을 아래로 밀어줘서 화면이 그대로 있게 만든다.
        const currentScrollHeight = containerRef.current.scrollHeight;
        containerRef.current.scrollTop = currentScrollHeight - previousScrollHeight.current;
        previousScrollHeight.current = 0; // 보정 후 초기화
      }
    }
  }, [messages, page]);

  const [showScrollBottom, setShowScrollBottom] = useState(false);

  // 스크롤 위치 감지 (맨 위면 과거 로딩, 맨 밑이 아니면 '아래로' 버튼 표시)
  const handleScroll = () => {
    if (containerRef.current) {
      const { scrollTop, scrollHeight, clientHeight } = containerRef.current;
      
      // 스크롤이 맨 바닥에서 100px 이상 위로 올라가 있으면 버튼 표시
      setShowScrollBottom(scrollHeight - scrollTop - clientHeight > 100);

      // 맨 위(0)에 닿으면 과거 데이터 로딩
      if (scrollTop === 0 && !isLast && !isLoading) {
        loadMoreMessages(page + 1);
      }
    }
  };

  const scrollToBottom = () => {
    if (containerRef.current) {
      containerRef.current.scrollTo({
        top: containerRef.current.scrollHeight,
        behavior: "smooth"
      });
    }
  };

  const send = (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim() || isCompleted || !stompClient.current?.connected) return;

    stompClient.current.publish({
      destination: "/pub/chat.send",
      body: JSON.stringify({
        roomId,
        memberId: customerId,
        content: input
      })
    });
    setInput("");
  };

  return (
    <div className="flex flex-col h-full bg-[#060606] relative">
      {/* 메시지 리스트 영역 */}
      <div 
        ref={containerRef}
        onScroll={handleScroll}
        className="flex-1 overflow-y-auto p-4 space-y-4 custom-scrollbar"
      >
        {isLoading && page > 0 && (
          <div className="text-center py-2 text-[#a08070] text-xs font-mono">
            Loading past communications...
          </div>
        )}

        {messages.map((m, idx) => {
          const isMe = m.memberId === customerId;
          return (
            <div key={idx} className={`flex ${isMe ? "justify-end" : "justify-start"}`}>
              <div 
                className="max-w-[80%] rounded px-3 py-2 text-sm shadow-md"
                style={{ 
                  background: isMe ? "#8b1a08" : "rgba(255,255,255,0.05)", 
                  color: "#f0e0d8",
                  border: isMe ? "none" : "1px solid rgba(255,255,255,0.1)",
                  borderBottomRightRadius: isMe ? 0 : "0.375rem",
                  borderBottomLeftRadius: !isMe ? 0 : "0.375rem"
                }}
              >
                {!isMe && <div className="text-[10px] mb-1 font-bold" style={{ color: "#cc2200", fontFamily: "Share Tech Mono" }}>AGENT / HQ</div>}
                <div className="whitespace-pre-wrap leading-relaxed">{m.content}</div>
                <div className="text-[9px] text-right mt-1" style={{ color: isMe ? "#f0e0d8" : "#a08070", opacity: 0.7 }}>
                  {new Date(m.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* 최신 메시지로 돌아가기 버튼 */}
      {/* 최신 메시지로 돌아가기 버튼 (채팅 가림 최소화) */}
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

      {/* 입력창 */}
      <form onSubmit={send} className="p-3 border-t flex gap-2" style={{ borderColor: "rgba(204,34,0,0.3)", background: "#0a0000" }}>
        <input 
          type="text" 
          value={input}
          onChange={(e) => setInput(e.target.value)}
          disabled={isCompleted}
          placeholder={isCompleted ? "상담이 완전히 종료되었습니다." : "메시지 전송..."}
          className="flex-1 bg-black border rounded px-3 py-2 text-sm text-white focus:outline-none focus:border-[#cc2200] transition-colors"
          style={{ borderColor: "rgba(204,34,0,0.25)", opacity: isCompleted ? 0.5 : 1 }}
        />
        <button 
          type="submit" 
          disabled={isCompleted || !input.trim()}
          className="px-4 py-2 font-bold rounded text-xs transition-colors hover:bg-[#e83010] disabled:hover:bg-[#333]"
          style={{ background: isCompleted ? "#333" : "#cc2200", color: isCompleted ? "#888" : "#fff", fontFamily: "Share Tech Mono" }}
        >
          SEND
        </button>
      </form>
    </div>
  );
}
