import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;


final class World {
    static final int AREA = 32;               
    static final int CS = 16;                 
    static final int HEIGHT = 64;             
    static final int SECTIONS = HEIGHT / CS;  

static final int AIR = 0, GRASS = 1, DIRT = 2, STONE = 4, BRICKS = 5, WOOD = 7, LEAVES = 8;
private static final Random treeRand = new Random();
static final int FLAT = 9;
static final int MAX_TORCH = 64;
static final int MAX_THIN = 256;
static final int LIGHT_R = 8;                 
private static final int[] torchPos = new int[MAX_TORCH * 3];
private static int torchN;
private static final int[] thinPos = new int[MAX_THIN * 4];
private static int thinN;
    
    static final int OUT = -1;
    
    static final int UNLOADED = -2;

    private static final long DEFAULT_SEED = 18295169L;
    private static long seed = DEFAULT_SEED;
    private static final String FILE_NAME = "mc4kworld.txt";   // world slot 0 (also the old single save)
    static final int MAX_WORLDS = 5;
    private static int slot;                                   // world currently loaded
    private static final int MAX_CHUNKS = 16;
    
    private static final float EDGE = 0.5f;

    static final class Chunk {
        final int cx, cz;
        final byte[] b = new byte[CS * HEIGHT * CS];
        
final boolean[] dirty = new boolean[SECTIONS];
final boolean[] urgent = new boolean[SECTIONS];
        
        final Object[] view = new Object[SECTIONS];
        boolean modified;

        Chunk(int cx, int cz) {
            this.cx = cx;
            this.cz = cz;
        }
    }

    private static final Hashtable table = new Hashtable();
    private static final Chunk[] list = new Chunk[MAX_CHUNKS];
    private static int count;
    private static Chunk last;               
    private static Chunk genC;               

    
    static boolean sceneryOn = true;
    static boolean instantGen = false;
    private static final String OPT_NAME = "mc4kopts.txt";

    
    private static int areaX, areaZ;
    
    private static final int SHIFT = AREA - 1;
    private static float noffX = (int) ((DEFAULT_SEED >>> 8) & 511) + 0.25f;
    private static float noffZ = (int) ((DEFAULT_SEED >>> 20) & 511) + 0.25f;

    private static void applySeed(long s) {
        seed = s;
        noffX = (int) ((s >>> 8) & 511) + 0.25f;
        noffZ = (int) ((s >>> 20) & 511) + 0.25f;
    }

    
    private static boolean pValid;
    private static int pAreaX, pAreaZ;
    private static final float[] pState = new float[6];   
    private static int pTick = 1000;                       

    
    private static String invLine;

    
    private static final Hashtable saved = new Hashtable();
private static final Hashtable signs = new Hashtable();

