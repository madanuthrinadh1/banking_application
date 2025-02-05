package utils;

public class ColorCodes {

    public static final String RESET = "\u001B[0m";
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    public static final String BOLD = "\u001B[1m";
    public static final String UNDERLINE = "\u001B[4m";
    public static final String ITALIC = "\u001B[3m";

    public static String colorize(String text, String colorCode) {
        return colorCode + text + RESET;
    }

    public static String bold(String text) {
        return BOLD + text + RESET;
    }

    public static String underline(String text) {
        return UNDERLINE + text + RESET;
    }

    public static String italic(String text) {
        return ITALIC + text + RESET;
    }

    public static String boldColorize(String text, String colorCode) {
        return BOLD + colorCode + text + RESET;
    }

    public static String underlineColorize(String text, String colorCode) {
        return UNDERLINE + colorCode + text + RESET;
    }

    public static String italicColorize(String text, String colorCode) {
        return ITALIC + colorCode + text + RESET;
    }
}