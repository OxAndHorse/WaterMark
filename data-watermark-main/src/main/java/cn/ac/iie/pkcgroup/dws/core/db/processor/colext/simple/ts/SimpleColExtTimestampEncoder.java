package cn.ac.iie.pkcgroup.dws.core.db.processor.colext.simple.ts;

import cn.ac.iie.pkcgroup.dws.core.db.processor.CommonCallable;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Collectors;

import static cn.ac.iie.pkcgroup.dws.core.db.processor.colext.simple.ts.Constants.PERIOD;
import static cn.ac.iie.pkcgroup.dws.core.db.processor.colext.simple.ts.Constants.PRECISION;

@Slf4j
public class SimpleColExtTimestampEncoder extends CommonCallable {

    @Override
    public String generateDWString(boolean isZero) {
        SecureRandom secureRandom = new SecureRandom();
        Timestamp ts;
        long currentTime = new Date().getTime();
        ArrayList<Long> randomPeriod = (ArrayList<Long>) secureRandom.longs(1, 0, PERIOD).boxed().collect(Collectors.toList());
        long material = currentTime + randomPeriod.get(0);

        boolean isOdd = ((material / PRECISION) & 0x01) == 0x01;
        if (isOdd && !isZero || !isOdd && isZero) {
            ts = new Timestamp(material);
        } else {
            ts = new Timestamp(material + PRECISION);
        }
        return ts.toString();
    }
}
