import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;


final class Game extends Canvas implements Runnable, CommandListener {
    static int targetFps = 30;
    static final int PHYS_MS = 10;
    static boolean showInfo = false;
    static final int SZ = 18;
    static final int GX = 4 + 2 * SZ;
    static final int GY = 16;
    static final int MAIN_Y = GY + 2 * SZ + 8;
    static final int HOT_Y = MAIN_Y + 3 * SZ + 4;
    static final int PW = 9 * SZ + 8;
    static final int PH = HOT_Y + SZ + 4;
    static final int MAIN_Y3 = GY + 3 * SZ + 8;
    static final int HOT_Y3 = MAIN_Y3 + 3 * SZ + 4;
    static final int PH3 = HOT_Y3 + SZ + 4;
    static final int CLOSE_BTN = Inventory.OUT + 1;
    static final float[] SC_DAY = { 1f, 1f, 1f };
    static final float[] AC_DAY = { 0.5f, 0.5f, 0.5f };
    static final float[] YC_DAY = rgb(0x51BAF7);
    static final float[] SC_TWILIGHT = { 1f, 0.5f, 0.01f };
    static final float[] AC_TWILIGHT = { 0.6f, 0.5f, 0.5f };
    static final float[] YC_TWILIGHT = rgb(0x463C53);
    static final float[] SC_NIGHT = { 0.3f, 0.3f, 0.5f };
    static final float[] AC_NIGHT = { 0.3f, 0.3f, 0.5f };
    static final float[] YC_NIGHT = { 0.004f, 0.004f, 0.008f };
    private long wakeStart;                  
    private static final int WAKE_MS = 1300; 
    private final Minecraft4kMidlet midlet;
    private final Command menuCmd = new Command("Menu", Command.BACK, 1);
    private final Command selectCmd = new Command("Select", Command.OK, 1);
    private int[] fb;
    private final Inventory inv = new Inventory();
    private boolean invOpen;
    private int invMode;
    private int invSel = Inventory.GRID;
    private final Image[] icons = new Image[Items.MAX_ID + 1];
    private final Font small = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);

    
    private boolean askOpen;
    private int askSel;           
    private int askSlot;

    private float playerX = 16.5f;
    private float playerY = 1f;
    private float playerZ = 16.5f;
    private float velX, velY, velZ;
    private final float[] tp = new float[3];
    private float yaw, pitch;
    private int held;

    private boolean kFwd, kBack, kLeft, kRight, kTurnL, kTurnR, kUp, kDown, kJump;
    private boolean clickMine, clickPlace, clickNext;

    private boolean kMine;
    private int mineTimer;
    private int mineX = -1, mineY = -1, mineZ = -1;

    static final int MAX_HP = 10;
    private final Zombies zombies = new Zombies();
    private final Sheeps sheeps = new Sheeps();
    private int hp = MAX_HP;
    private int hurtFrames;
    private int deathFrames;
    private int slashFrames;

    private final java.util.Random rng = new java.util.Random();
    private static final int MAX_FUSE = 16;
    private static final int BLAST = 3;
    private static final long FUSE_MS = 3000L;
    private final int[] fuseX = new int[MAX_FUSE];
    private final int[] fuseY = new int[MAX_FUSE];
    private final int[] fuseZ = new int[MAX_FUSE];
    private final long[] fuseAt = new long[MAX_FUSE];
    private int fuseN;
    // true from a restored save until the chunks under the saved position exist
    private boolean restorePending;

    private volatile boolean running = true;
    private volatile boolean paused;
    private long lastTime;
    private float acc;
    private int fps;

    private double dayTime = 1000;

    private M3GRenderer m3g;
    private boolean invBtn;
    private String renderer = "";
    private String m3gErr = "";
    private Image torchGlow;

    Game(Minecraft4kMidlet midlet) {
        this.midlet = midlet;
        invBtn = false;
        addCommand(menuCmd);
        addCommand(selectCmd);
        setCommandListener(this);
        World.init();
        float[] s = World.savedPlayer();
        if (s != null) {
            restorePending = true;
            playerX = s[0];
            playerY = s[1];
            playerZ = s[2];
            yaw = s[3];
            pitch = s[4];
            held = (int) s[5];
            if (held < 0 || held >= Inventory.HOTBAR) held = 0;
        }
        dayTime = World.savedTick();
        String invSave = World.savedInventory();
        if (invSave != null) inv.load(invSave);
        else inv.starter();
        Textures.build();
        Renderer.setResolution(getWidth(), getHeight());
        fb = new int[Renderer.RW * Renderer.RH];
        Renderer.zombies = zombies;
        Renderer.sheeps = sheeps;
        try {
            m3g = new M3GRenderer();
            renderer = "M3G";
        } catch (Throwable t) {
            m3gErr = t.toString();
            m3g = null;
            renderer = "SOFT";
        }
        lastTime = System.currentTimeMillis();
    }

    private static float[] rgb(int c) {
        return new float[] { ((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f };
    }

    private static void lerp3(float[] a, float[] b, float t, float[] out) {
        for (int i = 0; i < 3; i++) out[i] = a[i] + (b[i] - a[i]) * t;
    }

    private Image icon(int item) {
        if (icons[item] == null) {
            int[] px = new int[Textures.TEX * Textures.TEX];
            for (int y = 0; y < Textures.TEX; y++) {
                for (int x = 0; x < Textures.TEX; x++) {
                    px[y * Textures.TEX + x] = Textures.atlas[item * Textures.BLOCK + x + (y + Textures.TEX) * Textures.TEX];
                }
            }
            icons[item] = Image.createRGBImage(px, Textures.TEX, Textures.TEX, true);
        }
        return icons[item];
    }

    protected void keyPressed(int k) {
        if (invOpen) {
            invKey(k);
            return;
        }
        setKey(k, true);
    }

    protected void keyReleased(int k) {
        setKey(k, false);
    }

    
    public void commandAction(Command c, Displayable d) {
        if (c == selectCmd) {
            if (askOpen) confirmAsk();              
            else if (invOpen) selectSlot(invSel);   
            else clickPlace = true;                 
            repaint();
            return;
        }
        
        if (askOpen) {
            askOpen = false;
            repaint();
            return;
        }
        if (invOpen) closeInv();
        else midlet.showMenu();
    }

    private void setKey(int k, boolean d) {
        switch (k) {
            case '2': case 'T': case 't': kUp = d; break;
            case '8': case 'B': case 'b': kDown = d; break;
            case '4': case 'F': case 'f': kTurnL = d; break;
            case '6': case 'H': case 'h': kTurnR = d; break;

            case '1': case 'R': case 'r': if (d) clickPlace = true; break;

            case '5': case 'G': case 'g': kMine = d; if (d) clickMine = true; break;
            
            case '7': case 'V': case 'v': if (d) clickNext = true; break;
            case '0': case 'M': case 'm': if (d) toggleInv(); break;

            case '#': case 'J': case 'j': kJump = d; break;

            default:
                switch (getGameAction(k)) {
                    case UP: kFwd = d; break;
                    case DOWN: kBack = d; break;
                    case LEFT: kLeft = d; break;
                    case RIGHT: kRight = d; break;
                    case FIRE: kJump = d; break;
                    default: break;
                }
                break;
        }
    }
    
private int[] bedDir() {
    float fx = fwdX(), fz = fwdZ();
    if (Math.abs(fx) > Math.abs(fz)) return new int[] { fx > 0 ? 1 : -1, 0 };
    return new int[] { 0, fz > 0 ? 1 : -1 };
}

    private void invKey(int k) {
        int a = 0;
        try {
            a = getGameAction(k);
        } catch (Throwable t) {
        }
        boolean fire = (k == '5' || a == FIRE || k == '\n' || k == ' ');

        if (askOpen) {                                   
            if (k == '0' || k == 'e' || k == 'E') askOpen = false;
            else if (k == '2' || a == UP) askSel = 0;
            else if (k == '8' || a == DOWN) askSel = 1;
            else if (k == '1') { askSel = 1; confirmAsk(); }
            else if (fire) confirmAsk();
            repaint();
            return;
        }
        if (k == '0' || k == 'e' || k == 'E') {
            closeInv();
            return;
        }

        if (k == '2' || a == UP) moveCursor(0, -1);
        else if (k == '8' || a == DOWN) moveCursor(0, 1);
        else if (k == '4' || a == LEFT) moveCursor(-1, 0);
        else if (k == '6' || a == RIGHT) moveCursor(1, 0);
        else if (fire) selectSlot(invSel);
        repaint();
    }

private void selectSlot(int s) {
    if (s == CLOSE_BTN) {
        closeInv();
        return;
    }
    boolean holding = inv.cursorCount() > 0;
    boolean canStack = inv.count(s) == 0 || inv.id(s) == inv.cursorId();
    if (holding && inv.cursorCount() > 1 && canStack && s != Inventory.OUT) {
        askOpen = true;
        askSel = 0;
        askSlot = s;
    } else {
        clickSlot(s);
    }
}

    private void confirmAsk() {
        if (askSel == 0) inv.click(askSlot);        
        else inv.rightClick(askSlot);               
        askOpen = false;
    }

    private void toggleInv() {
        if (invOpen) closeInv();
        else openInv(0);
    }

    private void openInv(int mode) {
        invOpen = true;
        invMode = mode;
        invSel = Inventory.GRID;
        askOpen = false;
        inv.setGridSize(mode == 1 ? 9 : 4);
        clearMoves();
        clickMine = false;
        clickPlace = false;
        clickNext = false;
        
    }

    private void closeInv() {
        if (!invOpen) return;
        invOpen = false;
        askOpen = false;
        inv.returnAll();
        inv.setGridSize(4);
        clearMoves();
    }

    private void clearMoves() {
        kFwd = kBack = kLeft = kRight = kTurnL = kTurnR = kUp = kDown = kJump = false;
    }

    private void clickSlot(int s) {
        inv.click(s);
        repaint();
    }

    private void rightClickSlot(int s) {
        inv.rightClick(s);
        repaint();
    }

    private int gridCells() {
        return invMode == 1 ? 9 : 4;
    }

    private int mainY() {
        return invMode == 1 ? MAIN_Y3 : MAIN_Y;
    }

    private int hotY() {
        return invMode == 1 ? HOT_Y3 : HOT_Y;
    }

    private int panelH() {
        return invMode == 1 ? PH3 : PH;
    }

private boolean shown(int s) {
    if (s == CLOSE_BTN) return true;
    if (s >= Inventory.GRID && s < Inventory.OUT) return s - Inventory.GRID < gridCells();
    return true;
}

private int slotX(int s) {
    if (s == CLOSE_BTN) return PW - SZ - 2; 
    if (s == Inventory.OUT) return invMode == 1 ? 4 + 6 * SZ : GX + 3 * SZ;
    if (s >= Inventory.GRID) {
        int g = s - Inventory.GRID;
        return invMode == 1 ? 4 + (g % 3 + 2) * SZ : GX + (g % 2) * SZ;
    }
    if (s < Inventory.HOTBAR) return 4 + s * SZ;
    return 4 + ((s - Inventory.HOTBAR) % 9) * SZ;
}

private int slotY(int s) {
    if (s == CLOSE_BTN) return 2; 
    if (s == Inventory.OUT) return invMode == 1 ? GY + SZ : GY + SZ / 2;
    if (s >= Inventory.GRID) {
        int g = s - Inventory.GRID;
        return invMode == 1 ? GY + (g / 3) * SZ : GY + (g / 2) * SZ;
    }
    if (s < Inventory.HOTBAR) return hotY();
    return mainY() + ((s - Inventory.HOTBAR) / 9) * SZ;
}

private void moveCursor(int dx, int dy) {
        int cx = slotX(invSel) + SZ / 2, cy = slotY(invSel) + SZ / 2;
        int best = -1, bestScore = Integer.MAX_VALUE;
        for (int s = 0; s <= CLOSE_BTN; s++) { 
            if (s == invSel || !shown(s)) continue;
            int ex = slotX(s) + SZ / 2 - cx, ey = slotY(s) + SZ / 2 - cy;
            int primary = dx != 0 ? ex * dx : ey * dy;
            int side = dx != 0 ? Math.abs(ey) : Math.abs(ex);
            if (primary <= 0) continue;
            int score = primary + 2 * side;
            if (score < bestScore) {
                bestScore = score;
                best = s;
            }
        }
        if (best >= 0) invSel = best;
    }

protected void pointerPressed(int x, int y) {
        if (!invOpen) return;

        if (askOpen) {
            int bx = getWidth() / 2 - 40, by = getHeight() / 2 - 20;
            if (x >= bx && x < bx + 80 && y >= by && y < by + 40) {
                askSel = (y < by + 20) ? 0 : 1;
                confirmAsk();
            } else {
                askOpen = false;
            }
            repaint();
            return;
        }

        int px = (getWidth() - PW) / 2, py = (getHeight() - panelH()) / 2;
        for (int s = 0; s <= CLOSE_BTN; s++) {
            if (!shown(s)) continue;
            int sx = px + slotX(s), sy = py + slotY(s);
            if (x >= sx && x < sx + SZ && y >= sy && y < sy + SZ) {
                invSel = s;
                selectSlot(s);
                return;
            }
        }
    }

    private void physicsStep() {
        if (restorePending) {
            // Chunks load lazily, so wait for the area before trusting the saved height.
            if (!World.areaReady()) return;
            restorePending = false;
            int bx = Calc.floor(playerX), bz = Calc.floor(playerZ);
            int feet = Calc.floor(playerY);
            if (!World.passable(World.get(bx, feet, bz)) || !World.passable(World.get(bx, feet + 1, bz))) {
                playerY = World.surfaceY(playerX, playerZ);
            }
        }
        int turn = (kTurnR ? 0 : 1) - (kTurnL ? 0 : 1);
        yaw += turn * 0.02f;
        int look = (kUp ? 1 : 0) - (kDown ? 1 : 0);
        pitch += look * 0.02f;
        if (pitch < -1.57f) pitch = -1.57f;
        if (pitch > 1.57f) pitch = 1.57f;

        float inputX = ((kRight ? 0 : 1) - (kLeft ? 0 : 1)) * 0.02f;
        float inputZ = ((kFwd ? 1 : 0) - (kBack ? 1 : 0)) * 0.02f;

        velX *= 0.5f;
        velY *= 0.99f;
        velZ *= 0.5f;
        velX += Renderer.sinYaw * inputZ + Renderer.cosYaw * inputX;
        velZ += Renderer.cosYaw * inputZ - Renderer.sinYaw * inputX;
        velY += 0.003f;

        OUTER:
        for (int axis = 0; axis < 3; axis++) {
            float nx = playerX + velX * (((axis + 1) % 3) / 2);
            float ny = playerY + velY * (((axis + 0) % 3) / 2);
            float nz = playerZ + velZ * (((axis + 2) % 3) / 2);

            for (int c = 0; c < 12; c++) {
                int bx = Calc.floor(nx + (c & 1) * 0.6f - 0.3f);
                int by = Calc.floor(ny + ((c >> 2) - 1) * 0.8f + 0.65f);
                int bz = Calc.floor(nz + ((c >> 1) & 1) * 0.6f - 0.3f);
                if (by < 0) continue;
                if (!World.passable(World.get(bx, by, bz))
                        || (axis != 2 && World.get(bx, by - 1, bz) == Items.FENCE)) {
                    if (axis != 2) continue OUTER;
                    if (kJump && velY > 0f) {
                        velY = -0.1f;
                        break OUTER;
                    }
                    velY = 0f;
                    break OUTER;
                }
            }

            playerX = nx;
            playerY = ny;
            playerZ = nz;
        }
    }


    private void clearPlayerBody() {
        for (int c = 0; c < 12; c++) {
            int bx = Calc.floor(playerX + (c & 1) * 0.6f - 0.3f);
            int by = Calc.floor(playerY + ((c >> 2) - 1) * 0.8f + 0.65f);
            int bz = Calc.floor(playerZ + ((c >> 1) & 1) * 0.6f - 0.3f);
            if (by >= 0 && by < World.HEIGHT && World.get(bx, by, bz) != Items.TORCH
                    && !World.isThin(World.get(bx, by, bz))) {
                World.set(bx, by, bz, World.AIR);
            }
        }
    }

    private boolean overlapsPlayer(int x, int y, int z) {
        for (int c = 0; c < 12; c++) {
            int bx = Calc.floor(playerX + (c & 1) * 0.6f - 0.3f);
            int by = Calc.floor(playerY + ((c >> 2) - 1) * 0.8f + 0.65f);
            int bz = Calc.floor(playerZ + ((c >> 1) & 1) * 0.6f - 0.3f);
            if (bx == x && by == y && bz == z) return true;
        }
        return false;
    }

    private void updateLighting() {
        double d = dayTime / 24000.0 - 0.25;
        d -= Math.floor(d);
        double d1 = 0.5 - Math.cos(d * Math.PI) / 2.0;
        double angle = d + (d1 - d) / 3.0;
        double th = angle * 2.0 * Math.PI;

        float e = (float) Math.cos(th);
        Renderer.lx = 0f;
        Renderer.ly = -e;
        Renderer.lz = (float) Math.sin(th);

        float k = (e < 0f ? -e : e) * 4f;
        if (k > 1f) k = 1f;
        if (e >= 0f) {
            lerp3(SC_TWILIGHT, SC_DAY, k, Renderer.sunC);
            lerp3(AC_TWILIGHT, AC_DAY, k, Renderer.ambC);
            lerp3(YC_TWILIGHT, YC_DAY, k, Renderer.skyC);
        } else {
            lerp3(SC_TWILIGHT, SC_NIGHT, k, Renderer.sunC);
            lerp3(AC_TWILIGHT, AC_NIGHT, k, Renderer.ambC);
            lerp3(YC_TWILIGHT, YC_NIGHT, k, Renderer.skyC);
        }

        float b = e * 2f + 0.5f;
        if (b < 0f) b = 0f;
        if (b > 1f) b = 1f;
        float tw = 1f - Math.abs(b * 2f - 1f);
        Renderer.dimC[0] = 0.28f + 0.72f * b;
        Renderer.dimC[1] = (0.28f + 0.72f * b) * (1f - 0.15f * tw);
        Renderer.dimC[2] = (0.45f + 0.55f * b) * (1f - 0.35f * tw);
    }

    
    private void processClicks() {

        boolean hitEntity = false;
        if (clickMine) {
            int heldItem = inv.id(held);
            if (Items.isSword(heldItem)) {
                slashFrames = 6;
            }
            int tiZ = zombies.target(playerX, playerY, playerZ, fwdX(), fwdY(), fwdZ());
            int tiS = sheeps.target(playerX, playerY, playerZ, fwdX(), fwdY(), fwdZ());
            if (tiZ >= 0) {
                zombies.hit(tiZ, weaponDamage(), fwdX(), fwdZ());
                inv.damage(held);
                hitEntity = true;
            } else if (tiS >= 0) {
                sheeps.hit(tiS, weaponDamage(), inv);
                inv.damage(held);
                hitEntity = true;
            }
            clickMine = false;
        }

        if (kMine && !hitEntity && Renderer.hoverValid) {
            int bx = Renderer.hovX, by = Renderer.hovY, bz = Renderer.hovZ;
            if (bx == mineX && by == mineY && bz == mineZ) {
                mineTimer++;
                int b = World.get(bx, by, bz);
                int targetTime = mineTime(b);

                if (mineTimer >= targetTime) {
                    if (b > World.AIR) breakBlock(bx, by, bz, b);
                    mineTimer = 0;
                }
            } else {
                mineX = bx; mineY = by; mineZ = bz;
                mineTimer = 0;
            }
        } else {
            mineTimer = 0;
        }

if (clickPlace && !invBtn && inv.id(held) == Items.APPLE && hp < MAX_HP) {
    hp = Math.min(MAX_HP, hp + 4);
    inv.take(held);
    clickPlace = false;
}
if (clickPlace) {
    if (invBtn) {
        openInv(0);
    } else if (Renderer.hoverValid) {
        int hb = World.get(Renderer.hovX, Renderer.hovY, Renderer.hovZ);
        if (hb == Items.CRAFT_TABLE) {
            openInv(1);
        } else if (hb == Items.BED || hb == Items.BED_HEAD) {
    if (dayTime >= 12000 && dayTime < 23800) {
        dayTime = 0;
        wakeStart = System.currentTimeMillis();
        World.setSpawn(Renderer.hovX + 0.5f, Renderer.hovY + 2f, Renderer.hovZ + 0.5f);
    }
} else if (hb == Items.TNT && inv.id(held) == Items.TORCH) {
            igniteTnt(Renderer.hovX, Renderer.hovY, Renderer.hovZ);
        } else if (hb == Items.SIGN) {
            midlet.editSign(Renderer.hovX, Renderer.hovY, Renderer.hovZ);
        } else if (Items.isDoorBottom(hb) || Items.isDoorTop(hb)) {
            toggleDoor(Renderer.hovX, Renderer.hovY, Renderer.hovZ);
        } else {
            int tx = Renderer.hovX + Renderer.nrmX;
            int ty = Renderer.hovY + Renderer.nrmY;
            int tz = Renderer.hovZ + Renderer.nrmZ;
int item = inv.id(held);
if (item == Items.BED) {
    int[] d = findBedHead(tx, ty, tz);
    if (d != null
            && inv.count(held) > 0
            && World.get(tx, ty, tz) == World.AIR
            && !overlapsPlayer(tx, ty, tz)) {
        World.set(tx, ty, tz, Items.BED);
        World.set(tx + d[0], ty, tz + d[1], Items.BED_HEAD);
        inv.take(held);
    }
} else if (item == Items.DOOR) {
    int[] d = findDoorSpot(tx, ty, tz);
    if (d != null && inv.count(held) > 0) {
        World.set(d[0], d[1], d[2], Items.DOOR);
        World.set(d[3], d[4], d[5], Items.DOOR_TOP);
        inv.take(held);
    }
} else if (Items.placeable(item) && inv.count(held) > 0
        && World.get(tx, ty, tz) == World.AIR
        && !overlapsPlayer(tx, ty, tz)) {
    World.set(tx, ty, tz, item);
    inv.take(held);
}
        }
    }
    clickPlace = false;
}
if (clickNext) {
    if (invBtn) {
        invBtn = false;
        held = 0;
    } else if (held == Inventory.HOTBAR - 1) {
        invBtn = true;
    } else {
        held++;
    }
    clickNext = false;
}
    }

    private void updateZombies() {
        boolean night = dayTime >= 13000 && dayTime < 23000;
        int dmg = zombies.update(playerX, playerY, playerZ, night);
        if (hurtFrames > 0) hurtFrames--;
        if (deathFrames > 0) deathFrames--;
        if (slashFrames > 0) slashFrames--;
        if (dmg > 0) {
            hp -= dmg;
            hurtFrames = 8;
            if (hp <= 0) die();
        }
    }
    
private int[] bedPartner(int x, int y, int z, int b) {
    if (b != Items.BED && b != Items.BED_HEAD) return null;
    int want = b == Items.BED ? Items.BED_HEAD : Items.BED;
    int[][] n = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
    for (int i = 0; i < 4; i++) {
        int nx = x + n[i][0], nz = z + n[i][1];
        if (World.get(nx, y, nz) == want) return new int[] { nx, y, nz };
    }
    return null;
}

private void die() {
    hp = MAX_HP;
    zombies.clear();
    float[] s = World.savedSpawn();
    if (s != null) {
        World.moveToArea((int) s[0], (int) s[1]);
        playerX = s[2];
        playerY = s[3];
        playerZ = s[4];
    } else {
        World.moveToArea(0, 0);
        playerX = 16.5f;
        playerZ = 16.5f;
        playerY = World.surfaceY(playerX, playerZ);
    }
    velX = velY = velZ = 0f;
    deathFrames = 90;
}

    private int mineTime(int b) {
        int item = inv.id(held);
        int pl = Items.pickLevel(item);
        int al = Items.axeLevel(item);
        if (isRock(b)) return pl >= 3 ? 5 : (pl == 2 ? 8 : (pl == 1 ? 15 : 30));
        if (b == World.WOOD || b == Items.PLANKS || b == Items.CRAFT_TABLE
                || b == Items.SIGN || b == Items.DOOR || b == Items.DOOR_TOP
                || b == Items.DOOR_OPEN || b == Items.DOOR_TOP_OPEN) {
            return al >= 3 ? 3 : (al == 2 ? 5 : (al == 1 ? 8 : 15));
        }
        if (b == World.LEAVES) return al >= 1 ? 3 : 8;
        if (b == World.DIRT || b == World.GRASS) return 8;
        return 15;
    }

    private static boolean isRock(int b) {
        return b == World.STONE || b == World.BRICKS || b == Items.COBBLE
                || b == Items.COAL_ORE || b == Items.IRON_ORE || b == Items.IRON_BLOCK;
    }

    private void breakBlock(int bx, int by, int bz, int b) {
        int item = inv.id(held);
        int need = (b == Items.IRON_ORE || b == Items.IRON_BLOCK) ? 2 : 1;
        boolean canDrop = !isRock(b) || Items.pickLevel(item) >= need;
        boolean removed = !canDrop || inv.add(Items.dropOf(b), 1) == 0;
        if (!removed) return;
        World.set(bx, by, bz, World.AIR);
        int[] p = bedPartner(bx, by, bz, b);
        if (p != null) World.set(p[0], p[1], p[2], World.AIR);
        if (Items.isDoorBottom(b) && Items.isDoorTop(World.get(bx, by + 1, bz))) World.set(bx, by + 1, bz, World.AIR);
        if (Items.isDoorTop(b) && Items.isDoorBottom(World.get(bx, by - 1, bz))) World.set(bx, by - 1, bz, World.AIR);
        if (b == World.LEAVES && rng.nextInt(20) == 0) inv.add(Items.APPLE, 1);
        inv.damage(held);
    }

    private void igniteTnt(int x, int y, int z) {
        for (int i = 0; i < fuseN; i++) {
            if (fuseX[i] == x && fuseY[i] == y && fuseZ[i] == z) return;
        }
        if (fuseN >= MAX_FUSE) return;
        fuseX[fuseN] = x;
        fuseY[fuseN] = y;
        fuseZ[fuseN] = z;
        fuseAt[fuseN] = System.currentTimeMillis() + FUSE_MS;
        fuseN++;
    }

    private void updateTnt() {
        long now = System.currentTimeMillis();
        int i = 0;
        while (i < fuseN) {
            if (now < fuseAt[i]) {
                i++;
                continue;
            }
            int x = fuseX[i], y = fuseY[i], z = fuseZ[i];
            fuseN--;
            fuseX[i] = fuseX[fuseN];
            fuseY[i] = fuseY[fuseN];
            fuseZ[i] = fuseZ[fuseN];
            fuseAt[i] = fuseAt[fuseN];
            if (World.get(x, y, z) == Items.TNT) explode(x, y, z);
        }
    }

    private void explode(int cx, int cy, int cz) {
        World.set(cx, cy, cz, World.AIR);
        int r2 = BLAST * BLAST;
        for (int dx = -BLAST; dx <= BLAST; dx++) {
            for (int dy = -BLAST; dy <= BLAST; dy++) {
                for (int dz = -BLAST; dz <= BLAST; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r2) continue;
                    int x = cx + dx, y = cy + dy, z = cz + dz;
                    int b = World.get(x, y, z);
                    if (b == Items.TNT) {
                        igniteTnt(x, y, z);
                    } else if (b > World.AIR) {
                        World.set(x, y, z, World.AIR);
                    }
                }
            }
        }
        double ex = playerX - (cx + 0.5), ey = (playerY + 0.9) - (cy + 0.5), ez = playerZ - (cz + 0.5);
        if (ex * ex + ey * ey + ez * ez < (BLAST + 1) * (BLAST + 1)) {
            hp -= 6;
            hurtFrames = 8;
            if (hp <= 0) die();
        }
    }

    private void toggleDoor(int x, int y, int z) {
    int by = Items.isDoorTop(World.get(x, y, z)) ? y - 1 : y;
    int bot = World.get(x, by, z);
    int top = World.get(x, by + 1, z);
    if (!Items.isDoorBottom(bot) || !Items.isDoorTop(top)) return;
    boolean open = bot == Items.DOOR_OPEN;
    // Don't close the door on the player.
    if (open && (overlapsPlayer(x, by, z) || overlapsPlayer(x, by + 1, z))) return;
    World.set(x, by, z, open ? Items.DOOR : Items.DOOR_OPEN);
    World.set(x, by + 1, z, open ? Items.DOOR_TOP : Items.DOOR_TOP_OPEN);
}

private int[] findDoorSpot(int x, int y, int z) {
    // Bottom at the clicked spot with the top above it, otherwise top at the spot with the bottom below it.
    int[][] tries = { { x, y, z, x, y + 1, z }, { x, y - 1, z, x, y, z } };
    for (int i = 0; i < tries.length; i++) {
        int[] c = tries[i];
        if (World.get(c[0], c[1], c[2]) == World.AIR
                && World.get(c[3], c[4], c[5]) == World.AIR
                && !overlapsPlayer(c[0], c[1], c[2])
                && !overlapsPlayer(c[3], c[4], c[5])) {
            return c;
        }
    }
    return null;
}

private int[] findBedHead(int tx, int ty, int tz) {
    int[] f = bedDir();
    int[][] cand = { f, { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
    for (int i = 0; i < cand.length; i++) {
        int hx = tx + cand[i][0], hz = tz + cand[i][1];
        if (World.get(hx, ty, hz) == World.AIR && !overlapsPlayer(hx, ty, hz)) {
            return cand[i];
        }
    }
    return null;
}

    private float fwdX() {
        return Renderer.cosPitch * Renderer.sinYaw;
    }

    private float fwdY() {
        return -Renderer.sinPitch;
    }

    private float fwdZ() {
        return Renderer.cosPitch * Renderer.cosYaw;
    }

    private int weaponDamage() {
        int item = inv.id(held);
        if (item == Items.IRON_SWORD) return 6;
        if (item == Items.STONE_SWORD) return 5;
        if (item == Items.WOOD_SWORD) return 4;
        return 1;
    }

    public void run() {
        while (running) {
            long t0 = System.currentTimeMillis();
            if (!paused) {
                dayTime += (t0 - lastTime) * 0.02;
                if (dayTime >= 24000) dayTime -= 24000;
                acc += t0 - lastTime;
                lastTime = t0;
                int steps = 0;
                while (acc >= PHYS_MS && steps < 20) {
                    if (!invOpen) physicsStep();
                    acc -= PHYS_MS;
                    steps++;
                }
                if (steps == 20) acc = 0;

                clearPlayerBody();
                updateLighting();
                updateZombies();
                updateTnt();
                sheeps.update(dayTime);

                Renderer.sinYaw = (float) Math.sin(yaw);
                Renderer.cosYaw = (float) Math.cos(yaw);
                Renderer.sinPitch = (float) Math.sin(pitch);
                Renderer.cosPitch = (float) Math.cos(pitch);
                Renderer.px = playerX;
                Renderer.py = playerY;
                Renderer.pz = playerZ;
                M3GRenderer.heldTorch = inv.id(held) == Items.TORCH && !invBtn;
                if (m3g != null) {
                    float fx = Renderer.cosPitch * Renderer.sinYaw;
                    float fy = -Renderer.sinPitch;
                    float fz = Renderer.cosPitch * Renderer.cosYaw;
                    M3GRenderer.pick(playerX, playerY, playerZ, fx, fy, fz);
                }

                tp[0] = playerX;
                tp[1] = playerY;
                tp[2] = playerZ;
                if (World.teleport(tp)) {
                    playerX = tp[0];
                    playerY = tp[1];
                    playerZ = tp[2];
                    velX = velY = velZ = 0f;
                    zombies.clear();
                }
                processClicks();
                if (m3g == null) Renderer.renderFrame(fb);

                repaint();
                serviceRepaints();
            }
            long dt = System.currentTimeMillis() - t0;
fps = dt > 0 ? (int) (1000 / dt) : 0;
int frameMs = targetFps > 0 ? 1000 / targetFps : 0;
if (dt < frameMs) {
    try {
        Thread.sleep(frameMs - dt);
    } catch (InterruptedException e) {
    }
} else {
    Thread.yield();
}
        }
    }

    void setPaused(boolean p) {
        paused = p;
        if (p) {
            savePlayer();
            World.saveAll();
        }
        lastTime = System.currentTimeMillis();
    }

    void stop() {
        running = false;
        savePlayer();
        World.close();
    }

    private void savePlayer() {
        if (invOpen) closeInv();
        World.setPlayer(playerX, playerY, playerZ, yaw, pitch, held, (int) dayTime);
        World.setInventory(inv.serialize());
    }

private void drawTorchGlow(Graphics g, int W, int H) {
    if (torchGlow == null) {
        int n = 48;
        int[] px = new int[n * n];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                float dx = x - n / 2 + 0.5f, dy = y - n / 2 + 0.5f;
                float d = (float) Math.sqrt(dx * dx + dy * dy) / (n / 2f);
                float a = d >= 1f ? 0f : (1f - d) * (1f - d);
                int alpha = (int) (a * 170f);
                px[y * n + x] = (alpha << 24) | 0xFF9020;
            }
        }
        torchGlow = Image.createRGBImage(px, n, n, true);
    }
    if (Renderer.dimC[0] > 0.9f) return;   
    g.drawImage(torchGlow, W / 2 - 24, H - 72, Graphics.TOP | Graphics.LEFT);
}

    protected void paint(Graphics g) {
        int W = getWidth(), H = getHeight();
        if (m3g != null) {
            m3g.render(g, W, H);
        } else {
            g.setColor(0x000000);
            g.fillRect(0, 0, W, H);
            g.drawRGB(fb, 0, Renderer.RW, 0, 0, Renderer.RW, Renderer.RH, false);
        }
        if (m3g != null && inv.id(held) == Items.TORCH && !invBtn) drawTorchGlow(g, W, H);

if (showInfo) {
    g.setFont(Font.getDefaultFont());
    g.setColor(0xFFFFFF);
    g.drawString("fps " + fps + " " + renderer, 2, 2, Graphics.TOP | Graphics.LEFT);
    g.drawString("X " + (int) playerX + " Y " + (int) playerY + " Z " + (int) playerZ,
            2, 14, Graphics.TOP | Graphics.LEFT);
    g.drawString("save: " + World.fileStatus, 2, 26, Graphics.TOP | Graphics.LEFT);
    g.setFont(small);
g.setColor(0xFFFF00);
g.drawString("torches: " + World.torchCount(), 2, 66, Graphics.TOP | Graphics.LEFT);
g.setFont(Font.getDefaultFont());
}

if (m3g == null && m3gErr.length() > 0) {
    g.setFont(small);
    g.setColor(0xFFFF00);
    g.drawString("M3G error:", 2, 40, Graphics.TOP | Graphics.LEFT);
    g.setColor(0xFFFFFF);
    g.drawString(m3gErr, 2, 54, Graphics.TOP | Graphics.LEFT);
    g.setFont(Font.getDefaultFont());
}

        if (invOpen) {
            paintInv(g, W, H);
            return;
        }

        int cx = W / 2, cy = H / 2;
        g.setColor(0xFFFFFF);
        g.drawLine(cx - 4, cy, cx + 4, cy);
        g.drawLine(cx, cy - 4, cx, cy + 4);

        
        if (showInfo && Renderer.hoverValid) {
            int hb = World.get(Renderer.hovX, Renderer.hovY, Renderer.hovZ);
            g.setFont(small);
            g.setColor(0xFFFFFF);
            g.drawString(Items.name(Items.dropOf(hb)), W / 2, 40, Graphics.TOP | Graphics.HCENTER);
            g.setFont(Font.getDefaultFont());
        }

        if (Renderer.hoverValid && World.get(Renderer.hovX, Renderer.hovY, Renderer.hovZ) == Items.SIGN) {
            String st = World.signText(Renderer.hovX, Renderer.hovY, Renderer.hovZ);
            if (st.length() > 0) {
                g.setFont(small);
                g.setColor(0xFFFFFF);
                g.drawString(st, W / 2, 52, Graphics.TOP | Graphics.HCENTER);
                g.setFont(Font.getDefaultFont());
            }
        }

        if (mineTimer > 0 && Renderer.hoverValid) {
            int b = World.get(mineX, mineY, mineZ);
            int targetTime = mineTime(b);

            int w = 24;
            int progressW = (mineTimer * w) / targetTime;
            if (progressW > w) progressW = w;

            g.setColor(0x000000);
            g.drawRect(cx - w / 2 - 1, cy + 8, w + 2, 4);
            g.setColor(0x00FF00);
            g.fillRect(cx - w / 2, cy + 9, progressW, 3);
        }

        if (slashFrames > 0) {
            int progress = 6 - slashFrames;
            int shift = progress * 3;

            int x1 = cx - 24 + shift;
            int y1 = cy + 24 - shift;
            int x2 = cx + 24 + shift;
            int y2 = cy - 24 - shift;

            g.setColor(0x80C0FF);
            g.drawLine(x1 - 1, y1, x2 - 1, y2);
            g.setColor(0xFFFFFF);
            g.drawLine(x1, y1, x2, y2);
            g.setColor(0xC0E0FF);
            g.drawLine(x1 + 1, y1, x2 + 1, y2);
        }

        int hx = (W - SZ * Inventory.HOTBAR) / 2;
        int hy = H - SZ - 2;
for (int i = 0; i < Inventory.HOTBAR; i++) {
    drawSlot(g, hx + i * SZ, hy, i, i == held && !invBtn);
}


int bx = hx + Inventory.HOTBAR * SZ + 2;
g.setColor(0x333333);
g.fillRect(bx, hy, SZ - 2, SZ - 2);
g.setColor(0xFFFFFF);
g.fillRect(bx + 3, hy + 4, SZ - 8, 2);
g.fillRect(bx + 3, hy + 8, SZ - 8, 2);
g.fillRect(bx + 3, hy + 12, SZ - 8, 2);
if (invBtn) {
    g.setColor(0xFFFF00);
    g.drawRect(bx, hy, SZ - 3, SZ - 3);
}

        for (int i = 0; i < MAX_HP; i++) {
            g.setColor(i < hp ? 0xD01010 : 0x400000);
            g.fillRect(hx + i * 9, hy - 9, 7, 7);
        }
        if (hurtFrames > 0) {
            g.setColor(0xFF0000);
            g.drawRect(0, 0, W - 1, H - 1);
            g.drawRect(1, 1, W - 3, H - 3);
        }
                if (deathFrames > 0) {
            g.setColor(0xFFFFFF);
            g.setFont(small);
            g.drawString("You died - respawned", W / 2, H / 2 - 40, Graphics.TOP | Graphics.HCENTER);
            g.setFont(Font.getDefaultFont());
        }

        long left = wakeStart + WAKE_MS - System.currentTimeMillis();
        if (left > 0) {
            int step = 1 + (int) ((WAKE_MS - left) * 8 / WAKE_MS);
            g.setColor(0x000000);
            for (int y = 0; y < H; y += step) {
                g.drawLine(0, y, W - 1, y);
            }
        }
    }

    private void drawSlot(Graphics g, int x, int y, int s, boolean sel) {
        g.setColor(0x333333);
        g.fillRect(x, y, SZ - 2, SZ - 2);
        int cnt = inv.count(s);
        if (cnt > 0) {
            g.drawImage(icon(inv.id(s)), x + 1, y + 1, Graphics.TOP | Graphics.LEFT);
            if (cnt > 1) {
                g.setFont(small);
                g.setColor(0xFFFFFF);
                g.drawString(String.valueOf(cnt), x + SZ - 3, y + SZ - 3, Graphics.BOTTOM | Graphics.RIGHT);
                g.setFont(Font.getDefaultFont());
            }
        }
        int full = Items.durability(inv.id(s));
        int cur = inv.durability(s);
        if (cnt > 0 && full > 0 && cur < full) {
            int barW = (SZ - 4) * cur / full;
            g.setColor(0x000000);
            g.fillRect(x + 1, y + SZ - 5, SZ - 4, 2);
            g.setColor(cur * 3 > full ? 0x00FF00 : 0xFF4000);
            g.fillRect(x + 1, y + SZ - 5, barW, 2);
        }
        if (sel) {
            g.setColor(0xFFFF00);
            g.drawRect(x, y, SZ - 3, SZ - 3);
        }
    }
    
private void drawCloseButton(Graphics g, int x, int y, boolean sel) {
    g.setColor(0xAA2222); 
    g.fillRect(x, y, SZ - 2, SZ - 2);
    g.setColor(0xFFFFFF);
    g.setFont(small);
    g.drawString("X", x + (SZ - 2) / 2, y + 1, Graphics.TOP | Graphics.HCENTER);
    g.setFont(Font.getDefaultFont());
    if (sel) {
        g.setColor(0xFFFF00);
        g.drawRect(x, y, SZ - 3, SZ - 3);
    }
}

private void paintInv(Graphics g, int W, int H) {
    int ph = panelH();
    int px = (W - PW) / 2, py = (H - ph) / 2;
    g.setColor(0x000000);
    g.fillRect(px - 2, py - 2, PW + 4, ph + 4);
    g.setColor(0x8B8B8B);
    g.fillRect(px, py, PW, ph);

    g.setFont(small);
    g.setColor(0xFFFFFF);
    g.drawString(invMode == 1 ? "Crafting Table" : "Inventory", px + 4, py + 3, Graphics.TOP | Graphics.LEFT);

    g.setColor(0x000000);
    if (invMode == 1) {
        g.drawString("->", px + 4 + 5 * SZ + SZ / 2, py + GY + SZ + SZ / 2 - 6, Graphics.TOP | Graphics.HCENTER);
    } else {
        g.drawString("->", px + 4 + 4 * SZ + SZ / 2, py + GY + SZ / 2 - 6, Graphics.TOP | Graphics.HCENTER);
    }
    
    for (int s = 0; s <= CLOSE_BTN; s++) {
        if (!shown(s)) continue;
        if (s == CLOSE_BTN) {
            drawCloseButton(g, px + slotX(s), py + slotY(s), s == invSel);
            continue;
        }
        
        if (s >= 0 && s <= Inventory.OUT) {
            drawSlot(g, px + slotX(s), py + slotY(s), s, s == invSel);
        }
    }

    g.setFont(small);

    
    if (invSel >= 0 && invSel <= Inventory.OUT && inv.count(invSel) > 0) {
        String info = Items.name(inv.id(invSel)) + " x" + inv.count(invSel);
        int iw = small.stringWidth(info) + 8;
        g.setColor(0x000000);
        g.fillRect(W / 2 - iw / 2, py - 16, iw, 14);
        g.setColor(0xFFFF00);
        g.drawString(info, W / 2, py - 15, Graphics.TOP | Graphics.HCENTER);
    }

    g.setColor(0xFFFFFF);
    if (inv.cursorCount() > 0) {
        g.drawString("holding " + Items.name(inv.cursorId()) + " x" + inv.cursorCount(),
                W / 2, py + ph + 4, Graphics.TOP | Graphics.HCENTER);
    }

    
    if (askOpen) {
        int bx = W / 2 - 40, by = H / 2 - 20;
        g.setColor(0x000000);
        g.fillRect(bx - 1, by - 1, 82, 42);
        g.setColor(0x555555);
        g.fillRect(bx, by, 80, 40);
        g.setColor(0xFFFF00);
        g.fillRect(bx + 2, by + 2 + askSel * 20, 76, 16);
        g.setColor(askSel == 0 ? 0x000000 : 0xFFFFFF);
        g.drawString("Place all (" + inv.cursorCount() + ")", W / 2, by + 3, Graphics.TOP | Graphics.HCENTER);
        g.setColor(askSel == 1 ? 0x000000 : 0xFFFFFF);
        g.drawString("Place 1", W / 2, by + 23, Graphics.TOP | Graphics.HCENTER);
    }

    g.setFont(Font.getDefaultFont());
}
}