    private World() {}
    private static boolean spawnValid;
    private static final float[] spawnState = new float[5];
    
static int torchCount() { return torchN; }
static int torchX(int i) { return torchPos[i * 3]; }
static int torchY(int i) { return torchPos[i * 3 + 1]; }
static int torchZ(int i) { return torchPos[i * 3 + 2]; }
static int thinCount() { return thinN; }
static int thinX(int i) { return thinPos[i * 4]; }
static int thinY(int i) { return thinPos[i * 4 + 1]; }
static int thinZ(int i) { return thinPos[i * 4 + 2]; }
static int thinType(int i) { return thinPos[i * 4 + 3]; }

static void setSpawn(float x, float y, float z) {
    spawnValid = true;
    spawnState[0] = areaX;
    spawnState[1] = areaZ;
    spawnState[2] = x;
    spawnState[3] = y;
    spawnState[4] = z;
}

static float[] savedSpawn() {
    return spawnValid ? spawnState : null;
}

static void moveToArea(int ax, int az) {
    enterArea(ax, az);
}

private static boolean isListedThin(int id) {
    return id == Items.FENCE || id == Items.SIGN || id == Items.DOOR || id == Items.DOOR_OPEN;
}

/** Thin or non-cube blocks: skipped by cube meshing, and the door tops. */
static boolean isThin(int id) {
    return isListedThin(id) || id == Items.DOOR_TOP || id == Items.DOOR_TOP_OPEN;
}

private static void addThin(int x, int y, int z, int type) {
    if (thinN >= MAX_THIN) return;
    thinPos[thinN * 4] = x;
    thinPos[thinN * 4 + 1] = y;
    thinPos[thinN * 4 + 2] = z;
    thinPos[thinN * 4 + 3] = type;
    thinN++;
}

private static void removeThin(int x, int y, int z) {
    for (int i = 0; i < thinN; i++) {
        if (thinPos[i * 4] == x && thinPos[i * 4 + 1] == y && thinPos[i * 4 + 2] == z) {
            thinN--;
            System.arraycopy(thinPos, thinN * 4, thinPos, i * 4, 4);
            return;
        }
    }
}

private static void dropThin(int cx, int cz) {
    int i = 0;
    while (i < thinN) {
        if ((thinPos[i * 4] >> 4) == cx && (thinPos[i * 4 + 2] >> 4) == cz) {
            thinN--;
            System.arraycopy(thinPos, thinN * 4, thinPos, i * 4, 4);
        } else {
            i++;
        }
    }
}

private static void scanThin(Chunk c) {
    for (int y = 0; y < HEIGHT; y++) {
        for (int z = 0; z < CS; z++) {
            for (int x = 0; x < CS; x++) {
                int id = c.b[(y << 8) | (z << 4) | x];
                if (isListedThin(id)) {
                    addThin((c.cx << 4) + x, y, (c.cz << 4) + z, id);
                }
            }
        }
    }
}

static String signKey(int x, int y, int z) {
    return areaX + "/" + areaZ + "/" + x + "/" + y + "/" + z;
}

static String signText(int x, int y, int z) {
    String t = (String) signs.get(signKey(x, y, z));
    return t == null ? "" : t;
}

static void setSignText(int x, int y, int z, String text) {
    String k = signKey(x, y, z);
    if (text == null || text.length() == 0) signs.remove(k);
    else signs.put(k, text);
    saveAll();
}

private static String escapeSign(String t) {
    StringBuffer b = new StringBuffer();
    for (int i = 0; i < t.length(); i++) {
        char c = t.charAt(i);
        if (c == '\\') b.append("\\\\");
        else if (c == '\n') b.append("\\n");
        else if (c != '\r') b.append(c);
    }
    return b.toString();
}

private static String unescapeSign(String t) {
    StringBuffer b = new StringBuffer();
    for (int i = 0; i < t.length(); i++) {
        char c = t.charAt(i);
        if (c == '\\' && i + 1 < t.length()) {
            char n = t.charAt(++i);
            b.append(n == 'n' ? '\n' : n);
        } else {
            b.append(c);
        }
    }
    return b.toString();
}

private static void readSign(String line) {
    try {
        String s = line.substring(5);
        int sp = s.indexOf(' ');
        if (sp < 0) return;
        signs.put(s.substring(0, sp), unescapeSign(s.substring(sp + 1)));
    } catch (Throwable e) {
        System.out.println("[world] sign line bad: " + e);
    }
}

private static void addTorch(int x, int y, int z) {
    if (torchN >= MAX_TORCH) return;
    torchPos[torchN * 3] = x;
    torchPos[torchN * 3 + 1] = y;
    torchPos[torchN * 3 + 2] = z;
    torchN++;
}

private static void removeTorch(int x, int y, int z) {
    for (int i = 0; i < torchN; i++) {
        if (torchPos[i * 3] == x && torchPos[i * 3 + 1] == y && torchPos[i * 3 + 2] == z) {
            torchN--;
            torchPos[i * 3] = torchPos[torchN * 3];
            torchPos[i * 3 + 1] = torchPos[torchN * 3 + 1];
            torchPos[i * 3 + 2] = torchPos[torchN * 3 + 2];
            return;
        }
    }
}

private static void dropTorches(int cx, int cz) {
    int i = 0;
    while (i < torchN) {
        if ((torchPos[i * 3] >> 4) == cx && (torchPos[i * 3 + 2] >> 4) == cz) {
            torchN--;
            torchPos[i * 3] = torchPos[torchN * 3];
            torchPos[i * 3 + 1] = torchPos[torchN * 3 + 1];
            torchPos[i * 3 + 2] = torchPos[torchN * 3 + 2];
        } else {
            i++;
        }
    }
}

private static void scanTorches(Chunk c) {
    scanThin(c);
    for (int y = 0; y < HEIGHT; y++) {
        for (int z = 0; z < CS; z++) {
            for (int x = 0; x < CS; x++) {
                if (c.b[(y << 8) | (z << 4) | x] == Items.TORCH) {
                    int wx = (c.cx << 4) + x, wz = (c.cz << 4) + z;
                    addTorch(wx, y, wz);
                    relight(wx, y, wz);
                }
            }
        }
    }
}


private static void relight(int x, int y, int z) {
    int R = LIGHT_R;
    int s0 = Math.max(0, (y - R) >> 4), s1 = Math.min(SECTIONS - 1, (y + R) >> 4);
    for (int cx = (x - R) >> 4; cx <= (x + R) >> 4; cx++) {
        for (int cz = (z - R) >> 4; cz <= (z + R) >> 4; cz++) {
            Chunk c = find(cx, cz);
            if (c == null) continue;
            for (int sy = s0; sy <= s1; sy++) {
    c.dirty[sy] = true;
    c.urgent[sy] = true;
}
        }
    }
}

    private static int key(int cx, int cz) {
        return (cx << 16) | (cz & 0xFFFF);
    }

    
    private static int storeKey(int ax, int az, int cx, int cz) {
        return ((ax & 0x3FFF) << 18) | ((az & 0x3FFF) << 4) | (cx << 1) | cz;
    }

    static Chunk find(int cx, int cz) {
        if (last != null && last.cx == cx && last.cz == cz) return last;
        Chunk c = (Chunk) table.get(new Integer(key(cx, cz)));
        if (c != null) last = c;
        return c;
    }

    static int chunkCount() {
        return count;
    }

    static Chunk chunkAt(int i) {
        return list[i];
    }

    

    private static int index(int x, int y, int z) {
        return (((y << 4) | (z & 15)) << 4) | (x & 15);
    }

