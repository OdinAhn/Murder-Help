export type ChatRoomStatus = "WAITING" | "IN_PROGRESS" | "COMPLETED";

export interface ChatRoomResponse {
  roomId: number;
  title: string;
  status: ChatRoomStatus;
  createdAt: string;
}

export interface ChatMessageResponse {
  roomId: number;
  memberId: number;
  content: string;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  empty: boolean;
  first: boolean;
  last: boolean;
  number: number;
  numberOfElements: number;
}
