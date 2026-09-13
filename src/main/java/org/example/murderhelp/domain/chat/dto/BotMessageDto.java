package org.example.murderhelp.domain.chat.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record BotMessageDto(
    String text,
    List<BotOptionDto> options,
    List<BotProductDto> products
) {}
