package smartmail.platform.utils;

import java.util.Random;

public class Strings {
    public static String getSaltString(int length, boolean letters, boolean uppercase, boolean numbers, boolean specialCharacters) {
        String SALTCHARS = "";
        if (letters) {
            SALTCHARS = SALTCHARS + "abcdefghijklmnopqrstuvwxyz";
            if (uppercase)
                SALTCHARS = SALTCHARS + "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        }
        if (numbers)
            SALTCHARS = SALTCHARS + "1234567890";
        if (specialCharacters)
            SALTCHARS = SALTCHARS + "@\\\\/_*$&-#[](){}";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < length) {
            int index = (int)(rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;
    }

    public static String randomizeCase(String str) {
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(str.length());
        for (char c : str.toCharArray())
            sb.append(rnd.nextBoolean() ? Character.toLowerCase(c) : Character.toUpperCase(c));
        return sb.toString();
    }
}
