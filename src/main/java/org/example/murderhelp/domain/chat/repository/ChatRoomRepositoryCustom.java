package org.example.murderhelp.domain.chat.repository;

import org.example.murderhelp.domain.chat.entity.ChatRoom;
import org.example.murderhelp.domain.chat.entity.ChatRoomStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChatRoomRepositoryCustom {
    Page<ChatRoom> findRoomsByCondition(Long customerId, ChatRoomStatus status, Pageable pageable);
}
