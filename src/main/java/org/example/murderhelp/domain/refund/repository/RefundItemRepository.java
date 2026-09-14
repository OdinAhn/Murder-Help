package org.example.murderhelp.domain.refund.repository;

import org.example.murderhelp.domain.refund.entity.RefundItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundItemRepository extends JpaRepository<RefundItem, Long>, RefundItemRepositoryCustom {
}
