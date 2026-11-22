package com.pagoflex.reconocimientos.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public final class MutableClock extends Clock {
    private final ZoneId zone;
    private volatile Instant instant;

    public MutableClock(Instant instant, ZoneId zone) {
        this.instant = instant;
        this.zone = zone;
    }

    public void set(String isoInstant) {
        this.instant = Instant.parse(isoInstant);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
