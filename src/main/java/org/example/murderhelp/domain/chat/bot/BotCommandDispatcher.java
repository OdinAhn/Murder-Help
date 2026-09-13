package org.example.murderhelp.domain.chat.bot;

import jakarta.annotation.PostConstruct;
import org.example.murderhelp.domain.chat.entity.ChatRoom;
import org.example.murderhelp.domain.chat.service.ChatMessageService;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;

@Component
public class BotCommandDispatcher {

    private final Map<BotCommand, BiConsumer<ChatRoom, ChatMessageService>> actionMap = new EnumMap<>(BotCommand.class);

    @PostConstruct
    public void init() {
        actionMap.put(BotCommand.CONNECT_AGENT, this::connectAgent);
        actionMap.put(BotCommand.RECOMMEND_WEAPON, (room, svc) -> svc.sendBotScenarioMessage(room, BotScenario.RECOMMEND_WEAPON));
        actionMap.put(BotCommand.BACK_TO_MAIN, (room, svc) -> svc.sendBotScenarioMessage(room, BotScenario.WELCOME));
        actionMap.put(BotCommand.UNKNOWN, (room, svc) -> svc.sendBotScenarioMessage(room, BotScenario.FALLBACK));
    }

    public void execute(ChatRoom room, String content, ChatMessageService chatMessageService) {
        BotCommand command = BotCommand.from(content);
        actionMap.getOrDefault(command, (r, svc) -> svc.sendBotScenarioMessage(r, BotScenario.FALLBACK))
                 .accept(room, chatMessageService);
    }

    private void connectAgent(ChatRoom room, ChatMessageService svc) {
        room.changeToWaiting();
        svc.publishRoomUpdate(room);
        svc.sendSystemMessage(room, "상담사 연결을 대기 중입니다. 잠시만 기다려주세요.");
    }
}
