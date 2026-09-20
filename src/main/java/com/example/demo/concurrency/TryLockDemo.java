package com.example.demo.concurrency;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class TryLockDemo {

    private static final ReentrantLock lockA = new ReentrantLock();
    private static final ReentrantLock lockB = new ReentrantLock();

    public static void main(String[] args) throws InterruptedException {

        // Thread-1: thu lock A truoc, roi B (giong deadlock demo truoc)
        Thread thread1 = new Thread(() -> tryDoWork("Thread-1", lockA, lockB), "Thread-1");

        // Thread-2: thu lock B truoc, roi A (NGUOC THU TU - de tinh huong deadlock)
        Thread thread2 = new Thread(() -> tryDoWork("Thread-2", lockB, lockA), "Thread-2");

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        System.out.println();
        System.out.println(">>> CA 2 THREAD DEU DA XONG - KHONG CO AI BI TREO VINH VIEN");
    }

    // Ham dung chung: thu lock 2 cai lock theo thu tu (first, second)
    // Neu khong lay duoc trong thoi gian quy dinh -> bo cuoc, thu lai
    private static void tryDoWork(String name, ReentrantLock first, ReentrantLock second) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                // Thu lay lock thu nhat, cho toi da 200ms
                if (first.tryLock(200, TimeUnit.MILLISECONDS)) {
                    try {
                        System.out.println("[" + name + "] Lan thu " + attempt + ": da lay lock 1, dang thu lock 2...");
                        // Gia lap dang lam viec, du de doi phuong kip lay lock cua ho
                        Thread.sleep(300);

                        // Thu lay lock thu hai, cho toi da 200ms
                        if (second.tryLock(200, TimeUnit.MILLISECONDS)) {
                            try {
                                System.out.println("[" + name + "] Lan thu " + attempt + ": THANH CONG, lay duoc ca 2 lock!");
                                return; // xong viec, thoat vong lap
                            } finally {
                                second.unlock();
                            }
                        } else {
                            System.out.println("[" + name + "] Lan thu " + attempt + ": KHONG lay duoc lock 2 -> BO lock 1, thu lai");
                        }
                    } finally {
                        first.unlock(); // luon nha lock 1 ra du thanh cong hay khong
                    }
                } else {
                    System.out.println("[" + name + "] Lan thu " + attempt + ": khong lay duoc lock 1, thu lai");
                }

                // Doi mot chut ngau nhien roi thu lai, tranh 2 thread cu dung nhip nhau mai
                Thread.sleep((long) (Math.random() * 100));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}