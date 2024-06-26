package rocks.blackblock.fluxchat.monitoring;

public class ErrorSentry {

    public static void logWarning(String message) {
        System.out.println("[FluxChat] [WARNING] " + message);
    }

    public static void capture(Throwable e) {
        System.out.println("[FluxChat] [ERROR] " + e.getMessage());
        e.printStackTrace();
    }

}
