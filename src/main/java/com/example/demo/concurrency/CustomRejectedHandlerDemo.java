package com.example.demo.concurrency;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CustomRejectedHandlerDemo {

    private static void writeToFallbackFile(Runnable task) {
        System.out.println(
                "[" + Thread.currentThread().getName()
                        + "] >>> FALLBACK: Task bị reject!"
        );
        System.out.println(
                "[" + Thread.currentThread().getName()
                        + "] >>> Ghi task vào fallback file..."
        );
        // Giả lập ghi file
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println(
                "[" + Thread.currentThread().getName()
                        + "] >>> Ghi fallback thành công."
        );
    }

    public static void main(String[] args) throws InterruptedException {
        ThreadPoolExecutor auditExecutor = new ThreadPoolExecutor(
                2,
                4,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(2),
                (task, executor) -> {
                    // Task bị reject
                    writeToFallbackFile(task);
                });

        // Submit 6 task
        for (int i = 1; i <= 7; i++) {
            int taskNumber = i;

            System.out.println(
                    "[" + Thread.currentThread().getName()
                            + "] Submit task " + taskNumber
            );

            auditExecutor.submit(() -> {
                System.out.println(
                        "[" + Thread.currentThread().getName()
                                + "] START task " + taskNumber
                );

                try {
                    // Giả lập task chạy lâu
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                System.out.println(
                        "[" + Thread.currentThread().getName()
                                + "] END task " + taskNumber
                );
            });
        }
        auditExecutor.shutdown();
        auditExecutor.awaitTermination(30, TimeUnit.SECONDS);
        System.out.println("Main kết thúc.");
    }

}
