package com.example.demo.concurrency;

import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.*;

public class ThreadPoolExecutorDemo {

    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);      // 10 luong gui email song song binh thuong
        executor.setMaxPoolSize(30);       // luc cao diem (vd: gui campaign lon) co the tang len 30
        executor.setQueueCapacity(500);    // toi da 500 email cho trong hang doi
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    public static void main(String[] args) throws InterruptedException {
//        ThreadPoolExecutor executor = new ThreadPoolExecutor(
//                2,                          // corePoolSize: 2 dau bep co dinh
//                4,                                     // maxPoolSize: toi da 4 dau bep
//                5, TimeUnit.SECONDS,                   // keepAliveTime cho thread ngoai core
//                new LinkedBlockingQueue<>(2), // queue chi chua duoc 2 task cho
//                new ThreadPoolExecutor.AbortPolicy() // reject: nem exception neu qua tai
//        );

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 4, 5, TimeUnit.SECONDS,
                new SynchronousQueue<>(),             // KHONG co suc chua - tuong duong "queueCapacity = 0"
                new ThreadPoolExecutor.AbortPolicy()
        );

        // Submit 7 task, moi task chay 3 giay
        for (int i = 1; i <= 7; i++) {
            int taskNum = i;
            try {
                executor.submit(() -> {
                    // (A) In BỞI WORKER THREAD, khi nó THỰC SỰ bắt đầu chạy task
                    System.out.println("  -> [Task " + taskNum + "] BAT DAU chay tren " + Thread.currentThread().getName());
                    sleepSilently(3000);
                });
                // (B) In BỞI MAIN THREAD, ngay sau khi submit() xong
                System.out.println("Submit task " + taskNum + " OK. " +
                        "PoolSize=" + executor.getPoolSize() +
                        ", QueueSize=" + executor.getQueue().size() +
                        ", ActiveCount=" + executor.getActiveCount());
            } catch (RejectedExecutionException e) {
                System.out.println("Submit task " + taskNum + " BI TU CHOI! (pool va queue deu day)");
            }

            Thread.sleep(100); // cho log in ra tuan tu de de doc
        }
        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.SECONDS);
    }

    private static void sleepSilently(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
