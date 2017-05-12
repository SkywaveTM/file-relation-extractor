package utils;

import java.io.PrintStream;
import java.util.logging.*;

public class Logger {
    public static java.util.logging.Logger INSTANCE = java.util.logging.Logger.getGlobal();

    static {
        // remove default handler
        LogManager.getLogManager().reset();
        // INSTANCE.setLevel(Level.CONFIG);

        // Capture Standard outputs to the logger.
        // This is required because arguments options are printed only through the standard output.
        System.setOut(new PrintStream(System.out) {
            @Override
            public void print(String s) {
                for (String split : s.split("\n")) {
                    Logger.INSTANCE.info(split);
                }

                super.print(s);
            }
        });

        // Capture Standard errors to the logger.
        // This is required because of stack traces.
        System.setErr(new PrintStream(System.err) {
            @Override
            public void print(String s) {
                for (String split : s.split("\n")) {
                    Logger.INSTANCE.config(split);
                }

                super.print(s);
            }
        });

        // redirect log messages to the Standard output.
        Logger.INSTANCE.addHandler(new StreamHandler(System.out, new Formatter() {
            @Override
            public String format(LogRecord record) {
                return formatLogRecord(record) + "\n";
            }
        }));
    }

    public static String formatLogRecord(LogRecord log) {
        String prefix = "";
        Level level = log.getLevel();

        if (level.intValue() >= Level.WARNING.intValue() || level == Level.CONFIG) {
            prefix = String.format("<%s> ", level.getName().toUpperCase());
        }

        return prefix + log.getMessage();
    }
}
