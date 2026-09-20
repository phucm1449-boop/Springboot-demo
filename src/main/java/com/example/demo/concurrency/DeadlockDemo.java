package com.example.demo.concurrency;

public class DeadlockDemo {
    private static final Object lockA = new Object();
    private static final Object lockB = new Object();

    // giả lập đang xử lý gì đó, đủ lâu để thread kia kịp lock được khóa của nó (như đang tính toán, đang gọi API...)
    private static void sleepSilently(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void printThreadState(Thread t) {
        System.out.println("    " + t.getName() + " state = " + t.getState());
    }

    public static void main(String[] args) throws InterruptedException {

        // Thread 1: lock A trước, rồi lock B
        Thread thread1 = new Thread(() -> {
            synchronized (lockA) {
                System.out.println("[Thread-1] Da lock A, dang cho lock B...");
                sleepSilently(500); // gia lap dang xu ly, du de Thread-2 kip lock B
                synchronized (lockB) {
                    System.out.println("[Thread-1] Da lock ca A va B - KHONG BAO GIO IN DUOC DONG NAY");
                }
            }
        }, "Thread-1");

        // Thread 2: lock B truoc, roi lock A (NGUOC THU TU voi Thread-1 -> deadlock)
        Thread thread2 = new Thread(() -> {
            synchronized (lockB) {
                System.out.println("[Thread-2] Da lock B, dang cho lock A...");
                sleepSilently(500); // gia lap dang xu ly, du de Thread-1 kip lock A
                synchronized (lockA) {
                    System.out.println("[Thread-2] Da lock ca B va A - KHONG BAO GIO IN DUOC DONG NAY");
                }
            }
        }, "Thread-2");

        thread1.start();
        thread2.start();

        // Cho toi da 3 giay - neu deadlock thi 2 thread se khong bao gio ket thuc
        thread1.join(3000);
        thread2.join(3000);

        if (thread1.isAlive() && thread2.isAlive()) {
            System.out.println();
            System.out.println(">>> KET QUA: DEADLOCK XAY RA! Ca 2 thread deu bi treo vinh vien.");
            printThreadState(thread1);
            printThreadState(thread2);
        } else {
            System.out.println(">>> KET QUA: Khong deadlock, ca 2 thread da xong.");
        }
        System.exit(0);
    }
}
