package org.example.murderhelp.domain.member.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.murderhelp.domain.member.service.MembershipGradePolicy;
import org.example.murderhelp.global.entity.BaseTimeEntity;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "member_spending",
        uniqueConstraints = @UniqueConstraint(name = "uk_member_spending_member", columnNames = "member_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberSpending extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "net_spent_amount", nullable = false)
    private Long netSpentAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolved_grade", nullable = false, length = 20)
    private Grade resolvedGrade;

    @Column(name = "last_calculated_at", nullable = false)
    private LocalDateTime lastCalculatedAt;

    @Builder
    private  MemberSpending(Long memberId) {
        this.memberId = memberId;
        this.netSpentAmount = 0L;
        this.resolvedGrade = Grade.YELLOW;
        this.lastCalculatedAt = LocalDateTime.now();
    }

    public void addAmount(long amount) {
        this.netSpentAmount += amount;
        recalculate();
    }

    public void subtractAmount(long amount) {
        this.netSpentAmount = Math.max(0, this.netSpentAmount - amount);
        recalculate();
    }

    private void recalculate() {
        this.resolvedGrade = MembershipGradePolicy.resolve(this.netSpentAmount);
        this.lastCalculatedAt = LocalDateTime.now();
    }
}