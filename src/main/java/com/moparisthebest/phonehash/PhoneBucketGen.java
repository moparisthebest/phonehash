package com.moparisthebest.phonehash;

import com.moparisthebest.filelist.LongConverter40Bit;
import com.moparisthebest.filelist.RandomAccessFileList;

import java.io.*;
import java.util.*;

/**
 * Created by mopar on 2/10/17.
 */
public class PhoneBucketGen {

    public static int index(final byte a, final byte b) {
        return (((a & 0xff) << 8) | (b & 0xff));
    }

    public static void main(String[] args) throws Exception {
        final Object lock = new Object();
        final File path = new File(args[0]);
        final int numThreads = Integer.parseInt(args[1]);
        final long start = Long.parseLong(args[2]);
        final long end = Long.parseLong(args[3]) + 1;
        path.mkdirs();

        {

            final OutputStream[] osArr = new OutputStream[65536];

            {
                final Random r = new Random();
                byte a = Byte.MIN_VALUE;
                do {
                    final int ia = a + 128;
                    final File folder = new File(path, String.format("%03d", ia));
                    folder.mkdirs();
                    byte b = Byte.MIN_VALUE;
                    do {
                        final int index = index(a, b);
                        final int ib = b + 128;
                        //System.out.printf("%d/%d: %03d/%03d: %d%n", a, b, ia, ib, index);
                        final File file = new File(folder, String.format("%03d.list", ib));
                        //System.out.println(file.getAbsolutePath());
                        //osArr[index] = new FileOutputStream(file);
                        //osArr[index] = new BufferedOutputStream(new FileOutputStream(file), 4100 + (5 * r.nextInt(2458))); // random cache between 4100 and 16385
                        osArr[index] = new LockingBufferedOutputStream(new FileOutputStream(file), lock, 131075); // 128mb, will require ~8gb ram
                        ++b;
                    } while (b != Byte.MIN_VALUE);
                    //list.add((int) a);
                    ++a;
                } while (a != Byte.MIN_VALUE);
            }
            System.out.println("files set up");

            final long totalNums = end - start;
            final long numsPerThread = (totalNums / numThreads) + 1; // hacky round-up
            System.out.printf("numThreads: %d start: %d end: %d totalNums: %d numsPerThread: %d%n", numThreads, start, end, totalNums, numsPerThread);
            final List<Thread> threads = new ArrayList<>(numThreads);
            for (long threadStart = start; threadStart < end; threadStart += numsPerThread) {
                final long threadStartf = threadStart, threadEnd = Math.min(threadStart + numsPerThread, end);
                System.out.printf("threadStart: %d threadEnd: %d%n", threadStart, threadEnd);

                final Thread thread = new Thread() {
                    @Override
                    public void run() {
                        final PhoneComparator pc = new PhoneComparator();
                        final byte[] longToWrite = new byte[LongConverter40Bit.instance.numBytes()];
                        for (long l = threadStartf; l < threadEnd; ++l) {
                            final byte[] hash = pc.fastHashReadOnly(l);
                            try {
                                LongConverter40Bit.instance.toBytes(l, longToWrite, 0);
                                osArr[index(hash[0], hash[1])].write(longToWrite);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                };
                threads.add(thread);
                thread.start();
            }

            // wait for threads to finish
            for (final Thread t : threads) {
                t.join();
                System.out.println("thread finished");
            }
            System.out.println("all threads finished");

            for (final OutputStream os : osArr)
                os.close();

        }

        try (OutputStream finalOut = new BufferedOutputStream(new FileOutputStream(new File(path, "final.list")), 2097155)) { // 2 gigs
            final byte[] buf = new byte[LongConverter40Bit.instance.numBytes()];
            final PhoneComparator pc = new PhoneComparator();
            final TreeMap<byte[], Long> phone = new TreeMap<>(new ByteArrayComparator());

            byte a = Byte.MIN_VALUE;
            do {
                final int ia = a + 128;
                final File folder = new File(path, String.format("%03d", ia));
                System.out.println(new Date() + ": starting folder " + ia);
                byte b = Byte.MIN_VALUE;
                do {
                    final int ib = b + 128;
                    //System.out.printf("%d/%d: %03d/%03d: %d%n", a, b, ia, ib, index);
                    final File file = new File(folder, String.format("%03d.list", ib));

                    if (ib % 32 == 0)
                        System.out.println(new Date() + ": starting file " + ib);

                    if (!file.exists()) {
                        ++b;
                        continue;
                    }

                    phone.clear();
                    try (RandomAccessFileList<Long> raf = new RandomAccessFileList<Long>(file, LongConverter40Bit.instance)) {
                        for (final Long l : raf)
                            phone.put(pc.fastHashCopy(l), l);
                    }
                    for (final Long l : phone.values()) {
                        LongConverter40Bit.instance.toBytes(l, buf, 0);
                        finalOut.write(buf);
                    }
                    file.delete();
                    ++b;
                } while (b != Byte.MIN_VALUE);
                //System.out.println(new Date() + ": done with folder "+ia);
                folder.delete();
                ++a;
            } while (a != Byte.MIN_VALUE);

            System.out.println("closing " + new Date());
        }
        System.out.println("written " + new Date());
    }
}
