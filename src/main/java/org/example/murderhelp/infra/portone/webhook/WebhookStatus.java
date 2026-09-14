package org.example.murderhelp.infra.portone.webhook;
/**
 * 웹훅 이벤트 처리 상태
 * - RECEIVED:  수신 완료, 처리 전
 * - PROCESSED: 정상 처리 완료
 * - IGNORED:   알고는 있으나 처리 대상이 아닌 이벤트 (예: Transaction.Ready)
 * - FAILED:    처리 중 에러 발생 → 재처리 대상
 */
public enum WebhookStatus {
    RECEIVED,
    PROCESSED,
    IGNORED,
    FAILED
}