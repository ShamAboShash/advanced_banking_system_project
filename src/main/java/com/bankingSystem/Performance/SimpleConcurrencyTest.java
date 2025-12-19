package com.bankingSystem.Performance;

import com.bankingSystem.Account.SavingsAccount;
import com.bankingSystem.Transaction.TransactionService;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleConcurrencyTest {

    public static void main(String[] args) throws Exception {

        System.out.println("=== SIMPLE CONCURRENCY TEST (NO CODE CHANGES) ===");

        TransactionService service = TransactionService.getInstance();

        // حساب واحد فقط
        SavingsAccount account = new SavingsAccount("SAV-CONC-1", "owner-test");
        account.persist();

        int threads = 10;
        int totalTasks = 2000;

        ExecutorService pool = Executors.newFixedThreadPool(threads);

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(totalTasks);

        AtomicInteger failures = new AtomicInteger(0);

        long startTime = System.nanoTime();

        for (int i = 0; i < totalTasks; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    service.processTransaction(null, account, 100, "DEPOSIT");
                } catch (Exception e) {
                    failures.incrementAndGet();
                    System.out.println("❌ Exception: " + e.getMessage());
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();   // إطلاق كل الـthreads
        endGate.await();         // ننتظر الكل يخلص

        long endTime = System.nanoTime();

        pool.shutdown();

        double durationSec = (endTime - startTime) / 1_000_000_000.0;

        System.out.println("\n=== RESULTS ===");
        System.out.println("Threads        : " + threads);
        System.out.println("Total tasks    : " + totalTasks);
        System.out.println("Failures       : " + failures.get());
        System.out.printf("Total time     : %.3f sec%n", durationSec);
        System.out.printf("Throughput     : %.2f ops/sec%n", totalTasks / durationSec);

        System.out.println("\nFinal balance should be = " + (totalTasks * 100));
        account.showDetails();

        System.out.println("\n=== TEST FINISHED ===");
    }
}
