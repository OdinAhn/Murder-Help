import { useState } from "react";
import ChatModal from "./ChatModal";
import AdminChatDashboard from "./AdminChatDashboard";

export function FloatingChatWidget({ customerId, isAdmin }: { customerId: number; isAdmin?: boolean }) {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <>
      <div className="fixed bottom-8 right-8 z-50">
        <button
          onClick={() => setIsOpen(!isOpen)}
          className={`w-16 h-16 rounded-full flex items-center justify-center transition-all hover:scale-105 ${isAdmin ? 'shadow-[0_0_15px_rgba(16,185,129,0.5)]' : 'shadow-[0_0_15px_rgba(204,34,0,0.5)]'}`}
          style={{ background: isAdmin ? "#065f46" : "#cc2200", border: `2px solid ${isAdmin ? '#10b981' : '#ff4422'}` }}
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
          </svg>
        </button>
      </div>

      {isOpen && (
        isAdmin ? (
          <div className="fixed inset-0 z-[100] bg-black/80 flex items-center justify-center p-4">
            <div className="w-full max-w-6xl relative">
              <button 
                onClick={() => setIsOpen(false)}
                className="absolute -top-4 -right-4 w-10 h-10 bg-[#222] border border-[#444] text-white rounded-full flex items-center justify-center z-50 hover:bg-[#333]"
              >
                ✕
              </button>
              <AdminChatDashboard />
            </div>
          </div>
        ) : (
          <ChatModal customerId={customerId} onClose={() => setIsOpen(false)} />
        )
      )}
    </>
  );
}
