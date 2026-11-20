import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
import javax.microedition.lcdui.Image;

final class Calc {
    private Calc() {}

    

    
    static int floor(float v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    
    static int round(float v) {
        return (int) Math.floor(v + 0.5f);
    }

    
    static int mod(int a, int n) {
        int r = a % n;
        return r < 0 ? r + n : r;
    }

    
    static int wrapDeg(int deg) {
        return mod(deg, 360);
    }

    

    static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    
    static int channel(float unit) {
        int c = (int) (unit * 255f);
        return c < 0 ? 0 : (c > 255 ? 255 : c);
    }

    

    static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    
    static float fract(float v) {
        return v - floor(v);
    }

    
    static float scaledCosine(float t) {
        return (float) (0.5 * (1.0 - Math.cos(t * Math.PI)));
    }

    

    static float dist2(float dx, float dy, float dz) {
        return dx * dx + dy * dy + dz * dz;
    }

    static float dist(float dx, float dy, float dz) {
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    

    
    static float atan2(float y, float x) {
        final float pi = 3.1415927f;
        if (x == 0f) return y > 0f ? pi / 2f : (y == 0f ? 0f : -pi / 2f);
        float z = y / x;
        if (Math.abs(z) < 1f) {
            float a = z / (1f + 0.28f * z * z);
            if (x < 0f) return y < 0f ? a - pi : a + pi;
            return a;
        } else {
            float a = pi / 2f - z / (z * z + 0.28f);
            return y < 0f ? a - pi : a;
        }
    }

    
    static float toDeg(float rad) {
        return rad * 57.29578f;
    }
}

final class FastMath {
    private static final int TRIG_SETS = 360;
    private static final float[] SIN_TABLE = new float[TRIG_SETS];
    private static final float[] COS_TABLE = new float[TRIG_SETS];

    static {
        for (int i = 0; i < TRIG_SETS; i++) {
            double rad = Math.toRadians(i);
            SIN_TABLE[i] = (float) Math.sin(rad);
            COS_TABLE[i] = (float) Math.cos(rad);
        }
    }

    
    static float sinDeg(int deg) {
        deg = (deg % 360 + 360) % 360;
        return SIN_TABLE[deg];
    }

    
    static float cosDeg(int deg) {
        deg = (deg % 360 + 360) % 360;
        return COS_TABLE[deg];
    }

    
    static float invSqrt(float x) {
        float xhalf = 0.5f * x;
        int i = Float.floatToIntBits(x);
        i = 0x5f3759df - (i >> 1);
        x = Float.intBitsToFloat(i);
        x = x * (1.5f - xhalf * x * x); 
        return x;
    }

    
    static void normalize(float[] v) {
        float lenSq = v[0] * v[0] + v[1] * v[1] + v[2] * v[2];
        if (lenSq < 1e-6f) return;
        float invLen = invSqrt(lenSq);
        v[0] *= invLen;
        v[1] *= invLen;
        v[2] *= invLen;
    }
}

final class Noise {
    static final int RES = 1024;
    static final int YWRAPB = 4;
    static final int YWRAP = 1 << YWRAPB;
    static final int ZWRAPB = 8;
    static final int ZWRAP = 1 << ZWRAPB;
    static final int OCTAVES = 4;

    private static final float[] perlin = new float[RES + 1];

    static {
        Random r = new Random(18295169L);
        for (int i = 0; i <= RES; i++) {
            perlin[i] = (r.nextInt() & 0x7FFFFFFF) / 2147483647f;
        }
    }

    private Noise() {}



    static float noise(float x, float y) {
        if (x < 0) x = -x;
        if (y < 0) y = -y;

        int xi = (int) x;
        int yi = (int) y;
        float xf = x - xi;
        float yf = y - yi;

        float r = 0;
        float ampl = 0.5f;
        float rxf, ryf, n1, n2, n3;

        for (int i = 0; i < OCTAVES; i++) {
            int of = xi + (yi << YWRAPB);

            rxf = Calc.scaledCosine(xf);
            ryf = Calc.scaledCosine(yf);

            n1 = perlin[of % RES];
            n1 += rxf * (perlin[(of + 1) % RES] - n1);
            n2 = perlin[(of + YWRAP) % RES];
            n2 += rxf * (perlin[(of + YWRAP + 1) % RES] - n2);
            n1 += ryf * (n2 - n1);

            of += ZWRAP;
            n2 = perlin[of % RES];
            n2 += rxf * (perlin[(of + 1) % RES] - n2);
            n3 = perlin[(of + YWRAP) % RES];
            n3 += rxf * (perlin[(of + YWRAP + 1) % RES] - n3);
            n2 += ryf * (n3 - n2);

            n1 += Calc.scaledCosine(0) * (n2 - n1);

            r += n1 * ampl;
            ampl *= 0.5f;
            xi <<= 1;
            xf *= 2;
            yi <<= 1;
            yf *= 2;

            if (xf >= 1.0f) {
                xi++;
                xf--;
            }
            if (yf >= 1.0f) {
                yi++;
                yf--;
            }
        }
        return r;
    }
}

final class Items {
    static final int PLANKS = 10;
    static final int CRAFT_TABLE = 11;
    static final int COBBLE = 12;
    static final int COAL_ORE = 13;
    static final int IRON_ORE = 14;
    static final int STICK = 15;
    static final int COAL = 16;
    static final int WOOD_SWORD = 17;
    static final int STONE_SWORD = 18;
    static final int WOOL = 19;
    static final int BED = 20;
    static final int WOOD_PICKAXE = 21;
    static final int STONE_PICKAXE = 22;
    static final int WOOD_AXE = 23;
    static final int STONE_AXE = 24;
    static final int TORCH = 25;
    static final int BED_HEAD = 26;
    static final int TNT = 27;
    static final int IRON_BLOCK = 28;
    static final int APPLE = 29;
    static final int IRON_INGOT = 30;
    static final int IRON_SWORD = 31;
    static final int IRON_PICKAXE = 32;
    static final int IRON_AXE = 33;
    static final int SIGN = 34;
    static final int DOOR = 35;
    static final int DOOR_TOP = 36;
    static final int FENCE = 37;
    static final int DOOR_OPEN = 38;
    static final int DOOR_TOP_OPEN = 39;
    static final int FURNACE = 40;
static final int CHEST = 41;
static final int SLAB = 42;
static final int STAIR = 43;      
static final int STAIR_N = 44;    
static final int STAIR_E = 45;    
static final int STAIR_S = 46;    
static final int STAIR_W = 47;    
static final int MAX_ID = STAIR_W;
    static final int STACK = 64;

    static final String[] NAMES = { "", "Grass", "Dirt", "", "Stone", "Bricks", "", "Wood", "Leaves", "Scenery",
        "Planks", "Crafting Table", "Cobblestone", "Coal Ore", "Iron Ore", "Stick", "Coal", "Wooden Sword", "Stone Sword",
        "Wool", "Bed", "Wooden Pickaxe", "Stone Pickaxe", "Wooden Axe", "Stone Axe", "Torch", "Bed",
        "TNT", "Iron Block", "Apple", "Iron Ingot", "Iron Sword", "Iron Pickaxe", "Iron Axe",
        "Wooden Sign", "Wooden Door", "Wooden Door", "Fence",
        "Wooden Door", "Wooden Door", "Furnace", "Chest", "Cobble Slab", "Cobble Stairs",
        "Cobble Stairs", "Cobble Stairs", "Cobble Stairs", "Cobble Stairs"};

    static final Recipe[] RECIPES = {
        new Recipe(1, 1, new int[] { World.WOOD }, PLANKS, 4),
        new Recipe(2, 2, new int[] { World.STONE, World.DIRT, World.DIRT, World.STONE }, World.BRICKS, 4),
        new Recipe(2, 2, new int[] { PLANKS, PLANKS, PLANKS, PLANKS }, CRAFT_TABLE, 1),
        new Recipe(1, 2, new int[] { PLANKS, PLANKS }, STICK, 4),
        new Recipe(1, 3, new int[] { PLANKS, PLANKS, STICK }, WOOD_SWORD, 1),
        new Recipe(1, 3, new int[] { COBBLE, COBBLE, STICK }, STONE_SWORD, 1),
        new Recipe(3, 2, new int[] { WOOL, WOOL, WOOL, PLANKS, PLANKS, PLANKS }, BED, 1),
        new Recipe(3, 3, new int[] { PLANKS, PLANKS, PLANKS, 0, STICK, 0, 0, STICK, 0 }, WOOD_PICKAXE, 1),
        new Recipe(3, 3, new int[] { COBBLE, COBBLE, COBBLE, 0, STICK, 0, 0, STICK, 0 }, STONE_PICKAXE, 1),
        new Recipe(2, 3, new int[] { PLANKS, PLANKS, PLANKS, STICK, 0, STICK }, WOOD_AXE, 1),
        new Recipe(2, 3, new int[] { COBBLE, COBBLE, COBBLE, STICK, 0, STICK }, STONE_AXE, 1),
        new Recipe(1, 2, new int[] { COAL, STICK }, TORCH, 4),
        new Recipe(1, 1, new int[] { IRON_BLOCK }, IRON_INGOT, 4),
        new Recipe(2, 2, new int[] { IRON_INGOT, IRON_INGOT, IRON_INGOT, IRON_INGOT }, IRON_BLOCK, 1),
        new Recipe(2, 2, new int[] { COAL, COAL, COAL, COAL }, TNT, 1),
        new Recipe(1, 3, new int[] { IRON_INGOT, IRON_INGOT, STICK }, IRON_SWORD, 1),
        new Recipe(3, 3, new int[] { IRON_INGOT, IRON_INGOT, IRON_INGOT, 0, STICK, 0, 0, STICK, 0 }, IRON_PICKAXE, 1),
        new Recipe(2, 3, new int[] { IRON_INGOT, IRON_INGOT, IRON_INGOT, STICK, 0, STICK }, IRON_AXE, 1),
        new Recipe(3, 2, new int[] { PLANKS, PLANKS, PLANKS, 0, STICK, 0 }, SIGN, 3),
        new Recipe(2, 3, new int[] { PLANKS, PLANKS, PLANKS, PLANKS, PLANKS, PLANKS }, DOOR, 3),
        new Recipe(3, 2, new int[] { STICK, PLANKS, STICK, STICK, PLANKS, STICK }, FENCE, 3),
        new Recipe(3, 3, new int[] { COBBLE, COBBLE, COBBLE, COBBLE, 0, COBBLE, COBBLE, COBBLE, COBBLE }, FURNACE, 1),
        new Recipe(3, 3, new int[] { PLANKS, PLANKS, PLANKS, PLANKS, 0, PLANKS, PLANKS, PLANKS, PLANKS }, CHEST, 1),
        new Recipe(3, 1, new int[] { COBBLE, COBBLE, COBBLE }, SLAB, 6),
        new Recipe(3, 3, new int[] { COBBLE, 0, 0, COBBLE, COBBLE, 0, COBBLE, COBBLE, COBBLE }, STAIR, 4),
    };

    static int durability(int id) {
        switch (id) {
            case WOOD_PICKAXE:
            case WOOD_AXE:
            case WOOD_SWORD:
                return 60;
            case STONE_PICKAXE:
            case STONE_AXE:
            case STONE_SWORD:
                return 132;
            case IRON_PICKAXE:
            case IRON_AXE:
            case IRON_SWORD:
                return 251;
            default:
                return 0;
        }
    }

    static int maxStack(int id) {
        return durability(id) > 0 ? 1 : STACK;
    }

    
    static boolean placeable(int id) {
        switch (id) {
            case World.GRASS:
            case World.DIRT:
            case World.STONE:
            case World.BRICKS:
            case World.WOOD:
            case World.LEAVES:
            case PLANKS:
            case CRAFT_TABLE:
            case COBBLE:
            case COAL_ORE:
            case IRON_ORE:
            case WOOL:
            case BED:
            case TORCH:
            case TNT:
            case IRON_BLOCK:
            case SIGN:
            case DOOR:
            case FENCE:
            case CHEST:
            case FURNACE:
            case SLAB:
                return true;
            default:
                return false;
        }
    }

    static boolean isSword(int id) {
        return id == WOOD_SWORD || id == STONE_SWORD || id == IRON_SWORD;
    }

    
    static int pickLevel(int id) {
        switch (id) {
            case WOOD_PICKAXE: return 1;
            case STONE_PICKAXE: return 2;
            case IRON_PICKAXE: return 3;
            default: return 0;
        }
    }

    
    static int axeLevel(int id) {
        switch (id) {
            case WOOD_AXE: return 1;
            case STONE_AXE: return 2;
            case IRON_AXE: return 3;
            default: return 0;
        }
    }

    static final class Recipe {
        final int w;
        final int h;
        final int[] cells;
        final int out;
        final int outN;

        Recipe(int w, int h, int[] cells, int out, int outN) {
            this.w = w;
            this.h = h;
            this.cells = cells;
            this.out = out;
            this.outN = outN;
        }
    }

    private Items() {}

    static boolean isDoorBottom(int id) { return id == DOOR || id == DOOR_OPEN; }
    static boolean isDoorTop(int id) { return id == DOOR_TOP || id == DOOR_TOP_OPEN; }

    static String name(int id) {
        return id > 0 && id <= MAX_ID ? NAMES[id] : "";
    }

    static int dropOf(int block) {
        if (block == World.STONE) return COBBLE;
        if (block == COAL_ORE) return COAL;
        if (block == BED_HEAD) return BED;
        if (block == DOOR_TOP || block == DOOR_TOP_OPEN || block == DOOR_OPEN) return DOOR;
        if (block >= STAIR_N && block <= STAIR_W) return STAIR;
        return block;
    }
}

final class Inventory {
    static final int HOTBAR = 9;
    static final int MAIN = 36;
    static final int GRID = 36;
    static final int GRID_MAX = 9;
    static final int OUT = 45;
    static final int CONT = 47;        
static final int CONT_MAX = 27;
private static final int SLOTS = CONT + CONT_MAX;
static final int FUEL_MS = 8000;
static final int SMELT_MS = 5000;

int kind;          
int smelt, burn;   

    private final int[] sid = new int[SLOTS];
    private final int[] sn = new int[SLOTS];
    
    private final int[] sd = new int[SLOTS];
    private int cid, cn, cd;

    private int gridSize = 4;

    void setGridSize(int n) {
        gridSize = n;
    }

    void starter() {
        clear();
        int[] kit = { World.GRASS, World.DIRT, World.STONE, World.BRICKS, World.WOOD, World.LEAVES };
        for (int i = 0; i < kit.length; i++) {
            sid[i] = kit[i];
            sn[i] = 16;
        }
    }

    void clear() {
        for (int s = 0; s < SLOTS; s++) {
            sid[s] = 0;
            sn[s] = 0;
            sd[s] = 0;
        }
        cid = 0;
        cn = 0;
        cd = 0;
    }

    int id(int s) {
        if (s == OUT) {
            Items.Recipe r = match();
            return r == null ? 0 : r.out;
        }
        return sn[s] > 0 ? sid[s] : 0;
    }

    int count(int s) {
        if (s == OUT) {
            Items.Recipe r = match();
            return r == null ? 0 : r.outN;
        }
        return sn[s];
    }

    
    int durability(int s) {
        if (s == OUT) return Items.durability(id(OUT));
        return sd[s];
    }

    int cursorId() {
        return cn > 0 ? cid : 0;
    }

    int cursorCount() {
        return cn;
    }

    void take(int s) {
        if (sn[s] > 0 && --sn[s] == 0) {
            sid[s] = 0;
            sd[s] = 0;
        }
    }

    
    void damage(int s) {
        if (sn[s] == 0 || Items.durability(sid[s]) == 0) return;
        if (--sd[s] <= 0) {
            sid[s] = 0;
            sn[s] = 0;
            sd[s] = 0;
        }
    }

    int add(int item, int amount) {
        return add(item, amount, Items.durability(item));
    }

    
    int add(int item, int amount, int dur) {
        int max = Items.maxStack(item);
        for (int s = 0; s < MAIN && amount > 0; s++) {
            if (sn[s] > 0 && sid[s] == item && sn[s] < max) {
                int m = Math.min(max - sn[s], amount);
                sn[s] += m;
                amount -= m;
            }
        }
        for (int s = 0; s < MAIN && amount > 0; s++) {
            if (sn[s] == 0) {
                int m = Math.min(max, amount);
                sid[s] = item;
                sn[s] = m;
                sd[s] = dur;
                amount -= m;
            }
        }
        return amount;
    }

    boolean canFit(int item, int n) {
        int max = Items.maxStack(item);
        int room = 0;
        for (int s = 0; s < MAIN; s++) {
            if (sn[s] == 0) room += max;
            else if (sid[s] == item) room += max - sn[s];
        }
        return room >= n;
    }

    void click(int s) {
        if (s == OUT) {
            takeOutput();
            return;
        }
        if (cn == 0) {
            if (sn[s] > 0) {
                cid = sid[s];
                cn = sn[s];
                cd = sd[s];
                sid[s] = 0;
                sn[s] = 0;
                sd[s] = 0;
            }
        } else if (sn[s] == 0) {
            put(s, cid, cn, cd);
            cid = 0;
            cn = 0;
            cd = 0;
        } else if (sid[s] == cid && sn[s] < Items.maxStack(cid)) {
            int m = Math.min(Items.maxStack(cid) - sn[s], cn);
            sn[s] += m;
            cn -= m;
            if (cn == 0) {
                cid = 0;
                cd = 0;
            }
        } else {
            int tid = sid[s], tn = sn[s], td = sd[s];
            put(s, cid, cn, cd);
            cid = tid;
            cn = tn;
            cd = td;
        }
    }

    void rightClick(int s) {
        if (s == OUT) {
            takeOutput();
            return;
        }
        if (cn == 0) {
            if (sn[s] > 0) {
                int half = (sn[s] + 1) / 2;
                cid = sid[s];
                cn = half;
                cd = sd[s];
                sn[s] -= half;
                if (sn[s] == 0) {
                    sid[s] = 0;
                    sd[s] = 0;
                }
            }
        } else if (sn[s] == 0) {
            put(s, cid, 1, cd);
            cn--;
            if (cn == 0) {
                cid = 0;
                cd = 0;
            }
        } else if (sid[s] == cid && sn[s] < Items.maxStack(cid)) {
            sn[s]++;
            cn--;
            if (cn == 0) {
                cid = 0;
                cd = 0;
            }
        }
    }
    
    
void loadBox(Inventory b, int n) {
    for (int i = 0; i < CONT_MAX; i++) {
        int s = CONT + i;
        sid[s] = i < n ? b.sid[i] : 0;
        sn[s] = i < n ? b.sn[i] : 0;
        sd[s] = i < n ? b.sd[i] : 0;
    }
    smelt = b.smelt;
    burn = b.burn;
}


void storeBox(Inventory b, int n) {
    for (int i = 0; i < n; i++) {
        b.sid[i] = sid[CONT + i];
        b.sn[i] = sn[CONT + i];
        b.sd[i] = sd[CONT + i];
    }
    b.smelt = smelt;
    b.burn = burn;
    for (int i = 0; i < CONT_MAX; i++) {
        sid[CONT + i] = 0;
        sn[CONT + i] = 0;
        sd[CONT + i] = 0;
    }
}


void tickFurnace(int b, long ms) {
    int t = (int) ms;
    boolean hasIn = sn[b] > 0 && sid[b] == Items.IRON_ORE;
    boolean room = sn[b + 2] == 0 || (sid[b + 2] == Items.IRON_INGOT && sn[b + 2] < Items.STACK);
    if (burn <= 0 && hasIn && room && sn[b + 1] > 0 && sid[b + 1] == Items.COAL) {
        take(b + 1);
        burn = FUEL_MS;
    }
    if (burn > 0) burn -= t;
    if (burn > 0 && hasIn && room) {
        smelt += t;
        if (smelt >= SMELT_MS) {
            smelt = 0;
            take(b);
            if (sn[b + 2] == 0) put(b + 2, Items.IRON_INGOT, 1, 0);
            else sn[b + 2]++;
        }
    } else if (!hasIn) {
        smelt = 0;
    }
}

    private void put(int s, int item, int n, int dur) {
        sid[s] = item;
        sn[s] = n;
        sd[s] = dur;
    }

    private int gridWidth() {
        return gridSize == 9 ? 3 : 2;
    }

    private int matchX, matchY;
    
    Items.Recipe match() {
        int gw = gridWidth();
        for (int i = 0; i < Items.RECIPES.length; i++) {
            Items.Recipe r = Items.RECIPES[i];
            for (int oy = 0; oy + r.h <= gw; oy++) {
                for (int ox = 0; ox + r.w <= gw; ox++) {
                    if (fits(r, ox, oy, gw)) {
                        matchX = ox;
                        matchY = oy;
                        return r;
                    }
                }
            }
        }
        return null;
    }

    private boolean fits(Items.Recipe r, int ox, int oy, int gw) {
        for (int gy = 0; gy < gw; gy++) {
            for (int gx = 0; gx < gw; gx++) {
                int px = gx - ox, py = gy - oy;
                int want = 0;
                if (px >= 0 && py >= 0 && px < r.w && py < r.h) want = r.cells[py * r.w + px];
                int g = GRID + gy * gw + gx;
                int have = sn[g] > 0 ? sid[g] : 0;
                if (want != have) return false;
            }
        }
        return true;
    }

    private void takeOutput() {
        Items.Recipe r = match();
        if (r == null) return;
        if (cn > 0 && (cid != r.out || cn + r.outN > Items.maxStack(r.out))) {
            if (!canFit(r.out, r.outN)) return;
            useGrid(r);
            add(r.out, r.outN);
            return;
        }
        useGrid(r);
        if (cn == 0) {
            cid = r.out;
            cd = Items.durability(r.out);
        }
        cn += r.outN;
    }

    private void useGrid(Items.Recipe r) {
        int gw = gridWidth();
        for (int py = 0; py < r.h; py++) {
            for (int px = 0; px < r.w; px++) {
                if (r.cells[py * r.w + px] == 0) continue;
                int g = GRID + (matchY + py) * gw + (matchX + px);
                if (sn[g] > 0 && --sn[g] == 0) {
                    sid[g] = 0;
                    sd[g] = 0;
                }
            }
        }
    }

    void returnAll() {
        for (int g = GRID; g < GRID + GRID_MAX; g++) {
            if (sn[g] > 0) {
                add(sid[g], sn[g], sd[g]);
                sn[g] = 0;
                sid[g] = 0;
                sd[g] = 0;
            }
        }
        if (cn > 0) {
            add(cid, cn, cd);
            cid = 0;
            cn = 0;
            cd = 0;
        }
    }

    
String serialize() {
    return serializeN(MAIN);
}

String serializeN(int n) {
    StringBuffer sb = new StringBuffer();
    for (int s = 0; s < n; s++) {
        if (s > 0) sb.append(' ');
        boolean has = sn[s] > 0;
        sb.append(has ? sid[s] : 0).append(':').append(sn[s]).append(':').append(has ? sd[s] : 0);
    }
    return sb.toString();
}

void load(String str) {
    clear();
    loadN(MAIN, str);
}

void loadN(int n, String str) {
    try {
        int s = 0, p = 0;
        while (s < n && p < str.length()) {
            int sp = str.indexOf(' ', p);
            if (sp < 0) sp = str.length();
            int c1 = str.indexOf(':', p);
            if (c1 > p && c1 < sp) {
                int c2 = str.indexOf(':', c1 + 1);
                boolean hasDur = c2 > 0 && c2 < sp;
                int item = Integer.parseInt(str.substring(p, c1));
                int cnt = Integer.parseInt(str.substring(c1 + 1, hasDur ? c2 : sp));
                int d = hasDur ? Integer.parseInt(str.substring(c2 + 1, sp)) : 0;
                if (item >= 1 && item <= Items.MAX_ID && cnt > 0) {
                    int full = Items.durability(item);
                    sid[s] = item;
                    sn[s] = Math.min(cnt, Items.maxStack(item));
                    sd[s] = full == 0 ? 0 : (d > 0 ? d : full);
                }
            }
            s++;
            p = sp + 1;
        }
    } catch (Throwable t) {
        System.out.println("[inv] load failed: " + t);
        clear();
    }
}
}

final class Textures {
    static final int TEX = 16;
    
    static final int BLOCK = TEX * TEX * 3;

    
    static int[] atlas;
    
    static int[] darkAtlas;

    
    private static final String SHEET = "/tex/blocks.png";
    private static final String DARK_SHEET = "/tex/block_dark.png";

    private Textures() {}

static void build() {
    atlas = new int[(Items.MAX_ID + 1) * BLOCK];
    for (int i = World.FLAT * BLOCK; i < (World.FLAT + 1) * BLOCK; i++) {
        atlas[i] = 0xFF4CAF2E;
    }
    loadSheet(SHEET, atlas);
    buildTorch();
    buildNew();
    loadItemSheet();
    darkAtlas = new int[atlas.length];
    System.arraycopy(atlas, 0, darkAtlas, 0, atlas.length);
    loadSheet(DARK_SHEET, darkAtlas);
}


private static void buildNew() {
    for (int id = Items.TNT; id < Items.SIGN; id++) {
        for (int band = 0; band < 3; band++) {
            for (int y = 0; y < TEX; y++) {
                for (int x = 0; x < TEX; x++) {
                    atlas[id * BLOCK + band * TEX * TEX + y * TEX + x] = pixel(id, x, y);
                }
            }
        }
    }
    for (int id = Items.FURNACE; id <= Items.STAIR_W; id++) {
    for (int band = 0; band < 3; band++) {
        for (int y = 0; y < TEX; y++) {
            for (int x = 0; x < TEX; x++) {
                atlas[id * BLOCK + band * TEX * TEX + y * TEX + x] = pixel(id, x, y);
            }
        }
    }
}
}

private static void buildBedHead() {
    int base = Items.BED_HEAD * BLOCK;
    for (int band = 0; band < 3; band++) {
        for (int y = 0; y < TEX; y++) {
            for (int x = 0; x < TEX; x++) {
                int c;
                if (band == 2) {
                    
                    c = (y < 7) ? 0xFFEEEEEE : 0xFFAA2222;
                    if (y == 0 || y == TEX - 1 || x == 0 || x == TEX - 1) c = 0xFF8B5A2B;
                } else {
                    
                    c = (y < 6) ? 0xFFAA2222 : 0xFF8B5A2B;
                }
                atlas[base + band * TEX * TEX + y * TEX + x] = c;
            }
        }
    }
}

private static void buildTorch() {
    int base = Items.TORCH * BLOCK;
    for (int band = 0; band < 3; band++) {
        for (int y = 0; y < TEX; y++) {
            for (int x = 0; x < TEX; x++) {
                atlas[base + band * TEX * TEX + y * TEX + x] = torchPixel(x, y);
            }
        }
    }
}

private static int torchPixel(int x, int y) {
    if (y >= 2 && y <= 5 && x >= 6 && x <= 9) return y <= 3 ? 0xFFFFE066 : 0xFFFF9020; 
    if (y >= 6 && y <= 14 && (x == 7 || x == 8)) return 0xFF8B5A2B;                   
    return 0xFF2B2B2B;                                                                
}

private static final String ITEM_SHEET = "/tex/items.png";


private static void loadItemSheet() {
    int n = TEX;
    int[] ids = { Items.SIGN, Items.DOOR, Items.DOOR_TOP, Items.FENCE, Items.DOOR_OPEN, Items.DOOR_TOP_OPEN };
    Image img;
    try {
        img = Image.createImage(ITEM_SHEET);
    } catch (java.io.IOException e) {
        throw new IllegalStateException("missing texture sheet " + ITEM_SHEET + ": " + e);
    }
    int w = img.getWidth(), h = img.getHeight();
    if (w < ids.length * n || h < 3 * n) {
        throw new IllegalStateException("texture sheet " + ITEM_SHEET + " is " + w + "x" + h
                + ", needs at least " + (ids.length * n) + "x" + (3 * n));
    }
    int[] sheet = new int[w * h];
    img.getRGB(sheet, 0, w, 0, 0, w, h);
    for (int k = 0; k < ids.length; k++) {
        for (int band = 0; band < 3; band++) {
            int sx = k * n, sy = band * n;
            int dst = ids[k] * BLOCK + band * n * n;
            for (int y = 0; y < n; y++) {
                System.arraycopy(sheet, (sy + y) * w + sx, atlas, dst + y * n, n);
            }
        }
    }
}

private static void loadSheet(String path, int[] out) {
    int n = TEX;
    int[] ids = {
        World.GRASS, World.DIRT, World.STONE, World.BRICKS, World.WOOD, World.LEAVES,
        Items.PLANKS, Items.CRAFT_TABLE, Items.COBBLE, Items.COAL_ORE, Items.IRON_ORE,
        Items.STICK, Items.COAL, Items.WOOD_SWORD, Items.STONE_SWORD,
        Items.WOOD_PICKAXE, Items.STONE_PICKAXE, Items.WOOD_AXE, Items.STONE_AXE,
        Items.WOOL, Items.BED
    };
    final int headCol = 22;   

    Image img;
    try {
        img = Image.createImage(path);
    } catch (java.io.IOException e) {
        throw new IllegalStateException("missing texture sheet " + path + ": " + e);
    }
    int w = img.getWidth(), h = img.getHeight();
    if (w < (headCol + 1) * n || h < 3 * n) {
        throw new IllegalStateException("texture sheet " + path + " is " + w + "x" + h
                + ", needs at least " + ((headCol + 1) * n) + "x" + (3 * n));
    }
    int[] sheet = new int[w * h];
    img.getRGB(sheet, 0, w, 0, 0, w, h);
    System.out.println("[tex] loaded " + path + " " + w + "x" + h);

    for (int k = 0; k < ids.length; k++) {
        for (int band = 0; band < 3; band++) {
            int sx = k * n, sy = band * n;
            int dst = ids[k] * BLOCK + band * n * n;
            for (int y = 0; y < n; y++) {
                System.arraycopy(sheet, (sy + y) * w + sx, out, dst + y * n, n);
            }
        }
    }

    
    for (int band = 0; band < 3; band++) {
        for (int y = 0; y < n; y++) {
            System.arraycopy(sheet, (band * n + y) * w + headCol * n,
                    out, Items.BED_HEAD * BLOCK + band * n * n + y * n, n);
        }
    }
    }

static final String[] NAMES = { "", "grass", "dirt", "", "stone", "bricks", "", "wood", "leaves", "flat",
    "planks", "craft", "cobble", "coal ore", "iron ore", "stick", "coal", "wood sword", "stone sword",
    "wool", "bed", "wood pickaxe", "stone pickaxe", "wood axe", "stone axe", "torch", "bed head",
    "tnt", "iron block", "apple", "iron ingot", "iron sword", "iron pickaxe", "iron axe",
    "sign", "door", "door top", "fence", "door open", "door top open", "furnace", "chest", "cobble slab",
    "cobble stair", "cobble stair", "cobble stair", "cobble stair", "cobble stair" };
    
    
    private static void buildPlanks() {
        int base = Items.PLANKS * BLOCK;
        for (int band = 0; band < 3; band++) {
            for (int y = 0; y < TEX; y++) {
                for (int x = 0; x < TEX; x++) {
                    int c = 0xFFB8894F;
                    if (y % 4 == 0) c = 0xFF7A5530;
                    else if (((x + (y / 4) * 5) % 8) == 0) c = 0xFF8E6538;
                    atlas[base + band * TEX * TEX + y * TEX + x] = c;
                }
            }
        }
    }

    
    private static void buildCraftingTable() {
        int base = Items.CRAFT_TABLE * BLOCK;
        for (int band = 0; band < 3; band++) {
            for (int y = 0; y < TEX; y++) {
                for (int x = 0; x < TEX; x++) {
                    int c = 0xFFB8894F;
                    if (band == 0) {
                        if (x == 0 || y == 0 || x == TEX - 1 || y == TEX - 1 || x == 8 || y == 8) c = 0xFF4A2F18;
                    } else {
                        if (y % 4 == 0) c = 0xFF7A5530;
                        if (y == TEX - 1) c = 0xFF4A2F18;
                    }
                    atlas[base + band * TEX * TEX + y * TEX + x] = c;
                }
            }
        }
    }

    private static void buildDrawn() {
        for (int id = Items.COBBLE; id <= Items.MAX_ID; id++) {
            for (int band = 0; band < 3; band++) {
                for (int y = 0; y < TEX; y++) {
                    for (int x = 0; x < TEX; x++) {
                        atlas[id * BLOCK + band * TEX * TEX + y * TEX + x] = pixel(id, x, y);
                    }
                }
            }
        }
    }

private static int pixel(int id, int x, int y) {
        switch (id) {
            case Items.WOOL:
                return ((x + y) % 2 == 0) ? 0xFFEEEEEE : 0xFFDDDDDD;
            case Items.BED:
                if (y < 6) return 0xFFAA2222;  
                if (y < 10) return 0xFFEEEEEE; 
                return 0xFF8B5A2B;             
            case Items.COBBLE:
                if ((x * 7 + y * 3) % 13 == 0) return 0xFF5C5C5C;
                if ((x * 5 + y * 11) % 17 == 0) return 0xFF9C9C9C;
                return 0xFF7F7F7F;
            case Items.COAL_ORE:
                if ((x * 5 + y * 3) % 7 == 0 && (x + y * 2) % 3 == 0) return 0xFF1E1E1E;
                return 0xFF7A7A7A;
            case Items.IRON_ORE:
                if ((x * 3 + y * 5) % 7 == 0 && (x * 2 + y) % 3 == 0) return 0xFFD8A77A;
                return 0xFF7A7A7A;
            case Items.STICK:
                if (x % 5 == 0) return 0xFF6B4220;
                return 0xFF8B5A2B;
            case Items.COAL:
                if ((x * 3 + y * 7) % 11 == 0) return 0xFF555555;
                return 0xFF1A1A1A;
            case Items.WOOD_SWORD:
                return sword(x, y, 0xFF8B5A2B);
            case Items.STONE_SWORD:
                return sword(x, y, 0xFF9C9C9C);
            case Items.WOOD_PICKAXE:
                return pickaxe(x, y, 0xFF8B5A2B);
            case Items.STONE_PICKAXE:
                return pickaxe(x, y, 0xFF9C9C9C);
            case Items.WOOD_AXE:
                return axe(x, y, 0xFF8B5A2B);
            case Items.STONE_AXE:
                return axe(x, y, 0xFF9C9C9C);
            case Items.IRON_SWORD:
                return sword(x, y, 0xFFD0D0D0);
            case Items.IRON_PICKAXE:
                return pickaxe(x, y, 0xFFD0D0D0);
            case Items.IRON_AXE:
                return axe(x, y, 0xFFD0D0D0);
            case Items.IRON_INGOT:
                return ingot(x, y);
            case Items.APPLE:
                return apple(x, y);
            case Items.TNT:
                return tnt(x, y);
            case Items.IRON_BLOCK:
                return ironBlock(x, y);
            case Items.SIGN:
                return sign(x, y);
            case Items.DOOR:
            case Items.DOOR_TOP:
            case Items.DOOR_OPEN:
            case Items.DOOR_TOP_OPEN:
                return door(x, y);
            case Items.FENCE:
                return planksLike(x, y);
            case Items.FURNACE:
    return furnace(x, y);
case Items.CHEST:
    return chest(x, y);
case Items.SLAB:
case Items.STAIR:
case Items.STAIR_N:
case Items.STAIR_E:
case Items.STAIR_S:
case Items.STAIR_W:
    return pixel(Items.COBBLE, x, y);
            default:
                return 0xFFFF00FF;
        }
    }

private static int furnace(int x, int y) {
    if (y >= 9 && y <= 13 && x >= 4 && x <= 11) return y >= 11 ? 0xFFFF6A00 : 0xFF2B2B2B;
    return ((x * 3 + y * 5) % 7 == 0) ? 0xFF5C5C5C : 0xFF7F7F7F;
}

private static int chest(int x, int y) {
    if (x == 0 || y == 0 || x == TEX - 1 || y == TEX - 1) return 0xFF4A2F18;
    if (y == 7) return 0xFF4A2F18;
    if ((x == 7 || x == 8) && y >= 7 && y <= 9) return 0xFFD0D0D0;
    return ((x + y) % 4 == 0) ? 0xFF9C6D3C : 0xFFB8894F;
}
    
    private static int ingot(int x, int y) {
        if (y < 5 || y > 11) return 0;
        if (y >= 10 && (x < 3 || x > 12)) return 0;
        if (y == 5) return 0xFFF2F2F2;
        if (y >= 10) return 0xFF8A8A8A;
        return 0xFFC8C8C8;
    }

    private static int apple(int x, int y) {
        int dx = x - 8, dy = y - 9;
        if (x == 8 && y >= 2 && y <= 4) return 0xFF6B4220;
        if (x >= 9 && x <= 11 && y >= 1 && y <= 3) return 0xFF3FA030;
        if (dx * dx + dy * dy <= 25) {
            return (dx < -1 && dy < -1) ? 0xFFFF7070 : 0xFFC01010;
        }
        return 0;
    }

    private static int tnt(int x, int y) {
        if (y == 0 || y == TEX - 1 || x == 0 || x == TEX - 1) return 0xFF5A0000;
        if (y >= 6 && y <= 9) return (x % 4 == 0) ? 0xFF2B2B2B : 0xFFEEEEEE;
        return ((x + y) % 5 == 0) ? 0xFFA01818 : 0xFFC02020;
    }

    private static int sign(int x, int y) {
        if (x == 0 || y == 0 || x == TEX - 1 || y == TEX - 1) return 0xFF5A3A1E;
        if (x >= 2 && x <= 13 && y >= 3 && y <= 10) return 0xFFEDE3C4;
        return 0xFFB8894F;
    }

    private static int door(int x, int y) {
        if (x == 0 || y == 0 || x == TEX - 1 || y == TEX - 1) return 0xFF5A3A1E;
        if ((x == 3 || x == 12) && y > 2 && y < 13) return 0xFF7A5530;
        if (x == 11 && y == 8) return 0xFFD0D0D0;
        return 0xFF9C6D3C;
    }

    private static int planksLike(int x, int y) {
        return (y % 4 == 0) ? 0xFF7A5530 : 0xFFB8894F;
    }

    private static int ironBlock(int x, int y) {
        if (x == 0 || y == 0 || x == TEX - 1 || y == TEX - 1) return 0xFF8A8A8A;
        if ((x == 3 || x == 12) && (y == 3 || y == 12)) return 0xFF6E6E6E;
        return ((x + y) % 7 == 0) ? 0xFFD6D6D6 : 0xFFBFBFBF;
    }

    private static int pickaxe(int x, int y, int head) {
        if (y == 3 && x >= 2 && x <= 13) return head;
        if (y == 4 && (x == 2 || x == 13)) return head;
        if (y >= 4 && y <= 14 && x == 7 - (y - 4) / 3) return 0xFF8B5A2B;
        return 0;
    }

    private static int axe(int x, int y, int head) {
        if (x >= 9 && x <= 13 && y >= 2 && y <= 6) return head;
        if (y >= 6 && y <= 14 && x == 8 - (y - 6) / 2) return 0xFF8B5A2B;
        return 0;
    }

    private static int sword(int x, int y, int blade) {
        if (x == y && y <= 10) return blade;
        if (x <= 4 && y >= 11 && y - x >= 8) return 0xFF5A3A1E;
        return 0;
    }

    
}

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
    private static final String FILE_NAME = "mc4kworld.txt";   
    static final int MAX_WORLDS = 5;
    private static int slot;                                   
    private static final int MAX_CHUNKS = 32;
    static final float LOAD_R = 16f;          
    static final float UNLOAD_R = 24f;        
    private static int streamX = Integer.MIN_VALUE, streamZ = Integer.MIN_VALUE;
    
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
    
    private static final StringBuffer keptLines = new StringBuffer();
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
private static final Hashtable boxes = new Hashtable();
private static final Random growRand = new Random();
private static int growFrames;
static final int GROW_EVERY = 10;     
static final int GROW_TRIES = 16;     

static void tickGrass() {
    if (++growFrames < GROW_EVERY) return;
    growFrames = 0;
    int n = count;
    if (n == 0) return;

    for (int k = 0; k < GROW_TRIES; k++) {
        Chunk c = list[growRand.nextInt(n)];
        int lx = growRand.nextInt(CS);
        int lz = growRand.nextInt(CS);

        
        int y = HEIGHT - 1;
        while (y > 0 && c.b[(y << 8) | (lz << 4) | lx] == AIR) y--;
        if (c.b[(y << 8) | (lz << 4) | lx] != DIRT) continue;

        int x = (c.cx << 4) + lx;
        int z = (c.cz << 4) + lz;
        if (get(x, y + 1, z) != AIR) continue;

        
        if (get(x + 1, y, z) != GRASS && get(x - 1, y, z) != GRASS
                && get(x, y, z + 1) != GRASS && get(x, y, z - 1) != GRASS) continue;

        set(x, y, z, GRASS);
    }
}

static Inventory box(int x, int y, int z, int kind) {
    String k = signKey(x, y, z);
    Inventory b = (Inventory) boxes.get(k);
    if (b == null) {
        b = new Inventory();
        b.kind = kind;
        boxes.put(k, b);
    }
    return b;
}


static void dumpBox(int x, int y, int z, Inventory dst) {
    Inventory b = (Inventory) boxes.remove(signKey(x, y, z));
    if (b == null) return;
    int n = b.kind == 2 ? 3 : 27;
    for (int i = 0; i < n; i++) {
        if (b.count(i) > 0) dst.add(b.id(i), b.count(i), b.durability(i));
    }
}

static void tickBoxes(long ms, Inventory skip) {
    Enumeration e = boxes.elements();
    while (e.hasMoreElements()) {
        Inventory b = (Inventory) e.nextElement();
        if (b.kind == 2 && b != skip) b.tickFurnace(0, ms);
    }
}

private static void readBox(String line) {
    try {
        String s = line.substring(4);
        int sp = s.indexOf(' ');
        String key = s.substring(0, sp);
        s = s.substring(sp + 1);
        sp = s.indexOf(' ');
        int kind = Integer.parseInt(s.substring(0, sp));
        s = s.substring(sp + 1);
        Inventory b = new Inventory();
        b.kind = kind;
        if (kind == 2) {
            int last = s.lastIndexOf(' ');
            int prev = s.lastIndexOf(' ', last - 1);
            b.smelt = Integer.parseInt(s.substring(prev + 1, last));
            b.burn = Integer.parseInt(s.substring(last + 1));
            s = s.substring(0, prev);
        }
        b.loadN(kind == 2 ? 3 : 27, s);
        boxes.put(key, b);
    } catch (Throwable e) {
        System.out.println("[world] box line bad: " + e);
    }
}

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
    return id == Items.FENCE || id == Items.SIGN || id == Items.DOOR || id == Items.DOOR_OPEN
            || id == Items.SLAB || (id >= Items.STAIR_N && id <= Items.STAIR_W);
}


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

private static final Hashtable doors = new Hashtable();

static void setDoorRot(int x, int y, int z, int rot) {
    doors.put(signKey(x, y, z), new Integer(rot));
}

static int doorRot(int x, int y, int z) {
    Integer r = (Integer) doors.get(signKey(x, y, z));
    return r == null ? 0 : r.intValue();
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
        return key(cx, cz);   
    }

static Chunk find(int cx, int cz) {
    if (last != null && last.cx == cx && last.cz == cz) return last;
    for (int i = 0; i < count; i++) {
        Chunk c = list[i];
        if (c.cx == cx && c.cz == cz) {
            last = c;
            return c;
        }
    }
    return null;
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
if (Items.isDoorBottom(old) && !Items.isDoorBottom(b)) doors.remove(signKey(x, y, z));
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


static void purgeChunkMemory() {
    
    for (int i = 0; i < count; i++) {
        try {
            saveChunk(list[i]);
        } catch (Throwable t) {
            System.out.println("[world] purge save failed: " + t);
        }
    }
    while (count > 0) list[--count] = null;
    last = null;
    torchN = 0;
    thinN = 0;
    streamX = Integer.MIN_VALUE;
    streamZ = Integer.MIN_VALUE;
}
    

static String savedInventory() {
    if (invLine == null) {
        try {
            javax.microedition.rms.RecordStore rs = 
                javax.microedition.rms.RecordStore.openRecordStore("mc4k_save_" + slot, false);
            if (rs.getNumRecords() > 0) {
                byte[] data = rs.getRecord(1);
                if (data != null && data.length > 0) {
                    invLine = new String(data);
                }
            }
            rs.closeRecordStore();
        } catch (Throwable t) {
            
        }
    }
    return invLine;
}

    
    static int savedTick() {
        return pTick;
    }

    
    static boolean teleport(float[] p) {
        return false;   
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
    areaX = 0;
    areaZ = 0;
}

private static void queueChunk(int cx, int cz) {
    if (qTail < qx.length) { qx[qTail] = cx; qz[qTail] = cz; qTail++; }
}


private static float chunkDist2(int cx, int cz, float px, float pz) {
    float x0 = cx << 4, z0 = cz << 4;
    float dx = px < x0 ? x0 - px : (px > x0 + CS ? px - (x0 + CS) : 0f);
    float dz = pz < z0 ? z0 - pz : (pz > z0 + CS ? pz - (z0 + CS) : 0f);
    return dx * dx + dz * dz;
}




private static void updateStream() {
    int bx = Calc.floor(Renderer.px), bz = Calc.floor(Renderer.pz);
    if (bx == streamX && bz == streamZ) return;
    streamX = bx;
    streamZ = bz;
    float px = Renderer.px, pz = Renderer.pz;

    float unload2 = UNLOAD_R * UNLOAD_R;
    for (int i = count - 1; i >= 0; i--) {
        Chunk c = list[i];
        if (chunkDist2(c.cx, c.cz, px, pz) > unload2) unloadAt(i);
    }

    float load2 = LOAD_R * LOAD_R;
    int pcx = bx >> 4, pcz = bz >> 4;
    int r = (int) (LOAD_R / CS) + 1;
    qHead = qTail = 0;
    for (int cz = pcz - r; cz <= pcz + r; cz++)
        for (int cx = pcx - r; cx <= pcx + r; cx++)
            if (chunkDist2(cx, cz, px, pz) <= load2 && find(cx, cz) == null) queueChunk(cx, cz);
}


static boolean areaReady() {
    return find(Calc.floor(Renderer.px) >> 4, Calc.floor(Renderer.pz) >> 4) != null;
}

static boolean generating() { return building != null || qHead < qTail; }


static void tick(long deadline) {
    updateStream();
    do {
        if (building == null) {
            if (qHead >= qTail) return;
            int cx = qx[qHead], cz = qz[qHead];
            qHead++;
            if (find(cx, cz) != null) continue;
            startChunk(cx, cz);
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


static int lastWorld() {
    return lastWorld;
}


static void selectWorld(int s, boolean fresh) {
    if (s < 0 || s >= MAX_WORLDS) s = 0;
    slot = s;
    clearMemory();
    applySeed(DEFAULT_SEED);
    if (fresh) {
        
        applySeed((new Random()).nextLong() & 0x7FFFFFFFL);
        writeFile();
    }
    lastWorld = s;
    saveOptions();   
}


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
    
    last = null;
    torchN = 0;
    thinN = 0;
    signs.clear();
    streamX = Integer.MIN_VALUE;
    streamZ = Integer.MIN_VALUE;
    boxes.clear();
    doors.clear();
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
        return true;   
    }


    private static void saveChunk(Chunk c) {
        if (!c.modified) return;
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
        return basePath + OPT_NAME;   
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

    
    private static int put(OutputStream os, String s) throws IOException {
        byte[] b = s.getBytes();
        os.write(b);
        return b.length;
    }

    
    private static void writeFile() {
        FileConnection fc = null;
        OutputStream os = null;
        try {
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
            int bytes = 0;
            bytes += put(os, "mc4k world 1\n");
            if (seed != DEFAULT_SEED) bytes += put(os, "seed " + seed + "\n");
            if (pValid) {
                StringBuffer pl = new StringBuffer();
                pl.append("player ").append(pAreaX).append(' ').append(pAreaZ);
                for (int i = 0; i < 6; i++) pl.append(' ').append(pState[i]);
                pl.append('\n');
                pl.append("time ").append(pTick).append('\n');
                bytes += put(os, pl.toString());
            }
            if (spawnValid) {
                StringBuffer sp = new StringBuffer("spawn ");
                for (int i = 0; i < 5; i++) {
                    if (i > 0) sp.append(' ');
                    sp.append(spawnState[i]);
                }
                sp.append('\n');
                bytes += put(os, sp.toString());
            }
            if (invLine != null) bytes += put(os, "inv " + invLine + "\n");
            Enumeration se = signs.keys();
            while (se.hasMoreElements()) {
                String k = (String) se.nextElement();
                bytes += put(os, "sign " + k + " " + escapeSign((String) signs.get(k)) + "\n");
            }
            Enumeration be = boxes.keys();
while (be.hasMoreElements()) {
    String k = (String) be.nextElement();
    Inventory b = (Inventory) boxes.get(k);
    int n = b.kind == 2 ? 3 : 27;
    String extra = b.kind == 2 ? " " + b.smelt + " " + b.burn : "";
    bytes += put(os, "box " + k + " " + b.kind + " " + b.serializeN(n) + extra + "\n");
}
Enumeration de = doors.keys();
while (de.hasMoreElements()) {
    String k = (String) de.nextElement();
    bytes += put(os, "door " + k + " " + doors.get(k) + "\n");
}
            Enumeration e = saved.elements();
            while (e.hasMoreElements()) bytes += put(os, (String) e.nextElement() + "\n");
            bytes += put(os, keptLines.toString());
            os.flush();
            os.close();
            os = null;
            long size = fc.fileSize();
            System.out.println("[world] wrote " + bytes + " bytes, file is " + size);
            fileStatus = "saved " + size + " bytes";
        } catch (Throwable t) {
            System.out.println("[world] file write failed: " + t);
            fileStatus = filePath() == null ? "no save folder" : "save failed";
        } finally {
            try { if (os != null) os.close(); } catch (Throwable t) { }
            try { if (fc != null) fc.close(); } catch (Throwable t) { }
        }
    }
    
private static void readDoor(String line) {
    try {
        String s = line.substring(5);
        int sp = s.lastIndexOf(' ');
        doors.put(s.substring(0, sp), new Integer(Integer.parseInt(s.substring(sp + 1).trim())));
    } catch (Throwable e) {
        System.out.println("[world] door line bad: " + e);
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
        keptLines.setLength(0);
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
                try {
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
                if (line.startsWith("box ")) {
    readBox(line);
    continue;
}
if (line.startsWith("door ")) {
    readDoor(line);
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
                if (bar < 0) {
                    if (line.length() > 0 && !line.startsWith("mc4k world")) keptLines.append(line).append('\n');
                    continue;
                }
                String[] h = new String[4];
                int p = 0;
                for (int k = 0; k < 4; k++) {
                    int sp = k < 3 ? line.indexOf(' ', p) : bar;
                    h[k] = line.substring(p, sp);
                    p = sp + 1;
                }
                
                
                if (Integer.parseInt(h[0]) != 0 || Integer.parseInt(h[1]) != 0) {
                    keptLines.append(line).append('\n');
                    continue;
                }
                int key = storeKey(0, 0, Integer.parseInt(h[2]), Integer.parseInt(h[3]));
                saved.put(new Integer(key), line);
                } catch (Throwable t) {
                    System.out.println("[world] keeping unreadable line: " + t);
                    keptLines.append(line).append('\n');
                }
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

final class Pigs {
    static final int MAX = 6;
    static final int HP = 10;
    static final float SPEED = 0.035f;
    static final int SPAWN_FRAMES = 150;
    static final int HURT_MS = 2000;
    static final int FLEE_FRAMES = 60;
    static final float FLEE_SPEED = 0.09f;
    static final float MAX_RANGE = 3.5f;

    private final boolean[] alive = new boolean[MAX];
    private final float[] x = new float[MAX];
    private final float[] y = new float[MAX];
    private final float[] z = new float[MAX];
    private final float[] yaw = new float[MAX];
    private final float[] targetX = new float[MAX];
    private final float[] targetZ = new float[MAX];
    private final int[] hp = new int[MAX];
    private final int[] fleeTick = new int[MAX];
    private final int[] animTick = new int[MAX];
    private final long[] hurtUntil = new long[MAX];

    private int spawnCool;
    private final java.util.Random rnd = new java.util.Random();

    void clear() {
        for (int i = 0; i < MAX; i++) {
            alive[i] = false;
            animTick[i] = 0;
            hurtUntil[i] = 0;
            fleeTick[i] = 0;
            yaw[i] = 0f;
        }
        spawnCool = 0;
    }

    void update(float px, float pz) {
        if (--spawnCool <= 0) {
            spawnCool = SPAWN_FRAMES;
            spawn(px, pz);
        }

        for (int i = 0; i < MAX; i++) {
            if (!alive[i]) continue;

            float pdx = px - x[i], pdz = pz - z[i];
            if (pdx * pdx + pdz * pdz > 40f * 40f) {
                alive[i] = false;
                continue;
            }

            boolean fleeing = fleeTick[i] > 0;
            if (fleeing) {
                fleeTick[i]--;
                float ex = targetX[i] - x[i], ez = targetZ[i] - z[i];
                if (ex * ex + ez * ez < 0.25f) pickFlee(i);
            }

            float dx = targetX[i] - x[i];
            float dz = targetZ[i] - z[i];
            float d = (float) Math.sqrt(dx * dx + dz * dz);

            if (!fleeing && (d < 0.5f || rnd.nextInt(120) == 0)) {
                targetX[i] = x[i] + (rnd.nextFloat() * 10f - 5f);
                targetZ[i] = z[i] + (rnd.nextFloat() * 10f - 5f);
            } else if (d > 0.001f) {
                float spd = fleeing ? FLEE_SPEED : SPEED;
                float mx = dx / d * spd, mz = dz / d * spd;
                boolean movedX = false, movedZ = false;
                if (canWalk(x[i], y[i], z[i], x[i] + mx, z[i])) { x[i] += mx; movedX = true; }
                if (canWalk(x[i], y[i], z[i], x[i], z[i] + mz)) { z[i] += mz; movedZ = true; }
                if (fleeing && !movedX && !movedZ) pickFlee(i);

                yaw[i] = Calc.toDeg(Calc.atan2(dx, dz)) + 180f;
                animTick[i]++;
            }

            int g = groundY(x[i], z[i]);
            if (g >= World.HEIGHT) {
                alive[i] = false;
                continue;
            }
            y[i] = g;
        }
    }

    private void spawn(float px, float pz) {
        int free = -1;
        for (int i = 0; i < MAX; i++) {
            if (!alive[i]) { free = i; break; }
        }
        if (free < 0) return;

        float ang = rnd.nextFloat() * 6.2831853f;
        float dist = 12f + rnd.nextFloat() * 10f;
        float sx = px + (float) Math.cos(ang) * dist;
        float sz = pz + (float) Math.sin(ang) * dist;

        int bx = Calc.floor(sx), bz = Calc.floor(sz);
        int g = groundY(sx, sz);
        if (g >= World.HEIGHT || World.get(bx, g, bz) != World.GRASS) return;

        alive[free] = true;
        x[free] = bx + 0.5f;
        z[free] = bz + 0.5f;
        y[free] = g;
        hp[free] = HP;
        animTick[free] = 0;
        hurtUntil[free] = 0;
        fleeTick[free] = 0;
        yaw[free] = 0f;
        targetX[free] = x[free];
        targetZ[free] = z[free];
    }

    private void pickFlee(int i) {
        float a = rnd.nextFloat() * 6.2831855f;
        targetX[i] = x[i] + (float) Math.cos(a) * 8f;
        targetZ[i] = z[i] + (float) Math.sin(a) * 8f;
    }

    private boolean canWalk(float cx, float cy, float cz, float nx, float nz) {
        int currentGround = Calc.floor(cy);
        int targetGround = groundY(nx, nz);
        if (currentGround - targetGround > 1) return false;
        int tx = Calc.floor(nx), tz = Calc.floor(nz);
        if (World.get(tx, currentGround, tz) == Items.FENCE) return false;
        if (World.get(tx, currentGround + 1, tz) == Items.FENCE) return false;
        if (!World.passable(World.get(tx, targetGround - 1, tz))) return false;
        if (!World.passable(World.get(tx, targetGround - 2, tz))) return false;
        return true;
    }

    int target(float ex, float ey, float ez, float fx, float fy, float fz) {
        int best = -1;
        float bestT = Float.MAX_VALUE;
        for (int i = 0; i < MAX; i++) {
            if (!alive[i]) continue;
            float dx = x[i] - ex, dy = (y[i] - 0.5f) - ey, dz = z[i] - ez;
            float d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > MAX_RANGE * MAX_RANGE) continue;
            float t = dx * fx + dy * fy + dz * fz;
            if (t < 0.3f || t >= bestT) continue;
            if (d2 - t * t < 0.35f && visible(ex, ey, ez, x[i], y[i] - 0.5f, z[i])) {
                bestT = t;
                best = i;
            }
        }
        return best;
    }

    void hit(int i, int dmg, float fx, float fz) {
        if (i < 0 || !alive[i]) return;
        hp[i] -= dmg;
        if (hp[i] <= 0) {
            alive[i] = false;
        } else {
            fleeTick[i] = FLEE_FRAMES;
            hurtUntil[i] = System.currentTimeMillis() + HURT_MS;
            pickFlee(i);
        }
    }

    boolean isAlive(int i) { return alive[i]; }
    boolean isHurt(int i) { return hurtUntil[i] > System.currentTimeMillis(); }
    float getX(int i) { return x[i]; }
    float getY(int i) { return y[i]; }
    float getZ(int i) { return z[i]; }
    float getYaw(int i) { return yaw[i]; }

    private static int groundY(float px, float pz) {
        int bx = Calc.floor(px), bz = Calc.floor(pz);
        for (int yy = 0; yy < World.HEIGHT; yy++) {
            if (!World.passable(World.get(bx, yy, bz))) return yy;
        }
        return World.HEIGHT;
    }

    private static boolean visible(float ex, float ey, float ez, float tx, float ty, float tz) {
        for (int k = 1; k < 16; k++) {
            float f = k / 16f;
            int bx = Calc.floor(ex + (tx - ex) * f);
            int by = Calc.floor(ey + (ty - ey) * f);
            int bz = Calc.floor(ez + (tz - ez) * f);
            int id = World.peek(bx, by, bz);
            if (id > World.AIR && !World.passable(id)) return false;
        }
        return true;
    }
}

final class Zombies {
    static final int MAX = 6;
    static final int HP = 20;
    static final float SPEED = 0.08f;
    static final float STOP_DIST = 0.9f;
    static final float HIT_RANGE = 1.1f;
    static final int ATTACK_FRAMES = 30;
    static final int DAMAGE = 2;
    static final int SPAWN_FRAMES = 60;
    static final float MAX_RANGE = 3.5f;
    static final float HALF_W = 0.3f;
    static final float BODY_H = 1.8f;
    static final float HEAD_Y = 1.3f;

    static final int HURT_MS = 2000;         

    private final boolean[] alive = new boolean[MAX];
    private final float[] x = new float[MAX];
    private final float[] y = new float[MAX];
    private final float[] z = new float[MAX];

    private final float[] yaw = new float[MAX];
    private final int[] hp = new int[MAX];
    private final int[] cool = new int[MAX];
    private final int[] animTick = new int[MAX];
    private final long[] hurtUntil = new long[MAX];
    private int spawnCool;
    private final java.util.Random rnd = new java.util.Random();

    private int hitAxis;
    private boolean hitHead;

    void clear() {
        for (int i = 0; i < MAX; i++) {
            alive[i] = false;
            animTick[i] = 0;
            hurtUntil[i] = 0;
            yaw[i] = 0f;
        }
        spawnCool = 0;
    }




int update(float px, float py, float pz, boolean night) {
    if (!night) {
        clear();
        return 0;
    }
    if (--spawnCool <= 0) {
        spawnCool = SPAWN_FRAMES;
        spawn(px, pz);
    }
    int dmg = 0;
    for (int i = 0; i < MAX; i++) {
        if (!alive[i]) continue;
        if (cool[i] > 0) cool[i]--;

        float dx = px - x[i], dz = pz - z[i];
        float d = (float) Math.sqrt(dx * dx + dz * dz);

        
        if (d > 36f) {
            alive[i] = false;
            continue;
        }

        if (d > STOP_DIST) {
            float mx = dx / d * SPEED, mz = dz / d * SPEED;
            if (canWalk(x[i], y[i], z[i], x[i] + mx, z[i])) x[i] += mx;
            if (canWalk(x[i], y[i], z[i], x[i], z[i] + mz)) z[i] += mz;

            yaw[i] = Calc.toDeg(Calc.atan2(dx, dz)) + 180f;
            animTick[i]++;
        }
        int g = groundY(x[i], z[i]);
        if (g >= World.HEIGHT) {
            alive[i] = false;
            continue;
        }
        y[i] = g;
        if (d < HIT_RANGE && Math.abs(py - y[i]) < 2f && cool[i] == 0) {
            dmg += DAMAGE;
            cool[i] = ATTACK_FRAMES;
        }
    }
    return dmg;
}

private void spawn(float px, float pz) {
    int free = -1;
    for (int i = 0; i < MAX; i++) {
        if (!alive[i]) {
            free = i;
            break;
        }
    }
    if (free < 0) return;
    float ang = rnd.nextFloat() * 6.2831853f;
    float dist = 14f + rnd.nextFloat() * 6f;
    float sx = px + (float) Math.cos(ang) * dist, sz = pz + (float) Math.sin(ang) * dist;
    int bx = Calc.floor(sx), bz = Calc.floor(sz);

    
    int g = groundY(sx, sz);
    if (g >= World.HEIGHT || World.get(bx, g, bz) != World.GRASS) return;
    alive[free] = true;
    x[free] = bx + 0.5f;
    z[free] = bz + 0.5f;
    y[free] = g;
    hp[free] = HP;
    cool[free] = 0;
    animTick[free] = 0;
    hurtUntil[free] = 0;
}

    private boolean canWalk(float cx, float cy, float cz, float nx, float nz) {
        int currentGround = Calc.floor(cy);
        int targetGround = groundY(nx, nz);
        int diff = currentGround - targetGround;
        if (diff > 1) return false;
        int tx = Calc.floor(nx), tz = Calc.floor(nz);
        if (World.get(tx, currentGround, tz) == Items.FENCE) return false;
        if (World.get(tx, currentGround + 1, tz) == Items.FENCE) return false;
if (!World.passable(World.get(tx, targetGround - 1, tz))) return false;
if (!World.passable(World.get(tx, targetGround - 2, tz))) return false;
        return true;
    }

    int getAnimFrame(int i) {
        return (animTick[i] / 6) % 2;
    }

    float rayHit(float ox, float oy, float oz, float dx, float dy, float dz) {
        float best = -1f;
        for (int i = 0; i < MAX; i++) {
            if (!alive[i]) continue;
            float x0 = x[i] - HALF_W, x1 = x[i] + HALF_W;
            float y0 = y[i] - BODY_H, y1 = y[i];
            float z0 = z[i] - HALF_W, z1 = z[i] + HALF_W;
            float tEnter = -Float.MAX_VALUE, tExit = Float.MAX_VALUE;
            int enter = -1;
            boolean miss = false;
            for (int a = 0; a < 3; a++) {
                float o = a == 0 ? ox : (a == 1 ? oy : oz);
                float d = a == 0 ? dx : (a == 1 ? dy : dz);
                float lo = a == 0 ? x0 : (a == 1 ? y0 : z0);
                float hi = a == 0 ? x1 : (a == 1 ? y1 : z1);
                if (Math.abs(d) < 1e-6f) {
                    if (o < lo || o > hi) {
                        miss = true;
                        break;
                    }
                    continue;
                }
                float t1 = (lo - o) / d, t2 = (hi - o) / d;
                float tn = t1 < t2 ? t1 : t2;
                float tf = t1 < t2 ? t2 : t1;
                if (tn > tEnter) {
                    tEnter = tn;
                    enter = a;
                }
                if (tf < tExit) tExit = tf;
            }
            if (miss || enter < 0 || tEnter > tExit || tEnter < 0f) continue;
            if (best < 0f || tEnter < best) {
                best = tEnter;
                hitAxis = enter;
                float hy = oy + dy * tEnter;
                hitHead = hy < y[i] - HEAD_Y;
            }
        }
        return best;
    }

    int hitAxis() {
        return hitAxis;
    }

    boolean hitHead() {
        return hitHead;
    }

    boolean isAlive(int i) {
        return alive[i];
    }

    boolean isHurt(int i) {
        return hurtUntil[i] > System.currentTimeMillis();
    }

    float getX(int i) {
        return x[i];
    }

    float getY(int i) {
        return y[i];
    }

    float getZ(int i) {
        return z[i];
    }

    int target(float ex, float ey, float ez, float fx, float fy, float fz) {
        int best = -1;
        float bestT = Float.MAX_VALUE;
        for (int i = 0; i < MAX; i++) {
            if (!alive[i]) continue;
            float cy = y[i] - 0.9f;
            float vx = x[i] - ex, vy = cy - ey, vz = z[i] - ez;
            float t = vx * fx + vy * fy + vz * fz;
            if (t < 0.3f || t > MAX_RANGE) continue;
            float perp2 = vx * vx + vy * vy + vz * vz - t * t;

            if (perp2 < 1.0f && t < bestT && visible(ex, ey, ez, x[i], cy, z[i])) {
                bestT = t;
                best = i;
            }
        }
        return best;
    }

    void hit(int i, int dmg, float fx, float fz) {
        if (i < 0 || !alive[i]) return;
        hp[i] -= dmg;
        if (hp[i] <= 0) {
            alive[i] = false;
        } else {
            hurtUntil[i] = System.currentTimeMillis() + HURT_MS;

            float kb = 0.6f;
            float nx = x[i] + fx * kb;
            float nz = z[i] + fz * kb;

            if (canWalk(x[i], y[i], z[i], nx, z[i])) x[i] = nx;
            if (canWalk(x[i], y[i], z[i], x[i], nz)) z[i] = nz;

            cool[i] = 15;
        }
    }

    private boolean blocked(float px, float py, float pz) {
        return World.get(Calc.floor(px), Calc.floor(py - 0.5f), Calc.floor(pz)) != World.AIR;
    }

    private static int groundY(float px, float pz) {
        int bx = Calc.floor(px), bz = Calc.floor(pz);
        for (int yy = 0; yy < World.HEIGHT; yy++) {
            if (!World.passable(World.get(bx, yy, bz))) return yy;
        }
        return World.HEIGHT;
    }


    private static boolean visible(float ex, float ey, float ez, float tx, float ty, float tz) {
        for (int k = 1; k < 16; k++) {
            float f = k / 16f;
            int bx = Calc.floor(ex + (tx - ex) * f), by = Calc.floor(ey + (ty - ey) * f), bz = Calc.floor(ez + (tz - ez) * f);
            int id = World.peek(bx, by, bz);
if (id > World.AIR && !World.passable(id)) return false;
        }
        return true;
    }


    float getYaw(int i) {
        return yaw[i];
    }
}

final class Sheeps {
    static final int MAX = 6;
    static final int HP = 8;
    static final float SPEED = 0.04f;
    static final int SPAWN_FRAMES = 120;
    static final float MAX_RANGE = 3.5f;

    static final int HURT_MS = 2000;          
    static final int FLEE_FRAMES = 90;       
    static final float FLEE_SPEED = 0.10f;   

    static final float HALF = 0.55f;         
    static final float BOX_H = 1.1f;         

    private final boolean[] alive = new boolean[MAX];
    private final float[] x = new float[MAX];
    private final float[] y = new float[MAX];
    private final float[] z = new float[MAX];
    private final float[] yaw = new float[MAX];
    private final int[] hp = new int[MAX];
    private final int[] animTick = new int[MAX];
    private final long[] hurtUntil = new long[MAX];
    private final int[] fleeTick = new int[MAX];

    private final float[] targetX = new float[MAX];
    private final float[] targetZ = new float[MAX];

    private int spawnCool;
    private final java.util.Random rnd = new java.util.Random();

    private float tn, tf;

    
    private boolean slab(float e, float f, float lo, float hi) {
        if (f > -1e-6f && f < 1e-6f) return e >= lo && e <= hi;
        float a = (lo - e) / f, b = (hi - e) / f;
        if (a > b) { float t = a; a = b; b = t; }
        if (a > tn) tn = a;
        if (b < tf) tf = b;
        return tn <= tf;
    }




private int globalEatCool;


void clear() {
    for (int i = 0; i < MAX; i++) {
        alive[i] = false;
        animTick[i] = 0;
        hurtUntil[i] = 0;
        fleeTick[i] = 0;
        yaw[i] = 0f;
    }
    spawnCool = 0;
    globalEatCool = 0;
}


void update(float px, float pz, double dayTime) {
    boolean day = dayTime >= 0 && dayTime < 13000;
    if (day && --spawnCool <= 0) {
        spawnCool = SPAWN_FRAMES;
        spawn(px, pz);
    }

    if (globalEatCool > 0) globalEatCool--;

    for (int i = 0; i < MAX; i++) {
        if (!alive[i]) continue;

        
        float distToPlayer2 = (px - x[i]) * (px - x[i]) + (pz - z[i]) * (pz - z[i]);
        if (distToPlayer2 > 40f * 40f) {
            alive[i] = false;
            continue;
        }

        boolean fleeing = fleeTick[i] > 0;
        if (fleeing) {
            fleeTick[i]--;
            float ex = targetX[i] - x[i], ez = targetZ[i] - z[i];
            if (ex * ex + ez * ez < 0.25f) pickFlee(i);
        }

        float dx = targetX[i] - x[i];
        float dz = targetZ[i] - z[i];
        float d = (float) Math.sqrt(dx * dx + dz * dz);

        if (!fleeing && (d < 0.5f || rnd.nextInt(100) == 0)) {
            targetX[i] = x[i] + (rnd.nextFloat() * 10f - 5f);
            targetZ[i] = z[i] + (rnd.nextFloat() * 10f - 5f);
        } else if (d > 0.001f) {
            float spd = fleeing ? FLEE_SPEED : SPEED;
            float mx = dx / d * spd, mz = dz / d * spd;
            boolean movedX = false, movedZ = false;
            if (canWalk(x[i], y[i], z[i], x[i] + mx, z[i])) { x[i] += mx; movedX = true; }
            if (canWalk(x[i], y[i], z[i], x[i], z[i] + mz)) { z[i] += mz; movedZ = true; }
            if (fleeing && !movedX && !movedZ) pickFlee(i);

            yaw[i] = Calc.toDeg(Calc.atan2(dx, dz)) + 180f;
            animTick[i]++;
        }

        int g = groundY(x[i], z[i]);
        if (g >= World.HEIGHT) {
            alive[i] = false;
            continue;
        }
        y[i] = g;

        if (!fleeing && globalEatCool == 0 && rnd.nextInt(100) == 0) {
            int bx = Calc.floor(x[i]);
            int bz = Calc.floor(z[i]);
            if (World.get(bx, g, bz) == World.GRASS) {
                World.set(bx, g, bz, World.DIRT);
                targetX[i] = x[i];
                targetZ[i] = z[i];
                globalEatCool = 250; 
            }
        }
    }
}

private void spawn(float px, float pz) {
    int free = -1;
    for (int i = 0; i < MAX; i++) {
        if (!alive[i]) {
            free = i;
            break;
        }
    }
    if (free < 0) return;

    
    float ang = rnd.nextFloat() * 6.2831853f;
    float dist = 10f + rnd.nextFloat() * 10f;
    float sx = px + (float) Math.cos(ang) * dist;
    float sz = pz + (float) Math.sin(ang) * dist;

    int bx = Calc.floor(sx), bz = Calc.floor(sz);
    int g = groundY(sx, sz);
    if (g >= World.HEIGHT || World.get(bx, g, bz) != World.GRASS) return;

    alive[free] = true;
    x[free] = bx + 0.5f;
    z[free] = bz + 0.5f;
    y[free] = g;
    hp[free] = HP;
    animTick[free] = 0;
    hurtUntil[free] = 0;
    fleeTick[free] = 0;
    yaw[free] = 0f;
    targetX[free] = x[free];
    targetZ[free] = z[free];
}

    
    private void pickFlee(int i) {
        float a = rnd.nextFloat() * 6.2831855f;
        float tx = x[i] + (float) Math.cos(a) * 8f;
        float tz = z[i] + (float) Math.sin(a) * 8f;
        float lim = World.AREA - 1f;
        targetX[i] = tx < 1f ? 1f : (tx > lim ? lim : tx);
        targetZ[i] = tz < 1f ? 1f : (tz > lim ? lim : tz);
    }


    private boolean canWalk(float cx, float cy, float cz, float nx, float nz) {
        int currentGround = Calc.floor(cy);
        int targetGround = groundY(nx, nz);
        int diff = currentGround - targetGround;
        if (diff > 1) return false;
        int tx = Calc.floor(nx), tz = Calc.floor(nz);
        if (World.get(tx, currentGround, tz) == Items.FENCE) return false;
        if (World.get(tx, currentGround + 1, tz) == Items.FENCE) return false;
if (!World.passable(World.get(tx, targetGround - 1, tz))) return false;
if (!World.passable(World.get(tx, targetGround - 2, tz))) return false;
        return true;
    }


int target(float ex, float ey, float ez, float fx, float fy, float fz) {
    int best = -1;
    float bestT = Float.MAX_VALUE;
    for (int i = 0; i < MAX; i++) {
        if (!alive[i]) continue;
        
        
        float dx = x[i] - ex, dy = (y[i] - 0.55f) - ey, dz = z[i] - ez;
        float d2 = dx * dx + dy * dy + dz * dz;
        if (d2 > MAX_RANGE * MAX_RANGE) continue;
        
        
        float t = dx * fx + dy * fy + dz * fz;
        if (t < 0.3f || t >= bestT) continue;
        
        
        if (d2 - t * t < 0.35f && visible(ex, ey, ez, x[i], y[i] - 0.55f, z[i])) {
            bestT = t;
            best = i;
        }
    }
    return best;
}

    void hit(int i, int dmg, Inventory inv) {
        if (i < 0 || !alive[i]) return;
        hp[i] -= dmg;
        if (hp[i] <= 0) {
            alive[i] = false;
            inv.add(Items.WOOL, 1);
        } else {
            fleeTick[i] = FLEE_FRAMES;
            hurtUntil[i] = System.currentTimeMillis() + HURT_MS;
            pickFlee(i);
        }
    }

    boolean isAlive(int i) { return alive[i]; }
    boolean isHurt(int i) { return hurtUntil[i] > System.currentTimeMillis(); }
    float getX(int i) { return x[i]; }
    float getY(int i) { return y[i]; }
    float getZ(int i) { return z[i]; }
    float getYaw(int i) { return yaw[i]; }

    private static int groundY(float px, float pz) {
        int bx = Calc.floor(px), bz = Calc.floor(pz);
        for (int yy = 0; yy < World.HEIGHT; yy++) {
            if (!World.passable(World.get(bx, yy, bz))) return yy;
        }
        return World.HEIGHT;
    }

    private static boolean visible(float ex, float ey, float ez, float tx, float ty, float tz) {
        for (int k = 1; k < 16; k++) {
            float f = k / 16f;
            int bx = Calc.floor(ex + (tx - ex) * f), by = Calc.floor(ey + (ty - ey) * f), bz = Calc.floor(ez + (tz - ez) * f);
            int id = World.peek(bx, by, bz);
if (id > World.AIR && !World.passable(id)) return false;
        }
        return true;
    }

    private void spawn() {
        int free = -1;
        for (int i = 0; i < MAX; i++) {
            if (!alive[i]) {
                free = i;
                break;
            }
        }
        if (free < 0) return;

        float sx = rnd.nextFloat() * (World.AREA - 2) + 1;
        float sz = rnd.nextFloat() * (World.AREA - 2) + 1;

        int bx = Calc.floor(sx), bz = Calc.floor(sz);
        int g = groundY(sx, sz);
        if (g >= World.HEIGHT || World.get(bx, g, bz) != World.GRASS) return;

        alive[free] = true;
        x[free] = bx + 0.5f;
        z[free] = bz + 0.5f;
        y[free] = g;
        hp[free] = HP;
        animTick[free] = 0;
        hurtUntil[free] = 0;
        fleeTick[free] = 0;
        yaw[free] = 0f;
        targetX[free] = x[free];
        targetZ[free] = z[free];
    }

}
