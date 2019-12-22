package test;

import java.util.Arrays;
import java.util.List;

import app.*;

public class Main {

    public static void main(String[] args) {
     
        System.out.println("Hello");
        DenGenerator.original(0xD6F535D23B765984L, 1);
    }

    private static boolean equal(byte[] b, byte[] c) {
        if (b == null || c == null) {
            return b == c;
        } else if (b.length == c.length) {
            boolean result = true;
            for (int i = 0; i < b.length; i++) {
                result = b[i] == c[i];
                if (!result)
                    break;
            }
            return result;
        } else {
            return false;
        }
    }
}