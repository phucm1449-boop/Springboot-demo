package com.example.demo.concurrency;

public class DeadlockFixed {

    private static final Object lockA = new Object();
    private static final Object lockB = new Object();

    public static void main(String[] args) throws InterruptedException {

        // CA HAI thread deu lock theo THU TU CO DINH: A truoc, B sau
        Thread thread1 = new Thread(() -> {
            synchronized (lockA) {
                System.out.println("[Thread-1] Da lock A, dang cho lock B...");
                sleepSilently(500);
                synchronized (lockB) {
                    System.out.println("[Thread-1] Da lock ca A va B - THANH CONG");
                }
            }
        }, "Thread-1");

        Thread thread2 = new Thread(() -> {
            synchronized (lockA) {  // <-- SUA: lock A truoc, giong Thread-1 (khong con nguoc thu tu)
                System.out.println("[Thread-2] Da lock A, dang cho lock B...");
                sleepSilently(500);
                synchronized (lockB) {
                    System.out.println("[Thread-2] Da lock ca A va B - THANH CONG");
                }
            }
        }, "Thread-2");

        thread1.start();
        thread2.start();
        thread1.join(3000);
        thread2.join(3000);

        if (thread1.isAlive() && thread2.isAlive()) {
            System.out.println(">>> KET QUA: VAN DEADLOCK");
        } else {
            System.out.println(">>> KET QUA: KHONG DEADLOCK - ca 2 thread da xong binh thuong");
        }
        System.exit(0);
    }

    private static void sleepSilently(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
