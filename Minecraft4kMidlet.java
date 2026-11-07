import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;


public class Minecraft4kMidlet extends MIDlet {
    private Game game;
    private MenuScreen menu;

    protected void startApp() {
        if (game != null) {
            game.setPaused(false);
            return;
        }
        if (menu == null) menu = new MenuScreen(this);
        Display.getDisplay(this).setCurrent(menu);
    }

    protected void pauseApp() {
        if (game != null) game.setPaused(true);
    }

    protected void destroyApp(boolean unconditional) {
        if (game != null) game.stop();
    }

    
    void startGame(int slot, boolean fresh) {
        World.selectWorld(slot, fresh);
        game = new Game(this);
        Display.getDisplay(this).setCurrent(game);
        new Thread(game).start();
    }

    
    void showMenu() {
        if (game == null) return;   
        game.stop();
        game = null;
        menu = new MenuScreen(this);
        Display.getDisplay(this).setCurrent(menu);
    }

    void editSign(final int x, final int y, final int z) {
        final TextBox box = new TextBox("Sign text", World.signText(x, y, z), 60, TextField.ANY);
        box.addCommand(new Command("Save", Command.OK, 1));
        box.addCommand(new Command("Cancel", Command.CANCEL, 2));
        box.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                if (c.getCommandType() == Command.OK) World.setSignText(x, y, z, box.getString());
                if (game != null) Display.getDisplay(Minecraft4kMidlet.this).setCurrent(game);
            }
        });
        Display.getDisplay(this).setCurrent(box);
    }

    void exit() {
        destroyApp(true);
        notifyDestroyed();
    }
}