    static int get(int x, int y, int z) {
        if (y >= HEIGHT) return OUT;
        if (y < 0) return AIR;   
        if (x < 0 || z < 0 || x >= AREA || z >= AREA) return OUT;
        Chunk c = find(x >> 4, z >> 4);
        if (c == null) return UNLOADED;
        return c.b[index(x, y, z)];
    }

    
    static int peek(int x, int y, int z) {
        if (y >= HEIGHT) return OUT;
        if (y < 0) return AIR;
        Chunk c = find(x >> 4, z >> 4);
        if (c == null) return UNLOADED;
        return c.b[index(x, y, z)];
    }

static void set(int x, int y, int z, int b) {
    if (y < 0 || y >= HEIGHT) return;
    if (x < 0 || z < 0 || x >= AREA || z >= AREA) return;
    int cx = x >> 4, cz = z >> 4;
    Chunk c = find(cx, cz);
    if (c == null) return;
int i = index(x, y, z);
if (c.b[i] == (byte) b) return;
int old = c.b[i];
c.b[i] = (byte) b;
c.modified = true;
if (old == Items.TORCH) removeTorch(x, y, z);
if (b == Items.TORCH) addTorch(x, y, z);
if (old == Items.TORCH || b == Items.TORCH) relight(x, y, z);
if (isListedThin(old)) removeThin(x, y, z);
if (isListedThin(b)) addThin(x, y, z, b);
if (old == Items.SIGN) signs.remove(signKey(x, y, z));
    int sy = y >> 4;
    markDirty(c, sy);

    int lx = x & 15, lz = z & 15, ly = y & 15;
    if (lx == 0) markDirty(find(cx - 1, cz), sy);
    if (lx == 15) markDirty(find(cx + 1, cz), sy);
    if (lz == 0) markDirty(find(cx, cz - 1), sy);
    if (lz == 15) markDirty(find(cx, cz + 1), sy);
    if (ly == 0 && sy > 0) markDirty(c, sy - 1);
    if (ly == 15 && sy < SECTIONS - 1) markDirty(c, sy + 1);
}

private static void markDirty(Chunk c, int sy) {
    if (c == null) return;
    c.dirty[sy] = true;
    c.urgent[sy] = true;
}

    
    static int dirtyCount() {
        int n = 0;
        for (int i = 0; i < count; i++) {
            for (int s = 0; s < SECTIONS; s++) if (list[i].dirty[s]) n++;
        }
        return n;
    }

    static int areaX() {
        return areaX;
    }

