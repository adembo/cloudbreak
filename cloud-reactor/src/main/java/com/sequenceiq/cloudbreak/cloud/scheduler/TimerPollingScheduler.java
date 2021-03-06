package com.sequenceiq.cloudbreak.cloud.scheduler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import reactor.fn.Consumer;
import reactor.fn.Pausable;
import reactor.fn.timer.Timer;

@Component
@Scope("prototype")
public class TimerPollingScheduler implements Consumer<Long> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimerPollingScheduler.class);

    @Inject
    private Timer timer;

    private CountDownLatch latch;

    private Pausable selfSchedule;

    @Override
    public void accept(Long aLong) {
        latch.countDown();
        LOGGER.debug("Schedule received: {}, count: {}", this, latch.getCount());
        if (latch.getCount() == 0) {
            cancel();
        }


    }

    public void schedule(int count, int period) {
        latch = new CountDownLatch(count);
        selfSchedule = timer.schedule(this, period, TimeUnit.SECONDS);
    }

    public void cancel() {
        selfSchedule.cancel();
    }
}
