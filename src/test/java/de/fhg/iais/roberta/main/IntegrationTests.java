package de.fhg.iais.roberta.main;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class IntegrationTests {
    @Test
    void run_ShouldStartConnectorSuccessfully_WhenRun() throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<?> submit = executorService.submit(() -> {
            OpenRobertaConnector openRobertaConnector = new OpenRobertaConnector();
            openRobertaConnector.run();
        });

        try {
            submit.get(2L, TimeUnit.SECONDS);
        } catch ( TimeoutException e ) {
            // ignore
        }
        executorService.shutdownNow();
    }
}
