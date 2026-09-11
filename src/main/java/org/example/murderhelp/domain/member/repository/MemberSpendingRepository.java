package org.example.murderhelp.domain.member.repository;

import jakarta.persistence.LockModeType;
import org.example.murderhelp.domain.member.entity.MemberSpending;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberSpendingRepository extends JpaRepository<MemberSpending, Long> {

    Optional<MemberSpending> findByMemberId(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ms from MemberSpending ms where ms.memberId = :memberId")
    Optional<MemberSpending> findByMemberIdForUpdate(@Param("memberId") Long memberId);
}