import { useEffect, useRef, useState } from "react";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import ChatRoomView from "./ChatRoomView";
import type {ChatRoomResponse, PageResponse} from "./chat.types";
import { getAccessToken } from "@/api/client.ts";

type TabType = "ALL" | "WAITING" | "IN_PROGRESS" | "COMPLETED";

export default function AdminChatDashboard({ adminId }: { adminId: number }) {
  const [rooms, setRooms] = useState<ChatRoomResponse[]>([]);
  const [selectedRoomId, setSelectedRoomId] = useState<number | null>(null);
  const [activeTab, setActiveTab] = useState<TabType>("ALL");
  const [searchInput, setSearchInput] = useState("");
  const [searchKeyword, setSearchKeyword] = useState("");
  const stompClient = useRef<Client | null>(null);
  const activeTabRef = useRef<TabType>(activeTab);
  const selectedRoomIdRef = useRef<number | null>(selectedRoomId);

  const [isError, setIsError] = useState(false);
  const [waitingCount, setWaitingCount] = useState(0);
  const [inProgressCount, setInProgressCount] = useState(0);
  const [isLoading, setIsLoading] = useState(false);

  // 현재 탭 상태와 선택된 방 상태를 Ref에 동기화 (웹소켓 클로저에서 최신 상태 참조)
  useEffect(() => {
    activeTabRef.current = activeTab;
  }, [activeTab]);

  useEffect(() => {
    selectedRoomIdRef.current = selectedRoomId;
  }, [selectedRoomId]);

  // 🔹 별도로 WAITING과 IN_PROGRESS 개수를 병렬로 가져오는 함수 (size=1)
  const fetchTabCounts = async () => {
    try {
      const headers = { Authorization: `Bearer ${getAccessToken()}` };
      const [waitingRes, inProgressRes] = await Promise.all([
        fetch(`/api/chat/rooms?status=WAITING&size=1`, { headers }),
        fetch(`/api/chat/rooms?status=IN_PROGRESS&size=1`, { headers })
      ]);
      
      if (waitingRes.ok) {
        const json = await waitingRes.json();
        const pageData = (json.data ? json.data : json) as PageResponse<ChatRoomResponse>;
        setWaitingCount(pageData.totalElements || 0);
      }
      
      if (inProgressRes.ok) {
        const json = await inProgressRes.json();
        const pageData = (json.data ? json.data : json) as PageResponse<ChatRoomResponse>;
        setInProgressCount(pageData.totalElements || 0);
      }
    } catch (e) {
      console.error(e);
    }
  };

  const fetchRooms = async () => {
    setIsLoading(true);
    setIsError(false);
    try {
      const query = new URLSearchParams();
      if (activeTab !== "ALL") query.append("status", activeTab);
      if (searchKeyword.trim() !== "") query.append("keyword", searchKeyword);
      
      const res = await fetch(`/api/chat/rooms?${query.toString()}&size=100`, {
        headers: { Authorization: `Bearer ${getAccessToken()}` }
      });
      if (!res.ok) throw new Error("방 목록 조회 실패");
      const json = await res.json();
      const pageData = (json.data ? json.data : json) as PageResponse<ChatRoomResponse>;
      setRooms(pageData.content);
      
      // 꼼수 로직 제거: 탭 이동 시 무조건 서버에서 정확한 대기/진행중 개수를 가져옴
      fetchTabCounts();
    } catch (err) {
      console.warn("방 목록 조회 에러:", err);
      setIsError(true);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchRooms();
  }, [activeTab, searchKeyword]);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS("/ws"),
      connectHeaders: {
        Authorization: `Bearer ${getAccessToken()}`
      },
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe("/sub/chat/rooms/updates", (msg) => {
          const updatedRoom = JSON.parse(msg.body) as ChatRoomResponse;
          
          setRooms(prev => {
            const currentTab = activeTabRef.current;
            const currentRoomId = selectedRoomIdRef.current;
            
            // ✨ 핵심: 내가 현재 열어둔 방의 상태가 변했고, 그 상태가 현재 탭과 다를 때
            if (currentRoomId === updatedRoom.roomId && currentTab !== "ALL" && updatedRoom.status !== currentTab) {
              // 탭을 새로운 상태로 강제 전환 (이후 fetchRooms가 돌아가서 목록이 갱신됨)
              setActiveTab(updatedRoom.status as TabType);
              return prev; // 탭이 바뀌면서 리스트 전체를 다시 불러오므로 여기선 가만히 둠
            }

            // 내가 열어둔 방이 아니라면 기존처럼 조용히 목록 갱신
            if (currentTab !== "ALL" && updatedRoom.status !== currentTab) {
              return prev.filter(r => r.roomId !== updatedRoom.roomId);
            }

            const exists = prev.find(r => r.roomId === updatedRoom.roomId);
            if (exists) {
              return prev.map(r => r.roomId === updatedRoom.roomId ? updatedRoom : r);
            } else {
              return [updatedRoom, ...prev];
            }
          });

          // WebSocket으로 상태 변경 알림이 오면 묻지도 따지지도 않고 전체 카운트 단건 갱신
          fetchTabCounts();
        });
      }
    });

    client.activate();
    stompClient.current = client;

    return () => {
      client.deactivate();
    };
  }, []);

  return (
    // 전체 배경 컨테이너 (부드러운 차콜 다크: Zinc-950)
    <div className="flex flex-col h-[85vh] m-6 bg-[#0e0e11] border border-[#27272a] rounded-2xl overflow-hidden text-[#f4f4f5] shadow-[0_8px_30px_rgba(0,0,0,0.5)] font-sans tracking-tight">
      
      {/* 상단 통합 헤더 (Top Header) - Zinc-900 */}
      <div className="flex justify-between items-center px-8 py-5 bg-[#18181b] border-b border-[#27272a]">
        <div className="flex items-center gap-3">
          <h2 className="text-xl font-semibold text-[#f4f4f5] tracking-tight">Chat Management</h2>
        </div>
        <div className="flex items-center gap-6 text-sm text-[#a1a1aa]">
           <span>Admin ID: {adminId}</span>
           <div className="flex items-center gap-2 px-3 py-1 bg-[#27272a] border border-[#3f3f46] rounded-full shadow-sm">
             <div className="w-1.5 h-1.5 rounded-full bg-emerald-400"></div>
             <span className="text-[#e4e4e7] font-medium text-xs">System Online</span>
           </div>
        </div>
      </div>

      {/* 메인 콘텐츠 영역 */}
      <div className="flex flex-col lg:flex-row flex-1 gap-4 lg:gap-6 p-4 lg:p-6 bg-[#0e0e11] overflow-hidden">
        
        {/* 왼쪽 (상단) 카드: 채팅방 리스트 */}
        <div className="min-w-[280px] h-1/2 lg:h-auto lg:w-1/3 shrink-0 flex flex-col bg-[#18181b] border border-[#27272a] rounded-xl shadow-lg overflow-hidden">
          
          {/* 탭 영역 */}
          <div className="p-3 lg:p-4 border-b border-[#27272a]">
            <div 
              className="flex overflow-x-auto bg-[#09090b] rounded-lg p-1 border border-[#27272a]/50" 
              style={{ msOverflowStyle: 'none', scrollbarWidth: 'none' }}
            >
              {(["ALL", "WAITING", "IN_PROGRESS", "COMPLETED"] as TabType[]).map(tab => {
                const isActive = activeTab === tab;
                return (
                  <button 
                    key={tab} 
                    onClick={() => setActiveTab(tab)}
                    className={`flex-1 shrink-0 whitespace-nowrap py-1.5 px-3 text-xs font-medium text-center rounded-md transition-all duration-200 ${
                      isActive ? 'bg-[#3f3f46] text-[#f4f4f5] shadow-sm' : 'text-[#a1a1aa] hover:text-[#d4d4d8]'
                    }`}
                  >
                    <span className="relative inline-block pr-1">
                      {tab.replace('_', ' ')}
                      {tab === "WAITING" && waitingCount > 0 && (
                        <span className={`absolute -top-1.5 -right-2 px-1 py-0 rounded-full text-[8px] font-bold shadow-sm ${
                          isActive ? "bg-[#f4f4f5] text-[#09090b]" : "bg-[#3f3f46] text-[#d4d4d8]"
                        }`} style={{ lineHeight: '1.2' }}>
                          {waitingCount}
                        </span>
                      )}
                      {tab === "IN_PROGRESS" && inProgressCount > 0 && (
                        <span className={`absolute -top-1.5 -right-2 px-1 py-0 rounded-full text-[8px] font-bold shadow-sm ${
                          isActive ? "bg-[#f4f4f5] text-[#09090b]" : "bg-[#3f3f46] text-[#d4d4d8]"
                        }`} style={{ lineHeight: '1.2' }}>
                          {inProgressCount}
                        </span>
                      )}
                    </span>
                  </button>
                )
              })}
            </div>
          </div>

          {/* 검색바 영역 */}
          <div className="p-4 border-b border-[#27272a]">
            <div className="relative">
              <input 
                type="text" 
                placeholder="이름, 이메일, 방 제목 검색 (Enter)..." 
                value={searchInput}
                onChange={e => setSearchInput(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && setSearchKeyword(searchInput)}
                className="w-full bg-[#0e0e11] text-[#f4f4f5] text-sm py-2.5 px-9 rounded-lg border border-[#27272a] focus:border-[#52525b] focus:outline-none transition-all placeholder-[#71717a] shadow-inner"
              />
              <div className="absolute left-3 top-3 text-[#71717a]">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
              </div>
            </div>
          </div>

          {/* 리스트 영역 */}
          <div className="flex-1 overflow-y-scroll custom-scrollbar p-3 space-y-1">
            {isError && (
              <div className="p-4 text-center text-red-400 text-sm bg-red-950/20 rounded-lg border border-red-900/30">
                Failed to load chat history. System offline.
              </div>
            )}
            {rooms.map(room => {
              const isSelected = selectedRoomId === room.roomId;
              
              let badgeStyle = 'bg-[#27272a] text-[#a1a1aa] border-[#3f3f46]';
              if (room.status === 'WAITING') badgeStyle = 'bg-[#f4f4f5] text-[#18181b] border-[#f4f4f5] shadow-sm';
              else if (room.status === 'COMPLETED') badgeStyle = 'bg-transparent text-[#71717a] border-[#27272a]';

              // 아바타 이니셜 추출 (예: REQ-CUST... -> C)
              const initial = room.title.replace('REQ-CUST', '').charAt(3).toUpperCase() || '?';

              return (
                <div 
                  key={room.roomId}
                  onClick={() => setSelectedRoomId(room.roomId)}
                  className={`p-3 rounded-lg cursor-pointer transition-all duration-150 border flex gap-3 items-center ${
                    isSelected 
                      ? 'bg-[#27272a] border-[#3f3f46] shadow-sm' 
                      : 'bg-transparent border-transparent hover:bg-[#27272a]/50'
                  }`}
                >
                  {/* 아바타 (Avatar Icon) */}
                  <div className="w-10 h-10 flex-shrink-0 rounded-full bg-[#3f3f46] flex items-center justify-center text-sm font-bold text-white shadow-inner">
                    {initial !== '?' ? initial : '👤'}
                  </div>
                  
                  {/* 메시지 정보 영역 */}
                  <div className="flex-1 min-w-0">
                    <div className="flex justify-between items-center mb-0.5">
                      <span className={`font-semibold text-sm truncate pr-2 ${isSelected ? 'text-[#f4f4f5]' : 'text-[#d4d4d8]'}`}>
                        {room.title}
                      </span>
                      <span className={`text-[9px] px-1.5 py-0.5 rounded border font-medium whitespace-nowrap flex-shrink-0 ${badgeStyle}`}>
                        {room.status.replace('_', ' ')}
                      </span>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className={`text-xs truncate ${isSelected ? 'text-[#a1a1aa]' : 'text-[#71717a]'}`}>
                        {room.lastMessage ? room.lastMessage : '새로운 대화가 없습니다.'}
                      </span>
                      <span className="text-[9px] text-[#52525b] whitespace-nowrap pl-2">
                        {new Date(room.createdAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}
                      </span>
                    </div>
                  </div>
                </div>
              );
            })}
            {rooms.length === 0 && !isError && (
              <div className="p-8 mt-10 flex flex-col items-center justify-center text-[#71717a] text-sm">
                <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" className="mb-3"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>
                No previous chats found.
              </div>
            )}
          </div>
        </div>

        {/* 오른쪽 카드: 채팅 화면 */}
        <div className="flex-1 flex flex-col bg-[#18181b] border border-[#27272a] rounded-xl shadow-lg overflow-hidden">
          {selectedRoomId ? (
            (() => {
              const selectedRoom = rooms.find(r => r.roomId === selectedRoomId);
              const isCompleted = selectedRoom?.status === 'COMPLETED';
              return (
                <>
                  <div className="px-8 py-5 border-b border-[#27272a] flex justify-between items-center bg-[#18181b] z-10">
                    <div className="flex items-center gap-3">
                      <h3 className={`font-semibold text-lg tracking-tight ${isCompleted ? 'text-[#71717a]' : 'text-[#f4f4f5]'}`}>
                        Support Channel #{selectedRoomId}
                      </h3>
                    </div>
                    {!isCompleted && (
                      <button 
                        onClick={() => {
                          fetch(`/api/chat/rooms/${selectedRoomId}/close`, { 
                            method: "PATCH",
                            headers: {
                              Authorization: `Bearer ${getAccessToken()}`
                            }
                          })
                            .then(res => {
                              if (!res.ok) throw new Error("채널 닫기 실패");
                            })
                            .catch(err => alert(err.message));
                        }}
                        className="px-4 py-1.5 bg-[#27272a] hover:bg-[#3f3f46] text-[#e4e4e7] border border-[#3f3f46] rounded-md text-xs font-medium transition-colors shadow-sm"
                      >
                        Close Channel
                      </button>
                    )}
                  </div>
                  <div className="flex-1 relative overflow-hidden bg-[#0e0e11]">
                    <ChatRoomView key={selectedRoomId} roomId={selectedRoomId} customerId={adminId} isAdmin={true} />
                  </div>
                </>
              );
            })()
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center text-[#52525b]">
              <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round" className="mb-4">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
              </svg>
              <p className="text-sm font-medium">Select a channel to view</p>
            </div>
          )}
        </div>

      </div>
    </div>
  );
}
