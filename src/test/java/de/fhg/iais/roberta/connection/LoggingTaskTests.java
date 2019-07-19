package de.fhg.iais.roberta.connection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

// I don't know if these tests make sense yet
class LoggingTaskTests {

    private static final long TIMEOUT = 250L;

    private ExecutorService executorService = null;

    @BeforeEach
    void setUp() {
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @Test
    void loggingTask_ShouldLog_WhenSubmitted() {
        AbstractLoggingTask loggingTask = new TestLoggingTask();

        this.executorService.submit(loggingTask);

        List<String> messages = new ArrayList<>();
        loggingTask.registerListener(object -> messages.add(new String(object)));

        // Wait for the logger to log
        try {
            Thread.sleep(TIMEOUT);
        } catch ( InterruptedException e ) {
            // ignore
        }

        assertThat(messages, not(empty()));
        assertThat(messages.contains("test"), is(true));
    }

    @Test
    void loggingTask_ShouldStop_WhenCancelled() {
        AbstractLoggingTask loggingTask = new TestLoggingTask();

        Future<Void> submit = this.executorService.submit(loggingTask);

        List<String> messages = new ArrayList<>();
        loggingTask.registerListener(object -> messages.add(new String(object)));

        // Wait for the logger to log
        try {
            Thread.sleep(TIMEOUT);
        } catch ( InterruptedException e ) {
            // ignore
        }

        submit.cancel(true);

        // Wait for the thread to stop
        try {
            Thread.sleep(TIMEOUT);
        } catch ( InterruptedException e ) {
            // ignore
        }

        assertThat(messages.get(messages.size() - 1), is("finished"));
    }

    @AfterEach
    void tearDown() {
        this.executorService.shutdown();
    }

    private static class TestLoggingTask extends AbstractLoggingTask {
        @Override
        protected void log() {
            fire("test".getBytes());
        }

        @Override
        protected void finish() {
            fire("finished".getBytes());
        }
    }
}
