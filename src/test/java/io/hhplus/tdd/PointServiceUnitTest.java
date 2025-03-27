package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class PointServiceUnitTest {
    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @InjectMocks
    private PointService pointService;

    private final long userId = 1L;


    @Test
    void 유저_포인트_조회() {
        when(userPointTable.selectById(userId))
                .thenReturn(new UserPoint(userId, 50000, System.currentTimeMillis()));

        UserPoint userPoint = pointService.getUserPoint(userId);

        assertEquals(50000, userPoint.point());
    }

    @Test
    void 유저_포인트_내역_조회() {
        // given
        List<PointHistory> pointHistoryList = List.of(
                new PointHistory(1, userId, 58000, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2, userId, 7000, TransactionType.USE, System.currentTimeMillis())
        );

        when(pointHistoryTable.selectAllByUserId(userId))
                .thenReturn(pointHistoryList);
        // when
        List<PointHistory> historyList = pointService.getPointHistoryList(userId);

        // then
        assertEquals(2, historyList.size());

        assertEquals(58000, historyList.get(0).amount());
        assertEquals(TransactionType.CHARGE, historyList.get(0).type());

        assertEquals(7000, historyList.get(1).amount());
        assertEquals(TransactionType.USE, historyList.get(1).type());
    }

    @Test
    void 포인트_사용_성공() {
        // given
        long useAmount = 85000;
        when(userPointTable.selectById(userId))
                .thenReturn(new UserPoint(userId, 130000, System.currentTimeMillis()));

        // when
        pointService.use(userId, useAmount);
        UserPoint updateUserPoint = pointService.getUserPoint(userId);

        // then
        verify(userPointTable).insertOrUpdate(userId, 45000);
    }

    @Test
    void 포인트_충전_성공() {
        // given
        long chargeAmount = 77000;
        when(userPointTable.selectById(userId))
                . thenReturn(new UserPoint(userId, 70000, System.currentTimeMillis()));

        // when
        pointService.charge(userId, chargeAmount);
        UserPoint updateUserPoint = pointService.getUserPoint(userId);

        // then
        verify(userPointTable).insertOrUpdate(userId, 147000);
    }

    @Test
    void 충전_실패_금액_부족() {
        // given
        long invalidAmount = 0;

        // when & then
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            pointService.charge(userId, invalidAmount)
        );

        assertEquals("포인트 충전 실패", exception.getMessage());
    }

    @Test
    void 충전_한도_초과_실패() {
        // given
        long chargeAmount = 10001;
        when(userPointTable.selectById(userId))
                .thenReturn(new UserPoint(userId, 990000, System.currentTimeMillis()));

        // when & then
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            pointService.charge(userId, chargeAmount)
        );

        assertEquals("포인트 충전 실패", exception.getMessage());
    }

    @Test
    void 포인트_사용_금액_부족_실패() {
        // given
        long invalidAmount = -1;

        // when & then
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            pointService.use(userId, invalidAmount)
        );

        assertEquals("포인트 사용 실패", exception.getMessage());
    }

    @Test
    void 포인트_잔액_부족_실패() {
        // given
        long useAmount = 50000;
        when(userPointTable.selectById(userId))
                .thenReturn(new UserPoint(userId, 20000, System.currentTimeMillis()));

        // when & then
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            pointService.use(userId, useAmount)
        );

        assertEquals("포인트 사용 실패", exception.getMessage());
    }
}
