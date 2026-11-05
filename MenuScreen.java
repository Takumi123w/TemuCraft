import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

final class MenuScreen extends Canvas implements CommandListener {
    private static final int GAP = 4;

    private final Minecraft4kMidlet midlet;
    private final Command selectCmd = new Command("Select", Command.OK, 1);
    private final boolean canContinue;

    
    private int mode;
    private int sel;
    
    private int scroll;

    
    private int pressIdx = -1;
    private int pressY;
    private int lastY;
    private boolean dragging;

    
    private int[] dirtBg;
    private int bgW, bgH;

    private final Font titleFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_LARGE);
    private final Font itemFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
    private final Font hintFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);

private static final int[] FOVS = { 50, 60, 70, 80, 90, 100 };
private static final int[] TEXD = { 8, 16, 24, 32, 48, 9999 };
private static final int[] FPSV = { 15, 20, 30, 45, 60, 0 };

private static int next(int[] vals, int cur) {
    for (int i = 0; i < vals.length; i++) {
        if (vals[i] == cur) return vals[(i + 1) % vals.length];
    }
    return vals[0];
}
    
    MenuScreen(Minecraft4kMidlet midlet) {
        this.midlet = midlet;
        World.loadOptions();
        canContinue = World.hasSave();
        addCommand(selectCmd);
        setCommandListener(this);
    }

    private String[] items() {
        if (mode == 1) {
            return new String[] {
                "Scenery: " + (World.sceneryOn ? "ON" : "OFF"),
                "Generation: " + (World.instantGen ? "Full" : "Light"),
                "Debug Info: " + (Game.showInfo ? "ON" : "OFF"),
                "FOV: " + M3GRenderer.fov,
                "Tex radius: " + (M3GRenderer.texDist >= 9999 ? "All" : String.valueOf(M3GRenderer.texDist)),
                "FPS limit: " + (Game.targetFps == 0 ? "Max" : String.valueOf(Game.targetFps)),
                "Done"
            };
        }
        return new String[] { canContinue ? "Continue" : "Play", "New world", "Settings", "Quit Game" };
    }

    

    private int bw() {
        return getWidth() * 3 / 4;
    }

    private int bh() {
        return itemFont.getHeight() + 8;
    }

    private int bx() {
        return (getWidth() - bw()) / 2;
    }

    private int top() {
        return getHeight() * 5 / 12;
    }

    private int rowH() {
        return bh() + GAP;
    }

    
    private int visible() {
        int avail = getHeight() - top() - (hintFont.getHeight() + 6);
        int v = avail / rowH();
        return v < 1 ? 1 : v;
    }

    private int maxScroll() {
        return Math.max(0, items().length - visible());
    }

    private void clampScroll() {
        if (scroll > maxScroll()) scroll = maxScroll();
        if (scroll < 0) scroll = 0;
    }

    
    private void keepSelVisible() {
        int v = visible();
        if (sel < scroll) scroll = sel;
        if (sel >= scroll + v) scroll = sel - v + 1;
        clampScroll();
    }

    private int btnY(int i) {
        return top() + (i - scroll) * rowH();
    }

    

    protected void paint(Graphics g) {
        int W = getWidth(), H = getHeight();
        if (dirtBg == null || bgW != W || bgH != H) buildDirt(W, H);
        g.drawRGB(dirtBg, 0, W, 0, 0, W, H, false);

        
        g.setFont(titleFont);
        int logoY = H / 10;
        int lw = titleFont.stringWidth("TemuCraft");
        int lx = W / 2 - lw / 2;
        g.setColor(0x2B1A00);
        g.drawString("TemuCraft", lx + 2, logoY + 2, Graphics.TOP | Graphics.LEFT);
        g.setColor(0xFFD23A);
        g.drawString("TemuCraft", lx, logoY, Graphics.TOP | Graphics.LEFT);

        
        g.setFont(hintFont);
        g.setColor(0xFFFF55);
        g.drawString("Dig deep!", lx + lw - 6, logoY + titleFont.getHeight() - 4, Graphics.TOP | Graphics.LEFT);

        if (mode == 1) {
            drawShadowed(g, "Options", W / 2, logoY + titleFont.getHeight() + 8, 0xFFFFFF, 0x3F3F3F, 1,
                    Graphics.TOP | Graphics.HCENTER);
        }

        
        String[] items = items();
        keepSelVisible();
        g.setFont(itemFont);
        int w = bw(), h = bh(), x = bx();
        int end = Math.min(items.length, scroll + visible());
        for (int i = scroll; i < end; i++) {
            int y = btnY(i);
            boolean on = i == sel;
            g.setColor(on ? 0x8F8FB2 : 0x6E6E6E);
            g.fillRect(x, y, w, h);
            g.setColor(on ? 0xD8D8F0 : 0x9E9E9E);
            g.fillRect(x, y, w, 2);
            g.fillRect(x, y, 2, h);
            g.setColor(on ? 0x4A4A66 : 0x3A3A3A);
            g.fillRect(x, y + h - 2, w, 2);
            g.fillRect(x + w - 2, y, 2, h);
            g.setColor(0x000000);
            g.drawRect(x, y, w - 1, h - 1);

            drawShadowed(g, items[i], W / 2, y + (h - itemFont.getHeight()) / 2,
                    on ? 0xFFFFA0 : 0xE0E0E0, 0x3F3F3F, 1, Graphics.TOP | Graphics.HCENTER);
        }

        
        g.setFont(hintFont);
        if (scroll > 0) {
            drawShadowed(g, "^", W / 2, top() - hintFont.getHeight() - 2, 0xFFFFFF, 0x3F3F3F, 1,
                    Graphics.TOP | Graphics.HCENTER);
        }
        if (end < items.length) {
            drawShadowed(g, "v", W / 2, btnY(end - 1) + rowH() - 2, 0xFFFFFF, 0x3F3F3F, 1,
                    Graphics.TOP | Graphics.HCENTER);
        }

        drawShadowed(g, "TemuCraft 1.0", 4, H - hintFont.getHeight() - 2, 0xFFFFFF, 0x3F3F3F, 1,
                Graphics.TOP | Graphics.LEFT);
    }

    
    private void buildDirt(int W, int H) {
        if (Textures.atlas == null) Textures.build();
        int base = World.DIRT * Textures.BLOCK;
        int n = Textures.TEX;
        int[] tile = new int[n * n];
        for (int i = 0; i < tile.length; i++) {
            tile[i] = (Textures.atlas[base + i] >> 1) & 0x7F7F7F;
        }
        int[] bg = new int[W * H];
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                bg[y * W + x] = tile[(y % n) * n + (x % n)];
            }
        }
        dirtBg = bg;
        bgW = W;
        bgH = H;
    }

    private static void drawShadowed(Graphics g, String s, int x, int y, int color, int shadow, int off, int anchor) {
        g.setColor(shadow);
        g.drawString(s, x + off, y + off, anchor);
        g.setColor(color);
        g.drawString(s, x, y, anchor);
    }

    

    private void move(int d) {
        int n = items().length;
        sel = (sel + n + d) % n;
        keepSelVisible();
        repaint();
    }

    protected void keyPressed(int k) {
        int n = items().length;
        if (k >= '1' && k < '1' + n) {
            sel = k - '1';
            keepSelVisible();
            repaint();
            activate();
            return;
        }
        if (mode == 1 && k == '0') {
            back();
            return;
        }
        if (k == '\n' || k == '\r' || k == ' ') {
            activate();
            return;
        }
        if (k == 'w' || k == 'W') {
            move(-1);
            return;
        }
        if (k == 's' || k == 'S') {
            move(1);
            return;
        }
        int a = 0;
        try {
            a = getGameAction(k);
        } catch (Throwable t) {
        }
        if (a == UP) {
            move(-1);
        } else if (a == DOWN) {
            move(1);
        } else if (a == FIRE || k == '5') {
            activate();
        }
    }

    public void commandAction(Command c, Displayable d) {
        if (c == selectCmd) activate();
    }

    
    private int itemAt(int x, int y) {
        int w = bw(), h = bh(), bxx = bx();
        if (x < bxx || x >= bxx + w) return -1;
        int end = Math.min(items().length, scroll + visible());
        for (int i = scroll; i < end; i++) {
            if (y >= btnY(i) && y < btnY(i) + h) return i;
        }
        return -1;
    }

    
    protected void pointerPressed(int x, int y) {
        pressIdx = itemAt(x, y);
        pressY = y;
        lastY = y;
        dragging = false;
    }

    protected void pointerDragged(int x, int y) {
        if (!dragging && Math.abs(y - pressY) < 8) return;
        dragging = true;
        int step = rowH() / 2;
        if (y < lastY - step) {
            scroll++;
            lastY = y;
        } else if (y > lastY + step) {
            scroll--;
            lastY = y;
        }
        clampScroll();
        repaint();
    }

    protected void pointerReleased(int x, int y) {
        if (dragging) {
            dragging = false;
            return;
        }
        int i = pressIdx;
        pressIdx = -1;
        if (i >= 0 && i == itemAt(x, y)) {
            sel = i;
            repaint();
            activate();
        }
    }

    private void back() {
        mode = 0;
        sel = 2;
        scroll = 0;
        keepSelVisible();
        repaint();
    }

    private void activate() {
        if (mode == 1) {
            if (sel == 0) {
                World.setScenery(!World.sceneryOn);
            } else if (sel == 1) {
                Game.showInfo = !Game.showInfo;
                World.saveOptions();
            } else if (sel == 2) {
                M3GRenderer.fov = next(FOVS, M3GRenderer.fov);
                World.saveOptions();
            } else if (sel == 3) {
                M3GRenderer.texDist = next(TEXD, M3GRenderer.texDist);
                World.saveOptions();
            } else if (sel == 4) {
                Game.targetFps = next(FPSV, Game.targetFps);
                World.saveOptions();
            } else if (sel == 5) {                                    
                World.setInstantGen(!World.instantGen);
            } else {
                back();
                return;
            }
            repaint();
            return;
        }
        switch (sel) {
            case 0: midlet.startGame(false); break;
            case 1: midlet.startGame(true); break;
            case 2:
                mode = 1;
                sel = 0;
                scroll = 0;
                repaint();
                break;
            default: midlet.exit(); break;
        }
    }
}