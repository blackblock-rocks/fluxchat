package me.lucko.gchat.monitoring;

public class ErrorSentry {

    public static void logWarning(String message) {
        System.out.println("[GChat] [WARNING] " + message);
    }

    public static void capture(Throwable e) {
        System.out.println("[GChat] [ERROR] " + e.getMessage());
        e.printStackTrace();
    }

}
