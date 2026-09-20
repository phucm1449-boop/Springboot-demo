package com.example.demo.concurrency;
/**
 * Demo day du ve loi VISIBILITY trong Java Memory Model (JMM)
 *
 * Chay: java VisibilityBugDemo buggy   -> tai hien bug (co the treo)
 *       java VisibilityBugDemo fixed   -> da fix bang volatile
 *
 * Neu khong truyen tham so -> chay ca 2 de so sanh
 */

public class VisibilityDemo {

    // ========================================================
    // PHIEN BAN LOI: khong co volatile/synchronized
    // ========================================================
    static class BuggyCounter {
        private boolean running = true; // KHONG volatile -> khong dam bao visibility

        public void stop() {
            running = false;
        }

        public void work() {
            long counter = 0;
            while (running) {
                counter++; // giu vong lap "co viec lam" de JIT khong toi uu bat thuong
            }
            System.out.println("  [Thread-B] Da thoat vong lap, counter = " + counter);
        }
    }

    // ========================================================
    // PHIEN BAN DA FIX: dung volatile
    // ========================================================
    static class FixedCounter {
        private volatile boolean running = true; // CHI KHAC 1 TU KHOA

        public void stop() {
            running = false;
        }

        public void work() {
            long counter = 0;
            while (running) {
                counter++;
            }
            System.out.println("  [Thread-B] Da thoat vong lap, counter = " + counter);
        }
    }

    // ========================================================
    // Ham chay test chung, dung cho ca 2 phien ban
    // stopAction: hanh dong goi stop()
    // workAction: hanh dong goi work()
    // ========================================================
    public static void main(String[] args) throws InterruptedException {
        BuggyCounter demo = new BuggyCounter();

        Runnable taskA = () -> {
            demo.stop();
        };
        Runnable taskB = () -> {
            demo.work();
        };
        Thread threadA = new Thread(taskA, "Thread-A");
        Thread threadB = new Thread(taskB, "Thread-B");
        long startStop = System.currentTimeMillis();
        threadB.start();

        // QUAN TRONG: cho Thread-B chay du lau de JIT "lam nong" vong lap
        // (JIT can hang nghin/hang trieu lan lap moi quyet dinh toi uu hoa)
        Thread.sleep(1000);// <-- dòng này nằm trong main() -> MAIN THREAD bị sleep, KHÔNG phải threadB

        threadA.start();
        threadA.join();
        System.out.println("  [Thread-A] Da goi stop()");

        // Cho toi da 5 giay - neu bug xay ra thi Thread-B se khong bao gio tu dung
        threadB.join(5000);
        long elapsed = System.currentTimeMillis() - startStop;
        if (threadB.isAlive()) {
            System.out.println("  >>> KET QUA: TREO! Thread-B khong thay gia tri moi sau " + elapsed + "ms");
            threadB.interrupt(); // don dep, khong de thread rac chay mai
        } else {
            System.out.println("  >>> KET QUA: Thread-B dung binh thuong sau " + elapsed + "ms");
        }
        System.out.println();
    }
}
