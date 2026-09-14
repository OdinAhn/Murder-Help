package org.example.murderhelp.infra.portone.webhook;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class WebhookEventService {

    private final WebhookEventRepository webhookEventRepository;

    // 중복이 아니면 RECEIVED 상태로 저장하고, 중복이면 Optional.empty()를 돌려준다.
    // 호출부는 Optional.empty()를 받으면 "이미 처리된 웹훅이니 무시"로 해석해 200 OK만 응답하면 된다.
    //
    // 실제 쿼리 (두 단계로 나가며, 중복이면 1번 쿼리만 실행된다):
    //   1) 중복 체크: SELECT we.id FROM webhook_events we WHERE we.webhook_id = ? LIMIT 1;
    //   2) 신규 저장: INSERT INTO webhook_events
    //                (created_at, failure_reason, finished_at, payload, status, type, updated_at, webhook_id)
    //                values (?, ?, ?, ?, 'RECEIVED', ?, ?, ?);
    public Optional<WebhookEvent> saveIfNotDuplicate(String webhookId, String type, String payload) {
        if (webhookEventRepository.existsByWebhookId(webhookId)) {
            return Optional.empty();
        }
        return Optional.of(webhookEventRepository.save(new WebhookEvent(null, webhookId, type, payload)));
    }

    // 웹훅 본 처리 로직이 성공한 경우 호출. status를 PROCESSED로, finished_at을 now()로 바꾼다.
    public void markProcessed(Long eventId) {
        load(eventId).markAsProcessed();
    }

    // "처리할 필요 없는 이벤트"(예: 우리가 모르는 type, 이미 취소된 결제 등)일 때 호출
    // 실패가 아니라 "의도적으로 건너뜀"을 구분하기 위해 별도 상태(IGNORED)로 남긴다.
    public void markIgnored(Long eventId, String reason) {
        load(eventId).markAsIgnored(reason);
    }

    // 본 처리 중 예외가 난 경우 호출. 재처리/모니터링을 위해 실패 사유를 남긴다.
    public void markFailed(Long eventId, String reason) {
        load(eventId).markAsFailed(reason);
    }

    // 웹훅 이벤트가 어떤 결제(paymentId)와 연결된 것인지 사후에 채워 넣는다.
    // 처음 저장 시점(saveIfNotDuplicate)에는 아직 PG 재조회 전이라 결제를 특정할 수 없으므로,
    // handlePaid/handleCancel에서 결제를 찾아낸 직후 이 메서드로 갱신한다.
    // 이 값이 채워져야 회원별 웹훅 조회(findByMemberId)가 정상적으로 동작한다.
    public void attachPaymentId(Long eventId, Long paymentId) {
        load(eventId).assignPaymentId(paymentId);
    }

    // PK로 조회하되, 없으면 비즈니스 예외로 변환한다.
    private WebhookEvent load(Long eventId) {
        return webhookEventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WEBHOOK_EVENT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<WebhookEvent> getWebhookEvents() {
        return webhookEventRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    // 로그인한 회원의 결제/주문에 연결된 웹훅 이벤트만 조회
    @Transactional(readOnly = true)
    public List<WebhookEvent> getWebhookEventsByMember(Long memberId) {
        return webhookEventRepository.findByMemberId(memberId);
    }
}