    static int areaZ() {
        return areaZ;
    }

    

    
    static void init() {
        loadOptions();
        readFile();
        
        enterArea(pValid ? pAreaX : 0, pValid ? pAreaZ : 0);
    }

    
    static void setPlayer(float x, float y, float z, float yaw, float pitch, int held, int tick) {
        pValid = true;
        pTick = tick;
        pAreaX = areaX;
        pAreaZ = areaZ;
        pState[0] = x;
        pState[1] = y;
        pState[2] = z;
        pState[3] = yaw;
        pState[4] = pitch;
        pState[5] = held;
    }

    
    static float[] savedPlayer() {
        return pValid ? pState : null;
    }

    
    static void setInventory(String s) {
        invLine = s;
    }

    
    static String savedInventory() {
        return invLine;
    }

    
    static int savedTick() {
        return pTick;
    }

    
    static boolean teleport(float[] p) {
        float x = p[0], z = p[2];
        int dx = 0, dz = 0;
        if (x < EDGE) dx = -1;
        else if (x > AREA - EDGE) dx = 1;
        if (z < EDGE) dz = -1;
        else if (z > AREA - EDGE) dz = 1;
        if (dx == 0 && dz == 0) return false;

        float nx = dx == 0 ? x : (dx < 0 ? x + (AREA - 1) : x - (AREA - 1));
        float nz = dz == 0 ? z : (dz < 0 ? z + (AREA - 1) : z - (AREA - 1));

        enterArea(areaX + dx, areaZ + dz);
        p[0] = nx;
        p[2] = nz;
        p[1] = surfaceY(nx, nz);
        return true;
    }

    
static float surfaceY(float x, float z) {
    int bx = Calc.floor(x), bz = Calc.floor(z);
    if (find(bx >> 4, bz >> 4) != null) {
        for (int y = 0; y < HEIGHT; y++) {
            if (!passable(get(bx, y, bz))) return Math.max(0f, y - 3f);
        }
        return 1f;
    }
    return Math.max(0f, terrainH(bx, bz) - 3f);   
}

    

private static final int[] qx = new int[MAX_CHUNKS], qz = new int[MAX_CHUNKS];
private static int qHead, qTail;
private static Chunk building;
private static boolean bScenery;
private static int bCol, bStage;                 
private static int tAx, tAz, tAxEnd, tAzStart, tAzEnd;

private static void enterArea(int ax, int az) {
    while (count > 0) unloadAt(count - 1);
    building = null; genC = null; qHead = qTail = 0;
    areaX = ax;
    areaZ = az;
    for (int cz = 0; cz < 2; cz++)
        for (int cx = 0; cx < 2; cx++) queueChunk(cx, cz);
    if (sceneryOn) {
        for (int cz = -1; cz < 3; cz++)
            for (int cx = -1; cx < 3; cx++)
                if (cx < 0 || cx > 1 || cz < 0 || cz > 1) queueChunk(cx, cz);
    }
}

private static void queueChunk(int cx, int cz) {
    qx[qTail] = cx; qz[qTail] = cz; qTail++;
}


static boolean areaReady() {
    int n = 0;
    for (int i = 0; i < count; i++) if (isArea(list[i])) n++;
    return n == 4;
}

static boolean generating() { return building != null || qHead < qTail; }


static void tick(long deadline) {
    do {
        if (building == null) {
            if (qHead >= qTail) return;
            startChunk(qx[qHead], qz[qHead]);
            qHead++;
            if (building == null) continue;
        }
        stepBuild(deadline);
    } while (System.currentTimeMillis() < deadline);
}

private static void startChunk(int cx, int cz) {
    Chunk c = new Chunk(cx, cz);
    if (restore(c)) { finishChunk(c); return; }
    building = c;
    bScenery = !isArea(c);
    bCol = 0;
    bStage = 0;
}

private static void finishChunk(Chunk c) {
    list[count++] = c;
    table.put(new Integer(key(c.cx, c.cz)), c);
    if (isArea(c)) scanTorches(c);
    last = c;
    c.modified = false;
    for (int s = 0; s < SECTIONS; s++) c.dirty[s] = true;
    building = null;
}

private static void stepBuild(long deadline) {
    Chunk c = building;
    genC = c;
    if (bStage == 0) {
        do {
            genColumn(c, bCol & 15, bCol >> 4, bScenery);
            bCol++;
        } while (bCol < CS * CS && System.currentTimeMillis() < deadline);
        genC = null;
        if (bCol < CS * CS) return;
        if (bScenery) { finishChunk(c); return; }
        int gox = areaX * SHIFT + (c.cx << 4), goz = areaZ * SHIFT + (c.cz << 4);
        tAx = (gox - 20) >> 3;  tAxEnd = (gox + 36) >> 3;
        tAzStart = (goz - 20) >> 3;  tAzEnd = (goz + 36) >> 3;
        tAz = tAzStart;
        bStage = 1;
        return;
    }
    do {
        placeTree(tAx, tAz);
        if (++tAz > tAzEnd) { tAz = tAzStart; tAx++; }
    } while (tAx <= tAxEnd && System.currentTimeMillis() < deadline);
    genC = null;
    if (tAx > tAxEnd) finishChunk(c);
}

private static void genColumn(Chunk c, int lx, int lz, boolean scenery) {
    int gx = (c.cx << 4) + lx, gz = (c.cz << 4) + lz;
    int th = terrainH(gx, gz);
    int baseIdx = (lz << 4) | lx;
    byte[] b = c.b;
    if (scenery) {
        for (int y = th; y < HEIGHT; y++) b[(y << 8) | baseIdx] = (byte) GRASS;
        return;
    }
    if (th < HEIGHT) b[(th << 8) | baseIdx] = (byte) GRASS;
    int dirtEnd = Math.min(th + 5 + 1, HEIGHT);
    for (int y = th + 1; y < dirtEnd; y++) b[(y << 8) | baseIdx] = (byte) DIRT;
    for (int y = dirtEnd; y < HEIGHT; y++) b[(y << 8) | baseIdx] = (byte) oreOrStone(gx, y, gz);
}

    private static void loadChunk(int cx, int cz) {
        Chunk c = new Chunk(cx, cz);
        list[count++] = c;
        table.put(new Integer(key(cx, cz)), c);
        last = c;

        if (!restore(c)) generate(c);
        c.modified = false;
        for (int s = 0; s < SECTIONS; s++) c.dirty[s] = true;
    }

private static void unloadAt(int i) {
    Chunk c = list[i];
    saveChunk(c);
    dropTorches(c.cx, c.cz);
    dropThin(c.cx, c.cz);
    table.remove(new Integer(key(c.cx, c.cz)));
    if (last == c) last = null;
    list[i] = list[--count];
    list[count] = null;
}

    
    static void saveAll() {
        for (int i = 0; i < count; i++) saveChunk(list[i]);   
        writeFile();
    }

    
    static void close() {
        saveAll();
    }

    
    static boolean hasSave() {
        return hasSave(slot);
    }

