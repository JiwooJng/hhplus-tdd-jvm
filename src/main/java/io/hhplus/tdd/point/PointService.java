package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


@Service
public class PointService {
    private static final Logger log = LoggerFactory.getLogger(PointService.class);

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    public UserPoint getUserPoint(@PathVariable long userId) {
        UserPoint userPoint = this.userPointTable.selectById(userId);

        log.info("유저 포인트 조회 > 유저 id: {}, 잔액: {}", userId, userPoint.point());

        return userPoint;
    }

    public List<PointHistory> getPointHistoryList(@PathVariable long userId) {
         List<PointHistory> pointHistory = this.pointHistoryTable.selectAllByUserId(userId);

         log.info("유저 포인트 내역 조회 > 유저 id: {}%n 포인트 내역: {}", userId, pointHistory);

         return pointHistory;
    }

    public UserPoint charge(@PathVariable long userId, @RequestBody long chargeAmount) {
        UserPoint userPoint = this.userPointTable.selectById(userId);
        log.info("포인트 충전 | 유저 조회 > id: {}, 잔액: {}", userId, userPoint.point());

        log.info("포인트 충전 금액: {}", chargeAmount);
        long wholeAmount = userPoint.point() + chargeAmount;

        UserPoint updateUserPoint = this.userPointTable.insertOrUpdate(userId, wholeAmount);
        pointHistoryTable.insert(userId, wholeAmount, TransactionType.CHARGE, System.currentTimeMillis());

        log.info("포인트 충전 완료 > 잔액: {}", wholeAmount);
        return updateUserPoint;
    }

    public UserPoint use(@PathVariable long userId, @RequestBody long useAmount) {
        UserPoint userPoint = this.userPointTable.selectById(userId);
        log.info("포인트 사용 | 유저 조회 > id: {}, 잔액: {}", userId, userPoint.point());

        log.info("포인트 사용 금액: {}", useAmount);
        long remainPoint = userPoint.point() - useAmount;

        UserPoint updateUserPoint = this.userPointTable.insertOrUpdate(userId, remainPoint);
        pointHistoryTable.insert(userId, remainPoint, TransactionType.USE, System.currentTimeMillis());

        log.info("포인트 사용 성공 > 잔액: {}", remainPoint);
        return updateUserPoint;
    }

}
