package org.example.murderhelp.domain.chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.chat.dto.ChatMessageResponse;
import org.example.murderhelp.domain.chat.dto.ChatRoomCreateRequest;
import org.example.murderhelp.domain.chat.dto.ChatRoomResponse;
import org.example.murderhelp.domain.chat.entity.ChatRoomStatus;
import org.example.murderhelp.domain.chat.service.ChatMessageService;
import org.example.murderhelp.domain.chat.service.ChatRoomService;
import org.example.murderhelp.global.response.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms")
public class ChatRestController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    // 1단계: 채팅방 생성 (문의 시작)
    @PostMapping
    public ApiResponse<ChatRoomResponse> createRoom(@Valid @RequestBody ChatRoomCreateRequest request) {
        return ApiResponse.ok(chatRoomService.createRoom(request));
    }

    // 2단계: 전체/조건별 채팅방 목록 조회 (페이징 + 마지막 메시지 최신순 정렬)
    @GetMapping
    public ApiResponse<Page<ChatRoomResponse>> getRooms(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) ChatRoomStatus status,
            Pageable pageable) {
        return ApiResponse.ok(chatRoomService.getRooms(customerId, status, pageable));
    }

    // 특정 채팅방 단건 조회
    @GetMapping("/{roomId}")
    public ApiResponse<ChatRoomResponse> getRoom(@PathVariable Long roomId) {
        return ApiResponse.ok(chatRoomService.getRoom(roomId));
    }

    // 2단계: 과거 대화(메시지) 내역 조회 (페이징)
    @GetMapping("/{roomId}/messages")
    public ApiResponse<Page<ChatMessageResponse>> getMessageHistory(
            @PathVariable Long roomId, Pageable pageable) {
        return ApiResponse.ok(chatMessageService.getMessageHistory(roomId, pageable));
    }

    // 3단계: 상담 종료 처리
    @PatchMapping("/{roomId}/close")
    public ApiResponse<Void> closeRoom(@PathVariable Long roomId) {
        chatRoomService.closeRoom(roomId);
        return ApiResponse.ok();
    }
}
