package me.zort.commons.common;

import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

public final class Strings {

    public static final String STRAIGHT_LINE_SMALLER = "❘";
    public static final String L_BRACKET = "(";
    public static final String R_BRACKET = ")";
    public static final String NEW_LINE = "\n";
    public static final String SPACE = " ";
    public static final String EMPTY = "";

    public static boolean containsAll(String string, String... parts) {
        AtomicBoolean atomicBoolean = new AtomicBoolean(true);
        Arrays.stream(parts).forEach(part -> {
            if(!string.contains(part)) atomicBoolean.set(false);
        });
        return atomicBoolean.get();
    }

    public static String replaceAllOnce(String string, String replacement, String... placeholders) {
        AtomicReference<String> atomicString = new AtomicReference<>(string);
        Arrays.stream(placeholders).forEach(placeholder -> atomicString.set(atomicString.get().replaceAll(placeholder, replacement)));
        return atomicString.get();
    }

    public static List<String> makeBukkitBlock(String string, int averageRowSize) {
        return string.contains("&")
                ? continueSpecialChars(makeBlock(string, averageRowSize), "&", 1)
                : continueSpecialChars(makeBlock(string, averageRowSize), "§", 1);
    }

    public static String continueSpecialChars(String completeText, String part, String specialChar, int length) {
        if(completeText.startsWith(part)) return part;
        return continueSpecialChars(new String[] {completeText.split(part)[0], part}, specialChar, length)[1];
    }

    public static String deBukkit(String s) {
        List<String> specialChars = matchSpecialChars(s, '§', 1);
        List<String> specialChars2 = matchSpecialChars(s, '&', 1);
        for(String s1 : specialChars) {
            s = s.replaceAll(s1, Strings.EMPTY);
        }
        for(String s2 : specialChars2) {
            s = s.replaceAll(s2, Strings.EMPTY);
        }
        return s;
    }

    public static List<String> matchSpecialChars(String s, char specialChar, int length) {
        if(s.length() == 0) return Lists.newArrayList();
        List<String> res = Lists.newArrayList();
        for(int i = 0; i < s.length(); i++) {
            char current = s.charAt(i);
            if(current == specialChar) {
                res.add(s.substring(i, Math.min(i + length + 1, s.length())));
            }
        }
        return res;
    }

    public static List<String> continueSpecialChars(List<String> lines, String specialChar, int length) {
        IntStream.rangeClosed(0, lines.size()).forEach(i -> {
            if(i < lines.size() - 1) {
                String line = lines.get(i);
                if(line.contains(specialChar)) {
                    String[] subParts = line.split(specialChar);
                    String subPart = subParts[subParts.length - 1];
                    String preAddon = EMPTY;
                    String addon = subPart.substring(0, length);
                    if(subParts.length > 1) {
                        String befSubPart = subParts[subParts.length - 2];
                        if(befSubPart.length() == 1) {
                            preAddon = specialChar + befSubPart;
                        }
                    }
                    lines.set(i + 1, preAddon + specialChar + addon + lines.get(i + 1));
                }
            }
        });
        return lines;
    }

    public static String[] continueSpecialChars(String[] lines, String specialChar, int length) {
        IntStream.rangeClosed(0, lines.length).forEach(i -> {
            if(i < lines.length - 1) {
                String line = lines[i];
                if(line.contains(specialChar)) {
                    String[] subParts = line.split(specialChar);
                    String subPart = subParts[subParts.length - 1];
                    String preAddon = EMPTY;
                    String addon = subPart.substring(0, length);
                    if(subParts.length > 1) {
                        String befSubPart = subParts[subParts.length - 2];
                        if(befSubPart.length() == 1) {
                            preAddon = specialChar + befSubPart;
                        }
                    }
                    lines[i + 1] = preAddon + specialChar + addon + lines[i + 1];
                }
            }
        });
        return lines;
    }

    public static List<String> makeBlock(String string, int averageRowSize) {
        if(!string.contains(SPACE)) return Lists.newArrayList(string);
        String prefix = null;
        String suffix = null;
        if(string.startsWith(SPACE)) {
            string = string.replaceFirst(SPACE, EMPTY);
            prefix = SPACE;
        }
        if(string.endsWith(SPACE)) {
            string = string.substring(0, string.length() - 1);
            suffix = SPACE;
        }
        List<String> result = Lists.newArrayList();
        String[] words = string.split(SPACE);
        StringBuilder currentRow = null;
        boolean hasBeenBigger = false;
        for(String word : words) {
            if (hasBeenBigger) {
                result.add(currentRow.toString());
                hasBeenBigger = false;
                currentRow = null;
            }
            if (currentRow == null) {
                currentRow = new StringBuilder(word);
                continue;
            }
            currentRow.append(SPACE).append(word);
            hasBeenBigger = currentRow.length()
                    >= averageRowSize;
        }
        if(currentRow != null) result.add(currentRow.toString());
        result.set(0, (
                prefix != null
                ? prefix
                : EMPTY) + result.get(0)
        );
        result.set(result.size() - 1, result.get(result.size() - 1) + (
                suffix != null
                ? suffix
                : EMPTY
        ));
        return result;
    }

}
