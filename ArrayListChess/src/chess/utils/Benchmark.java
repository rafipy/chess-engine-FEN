package chess.utils;

public class Benchmark {
    public static void getTime(long startTime, long endTime) {
        //get runtime by substracting endtime with startime
        double nanoSeconds = endTime - startTime;

        //convert it to miliseconds
        double milliSeconds = nanoSeconds/1000000;

        //print the result
        System.out.println("Time used: " + milliSeconds + " milliseconds");
        System.out.println();
    }

    public static void getSpace(){
        //get space by first access the total memory and free memory
        Runtime rt = Runtime.getRuntime();
        long total_mem = rt.totalMemory();
        long free_mem = rt.freeMemory();
        //count the used memory
        long used_mem = total_mem - free_mem;

        //print the result
        String str = String.format("%,d", used_mem);
        System.out.println("Amount of used memory: " + str);
    }
}

