import java.util.concurrent.CountDownLatch;

/**
 * CSC450 Module 8 Portfolio Project Part II
 * Author: Caitlin Rucker
 * Starts two workers and orders their counting with a completion latch.
 * Uses standard Java APIs compatible with Java 17.
 */
public final class ConcurrencyCounters {
    private static final int MAXIMUM_COUNT = 20;
    private final CountDownLatch countUpFinished = new CountDownLatch(1);

    // Published through the latch and successful joins, not by polling.
    private boolean countUpCompleted;
    private boolean countDownCompleted;

    private void countUp() {
        try {
            for (int count = 0; count <= MAXIMUM_COUNT; count++) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                System.out.println("Thread 1 counting up: " + count);
            }
            countUpCompleted = true;
        } finally {
            // Release the waiter even if counting exits unsuccessfully.
            countUpFinished.countDown();
        }
    }

    private void countDown() {
        try {
            countUpFinished.await();
            if (!countUpCompleted) {
                return; // Never count down after an incomplete count up.
            }
            for (int count = MAXIMUM_COUNT; count >= 0; count--) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                System.out.println("Thread 2 counting down: " + count);
            }
            countDownCompleted = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Countdown interrupted; stopping safely.");
        }
    }

    private void execute() throws InterruptedException {
        Thread firstThread = new Thread(this::countUp, "CountUpWorker");
        Thread secondThread = new Thread(this::countDown, "CountDownWorker");

        System.out.println("Starting the Java counting application.");
        // Start both workers before main waits for either worker.
        firstThread.start();
        secondThread.start();

        try {
            firstThread.join();
            secondThread.join();
        } catch (InterruptedException e) {
            // Request cooperative cancellation if main cannot keep waiting.
            firstThread.interrupt();
            secondThread.interrupt();
            throw e;
        }

        if (countUpCompleted && countDownCompleted) {
            System.out.println("Both counting threads have completed.");
        } else {
            System.err.println("Counting stopped before normal completion.");
        }
    }

    public static void main(String[] args) {
        try {
            // A fresh application owns its one-use latch and status flags.
            new ConcurrencyCounters().execute();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Main thread interrupted; cancellation requested.");
        }
    }
}