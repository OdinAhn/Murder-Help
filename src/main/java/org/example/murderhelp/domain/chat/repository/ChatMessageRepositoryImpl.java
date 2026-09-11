package org.example.murderhelp.domain.chat.repository;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.example.murderhelp.domain.chat.entity.QChatMessage.chatMessage;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ChatMessage> findMessagesByRoomId(Long roomId, Pageable pageable) {
        List<ChatMessage> content = queryFactory
                .selectFrom(chatMessage)
                .where(chatMessage.chatRoom.id.eq(roomId))
                .orderBy(chatMessage.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(chatMessage.count())
                .from(chatMessage)
                .where(chatMessage.chatRoom.id.eq(roomId));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }
}
