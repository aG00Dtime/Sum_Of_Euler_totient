import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

public class TotientRange implements Runnable {
    public int rangestart, rangeend;
    public AtomicLong totalSum;

    public TotientRange(AtomicLong total, int rangeStart, int rangeEnd) {

        this.rangestart = rangeStart;
        this.rangeend = rangeEnd;
        this.totalSum = total;
    }

    long hcf(long x, long y) {

        long t;
        while (y != 0) {
            t = x % y;
            x = y;
            y = t;
        }
        return x;
    }

    boolean relprime(long x, long y) {
        return hcf(x, y) == 1;

    }

    // this could also be threaded but :/
    long euler(long n) {

        long length, i;
        length = 0;
        for (i = 1; i <= n; i++)
            if (relprime(n, i))
                length++;

        return length;
    }

    // thread this
    long sumTotient(long lower, long upper) {
        long sum, i;
        sum = 0;

        for (i = lower; i <= upper; i++)
            sum = sum + euler(i);
        return sum;
    }

    // run method to execute the sum method when a new runnable is constructed
    @Override
    public void run() {
        // System.out.println("-Processing Integers from :" + this.rangestart + "-" +
        // this.rangeend);

        // get current sum and add to it
        totalSum.getAndAdd(sumTotient(this.rangestart, this.rangeend));
    }

    // split the array into chucks for the threads to process
    public static int[][] splitArray(int[] inputArray, int chunkSize) {
        return IntStream.iterate(0, i -> i + chunkSize)
                .limit((int) Math.ceil((double) inputArray.length / chunkSize))
                .mapToObj(j -> Arrays.copyOfRange(inputArray, j, Math.min(inputArray.length, j + chunkSize)))
                .toArray(int[][]::new);
    }

    // main
    public static void main(String[] args) throws ExecutionException, InterruptedException {

        long lower = 0;
        long upper = 0;

        try {

            lower = Long.parseLong(args[0]);
            upper = Long.parseLong(args[1]);

        } catch (Exception e) {

            System.out.println("Error (lower: " + lower + ", upper: " + upper + ")");

            return;
        }

        // max threads in the pool
        int[] threadsCounts = new int[] { 8, 16, 32, 64 };

        // compute for all thread counts
        for (int threadCount : threadsCounts) {

            System.out.println("Max Threads:" + threadCount);

            // atomic long to share total among threads
            AtomicLong tot = new AtomicLong();

            int[] arrayList = IntStream.rangeClosed((int) lower, (int) upper).toArray();

            // split the list into equal sizes
            int chunkSize = arrayList.length / threadCount;

            // Split the list in chunks
            int[][] arraySplit = splitArray(arrayList, chunkSize);

            // create future list of threads to check if they're still running
            // https://stackoverflow.com/questions/33845405/how-to-check-if-all-tasks-running-on-executorservice-are-completed
            List<Future<?>> futures = new ArrayList<>();

            // https://www.javatpoint.com/java-thread-pool
            ExecutorService exec = Executors.newFixedThreadPool(threadCount);

            long startTime = System.nanoTime();

            for (int[] array : arraySplit) {
                // this is a bit inefficient but it works
                int l = array[0];
                int u = array[array.length - 1];

                // create runnable tasks and add to executor pool
                Runnable t = new TotientRange(tot, l, u);
                Future<?> f = exec.submit(t);
                futures.add(f);
            }

            // wait for all runnables to be done
            for (Future<?> future : futures)
                future.get(); // get will block until the future is done

            // shutdown exec after all threads are finished running
            exec.shutdown();

            // total is the final sum
            System.out.println("Totient Sum :" + tot);

            long endTime = System.nanoTime();

            long duration = ((endTime - startTime) / 1000000) / 1000;

            System.out.println("Time Taken on " + threadCount + " Threads : " + +duration + " seconds\n");

        }
    }
}