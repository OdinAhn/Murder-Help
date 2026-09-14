package org.example.murderhelp.domain.refund.repository;

import org.example.murderhelp.domain.refund.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, Long>, RefundRepositoryCustom {
}
