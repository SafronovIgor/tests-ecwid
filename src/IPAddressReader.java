import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class IPAddressReader {
    private static final int CHUNK_SIZE = 10_000;

    public static void main(String[] args) throws IOException {
        long startTime = System.currentTimeMillis();
        new IPAddressReader().processFile();
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        System.out.println("Total execution time: " + totalTime + " milliseconds");
    }

    //The best thing to do is to do it in parallel.
    private void processFile() throws IOException {
        AtomicLong totalCount = new AtomicLong(0);
        AtomicLong uniqueCount = new AtomicLong(0);

        try (BufferedReader reader = new BufferedReader(new FileReader("ip_addresses.txt"))) {
            List<String> chunk = new ArrayList<>(CHUNK_SIZE);
            BitSet ipSet = new BitSet();

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    chunk.add(line);
                    totalCount.incrementAndGet();
                }

                if (chunk.size() == CHUNK_SIZE) {
                    processChunk(chunk, ipSet, uniqueCount);
                    chunk.clear();
                }
            }
            if (!chunk.isEmpty()) {
                processChunk(chunk, ipSet, uniqueCount);
            }
        }

        System.out.println("Total IP addresses read: " + totalCount);
        System.out.println("Unique IP addresses: " + uniqueCount);
    }

    private void processChunk(List<String> chunk, BitSet ipSet, AtomicLong uniqueCount) {
        for (String line : chunk) {
            int ipAsInt = ipToInt(line);
            if (!ipSet.get(ipAsInt)) {
                ipSet.set(ipAsInt);
                uniqueCount.incrementAndGet();
            }
        }
    }

    public static int ipToInt(String ipAddress) {
        return Integer.parseInt(ipAddress.replace(".", ""));
    }
}