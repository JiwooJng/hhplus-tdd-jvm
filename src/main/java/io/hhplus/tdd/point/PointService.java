package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class PointService {
    private static final Logger log = LoggerFactory.getLogger(PointService.class);

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    private static final long MAX_POINT = 1_000_000;

    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    public UserPoint getUserPoint(long userId) {
        UserPoint userPoint = userPointTable.selectById(userId);

        log.info("유저 포인트 조회 > 유저 id: {}, 잔액: {}", userId, userPoint.point());

        return userPoint;
    }

    public List<PointHistory> getPointHistoryList(long userId) {
         List<PointHistory> pointHistory = pointHistoryTable.selectAllByUserId(userId);

         log.info("유저 포인트 내역 조회 > 유저 id: {} 포인트 내역: {}", userId, pointHistory);

         return pointHistory;
    }

    public UserPoint charge(long userId, long chargeAmount) {
        if (chargeAmount <= 0) {
            log.error("포인트 충전 금액은 1원 이상이어야 합니다.");
            throw new IllegalArgumentException("포인트 충전 실패");
        }

        UserPoint userPoint = userPointTable.selectById(userId);
        log.info("포인트 충전 | 유저 조회 > id: {}, 잔액: {}", userId, userPoint.point());

        log.info("포인트 충전 금액: {}", chargeAmount);
        long wholeAmount = userPoint.point() + chargeAmount;

        if (wholeAmount > MAX_POINT) {
            log.error("포인트 충전 최대 한도는 1,000,000(원)입니다.");
            throw new IllegalArgumentException("포인트 충전 실패");
        }

        UserPoint updateUserPoint = userPointTable.insertOrUpdate(userId, wholeAmount);
        pointHistoryTable.insert(userId, wholeAmount, TransactionType.CHARGE, System.currentTimeMillis());

        log.info("포인트 충전 완료 > 잔액: {}", wholeAmount);
        return updateUserPoint;
    }

    public UserPoint use(long userId, long useAmount) {
        if (useAmount <= 0) {
            log.error("포인트 사용 금액은 1원 이상이어야 합니다.");
            throw new IllegalArgumentException("포인트 사용 실패");
        }

        UserPoint userPoint = userPointTable.selectById(userId);
        log.info("포인트 사용 | 유저 조회 > id: {}, 잔액: {}", userId, userPoint.point());

        log.info("포인트 사용 금액: {}", useAmount);
        long remainPoint = userPoint.point() - useAmount;

        if (remainPoint < 0) {
            log.error("포인트 잔액이 부족합니다.");
            throw new IllegalArgumentException("포인트 사용 실패");
        }

        UserPoint updateUserPoint = userPointTable.insertOrUpdate(userId, remainPoint);
        pointHistoryTable.insert(userId, remainPoint, TransactionType.USE, System.currentTimeMillis());

        log.info("포인트 사용 성공 > 잔액: {}", remainPoint);
        return updateUserPoint;
    }

}
