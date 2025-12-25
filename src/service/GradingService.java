package service;

import util.TextUtil;

import java.util.ArrayList;
import java.util.List;

public class GradingService {
    /**
     * Сравнение ответа с эталоном
     */
    public double calculateSimilarity(String userRaw, String correctRaw){
        if (correctRaw == null || userRaw == null) return 0.0;

        List<String> userTokens = TextUtil.tokenize(userRaw);
        List<String> correctTokens = TextUtil.tokenize(correctRaw);

        if (correctRaw.isEmpty()) return 0.0;

        int matches = 0;
        List<String> userPool = new ArrayList<>(userTokens);

        for (String correctWord : correctTokens){
            for (int i=0;i<userPool.size();i++){
                String userWord = userPool.get(i);
                if (isSimilar(correctWord, userWord)){
                    matches++;
                    userPool.remove(i);
                    break;
                }
            }
        }
        return (double) matches / correctTokens.size()*100.0;
    }
    private boolean isSimilar(String a,String b){
        if (a.equals(b)) return true;
        if (Math.abs(a.length()-b.length())>2) return false;
        int limit = a.length()>6?2:1;
        return levenstein(a,b)<=limit;
    }
    private int levenstein(String a,String b){
        int[][] dp = new int[a.length()+1][b.length()+1];
        for (int i = 0; i<=a.length(); i++) dp[i][0]=i;
        for (int j = 0; j<=b.length(); j++) dp[0][j]=j;

        for (int i =1; i<=a.length();i++){
            for (int j=1;j<=b.length();j++){
                int cost = (a.charAt(i-1)==b.charAt(j-1))?0:1;
                dp[i][j]=Math.min(Math.min(
                        dp[i-1][j]+1,
                        dp[i][j-1]+1),
                        dp[i-1][j-1]+cost);
            }
        }
        return dp[a.length()][b.length()];
    }
}
