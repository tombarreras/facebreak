package com.thomasjbarrerasconsulting.faces;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class ScopedExecutorTest {

    @Test
    public void execute_delegatesToUnderlyingExecutor() throws InterruptedException {
        AtomicBoolean ran = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        // Use a direct (same-thread) executor
        ScopedExecutor executor = new ScopedExecutor(Runnable::run);
        executor.execute(() -> {
            ran.set(true);
            latch.countDown();
        });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertTrue(ran.get());
    }

    @Test
    public void execute_afterShutdown_doesNotRun() {
        AtomicBoolean ran = new AtomicBoolean(false);

        ScopedExecutor executor = new ScopedExecutor(Runnable::run);
        executor.shutdown();
        executor.execute(() -> ran.set(true));

        assertFalse(ran.get());
    }

    @Test
    public void execute_shutdownBetweenDelegateAndRun_doesNotRunCommand() {
        AtomicBoolean commandRan = new AtomicBoolean(false);

        // Executor that captures the runnable but doesn't run it immediately
        final Runnable[] captured = new Runnable[1];
        ScopedExecutor executor = new ScopedExecutor(r -> captured[0] = r);

        executor.execute(() -> commandRan.set(true));

        // Shutdown after the outer execute passed the first check,
        // but before the captured runnable is invoked
        executor.shutdown();

        // Now run the captured runnable — it should check shutdown again and bail
        assertNotNull(captured[0]);
        captured[0].run();

        assertFalse(commandRan.get());
    }
}
