package org.example.murderhelp.domain.refund.repository.dto;

import org.example.murderhelp.domain.refund.entity.Refund;
import org.example.murderhelp.domain.refund.entity.RefundItem;

import java.util.List;

public record RefundWithItems(Refund refund, List<RefundItem> refundItems) {}