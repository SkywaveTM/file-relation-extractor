import command.CommandExecutor;
import gui.CommandBuilderGui;
import utils.Logger;

public class Main {
    public static void main(String... args) throws Exception {
         if (args.length != 0) { // batch mode
            try {
                new CommandExecutor().run(args);
            } catch (Exception e) {
                Logger.INSTANCE.severe(e.getMessage());
                e.printStackTrace();
            }
        } else { // gui mode
            // show gui
            CommandBuilderGui.launch(CommandBuilderGui.class);
        }
    }
}
