package com.bankingSystem.Performance;

import com.bankingSystem.Account.Account;
import com.bankingSystem.Account.CheckingAccount;
import com.bankingSystem.Account.SavingsAccount;
import com.bankingSystem.Database.AccountDAO;
import com.bankingSystem.Transaction.TransactionService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MixedConcurrencyBenchmark {

    // ===== Config =====
    private static final int ACCOUNTS = 100;

    private static final int WARMUP_TASKS = 1_000;
    private static final int MEASURE_TASKS = 5000;

    // amounts صغيرة لتجنب manager approval
    private static final int AMOUNT_MIN = 50;
    private static final int AMOUNT_RANGE = 500;

    public static void main(String[] args) throws Exception {

        System.out.println("=== MIXED CONCURRENCY BENCHMARK (NO SYSTEM CODE CHANGES) ===");

        TransactionService service = TransactionService.getInstance();
        AccountDAO accountDAO = new AccountDAO();

        // ===== Prepare accounts =====
        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < ACCOUNTS; i++) {
            Account a = (i % 2 == 0)
                    ? new SavingsAccount("SAV-MIX-" + i, "owner-mix")
                    : new CheckingAccount("CHK-MIX-" + i, "owner-mix");

            a.persist();

            // seed balance لتقليل فشل withdraw/transfer
            service.processTransaction(null, a, 20_000, "DEPOSIT");

            accounts.add(a);
        }

        // ===== Run 1: Single thread baseline =====
        runScenario("BASELINE (1 thread)", service, accountDAO, accounts, 1, WARMUP_TASKS, false);
        Result r1 = runScenario("MEASURE (1 thread)", service, accountDAO, accounts, 1, MEASURE_TASKS, true);

        // ===== Run 2: Concurrent load =====
        runScenario("WARMUP (200 threads)", service, accountDAO, accounts, 200, WARMUP_TASKS, false);
        Result r100 = runScenario("MEASURE (200 threads)", service, accountDAO, accounts, 200, MEASURE_TASKS, true);

        // ===== Query speed benchmark (Account inquiry only) =====
        querySpeedBenchmark(accountDAO, accounts, 500);

        // ===== Summary comparison =====
        System.out.println("\n=== SUMMARY COMPARISON ===");
        System.out.printf("Throughput 1-thread : %.2f ops/sec%n", r1.throughputOps);
        System.out.printf("Throughput 200-threads: %.2f ops/sec%n", r100.throughputOps);
        System.out.printf("p95 latency 1-thread : %d ms%n", r1.p95Ms);
        System.out.printf("p95 latency 100-threads: %d ms%n", r100.p95Ms);
        System.out.printf("Failures 1-thread=%d | 200-threads=%d%n", r1.failures, r100.failures);
        System.out.printf("Rejects  1-thread=%d | 200-threads=%d (business rule)%n", r1.rejects, r100.rejects);

        System.out.println("\n=== BENCHMARK FINISHED ===");
    }

    private static Result runScenario(
            String title,
            TransactionService service,
            AccountDAO accountDAO,
            List<Account> accounts,
            int threads,
            int totalTasks,
            boolean collectLatency
    ) throws Exception {

        System.out.println("\n--- " + title + " ---");
        System.out.println("Threads: " + threads + " | Tasks: " + totalTasks);

        ExecutorService pool = Executors.newFixedThreadPool(threads);

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(totalTasks);

        AtomicInteger failures = new AtomicInteger(0);
        AtomicInteger rejects = new AtomicInteger(0);

        AtomicInteger depositOps = new AtomicInteger(0);
        AtomicInteger withdrawOps = new AtomicInteger(0);
        AtomicInteger transferOps = new AtomicInteger(0);
        AtomicInteger inquiryOps = new AtomicInteger(0);

        final long[] latenciesNs = collectLatency ? new long[totalTasks] : null;
        final AtomicInteger idx = new AtomicInteger(0);

        long startAll = System.nanoTime();

        for (int i = 0; i < totalTasks; i++) {
            pool.submit(() -> {
                int myIndex = -1;
                long t0 = 0L;

                try {
                    if (collectLatency) myIndex = idx.getAndIncrement();

                    startGate.await();

                    if (collectLatency) t0 = System.nanoTime();

                    int op = ThreadLocalRandom.current().nextInt(4);

                    Account a1 = accounts.get(ThreadLocalRandom.current().nextInt(accounts.size()));
                    Account a2 = accounts.get(ThreadLocalRandom.current().nextInt(accounts.size()));
                    while (a2 == a1) {
                        a2 = accounts.get(ThreadLocalRandom.current().nextInt(accounts.size()));
                    }

                    double amount = AMOUNT_MIN + ThreadLocalRandom.current().nextInt(AMOUNT_RANGE);

                    switch (op) {
                        case 0 -> { // deposit
                            depositOps.incrementAndGet();
                            service.processTransaction(null, a1, amount, "DEPOSIT");
                        }
                        case 1 -> { // withdraw
                            withdrawOps.incrementAndGet();
                            service.processTransaction(a1, null, amount, "WITHDRAW");
                        }
                        case 2 -> { // transfer
                            transferOps.incrementAndGet();
                            service.processTransaction(a1, a2, amount, "TRANSFER");
                        }
                        case 3 -> { // inquiry
                            inquiryOps.incrementAndGet();
                            accountDAO.loadAccount(a1.getAccountNumber());
                        }
                    }

                } catch (IllegalStateException e) {
                    // business reject مثل insufficient funds أو حالة حساب
                    rejects.incrementAndGet();
                } catch (Exception e) {
                    // technical failure
                    failures.incrementAndGet();
                } finally {
                    if (collectLatency && myIndex >= 0 && myIndex < latenciesNs.length) {
                        latenciesNs[myIndex] = System.nanoTime() - t0;
                    }
                    endGate.countDown();
                }
            });
        }

        // start!
        startGate.countDown();
        endGate.await();

        long endAll = System.nanoTime();
        pool.shutdown();

        double durationSec = (endAll - startAll) / 1_000_000_000.0;
        double throughput = totalTasks / durationSec;

        Result r = new Result();
        r.threads = threads;
        r.tasks = totalTasks;
        r.deposits = depositOps.get();
        r.withdraws = withdrawOps.get();
        r.transfers = transferOps.get();
        r.inquiries = inquiryOps.get();
        r.failures = failures.get();
        r.rejects = rejects.get();
        r.durationSec = durationSec;
        r.throughputOps = throughput;

        if (collectLatency) {
            LatStats stats = computeLatencyStats(latenciesNs);
            r.p50Ms = stats.p50Ms;
            r.p95Ms = stats.p95Ms;
            r.p99Ms = stats.p99Ms;
            r.maxMs = stats.maxMs;
        }

        // print
        System.out.println("\n=== RESULTS (" + title + ") ===");
        System.out.println("Threads     : " + threads);
        System.out.println("Tasks       : " + totalTasks);
        System.out.printf("Deposits    : %d | Withdraws: %d | Transfers: %d | Inquiries: %d%n",
                r.deposits, r.withdraws, r.transfers, r.inquiries);
        System.out.println("Failures    : " + r.failures + " (technical)");
        System.out.println("Rejects     : " + r.rejects + " (business rules)");
        System.out.printf("Total time  : %.3f sec%n", r.durationSec);
        System.out.printf("Throughput  : %.2f ops/sec%n", r.throughputOps);

        if (collectLatency) {
            System.out.printf("Latency p50 : %d ms%n", r.p50Ms);
            System.out.printf("Latency p95 : %d ms%n", r.p95Ms);
            System.out.printf("Latency p99 : %d ms%n", r.p99Ms);
            System.out.printf("Latency max : %d ms%n", r.maxMs);
        }

        return r;
    }

    private static void querySpeedBenchmark(AccountDAO accountDAO, List<Account> accounts, int queries) {
        System.out.println("\n=== QUERY SPEED BENCHMARK (loadAccount) ===");
        System.out.println("Queries: " + queries);

        long[] latNs = new long[queries];

        long start = System.nanoTime();
        for (int i = 0; i < queries; i++) {
            Account a = accounts.get(i % accounts.size());
            long t0 = System.nanoTime();
            accountDAO.loadAccount(a.getAccountNumber());
            latNs[i] = System.nanoTime() - t0;
        }
        long end = System.nanoTime();

        double sec = (end - start) / 1_000_000_000.0;
        double qps = queries / sec;

        LatStats s = computeLatencyStats(latNs);
        System.out.printf("QPS         : %.2f queries/sec%n", qps);
        System.out.printf("Query p50   : %d ms%n", s.p50Ms);
        System.out.printf("Query p95   : %d ms%n", s.p95Ms);
        System.out.printf("Query p99   : %d ms%n", s.p99Ms);
        System.out.printf("Query max   : %d ms%n", s.maxMs);
    }

    private static LatStats computeLatencyStats(long[] latenciesNs) {
        long[] copy = Arrays.copyOf(latenciesNs, latenciesNs.length);
        Arrays.sort(copy);

        LatStats s = new LatStats();
        s.p50Ms = nsToMs(copy[(int) Math.floor(0.50 * (copy.length - 1))]);
        s.p95Ms = nsToMs(copy[(int) Math.floor(0.95 * (copy.length - 1))]);
        s.p99Ms = nsToMs(copy[(int) Math.floor(0.99 * (copy.length - 1))]);
        s.maxMs = nsToMs(copy[copy.length - 1]);
        return s;
    }

    private static int nsToMs(long ns) {
        return (int) Math.max(0, Math.round(ns / 1_000_000.0));
    }

    private static class LatStats {
        int p50Ms, p95Ms, p99Ms, maxMs;
    }

    private static class Result {
        int threads;
        int tasks;
        int deposits, withdraws, transfers, inquiries;
        int failures;
        int rejects;
        double durationSec;
        double throughputOps;

        int p50Ms, p95Ms, p99Ms, maxMs;
    }
}
