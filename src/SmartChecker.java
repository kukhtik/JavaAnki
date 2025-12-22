import java.util.*;
import java.util.stream.Collectors;

public class SmartChecker {
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "и","в","на","с","для","а","но","to","the","of","is","are","что","это","как"
    ));

    public static double check(String userRaw, String correctRaw) {
        if (correctRaw == null || userRaw == null) return 0.0;
        List<String> userTokens = tokenize(userRaw);
        List<String> correctTokens = tokenize(correctRaw);
        if (correctTokens.isEmpty()) return 0.0;

        int matches = 0;
        List<String> userPool = new ArrayList<>(userTokens);
        for(String correctWord : correctTokens) {
            Iterator<String> it = userPool.iterator();
            while(it.hasNext()){
                String userWord = it.next();
                if(isSimilar(correctWord, userWord)) { matches++; it.remove(); break; }
            }
        }
        return (double)matches / correctTokens.size() * 100.0;
    }

    private static List<String> tokenize(String t) {
        return Arrays.stream(t.toLowerCase().replaceAll("[^a-zа-я0-9]", " ").split("\\s+"))
                .filter(w -> w.length() > 1 && !STOP_WORDS.contains(w))
                .collect(Collectors.toList());
    }

    private static boolean isSimilar(String a, String b) {
        if(a.equals(b)) return true;
        if(Math.abs(a.length() - b.length()) > 2) return false;
        return levenshtein(a,b) <= (a.length() > 6 ? 2 : 1);
    }

    private static int levenshtein(String a, String b) {
        int[][] dp = new int[a.length()+1][b.length()+1];
        for(int i=0;i<=a.length();i++) dp[i][0]=i;
        for(int j=0;j<=b.length();j++) dp[0][j]=j;
        for(int i=1;i<=a.length();i++) {
            for(int j=1;j<=b.length();j++) {
                int cost = (a.charAt(i-1) == b.charAt(j-1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i-1][j]+1, dp[i][j-1]+1), dp[i-1][j-1]+cost);
            }
        }
        return dp[a.length()][b.length()];
    }
}