package org.example.murderhelp.domain.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.murderhelp.global.entity.BaseTimeEntity;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.example.murderhelp.domain.member.entity.Member;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_rooms")
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Member customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Member admin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomStatus status;

    @Builder
    public ChatRoom(String title, Member customer) {
        this.title = title;
        this.customer = customer;
        this.status = ChatRoomStatus.BOT_MODE; // 기본값: 챗봇 모드
    }

    public void changeToWaiting() {
        this.status = ChatRoomStatus.WAITING;
    }

    public void assignAdmin(Member admin) {
        this.admin = admin;
        this.status = ChatRoomStatus.IN_PROGRESS;
    }

    public void closeRoom() {
        if (!this.status.canClose()) {
            throw new BusinessException(ErrorCode.INVALID_CHAT_ROOM_STATUS);
        }
        this.status = ChatRoomStatus.COMPLETED;
    }

    public boolean isCustomer(Long memberId) {
        return this.customer.getId().equals(memberId);
    }
}
