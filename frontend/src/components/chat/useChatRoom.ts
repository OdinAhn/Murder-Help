import { useEffect, useRef, useState, useLayoutEffect } from "react";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import type { ChatMessageResponse } from "./chat.types";
import { getAccessToken } from "../../api/client";

export function useChatRoom(roomId: number) {
  const [messages, setMessages] = useState<ChatMessageResponse[]>([]);
  const [page, setPage] = useState(0);
  const [isLast, setIsLast] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isCompleted, setIsCompleted] = useState(false);
  const [status, setStatus] = useState<string>("BOT_MODE");
  const [isError, setIsError] = useState(false);
  const [showScrollBottom, setShowScrollBottom] = useState(false);

  const stompClient = useRef<Client | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const previousScrollHeight = useRef<number>(0);
  const isFetchingHistory = useRef(false);
  const isInitialMount = useRef(true);

  // 1. 초기 데이터 및 상태 조회, STOMP 연결
  useEffect(() => {
    fetch(`/api/chat/rooms/${roomId}`, {
      headers: { Authorization: `Bearer ${getAccessToken()}` }
    })
      .then(res => res.json())
      .then(json => {
        if (json.data) {
          setStatus(json.data.status);
          if (json.data.status === "COMPLETED") setIsCompleted(true);
        }
      })
      .catch(err => console.warn("방 상태 조회 실패:", err));

    loadMoreMessages(0, true);

    const client = new Client({
      webSocketFactory: () => new SockJS("/ws"),
      connectHeaders: {
        Authorization: `Bearer ${getAccessToken()}`
      },
      debug: (str) => console.log(str),
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/sub/chat/room/${roomId}`, (msg) => {
          const newMsg = JSON.parse(msg.body) as ChatMessageResponse;
          setMessages(prev => [...prev, newMsg]);
          
          if (newMsg.messageType === "SYSTEM" && newMsg.content.includes("[CLOSED]")) {
            setIsCompleted(true);
          }

          setTimeout(() => {
             if (containerRef.current) {
                containerRef.current.scrollTo({
                  top: containerRef.current.scrollHeight,
                  behavior: "auto"
                });
             }
          }, 50);
        });

        // 실시간 방 상태 업데이트 구독 (상담사 연결 등으로 상태가 변경될 때 즉각 반영)
        client.subscribe('/sub/chat/rooms/updates', (msg) => {
          try {
            const updatedRoom = JSON.parse(msg.body);
            if (updatedRoom.roomId === roomId) {
              setStatus(updatedRoom.status);
              if (updatedRoom.status === "COMPLETED") {
                setIsCompleted(true);
              }
            }
          } catch (e) {
            console.error(e);
          }
        });
      }
    });

    client.activate();
    stompClient.current = client;

    return () => { 
      client.deactivate(); 
    };
  }, [roomId]);

  // 2. 과거 메시지 로딩 함수
  const loadMoreMessages = async (pageToLoad: number, isInitial = false) => {
    if (isFetchingHistory.current) return;
    isFetchingHistory.current = true;
    setIsLoading(true);
    setIsError(false);

    try {
      const res = await fetch(`/api/chat/rooms/${roomId}/messages?page=${pageToLoad}&size=20`, {
        headers: { Authorization: `Bearer ${getAccessToken()}` }
      });
      if (!res.ok) throw new Error("메시지 내역 조회 실패");
      const json = await res.json();
      
      if (json.data && json.data.content) {
        const newMsgs = [...json.data.content].reverse(); 
        
        if (!isInitial && containerRef.current) {
          previousScrollHeight.current = containerRef.current.scrollHeight;
        }

        setMessages(prev => isInitial ? newMsgs : [...newMsgs, ...prev]);
        setIsLast(json.data.last);
        setPage(pageToLoad);
      }
    } catch (error) {
      console.warn("과거 메시지 로드 에러:", error);
      setIsError(true);
    } finally {
      setIsLoading(false);
      isFetchingHistory.current = false;
    }
  };

  // 3. 스크롤 위치 보정
  useLayoutEffect(() => {
    if (containerRef.current) {
      if (isInitialMount.current && messages.length > 0) {
        containerRef.current.scrollTop = containerRef.current.scrollHeight;
        isInitialMount.current = false;
      } else if (page > 0 && previousScrollHeight.current > 0) {
        const currentScrollHeight = containerRef.current.scrollHeight;
        containerRef.current.scrollTop = currentScrollHeight - previousScrollHeight.current;
        previousScrollHeight.current = 0;
      }
    }
  }, [messages, page]);

  // 4. 스크롤 이벤트 핸들러
  const handleScroll = () => {
    if (containerRef.current) {
      const { scrollTop, scrollHeight, clientHeight } = containerRef.current;
      setShowScrollBottom(scrollHeight - scrollTop - clientHeight > 100);

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

  const sendMessage = (customerId: number, content: string) => {
    if (!content.trim() || isCompleted || !stompClient.current?.connected) return;
    stompClient.current.publish({
      destination: "/pub/chat.send",
      body: JSON.stringify({ roomId, memberId: customerId, content })
    });
  };

  return {
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
  };
}
