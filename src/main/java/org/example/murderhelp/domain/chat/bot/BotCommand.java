package org.example.murderhelp.domain.chat.bot;

import java.util.Arrays;

public enum BotCommand {

    CONNECT_AGENT("상담사 연결"),
    RECOMMEND_WEAPON("무기 추천"),
    BACK_TO_MAIN("처음으로 돌아가기"),
    UNKNOWN("");

    private final String text;

    BotCommand(String text) {
        this.text = text;
    }

    public static BotCommand from(String text) {
        if (text == null) return UNKNOWN;
        return Arrays.stream(values())
                .filter(cmd -> cmd.text.equals(text))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
