package org.example.murderhelp.infra.portone.webhook;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    // 동일한 webhookId로 이미 처리된 웹훅이 있는지 확인 (멱등성 보장용)
    // PortOne이 네트워크 타임아웃 등으로 같은 이벤트를 재전송해도 중복 처리되지 않도록 사전 체크한다.
    // 실제 쿼리: SELECT we.id FROM webhook_events we WHERE we.webhook_id = ? LIMIT 1;
    boolean existsByWebhookId(String webhookId);

    @Query("SELECT we FROM WebhookEvent we " +
            "JOIN Payment p ON we.paymentId = p.id " +
            "JOIN p.order o " +
            "WHERE o.member.id = :memberId "+
            "ORDER BY we.createdAt DESC")
    List<WebhookEvent> findByMemberId(@Param("memberId") Long memberId);
}