    /** True if world slot s has a save file with content. */
    static boolean hasSave(int s) {
        if (s == slot && saved.size() > 0) return true;
        FileConnection fc = null;
        try {
            String p = pathFor(s);
            if (p == null) return false;
            fc = (FileConnection) Connector.open(p, Connector.READ);
            return fc.exists() && fc.fileSize() > 13;   
        } catch (Throwable t) {
            return false;
        } finally {
            try { if (fc != null) fc.close(); } catch (Throwable t) { }
        }
    }

static boolean passable(int id) {
    return id == AIR || id == Items.TORCH || id == Items.DOOR_OPEN || id == Items.DOOR_TOP_OPEN;
}
    
static int currentSlot() {
    return slot;
}

private static int lastWorld;

/** The world the player had open last time (used by the menu's Continue). */
static int lastWorld() {
    return lastWorld;
}

/** Make slot s the loaded world. Game's constructor (World.init) then reads its file. */
static void selectWorld(int s, boolean fresh) {
    if (s < 0 || s >= MAX_WORLDS) s = 0;
    slot = s;
    clearMemory();
    applySeed(DEFAULT_SEED);
    if (fresh) {
        // every new world gets its own terrain
        applySeed((new Random()).nextLong() & 0x7FFFFFFFL);
        writeFile();
    }
    lastWorld = s;
    saveOptions();   // remembers the last played world
}

/** Delete world slot s from storage. */
static void deleteWorld(int s) {
    if (s == slot) clearMemory();
    FileConnection fc = null;
    try {
        String p = pathFor(s);
        if (p == null) return;
        fc = (FileConnection) Connector.open(p, Connector.READ_WRITE);
        if (fc.exists()) fc.delete();
    } catch (Throwable t) {
        System.out.println("[world] delete failed: " + t);
    } finally {
        try { if (fc != null) fc.close(); } catch (Throwable t) { }
    }
}

static void deleteSave() {
    clearMemory();
    writeFile();
}

private static void clearMemory() {
    saved.clear();
    invLine = null;
    pValid = false;
    spawnValid = false;
    pTick = 1000;
    while (count > 0) list[--count] = null;
    table.clear();
    last = null;
    torchN = 0;
    thinN = 0;
    signs.clear();
}

private static int terrainH(int gx, int gz) {
    float wx = areaX * SHIFT + gx;
    float wz = areaZ * SHIFT + gz;
    return Calc.round(32f + Noise.noise(wx / 16f + noffX, wz / 16f + noffZ) * 12f);
}

    
    private static int oreOrStone(int gx, int y, int gz) {
        int wx = areaX * SHIFT + gx, wz = areaZ * SHIFT + gz;
        int h = (wx * 73856093) ^ (y * 19349663) ^ (wz * 83492791);
        h = (h ^ (h >>> 13)) & 0x7FFFFFFF;
        if (h % 40 == 0) return Items.COAL_ORE;
        if (h % 150 == 1) return Items.IRON_ORE;
        return STONE;
    }

    
    private static void put(int x, int y, int z, int b) {
        if (y < 0 || y >= HEIGHT) return;
        if ((x >> 4) != genC.cx || (z >> 4) != genC.cz) return;
        genC.b[index(x, y, z)] = (byte) b;
    }

    
    private static int at(int x, int y, int z) {
        if (y >= HEIGHT) return OUT;
        if (y < 0) return AIR;
        if ((x >> 4) != genC.cx || (z >> 4) != genC.cz) return UNLOADED;
        return genC.b[index(x, y, z)];
    }

    private static void fillBox(int b, int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                for (int z = z0; z < z1; z++) {
                    if (at(x, y, z) != AIR) continue;
                    put(x, y, z, b);
                }
            }
        }
    }

private static void generate(Chunk c) {
        genC = c;
        int ox = c.cx << 4, oz = c.cz << 4;
        final int stoneDepth = 5;
        final boolean scenery = !isArea(c);
        byte[] b = c.b;

        for (int lx = 0; lx < CS; lx++) {
            int gx = ox + lx;
            for (int lz = 0; lz < CS; lz++) {
                int gz = oz + lz;
                int th = terrainH(gx, gz);
                int baseIdx = (lz << 4) | lx; 

                if (scenery) {
                    for (int y = th; y < HEIGHT; y++) {
                        b[(y << 8) | baseIdx] = (byte) GRASS;
                    }
                } else {
                    
                    if (th < HEIGHT) {
                        b[(th << 8) | baseIdx] = (byte) GRASS;
                    }
                    
                    
                    int dirtEnd = Math.min(th + stoneDepth + 1, HEIGHT);
                    for (int y = th + 1; y < dirtEnd; y++) {
                        b[(y << 8) | baseIdx] = (byte) DIRT;
                    }

                    
                    for (int y = dirtEnd; y < HEIGHT; y++) {
                        b[(y << 8) | baseIdx] = (byte) oreOrStone(gx, y, gz);
                    }
                }
            }
        }

        if (!scenery) {
            int gox = areaX * SHIFT + ox, goz = areaZ * SHIFT + oz;
            for (int ax = (gox - 20) >> 3; ax <= (gox + 36) >> 3; ax++) {
                for (int az = (goz - 20) >> 3; az <= (goz + 36) >> 3; az++) {
                    placeTree(ax, az);
                }
            }
        }
        genC = null;
    }

    
