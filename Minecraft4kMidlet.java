import javax.microedition.lcdui.Display;
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

    
    void startGame(boolean fresh) {
        if (fresh) World.deleteSave();
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

    void exit() {
        destroyApp(true);
        notifyDestroyed();
    }
}