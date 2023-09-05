import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class PseudowordGeneratorV1 {

    public static class CharacterGraph{
        private static int[][] graph = new int[128][128];
        private static final String WORDS_PATH = "words.txt"; 
        private static final int I = 127;

        static {
            try {
                Scanner words = new Scanner(new File(WORDS_PATH));

                while(words.hasNext()){        
                    String s = words.next();
                    char prev = '\0';

                    for (char c : s.toCharArray()) {
                        graph[prev][Character.toLowerCase(c)]++;
                        prev = c;
                    }
                }
                
                //word index
                for (int i = 0; i < graph.length; i++) {
                    //possible next char probability count
                    for (int j = 1; j < graph[0].length-1; j++) {
                        graph[i][I] += graph[i][j];
                    }
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        public static char nextChar(char prev, double guess, double accentPercentInc, boolean[] accentMap){
            double t = graph[prev][I];
            double min = 0, max = 0;
            
            for (int i = 'a'; i < ('z'+1); i++) {
                min = max;
                max += graph[prev][i] / t;
                
                // System.out.println(min + "  " + guess + "  " + max);
                
                if(accentMap[i]){
                    double spread = (max - min) * accentPercentInc; 
                    double mins = min - spread/2;
                    double maxs = max + spread/2;
                    
                    //distribute overflow
                    if(mins < 0){
                        maxs += (-mins);
                        mins = 0;
                    }

                    if(maxs >= 1){
                        mins -= (maxs - 1);
                        maxs = 1;
                    }
                    
                    // System.out.println((max) + "  " + min + "  " + maxs + "  " + mins);

                    if(guess >= (mins) && guess < (maxs)){
                        return (char)i;
                    }
                }
                else if(guess >= (min) && guess < (max)){
                    return (char)i;
                }
            }

            return 'E';
        }
    }

    public static int RANDOM_LENGTH = -1;

    private long seed;
    private Random gen;

    public PseudowordGeneratorV1(long seed){
        gen = new Random(seed);
        this.seed = seed;
    }

    public String[] next(int count, int len, double accentPercentInc, String accentChars){
        String[] res = new String[count];

        boolean[] accentMap = new boolean[128];
        for (char b : accentChars.toCharArray()) {
            accentMap[b] = true;
        }

        for (int i = 0; i < count; i++) {
            int len0 = len;
            if(len0 == RANDOM_LENGTH){
                len0 = gen.nextInt(15) + 2;
            }

            char prev = '\0';
            String cur = "";

            for (int j = 0; j < len0; j++) {
                cur += (prev = CharacterGraph.nextChar(prev, gen.nextDouble(), accentPercentInc, accentMap));
            }

            res[i] = cur;
        }

        return res;
    }

    public static void main(String[] args) throws Exception {

        System.out.println("\0".length());

        // PseudowordGeneratorV1 wg = new PseudowordGeneratorV1((int)(Math.random()* 1000));
        // String[] res = wg.next(1000, RANDOM_LENGTH, 5, "d");

        // for (String s : res) {
        //     System.out.println(s);
        // }
    }
}