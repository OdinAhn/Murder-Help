package org.example.murderhelp.domain.chat.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.chat.entity.ChatRoom;
import org.example.murderhelp.domain.chat.entity.ChatRoomStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.example.murderhelp.domain.chat.entity.QChatRoom.chatRoom;

@Repository
@RequiredArgsConstructor
public class ChatRoomRepositoryImpl implements ChatRoomRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ChatRoom> findRoomsByCondition(Long customerId, ChatRoomStatus status, Pageable pageable) {
        List<ChatRoom> content = queryFactory
                .selectFrom(chatRoom)
                .where(
                        eqCustomerId(customerId),
                        eqStatus(status)
                )
                .orderBy(chatRoom.updatedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(chatRoom.count())
                .from(chatRoom)
                .where(eqCustomerId(customerId), eqStatus(status));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }
    
    private BooleanExpression eqCustomerId(Long customerId) {
        return customerId != null ? chatRoom.customerId.eq(customerId) : null;
    }
    
    private BooleanExpression eqStatus(ChatRoomStatus status) {
        return status != null ? chatRoom.status.eq(status) : null;
    }
}