private static void placeTree(int ax, int az) {
        treeRand.setSeed(seed ^((long) ax * 73856093L) ^ ((long) az * 19349663L));
        if (treeRand.nextInt(4) != 0) return;

        int tx = ax * 8 + 4 + (treeRand.nextInt(4) - 2) - areaX * SHIFT;
        int tz = az * 8 + 4 + (treeRand.nextInt(4) - 2) - areaZ * SHIFT;
        int th = terrainH(tx, tz) - 1;
        int tH = 4 + treeRand.nextInt(2);

        for (int y = th; y >= th - tH; y--) put(tx, y, tz, WOOD);

        fillBox(LEAVES, tx - 2, th - tH + 1, tz - 2, tx + 3, th - tH + 3, tz + 3);
        fillBox(LEAVES, tx - 1, th - tH - 1, tz - 1, tx + 2, th - tH + 1, tz + 2);

        int[] fx = { tx - 2, tx - 2, tx + 2, tx + 2 };
        int[] fz = { tz - 2, tz + 2, tz + 2, tz - 2 };
        int[] cx = { tx - 1, tx - 1, tx + 1, tx + 1 };
        int[] cz = { tz - 1, tz + 1, tz + 1, tz - 1 };

        for (int i = 0; i < 4; i++) {
            int foliageCut = treeRand.nextInt(10);
            switch (foliageCut) {
                case 0:
                    put(fx[i], th - tH + 1, fz[i], AIR);
                    break;
                case 1:
                    put(fx[i], th - tH + 2, fz[i], AIR);
                    break;
                case 2:
                    put(fx[i], th - tH + 1, fz[i], AIR);
                    put(fx[i], th - tH + 2, fz[i], AIR);
                    break;
                default:
                    break;
            }

            int crownCut = treeRand.nextInt(10);
            if (crownCut == 0) {
                put(cx[i], th - tH - 1, cz[i], AIR);
                put(cx[i], th - tH, cz[i], AIR);
            } else {
                put(cx[i], th - tH - 1, cz[i], AIR);
            }
        }
    }


    

    
    private static String encodeText(Chunk c) {
        StringBuffer sb = new StringBuffer();
        sb.append(areaX).append(' ').append(areaZ).append(' ')
          .append(c.cx).append(' ').append(c.cz).append('|');
        int n = c.b.length, i = 0;
        while (i < n) {
            byte v = c.b[i];
            int run = 1;
            while (i + run < n && c.b[i + run] == v) run++;
            sb.append(run).append(':').append((int) v).append(' ');
            i += run;
        }
        return sb.toString();
    }

    
    private static boolean isArea(Chunk c) {
        return c.cx >= 0 && c.cx < 2 && c.cz >= 0 && c.cz < 2;
    }

    
    private static void saveChunk(Chunk c) {
        if (!isArea(c)) return;
        saved.put(new Integer(storeKey(areaX, areaZ, c.cx, c.cz)), encodeText(c));
        c.modified = false;
    }

    
    private static boolean restore(Chunk c) {
        if (!isArea(c)) return false;
        String line = (String) saved.get(new Integer(storeKey(areaX, areaZ, c.cx, c.cz)));
        if (line == null) return false;
        try {
            int pos = line.indexOf('|') + 1;
            int i = 0, len = line.length();
            while (pos < len && i < c.b.length) {
                int sp = line.indexOf(' ', pos);
                if (sp < 0) break;
                int colon = line.indexOf(':', pos);
                int run = Integer.parseInt(line.substring(pos, colon));
                byte v = (byte) Integer.parseInt(line.substring(colon + 1, sp));
                for (int k = 0; k < run && i < c.b.length; k++) c.b[i++] = v;
                pos = sp + 1;
            }
            return true;
        } catch (Throwable t) {
            System.out.println("[world] restore failed: " + t);
            return false;
        }
    }

    
    private static String optPath() {
        if (filePath() == null) return null;
        return basePath + OPT_NAME;   // options are shared by all worlds
    }

    private static String fileName(int s) {
        return s == 0 ? FILE_NAME : "mc4kworld" + s + ".txt";
    }

    private static String pathFor(int s) {
        if (filePath() == null) return null;
        return basePath + fileName(s);
    }

    private static boolean anyWorldAt(String root) {
        for (int s = 0; s < MAX_WORLDS; s++) {
            if (fileExists(root + fileName(s))) return true;
        }
        return false;
    }

