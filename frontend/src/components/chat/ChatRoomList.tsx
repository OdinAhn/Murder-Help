import { useEffect, useState } from "react";
import type { ChatRoomResponse } from "./chat.types";

export default function ChatRoomList({ customerId, onSelectRoom, preventAutoJoin }: { customerId: number; onSelectRoom: (id: number) => void; preventAutoJoin?: boolean }) {
  const [rooms, setRooms] = useState<ChatRoomResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch(`/api/chat/rooms?customerId=${customerId}&page=0&size=50`)
      .then(res => res.json())
      .then(json => {
        if (json.data && json.data.content) {
          const list = json.data.content as ChatRoomResponse[];
          setRooms(list);
          
          // 요구사항: 활성 방(WAITING, IN_PROGRESS)이 있으면 리스트 건너뛰고 바로 진입!
          // 단, 뒤로가기를 누른 상태(preventAutoJoin)면 진입하지 않음
          const activeRoom = list.find(r => r.status !== "COMPLETED");
          if (activeRoom && !preventAutoJoin) {
            onSelectRoom(activeRoom.roomId);
          }
        }
      })
      .catch(err => console.error("채팅방 조회 실패", err))
      .finally(() => setLoading(false));
  }, [customerId, onSelectRoom, preventAutoJoin]);

  const handleStartNew = async () => {
    try {
      const res = await fetch(`/api/chat/rooms`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ customerId })
      });
      
      const json = await res.json();
      
      // 백엔드에서 에러를 던진 경우 (진행 중인 상담 있음)
      if (!res.ok || (json.code && json.code === "CHAT_003")) {
        alert(json.message || "이미 진행 중인 상담이 존재합니다.");
        return;
      }

      if (json.data && json.data.roomId) {
        onSelectRoom(json.data.roomId);
      }
    } catch (error) {
      console.error("방 생성 실패", error);
      alert("방 생성 중 오류가 발생했습니다.");
    }
  };

  if (loading) return <div className="p-4 text-[#a08070] text-sm text-center mt-10" style={{ fontFamily: "Share Tech Mono" }}>Decrypting signals...</div>;

  return (
    <div className="h-full flex flex-col p-4">
      {/* 텅 빈 상태일 때 혹은 새로운 문의 버튼 */}
      <button 
        onClick={handleStartNew}
        className="w-full py-3 mb-4 text-sm font-bold tracking-widest uppercase transition-colors hover:bg-[#e83010]"
        style={{ background: "#cc2200", color: "#fff", border: "1px solid #ff4422", fontFamily: "Share Tech Mono, monospace" }}
      >
        [ 새로운 문의 시작하기 ]
      </button>
      
      <div className="flex-1 overflow-y-auto custom-scrollbar">
        <h4 className="text-[#a08070] text-xs mb-3 uppercase tracking-widest font-bold">Past Communications</h4>
        
        {rooms.length === 0 ? (
          <div className="text-center mt-20 opacity-50">
            <svg className="w-12 h-12 mx-auto mb-3 text-[#cc2200]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"></path></svg>
            <p className="text-[#f0e0d8] text-xs font-mono">통신 기록이 존재하지 않습니다.</p>
          </div>
        ) : (
          <ul className="space-y-2">
            {rooms.map(room => (
              <li 
                key={room.roomId} 
                onClick={() => onSelectRoom(room.roomId)}
                className="p-3 cursor-pointer transition-colors hover:bg-[#1a0000]"
                style={{ border: "1px solid rgba(204,34,0,0.3)", borderLeftWidth: "4px", borderLeftColor: room.status === "COMPLETED" ? "#444" : "#cc2200" }}
              >
                <div className="flex justify-between items-center mb-1">
                  <span className="text-sm font-bold" style={{ color: "#f0e0d8" }}>{room.title}</span>
                  <span className="text-[10px] px-1.5 py-0.5 font-bold tracking-wider" style={{
                    background: room.status === "COMPLETED" ? "#333" : "#cc2200",
                    color: "#fff"
                  }}>
                    {room.status}
                  </span>
                </div>
                <div className="text-[10px]" style={{ color: "#a08070" }}>
                  {new Date(room.createdAt).toLocaleString()}
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
