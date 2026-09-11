package org.example.murderhelp.domain.member.service;

import org.example.murderhelp.domain.member.entity.Grade;

public class MembershipGradePolicy {

    //가격 변경
    private static final long RED_THRESHOLD = 500_000L;
    private static final long PURPLE_THRESHOLD = 100_000L;

    private MembershipGradePolicy() {
    }

    public static Grade resolve(long netSpentAmount) {
        if (netSpentAmount >= RED_THRESHOLD) {
            return Grade.RED;
        }
        if (netSpentAmount >= PURPLE_THRESHOLD) {
            return Grade.PURPLE;
        }
        return Grade.YELLOW;
    }
}