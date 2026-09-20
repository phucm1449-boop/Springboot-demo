package com.example.demo.concurrency;

import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.*;

public class BasicExecutorDemo {

    // Spring Boot thực tế — cấu hình mẫu
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);       // theo cong thuc tren, tuy tai I/O hay CPU-bound
        executor.setMaxPoolSize(20);        // gioi han ro rang, khong de vo han
        executor.setQueueCapacity(50);      // GIOI HAN CU THE - khong bao gio de mac dinh vo han
        executor.setRejectedExecutionHandler(
                new ThreadPoolExecutor.CallerRunsPolicy()); // co chien luoc ro rang khi qua tai
        executor.setThreadNamePrefix("MyApp-Async-");
        executor.initialize();
        return executor;
    }

    public static void main(String[] args) throws InterruptedException {
        // Thue san 2 "dau bep" (2 thread) lam viec co dinh
        ExecutorService kitchen = Executors.newFixedThreadPool(2);
        ExecutorService kitchen2 = new ThreadPoolExecutor(
                2,
                2,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());

        System.out.println("Quan mo cua voi 2 dau bep. Co 5 mon can nau...\n");

        // Giao 5 "mon an" (task) cho doi 2 dau bep
        for (int i = 1; i <= 5; i++) {
            int monSo = i;
            kitchen.submit(() -> {
                String tenThread = Thread.currentThread().getName();
                System.out.println("[" + tenThread + "] Bat dau nau mon " + monSo);
                sleepSilently(1000); // gia lap nau mon mat 1 giay
                System.out.println("[" + tenThread + "] XONG mon " + monSo);
            });
        }
        // Bao cho executor: "khong nhan them viec moi nua"
        kitchen.shutdown();
        // Cho toi da 10 giay de tat ca mon nau xong
        kitchen.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("\nQuan dong cua, tat ca mon da xong.");
    }

    private static void sleepSilently(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
