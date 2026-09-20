package com.example.demo.concurrency;

//-----------Race Condition--------------//

import java.util.concurrent.atomic.AtomicInteger;

public class CounterDemo {
//    static int count = 0;

//    static synchronized void increment() {
//        count++;
//    }

    static AtomicInteger count = new AtomicInteger();

    public static void main(String[] args) throws InterruptedException {
        Runnable task = () -> {
            for (int i = 0; i < 10000; i++) {
                count.incrementAndGet();
            }
        };
        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println(count);
    }
}
