import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class PseudowordGeneratorV2 {

    public static class CharacterGraph{
        
        public static class CData{
            //chances of each character coming before or aftercurrent character with n distance 
            //pre[n distance (0 is +/- 1)][char c] = chance of c appearing
            //spatial data
            int[][] pre;
            int[][] post;
            char c;

            public CData(char c){
                this.c = c;
                pre = new int[2][SAMPLE_CHARS];
                post = new int[2][SAMPLE_CHARS];
            }
        }

        private static final String WORDS_PATH = "words.txt"; 
        private static final int I = 127;
        private static final int SAMPLE_CHARS = 128;
        private static final int SAMPLE_CHARS_EX = 127;
        

        private CData[] charData;
        
        public CharacterGraph(){
            
            charData = new CData[SAMPLE_CHARS];

            for (int i = 0; i < charData.length; i++) {
                charData[i] = new CData((char)i);
            }

            try {
                Scanner words = new Scanner(new File(WORDS_PATH));

                while(words.hasNext()){        
                    String s = words.next();
                    
                    char[] schars = s.toCharArray();

                    for (int i = 0; i < schars.length; i++) {
                        char c = schars[i];
                        CData cd = charData[c];

                        char pre1 = '\0';
                        char pre2 = '\0';
                        char post1 = '\0';
                        char post2 = '\0';

                        if(i-1 >= 0)
                            pre1 = schars[i-1];
                            
                        if(i-2 >= 0)
                            pre2 = schars[i-2];
                            
                        if(i+1 < schars.length)
                            post1 = schars[i+1];
                            
                        if(i+2 < schars.length)
                            post2 = schars[i+2];
                        
                        // System.out.println("word: " + s + "| " + pre2 + " " + pre1 + " " + c + " " + post1 + " " + post2);
                        // System.out.println("word: " + s + "| " + (int)pre2 + " " + (int)pre1 + " " + (int)c + " " + (int)post1 + " " + (int)post2);

                        cd.pre[0][pre1]+=1;
                        cd.pre[1][pre2]+=1;
                        cd.post[0][post1]+=1;
                        cd.post[1][post2]+=1;

                        // System.out.println("\t\tletter: " + c + "| pre1 " + cd.pre[0][pre1] + " pre2 " + cd.pre[0][pre2] + " post1 " + cd.post[0][post1] + " post2 " + cd.post[0][post2]);
                    }
                }
                
                //set totals
                // int end = 'z'+1;
                // int st = 'a';
                int end = SAMPLE_CHARS_EX;
                int st = 0;
                for (int i = 0; i < charData.length; i++) {
                    CData cd = charData[i];

                    for (int j = st; j < end; j++) {
                        cd.pre[0][I] += cd.pre[0][j];
                        // if(cd.post[0][I] > 0)
                        //     System.out.println("total " + (char)j + " 1 before " + (char)i + " : " + cd.pre[0][I]);
                    }

                    for (int j = st; j < end; j++) {
                        cd.pre[1][I] += cd.pre[1][j];
                        // if(cd.pre[1][I] > 0)
                        //     System.out.println("total " + (char)j + " 2 before " + (char)i + " : " + cd.pre[1][I]);
                    }

                    for (int j = st; j < end; j++) {
                        cd.post[0][I] += cd.post[0][j];
                        // if(cd.post[0][I] > 0)
                        //     System.out.println("total " + (char)j + " 1 after " + (char)i + " : " + cd.post[0][I]);
                    }

                    for (int j = st; j < end; j++) {
                        cd.post[1][I] += cd.post[1][j];
                        // if(cd.post[1][I] > 0)
                        //     System.out.println("total " + (char)j + " 2 after " + (char)i + " : " + cd.post[1][I]);
                    }

                    // System.out.println("totals for char : " + (char)i + "  " + cd.pre[1][I] + "  " + cd.pre[0][I] + "  " + cd.post[0][I] + "  " + cd.post[1][I]);
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        // public static final char deletechar = '~'+1;

        public char nextChar(String context, double guess, double[] accentMap){
            char[] cchars = context.toCharArray();
            int clen = context.length();

            //c1 is closest preceding char
            char c1 = '\0', c2 = '\0';
            if(clen-1 >= 0)
                c1 = cchars[clen-1];
            if(clen-2 >= 0)
                c2 = cchars[clen-2];
                
            double[][] ranges = calcAdjustedRanges(c1, c2, accentMap);
            
            for (int i = 0; i < ranges.length; i++) {
                // System.out.println(guess + "  " + ranges[i][0] + "  " + ranges[i][1]);
                if(guess >= ranges[i][0] && guess < ranges[i][1]){
                    return (char)i;
                }
            }

            return '~';
        }

        private double[][] calcAdjustedRanges(char c1, char c2, double[] accentMap){
            double[][] adjusted = new double[SAMPLE_CHARS][2];

            //0 is lower bound 1 is upper bound

            //get most probable subsequent char of c1 (post[0])
            //get most probable subsequent char of c2 after 2 steps (post[1])
            //add those 2 and apply multiplier if applicable

            //fill result array with accented, but unscaled probability ranges
            double min = 0, max = 0;
            CData c1d = charData[c1];
            CData c2d = charData[c2];
            
            double total1 = c1d.post[0][I];
            double total2 = c2d.post[1][I];
            
            // System.out.println(c1 + "  " + total1 + "   " + c2 + "  " + total2);

            for (int i = 0; i < SAMPLE_CHARS_EX; i++) {
                min = max;

                double chance1 = (c1d.post[0][i] / total1) * ((accentMap[i] >= 0) ? accentMap[i] : 1);
                double chance2 = (c2d.post[1][i] / total2) * ((accentMap[i] >= 0) ? accentMap[i] : 1);
                
                // if((accentMap[i] >= 0)){
                //     System.out.println(i + "  [" + ((c1d.post[0][i] / total1) + "  " + (c2d.post[1][i] / total2)) + "]  [" + chance1 + "  " + chance2 + "]");
                // }

                                                    //hardcoding threshold
                if(!(chance1 == 0 || chance2 == 0 || chance1 < 0.01 || chance2 < 0.01))
                    max += chance1 + chance2;
                
                // if(min != max)
                //     System.out.println("pre adj: " + min + "  " + max);
                // System.out.println("stats: " + c1d.post[0][i] + "  " + c2d.post[1][i]);

                adjusted[i][0] = min;
                adjusted[i][1] = max;
            }

            //scale all ranges to 1
            //total range = max range of last character
            double downscale = adjusted[SAMPLE_CHARS_EX-1][1] / 1;
            // System.out.println("downscale = " + downscale);
            for (int i = 0; i < SAMPLE_CHARS_EX; i++) {
                adjusted[i][0] /= downscale;
                adjusted[i][1] /= downscale;
            }

            return adjusted;
        }
    }

    public final static int RANDOM_LENGTH = (1 << 31);

    private long seed;
    private Random gen;
    private CharacterGraph cgraph;

    public PseudowordGeneratorV2(long seed){
        this.gen = new Random(seed);
        this.seed = seed;
        this.cgraph = new CharacterGraph();
    }

    public String[] next(String root, int count, int maxLength, Object[][] accents){
        String[] res = new String[count];

        double[] accentMap = new double[128];
        Arrays.fill(accentMap, -1);
        for (Object[] b : accents) {
            if((double)b[1] >= 0){
                accentMap[(char)b[0]] = (double)b[1];
            }
        }

        boolean randLen = (maxLength & RANDOM_LENGTH) != 0;
        int maxl = maxLength;
        if(randLen)
            maxl ^= RANDOM_LENGTH;
        maxLength = maxl;

        for (int i = 0; i < count; i++) {
            String cur = root;
            
            if(randLen)
                maxLength = (int)(gen.nextDouble() * (maxl))+1;
                
            while (cur.length() < maxLength) {
                
                char add = cgraph.nextChar(cur, gen.nextDouble(), accentMap);
                cur += add;
                
                if(add == '\0')
                    break;
            }
            
            res[i] = cur;
        }

        return res;
    }

    public static void main(String[] args) throws Exception {
        PseudowordGeneratorV2 wg = new PseudowordGeneratorV2((int)(Math.random()* 1000));

        
        Object[][] hints = new Object[][]{
            // {'\0', 0.5},
            // {'t', 5.0},
            // {'e', 5.0},
            // {'c', 5.0},
            // {'h', 5.0},
        };

        String[] res = wg.next("zoo", 1000, RANDOM_LENGTH | 8, hints);

        for (String s : res) {
            System.out.println(s);
        }
    }
}

/*
 * 
        Object[][] hints = new Object[][]{
            {'\0', 0.1},
            {'a', 0.0},
            {'b', 0.0},
            {'c', 0.0},
            {'d', 0.0},
            {'e', 1.0},
            {'f', 0.0},
            {'g', 0.0},
            {'h', 1.0},
            {'i', 1.0},
            {'j', 0.0},
            {'k', 0.0},
            {'l', 1.0},
            {'m', 0.0},
            {'n', 0.0},
            {'o', 0.0},
            {'p', 1.0},
            {'q', 0.0},
            {'r', 0.0},
            {'s', 0.0},
            {'t', 0.0},
            {'u', 0.0},
            {'v', 0.0},
            {'w', 0.0},
            {'x', 10.0},
            {'y', 1.0},
            {'z', 0.0},
        };
 * 
 * 
 */