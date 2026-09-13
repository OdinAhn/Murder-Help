package org.example.murderhelp.domain.chat.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.chat.dto.BotMessageDto;
import org.example.murderhelp.domain.chat.dto.BotOptionDto;
import org.example.murderhelp.domain.chat.dto.BotProductDto;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum BotScenario {
    
    WELCOME(
        BotMessageDto.builder()
            .text("MurderHelp 고객 지원 센터에 오신 것을 환영합니다.\n원하시는 항목을 선택해 주세요.")
            .options(List.of(
                new BotOptionDto("상담사 연결", "CONNECT_AGENT"),
                new BotOptionDto("무기 추천", "RECOMMEND_WEAPON")
            ))
            .build()
    ),
    
    RECOMMEND_WEAPON(
        BotMessageDto.builder()
            .text("고객님의 등급에 맞는 추천 무기 리스트입니다.")
            .options(List.of(
                new BotOptionDto("상담사 연결", "CONNECT_AGENT"),
                new BotOptionDto("처음으로 돌아가기", "BACK_TO_MAIN")
            ))
            .products(List.of(
                new BotProductDto("w-001", "소음기 장착 권총", 1500),
                new BotProductDto("w-002", "저격 라이플", 3000)
            ))
            .build()
    ),

    FALLBACK(
        BotMessageDto.builder()
            .text("버튼을 통해 원하시는 항목을 선택해 주세요.")
            .options(List.of(
                new BotOptionDto("상담사 연결", "CONNECT_AGENT"),
                new BotOptionDto("무기 추천", "RECOMMEND_WEAPON")
            ))
            .build()
    );

    private final BotMessageDto messageDto;
}
