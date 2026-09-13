package org.example.murderhelp.domain.chat.controller;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.chat.dto.ChatMessageResponse;
import org.example.murderhelp.domain.chat.dto.ChatRoomResponse;
import org.example.murderhelp.domain.chat.entity.ChatRoomStatus;
import org.example.murderhelp.domain.chat.service.ChatMessageService;
import org.example.murderhelp.domain.chat.service.ChatRoomService;
import org.example.murderhelp.global.response.ApiResponse;
import org.example.murderhelp.domain.chat.facade.ChatFacade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms")
public class ChatRestController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final ChatFacade chatFacade;

    // 1단계: 채팅방 생성 (문의 시작)
    @PostMapping
    public ApiResponse<ChatRoomResponse> createRoom(@AuthenticationPrincipal Long memberId) {
        return ApiResponse.ok(chatFacade.createRoomAndSendGreeting(memberId));
    }

    // 2-1단계: 고객용 내 채팅방 목록 조회 (페이징 + 마지막 메시지 최신순 정렬)
    @GetMapping("/my")
    public ApiResponse<Page<ChatRoomResponse>> getMyRooms(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) ChatRoomStatus status,
            Pageable pageable) {
        
        // 내 방만 조회
        return ApiResponse.ok(chatRoomService.getRooms(memberId, status, pageable));
    }

    // 2-2단계: 관리자용 전체 채팅방 목록 조회 (검색 조건 포함)
    @PreAuthorize("hasRole('GREEN')")
    @GetMapping
    public ApiResponse<Page<ChatRoomResponse>> getAllRooms(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) ChatRoomStatus status,
            Pageable pageable) {
        
        // 관리자는 파라미터가 없으면 전체 조회, 있으면 특정 조건 검색
        return ApiResponse.ok(chatRoomService.getRooms(customerId, status, pageable));
    }

    // 특정 채팅방 단건 조회
    @GetMapping("/{roomId}")
    public ApiResponse<ChatRoomResponse> getRoom(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long roomId) {
        return ApiResponse.ok(chatRoomService.getRoom(roomId));
    }

    // 2단계: 과거 대화(메시지) 내역 조회 (페이징)
    @GetMapping("/{roomId}/messages")
    public ApiResponse<Page<ChatMessageResponse>> getMessageHistory(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long roomId, Pageable pageable) {
        return ApiResponse.ok(chatMessageService.getMessageHistory(roomId, pageable));
    }

    // 3단계: 상담 종료 처리
    @PatchMapping("/{roomId}/close")
    public ApiResponse<Void> closeRoom(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long roomId) {
        chatFacade.closeRoom(roomId);
        return ApiResponse.ok();
    }
}
