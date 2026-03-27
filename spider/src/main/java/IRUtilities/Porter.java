package IRUtilities;

import java.util.Locale;

/**
 * Porter stemming algorithm (English).
 * Included to align with COMP4321 lab 3 materials.
 */
public final class Porter {
    private char[] buffer = new char[64];
    private int length = 0;

    public Porter() {}

    public String stem(String word) {
        if (word == null) {
            return "";
        }
        String w = word.toLowerCase(Locale.ROOT).strip();
        if (w.length() <= 2) {
            return w;
        }
        reset(w.length());
        for (int i = 0; i < w.length(); i++) {
            add(w.charAt(i));
        }
        stemInPlace();
        return new String(buffer, 0, length);
    }

    private void reset(int capacity) {
        if (buffer.length < capacity) {
            buffer = new char[Math.max(capacity, buffer.length * 2)];
        }
        length = 0;
    }

    private void add(char ch) {
        buffer[length++] = ch;
    }

    private boolean isConsonant(int i) {
        char ch = buffer[i];
        switch (ch) {
            case 'a':
            case 'e':
            case 'i':
            case 'o':
            case 'u':
                return false;
            case 'y':
                return (i == 0) || !isConsonant(i - 1);
            default:
                return true;
        }
    }

    private int measure(int end) {
        int m = 0;
        int i = 0;
        while (true) {
            if (i > end) {
                return m;
            }
            if (!isConsonant(i)) {
                break;
            }
            i++;
        }
        i++;
        while (true) {
            while (true) {
                if (i > end) {
                    return m;
                }
                if (isConsonant(i)) {
                    break;
                }
                i++;
            }
            i++;
            m++;
            while (true) {
                if (i > end) {
                    return m;
                }
                if (!isConsonant(i)) {
                    break;
                }
                i++;
            }
            i++;
        }
    }

    private boolean vowelInStem(int end) {
        for (int i = 0; i <= end; i++) {
            if (!isConsonant(i)) {
                return true;
            }
        }
        return false;
    }

    private boolean doubleConsonant(int i) {
        if (i < 1) {
            return false;
        }
        if (buffer[i] != buffer[i - 1]) {
            return false;
        }
        return isConsonant(i);
    }

    private boolean cvc(int i) {
        if (i < 2 || !isConsonant(i) || isConsonant(i - 1) || !isConsonant(i - 2)) {
            return false;
        }
        char ch = buffer[i];
        return ch != 'w' && ch != 'x' && ch != 'y';
    }

    private boolean endsWith(String s) {
        int slen = s.length();
        if (slen > length) {
            return false;
        }
        int offset = length - slen;
        for (int i = 0; i < slen; i++) {
            if (buffer[offset + i] != s.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private void setTo(String s) {
        int slen = s.length();
        int offset = length - slen;
        for (int i = 0; i < slen; i++) {
            buffer[offset + i] = s.charAt(i);
        }
    }

    private void replaceSuffix(String suffix, String replacement) {
        if (!endsWith(suffix)) {
            return;
        }
        int newLen = length - suffix.length() + replacement.length();
        if (buffer.length < newLen) {
            char[] n = new char[Math.max(newLen, buffer.length * 2)];
            System.arraycopy(buffer, 0, n, 0, length);
            buffer = n;
        }
        int start = length - suffix.length();
        for (int i = 0; i < replacement.length(); i++) {
            buffer[start + i] = replacement.charAt(i);
        }
        length = newLen;
    }

    private void step1() {
        if (endsWith("sses")) {
            length -= 2;
        } else if (endsWith("ies")) {
            replaceSuffix("ies", "i");
        } else if (endsWith("ss")) {
            // keep
        } else if (endsWith("s")) {
            length -= 1;
        }

        if (endsWith("eed")) {
            int end = length - 4;
            if (measure(end) > 0) {
                length -= 1;
            }
        } else if ((endsWith("ed") && vowelInStem(length - 3)) || (endsWith("ing") && vowelInStem(length - 4))) {
            if (endsWith("ed")) {
                length -= 2;
            } else {
                length -= 3;
            }

            if (endsWith("at") || endsWith("bl") || endsWith("iz")) {
                add('e');
            } else if (doubleConsonant(length - 1)) {
                char ch = buffer[length - 1];
                if (ch != 'l' && ch != 's' && ch != 'z') {
                    length -= 1;
                }
            } else if (measure(length - 1) == 1 && cvc(length - 1)) {
                add('e');
            }
        }

        if (endsWith("y") && vowelInStem(length - 2)) {
            buffer[length - 1] = 'i';
        }
    }

    private void step2() {
        String[][] rules = {
                {"ational", "ate"},
                {"tional", "tion"},
                {"enci", "ence"},
                {"anci", "ance"},
                {"izer", "ize"},
                {"abli", "able"},
                {"alli", "al"},
                {"entli", "ent"},
                {"eli", "e"},
                {"ousli", "ous"},
                {"ization", "ize"},
                {"ation", "ate"},
                {"ator", "ate"},
                {"alism", "al"},
                {"iveness", "ive"},
                {"fulness", "ful"},
                {"ousness", "ous"},
                {"aliti", "al"},
                {"iviti", "ive"},
                {"biliti", "ble"},
        };
        for (String[] r : rules) {
            String suf = r[0];
            if (endsWith(suf)) {
                int end = length - suf.length() - 1;
                if (measure(end) > 0) {
                    replaceSuffix(suf, r[1]);
                }
                return;
            }
        }
    }

    private void step3() {
        String[][] rules = {
                {"icate", "ic"},
                {"ative", ""},
                {"alize", "al"},
                {"iciti", "ic"},
                {"ical", "ic"},
                {"ful", ""},
                {"ness", ""},
        };
        for (String[] r : rules) {
            String suf = r[0];
            if (endsWith(suf)) {
                int end = length - suf.length() - 1;
                if (measure(end) > 0) {
                    replaceSuffix(suf, r[1]);
                }
                return;
            }
        }
    }

    private void step4() {
        String[] suffixes = {
                "al", "ance", "ence", "er", "ic", "able", "ible", "ant", "ement", "ment", "ent",
                "ion", "ou", "ism", "ate", "iti", "ous", "ive", "ize"
        };
        for (String suf : suffixes) {
            if (!endsWith(suf)) {
                continue;
            }
            int end = length - suf.length() - 1;
            if (suf.equals("ion")) {
                if (end < 0) {
                    return;
                }
                char ch = buffer[end];
                if (ch != 's' && ch != 't') {
                    return;
                }
            }
            if (measure(end) > 1) {
                length -= suf.length();
            }
            return;
        }
    }

    private void step5() {
        int end = length - 2;
        if (endsWith("e")) {
            int m = measure(end);
            if (m > 1 || (m == 1 && !cvc(end))) {
                length -= 1;
            }
        }
        if (measure(length - 1) > 1 && doubleConsonant(length - 1) && buffer[length - 1] == 'l') {
            length -= 1;
        }
    }

    private void stemInPlace() {
        step1();
        step2();
        step3();
        step4();
        step5();
    }
}

