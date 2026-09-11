import { useEffect, useRef, useState } from "react";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import ChatRoomView from "./ChatRoomView";
import type { ChatRoomResponse } from "./chat.types";

export default function AdminChatDashboard() {
  const [rooms, setRooms] = useState<ChatRoomResponse[]>([]);
  const [selectedRoomId, setSelectedRoomId] = useState<number | null>(null);
  const ADMIN_ID = 999999; // 덤프 관리자 ID
  const stompClient = useRef<Client | null>(null);

  useEffect(() => {
    // 1. 최초 1회만 REST로 기존 채팅방 목록 조회
    fetch(`/api/chat/rooms?page=0&size=100`)
      .then(res => res.json())
      .then(json => {
        if (json.data && json.data.content) {
          setRooms(json.data.content);
        }
      });

    // 2. STOMP 구독으로 새 채팅방/상태 변경을 실시간 수신
    const client = new Client({
      webSocketFactory: () => new SockJS("/ws"),
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe("/sub/chat/rooms/updates", (msg) => {
          const updatedRoom = JSON.parse(msg.body) as ChatRoomResponse;
          setRooms(prev => {
            const exists = prev.find(r => r.roomId === updatedRoom.roomId);
            if (exists) {
              // 기존 방 상태 업데이트 (예: COMPLETED)
              return prev.map(r => r.roomId === updatedRoom.roomId ? updatedRoom : r);
            } else {
              // 새 방 추가
              return [updatedRoom, ...prev];
            }
          });
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
    <div className="flex h-[80vh] border border-[#333] m-8 rounded-lg overflow-hidden bg-black text-white shadow-2xl">
      {/* 왼쪽: 채팅방 리스트 */}
      <div className="w-1/3 border-r border-[#333] flex flex-col bg-[#050505]">
        <div className="p-4 border-b border-[#333] bg-[#111]">
          <h2 className="text-lg font-bold text-[#10b981] font-mono tracking-widest">HQ COMMS</h2>
          <p className="text-xs text-gray-500 mt-1">Total Active Signals: {rooms.filter(r => r.status !== 'COMPLETED').length}</p>
        </div>
        <div className="flex-1 overflow-y-auto custom-scrollbar">
          {rooms.map(room => (
            <div 
              key={room.roomId}
              onClick={() => setSelectedRoomId(room.roomId)}
              className={`p-4 border-b border-[#222] cursor-pointer hover:bg-[#1a1a1a] transition-all ${selectedRoomId === room.roomId ? 'bg-[#1a1a1a] border-l-4 border-l-[#10b981]' : ''}`}
            >
              <div className="flex justify-between items-center mb-1">
                <span className="font-bold text-sm text-gray-200">{room.title}</span>
                <span className={`text-[10px] px-1.5 py-0.5 rounded ${room.status === 'COMPLETED' ? 'bg-gray-800 text-gray-400' : 'bg-[#10b981]/20 text-[#10b981]'}`}>
                  {room.status}
                </span>
              </div>
              <div className="text-xs text-gray-500 truncate">
                {new Date(room.createdAt).toLocaleString()}
              </div>
            </div>
          ))}
          {rooms.length === 0 && (
            <div className="p-8 text-center text-gray-500 text-sm">
              No communications intercepted.
            </div>
          )}
        </div>
      </div>

      {/* 오른쪽: 채팅 화면 */}
      <div className="flex-1 bg-[#0a0a0a] flex flex-col relative">
        {selectedRoomId ? (
          <>
            <div className="p-4 border-b border-[#333] flex justify-between items-center bg-[#111] z-10">
              <h3 className="font-bold text-[#10b981] font-mono">SECURE CHANNEL #{selectedRoomId}</h3>
              <button 
                onClick={() => {
                  fetch(`/api/chat/rooms/${selectedRoomId}/close`, { method: "PATCH" });
                  setSelectedRoomId(null);
                }}
                className="text-xs bg-[#222] px-3 py-1.5 rounded hover:bg-[#333] transition-colors"
              >
                Close Channel
              </button>
            </div>
            {/* ChatRoomView 재사용 (adminId 전달) */}
            <div className="flex-1 relative overflow-hidden">
              <ChatRoomView roomId={selectedRoomId} customerId={ADMIN_ID} />
            </div>
          </>
        ) : (
          <div className="flex-1 flex flex-col items-center justify-center text-gray-600 opacity-50">
            <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round" className="mb-4">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
            </svg>
            <p className="font-mono tracking-widest text-sm">SELECT A CHANNEL TO INTERCEPT</p>
          </div>
        )}
      </div>
    </div>
  );
}
