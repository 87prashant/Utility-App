package com.util.util;

import android.os.Build;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Common {
    private static Random random;
    public static String getRandomDigits(int digits){
        if(random == null)
            random = new Random();
        StringBuilder result = new StringBuilder();
        System.out.println("================ digits1: " + digits);
        while(digits > 0) {
            int currentDigit = digits;
            // to handle integer range
            if(digits > 9) {
                currentDigit = 9;
                digits -= currentDigit;
            }
            else
                digits = 0;
            int min = (int) Math.pow(10, currentDigit - 1);
            int max = (int) Math.pow(10, currentDigit) - 1;
            result.append(String.valueOf(min + random.nextInt(max - min + 1)));
        }
        return result.toString();
    }

    public static String getRandomString(int digits) {
        String allowedChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

        // Use StringBuilder to efficiently build the string
        StringBuilder randomStringBuilder = new StringBuilder(digits);

        if(random == null)
            random = new Random();
        // Generate the random string
        for (int i = 0; i < digits; i++) {
            int randomIndex = random.nextInt(allowedChars.length());
            char randomChar = allowedChars.charAt(randomIndex);
            randomStringBuilder.append(randomChar);
        }

        return randomStringBuilder.toString();
    }

    public static String replacePlaceholderWithNumberStr(String original, String searchString, int digits) {
        Pattern pattern = Pattern.compile(searchString);
        Matcher matcher = pattern.matcher(original);
        String result = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            result = matcher.replaceAll(m -> getRandomDigits(digits));
        }
        return result;
    }

    public static String replacePlaceholderWithCharStr(String original, String searchString, int digits) {
        Pattern pattern = Pattern.compile(searchString);
        Matcher matcher = pattern.matcher(original);
        String result = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            result = matcher.replaceAll(m -> getRandomString(digits));
        }
        return result;
    }
}