static void loadOptions() {
        FileConnection fc = null;
        InputStream is = null;
        try {
            String p = optPath();
            if (p == null) return;
            fc = (FileConnection) Connector.open(p, Connector.READ);
            if (!fc.exists()) return;
            is = fc.openInputStream();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] b = new byte[256];
            int n;
            while ((n = is.read(b)) > 0) bo.write(b, 0, n);
            String s = new String(bo.toByteArray());

            int start = 0;
            while (start < s.length()) {
                int nl = s.indexOf('\n', start);
                if (nl < 0) nl = s.length();
                String line = s.substring(start, nl).trim();
                start = nl + 1;
                if (line.length() == 0) continue;

                if (line.startsWith("scenery ")) {
                    sceneryOn = line.substring(8).trim().equals("1");
                } else if (line.startsWith("instantGen ")) {        
                    instantGen = line.substring(11).trim().equals("1");
                } else if (line.startsWith("info ")) {
                    Game.showInfo = line.substring(5).trim().equals("1");
                } else if (line.startsWith("fov ")) {
                    M3GRenderer.fov = Integer.parseInt(line.substring(4).trim());
                } else if (line.startsWith("texDist ")) {
                    M3GRenderer.texDist = Integer.parseInt(line.substring(8).trim());
                } else if (line.startsWith("targetFps ")) {
                    Game.targetFps = Integer.parseInt(line.substring(10).trim());
                } else if (line.startsWith("autoJump ")) {
                    Game.autoJump = line.substring(9).trim().equals("1");
                } else if (line.startsWith("touch ")) {
                    Game.touchOn = line.substring(6).trim().equals("1");
                    Game.touchSet = true;
                } else if (line.startsWith("lastWorld ")) {
                    int w = Integer.parseInt(line.substring(10).trim());
                    if (w >= 0 && w < MAX_WORLDS) lastWorld = w;
                }
            }
        } catch (Throwable t) {
            System.out.println("[world] options read failed: " + t);
        } finally {
            try { if (is != null) is.close(); } catch (Throwable t) { }
            try { if (fc != null) fc.close(); } catch (Throwable t) { }
        }
    }

    static void setScenery(boolean on) {
        sceneryOn = on;
        saveOptions();
    }
    
    static void setInstantGen(boolean on) {    
        instantGen = on;
        saveOptions();
    }

    static void saveOptions() {
        FileConnection fc = null;
        OutputStream os = null;
        try {
            String p = optPath();
            if (p == null) return;
            fc = (FileConnection) Connector.open(p, Connector.READ_WRITE);
            if (!fc.exists()) {
                fc.create();
            } else {
                try {
                    fc.truncate(0);
                } catch (Throwable t) {
                    fc.delete();
                    fc.create();
                }
            }
            os = fc.openOutputStream();
            StringBuffer sb = new StringBuffer();
            sb.append("scenery ").append(sceneryOn ? 1 : 0).append('\n');
            sb.append("instantGen ").append(instantGen ? 1 : 0).append('\n');   
            sb.append("info ").append(Game.showInfo ? 1 : 0).append('\n');
            sb.append("fov ").append(M3GRenderer.fov).append('\n');
            sb.append("texDist ").append(M3GRenderer.texDist).append('\n');
            sb.append("targetFps ").append(Game.targetFps).append('\n');
            sb.append("autoJump ").append(Game.autoJump ? 1 : 0).append('\n');
            if (Game.touchSet) sb.append("touch ").append(Game.touchOn ? 1 : 0).append('\n');
            sb.append("lastWorld ").append(lastWorld).append('\n');
            os.write(sb.toString().getBytes());
            os.flush();
        } catch (Throwable t) {
            System.out.println("[world] options write failed: " + t);
        } finally {
            try { if (os != null) os.close(); } catch (Throwable t) { }
            try { if (fc != null) fc.close(); } catch (Throwable t) { }
        }
    }

    private static String basePath;
    
    static String fileStatus = "";

    private static void addRoot(Vector v, String r) {
        if (r == null || r.length() == 0) return;
        if (!r.endsWith("/")) r += "/";
        if (!r.startsWith("file:")) r = "file:///" + r;
        for (int i = 0; i < v.size(); i++) {
            if (r.equals(v.elementAt(i))) return;
        }
        v.addElement(r);
    }

    
    private static Vector roots() {
        Vector v = new Vector();
        addRoot(v, System.getProperty("fileconn.dir.memorycard"));
        addRoot(v, System.getProperty("fileconn.dir.photos"));
        addRoot(v, System.getProperty("fileconn.dir.private"));
        try {
            Enumeration e = FileSystemRegistry.listRoots();
            while (e.hasMoreElements()) addRoot(v, (String) e.nextElement());
        } catch (Throwable t) {
            System.out.println("[world] listRoots failed: " + t);
        }
        addRoot(v, "E:/");
        addRoot(v, "C:/");
        return v;
    }

    private static boolean fileExists(String p) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(p, Connector.READ);
            return fc.exists();
        } catch (Throwable t) {
            return false;
        } finally {
            try { if (fc != null) fc.close(); } catch (Throwable t) { }
        }
    }

    private static boolean canCreate(String p) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(p, Connector.READ_WRITE);
            if (!fc.exists()) {
                fc.create();
                fc.delete();
            }
            return true;
        } catch (Throwable t) {
            System.out.println("[world] cannot write " + p + " : " + t);
            return false;
        } finally {
            try { if (fc != null) fc.close(); } catch (Throwable t) { }
        }
    }

    
    private static String filePath() {
        if (basePath == null) {
            Vector v = roots();
            for (int i = 0; i < v.size() && basePath == null; i++) {
                String r = (String) v.elementAt(i);
                if (anyWorldAt(r)) basePath = r;
            }
            for (int i = 0; i < v.size() && basePath == null; i++) {
                String r = (String) v.elementAt(i);
                if (canCreate(r + FILE_NAME)) basePath = r;
            }
            if (basePath == null) {
                fileStatus = "no save folder";
                return null;
            }
            fileStatus = basePath;
            System.out.println("[world] save file: " + basePath + FILE_NAME);
        }
        return basePath + fileName(slot);
    }

    private static void writeFile() {
        FileConnection fc = null;
        OutputStream os = null;
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("mc4k world 1\n");
            if (seed != DEFAULT_SEED) sb.append("seed ").append(seed).append('\n');
            if (pValid) {
                sb.append("player ").append(pAreaX).append(' ').append(pAreaZ);
                for (int i = 0; i < 6; i++) sb.append(' ').append(pState[i]);
                sb.append('\n');
                sb.append("time ").append(pTick).append('\n');
            }
            if (spawnValid) {
    sb.append("spawn ");
    for (int i = 0; i < 5; i++) {
        if (i > 0) sb.append(' ');
        sb.append(spawnState[i]);
    }
    sb.append('\n');
}
if (invLine != null) sb.append("inv ").append(invLine).append('\n');
            Enumeration se = signs.keys();
            while (se.hasMoreElements()) {
                String k = (String) se.nextElement();
                sb.append("sign ").append(k).append(' ').append(escapeSign((String) signs.get(k))).append('\n');
            }
            Enumeration e = saved.elements();
            while (e.hasMoreElements()) sb.append((String) e.nextElement()).append('\n');
            byte[] data = sb.toString().getBytes();

            fc = (FileConnection) Connector.open(filePath(), Connector.READ_WRITE);
            if (!fc.exists()) {
                fc.create();
            } else {
                try {
                    fc.truncate(0);
                } catch (Throwable t) {
                    fc.delete();
                    fc.create();
                }
            }
            os = fc.openOutputStream();
            os.write(data);
            os.flush();
            os.close();
            os = null;
            long size = fc.fileSize();   
            System.out.println("[world] wrote " + data.length + " bytes, file is " + size);
            fileStatus = "saved " + size + " bytes";
        } catch (Throwable t) {
            System.out.println("[world] file write failed: " + t);
            fileStatus = filePath() == null ? "no save folder" : "save failed";
        } finally {
            try { if (os != null) os.close(); } catch (Throwable t) { }
            try { if (fc != null) fc.close(); } catch (Throwable t) { }
        }
    }
    
