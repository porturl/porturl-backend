package org.friesoft.porturl.util;

import java.util.Comparator;

/**
 * A comparator that sorts strings naturally, taking into account numeric values within the strings.
 * It is case-insensitive.
 * e.g., "App 2" comes before "App 10".
 */
public class NaturalOrderComparator implements Comparator<String> {

    @Override
    public int compare(String s1, String s2) {
        if (s1 == null) return s2 == null ? 0 : -1;
        if (s2 == null) return 1;

        int i1 = 0;
        int i2 = 0;

        while (i1 < s1.length() && i2 < s2.length()) {
            char c1 = s1.charAt(i1);
            char c2 = s2.charAt(i2);

            if (Character.isDigit(c1) && Character.isDigit(c2)) {
                String n1 = extractNumber(s1, i1);
                String n2 = extractNumber(s2, i2);

                int comp = compareNumbers(n1, n2);
                if (comp != 0) return comp;

                i1 += n1.length();
                i2 += n2.length();
            } else {
                int comp = Character.compare(Character.toLowerCase(c1), Character.toLowerCase(c2));
                if (comp != 0) return comp;
                i1++;
                i2++;
            }
        }

        return Integer.compare(s1.length(), s2.length());
    }

    private String extractNumber(String s, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                sb.append(c);
            } else {
                break;
            }
        }
        return sb.toString();
    }

    private int compareNumbers(String n1, String n2) {
        // Remove leading zeros for comparison
        String s1 = n1.replaceFirst("^0+(?!$)", "");
        String s2 = n2.replaceFirst("^0+(?!$)", "");

        if (s1.length() != s2.length()) {
            return Integer.compare(s1.length(), s2.length());
        }
        return s1.compareTo(s2);
    }
}
