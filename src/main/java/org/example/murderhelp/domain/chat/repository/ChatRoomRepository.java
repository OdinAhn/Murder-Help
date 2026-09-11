package org.example.murderhelp.domain.chat.repository;

import org.example.murderhelp.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import org.example.murderhelp.domain.chat.entity.ChatRoomStatus;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long>, ChatRoomRepositoryCustom {

    boolean existsByCustomerIdAndStatusIn(Long customerId, List<ChatRoomStatus> statuses);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChatRoom c SET c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :roomId")
    void updateLastMessageTime(@Param("roomId") Long roomId);
}