private static void readSpawn(String line) {
    try {
        String s = line.substring(6).trim();
        int p = 0;
        for (int k = 0; k < 5; k++) {
            int sp = k < 4 ? s.indexOf(' ', p) : s.length();
            spawnState[k] = Float.parseFloat(s.substring(p, sp));
            p = sp + 1;
        }
        spawnValid = true;
    } catch (Throwable e) {
        System.out.println("[world] spawn line bad: " + e);
    }
}

    
    private static void readPlayer(String line) {
        try {
            line = line.trim();
            String[] t = new String[8];
            int p = 7;   
            for (int k = 0; k < 8; k++) {
                int sp = line.indexOf(' ', p);
                if (sp < 0) sp = line.length();
                t[k] = line.substring(p, sp);
                p = sp + 1;
            }
            pAreaX = Integer.parseInt(t[0]);
            pAreaZ = Integer.parseInt(t[1]);
            for (int k = 0; k < 6; k++) pState[k] = Float.parseFloat(t[2 + k]);
            pValid = true;
        } catch (Throwable e) {
            System.out.println("[world] player line bad: " + e);
        }
    }

    private static void readFile() {
        FileConnection fc = null;
        InputStream is = null;
        try {
            fc = (FileConnection) Connector.open(filePath(), Connector.READ);
            if (!fc.exists()) return;
            is = fc.openInputStream();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int n;
            while ((n = is.read(buf)) > 0) bo.write(buf, 0, n);
            String all = new String(bo.toByteArray());

            int start = 0;
            while (start < all.length()) {
                int nl = all.indexOf('\n', start);
                if (nl < 0) nl = all.length();
                String line = all.substring(start, nl);
                start = nl + 1;
                if (line.startsWith("seed ")) {
                    try {
                        applySeed(Long.parseLong(line.substring(5).trim()));
                    } catch (Throwable t) {
                        System.out.println("[world] seed line bad: " + t);
                    }
                    continue;
                }
                if (line.startsWith("player ")) {
                    readPlayer(line);
                    continue;
                }
                if (line.startsWith("spawn ")) {
                readSpawn(line);
                continue;
                }
                if (line.startsWith("sign ")) {
                    readSign(line);
                    continue;
                }
                if (line.startsWith("inv ")) {
                    invLine = line.substring(4);
                    continue;
                }
                if (line.startsWith("time ")) {
                    try {
                        pTick = Integer.parseInt(line.substring(5).trim());
                    } catch (Throwable t) {
                        System.out.println("[world] time line bad: " + t);
                    }
                    continue;
                }
                int bar = line.indexOf('|');
                if (bar < 0) continue;
                String[] h = new String[4];
                int p = 0;
                for (int k = 0; k < 4; k++) {
                    int sp = k < 3 ? line.indexOf(' ', p) : bar;
                    h[k] = line.substring(p, sp);
                    p = sp + 1;
                }
                int key = storeKey(Integer.parseInt(h[0]), Integer.parseInt(h[1]),
                                   Integer.parseInt(h[2]), Integer.parseInt(h[3]));
                saved.put(new Integer(key), line);
            }
            System.out.println("[world] loaded " + saved.size() + " saved chunks");
            fileStatus = "loaded " + saved.size() + " chunks";
        } catch (Throwable t) {
            System.out.println("[world] file read failed: " + t);
        } finally {
            try { if (is != null) is.close(); } catch (Throwable t) { }
            try { if (fc != null) fc.close(); } catch (Throwable t) { }
        }
    }
}