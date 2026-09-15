package org.example.murderhelp.domain.chat.bot;

import jakarta.annotation.PostConstruct;
import org.example.murderhelp.domain.chat.entity.ChatRoom;
import org.example.murderhelp.domain.chat.service.ChatMessageService;
import org.example.murderhelp.domain.order.service.OrderService;
import org.example.murderhelp.domain.order.dto.OrderListRequest;
import org.example.murderhelp.domain.order.dto.OrderListPeriod;
import org.example.murderhelp.domain.order.dto.OrderResponse;
import org.example.murderhelp.domain.chat.bot.dto.BotMessageDto;
import org.example.murderhelp.domain.chat.bot.dto.BotProductDto;
import org.example.murderhelp.domain.product.entity.Product;
import org.example.murderhelp.domain.product.entity.ProductTier;
import org.example.murderhelp.domain.product.service.ProductService;
import org.example.murderhelp.domain.product.service.ProductRankingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Component
@lombok.RequiredArgsConstructor
public class BotCommandDispatcher {

    private final OrderService orderService;
    private final ProductService productService;
    private final StringRedisTemplate redisTemplate;
    private final Map<BotCommand, BiConsumer<ChatRoom, ChatMessageService>> actionMap = new EnumMap<>(BotCommand.class);

    @PostConstruct
    public void init() {
        actionMap.put(BotCommand.CONNECT_AGENT, this::connectAgent);
        actionMap.put(BotCommand.RECOMMEND_WEAPON, this::handleRecommendWeapon);
        actionMap.put(BotCommand.CHECK_ORDER, this::handleCheckOrder);
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
        svc.sendSystemMessage(room, "상담사 연결을 대기 중입니다. 잠시만 기다려주세요.");
    }

    private void handleRecommendWeapon(ChatRoom room, ChatMessageService svc) {
        String userTierName = room.getCustomer().getGrade().name();
        String userTier = userTierName.toLowerCase();
        String redisKey = ProductRankingService.RANKING_TARGET_KEY_PREFIX + userTier;
        
        List<String> topProductIdsStr = redisTemplate.opsForList().range(redisKey, 0, -1);
        
        List<Product> products;
        if (topProductIdsStr != null && !topProductIdsStr.isEmpty()) {
            List<Long> ids = topProductIdsStr.stream().map(Long::valueOf).toList();
            products = productService.getProducts(ids);
            
            // Redis에서 꺼낸 랭킹 순서(ids)대로 상품 리스트 재정렬
            products.sort(Comparator.comparing(p -> ids.indexOf(p.getId())));
        } else {
            ProductTier userProductTier = ProductTier.valueOf(userTierName);
            // Fallback 로직: 최근 7일간 판매가 없으면 '최신 상품' 중 유저가 볼 수 있는 것 5개를 DB 쿼리 레벨에서 가져오기
            products = productService.getNewestProducts(userProductTier);
        }

        List<BotProductDto> productDtos = products.stream()
                .map(product -> new BotProductDto(product.getId(), product.getName(), (int) product.getPrice()))
                .toList();

        BotMessageDto messageDto = BotMessageDto.builder()
                .text("고객님의 등급에 맞는 추천 무기 리스트입니다.\n주간 베스트 🏆")
                .options(BotScenario.Constants.RETURN_MENU_OPTIONS)
                .products(productDtos)
                .build();
        svc.sendBotMessage(room, messageDto);
    }

    private void handleCheckOrder(ChatRoom room, ChatMessageService svc) {
        Page<OrderResponse> orders = orderService.getOrderList(
                room.getCustomer().getId(),
                new OrderListRequest(OrderListPeriod.MONTH_3, null),
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        String text;
        if (orders.isEmpty()) {
            text = "최근 3개월간 결제하신 주문 내역이 없습니다.";
        } else {
            OrderResponse order = orders.getContent().get(0);
            String firstItemName = order.items().get(0).productName();
            int extraCount = order.items().size() - 1;
            String productTitle = extraCount > 0 ? String.format("%s 외 %d건", firstItemName, extraCount) : firstItemName;
            text = String.format("📦 [최근 주문 내역 안내]\n\n▪️ 주문 상품: %s\n▪️ 진행 상태: %s", productTitle, order.status().name());
        }

        BotMessageDto messageDto = BotMessageDto.builder()
                .text(text)
                .options(BotScenario.Constants.RETURN_MENU_OPTIONS)
                .build();
        svc.sendBotMessage(room, messageDto);
    }
}
