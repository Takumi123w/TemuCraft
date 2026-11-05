final class Inventory {
    static final int HOTBAR = 9;
    static final int MAIN = 36;
    static final int GRID = 36;
    static final int GRID_MAX = 9;
    static final int OUT = 45;
    private static final int SLOTS = 45;

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
        StringBuffer sb = new StringBuffer();
        for (int s = 0; s < MAIN; s++) {
            if (s > 0) sb.append(' ');
            boolean has = sn[s] > 0;
            sb.append(has ? sid[s] : 0).append(':').append(sn[s]).append(':').append(has ? sd[s] : 0);
        }
        return sb.toString();
    }

    void load(String str) {
        clear();
        try {
            int s = 0, p = 0;
            while (s < MAIN && p < str.length()) {
                int sp = str.indexOf(' ', p);
                if (sp < 0) sp = str.length();
                int c1 = str.indexOf(':', p);
                if (c1 > p && c1 < sp) {
                    int c2 = str.indexOf(':', c1 + 1);
                    boolean hasDur = c2 > 0 && c2 < sp;
                    int item = Integer.parseInt(str.substring(p, c1));
                    int n = Integer.parseInt(str.substring(c1 + 1, hasDur ? c2 : sp));
                    int d = hasDur ? Integer.parseInt(str.substring(c2 + 1, sp)) : 0;
                    if (item >= 1 && item <= Items.MAX_ID && n > 0) {
                        int full = Items.durability(item);
                        sid[s] = item;
                        sn[s] = Math.min(n, Items.maxStack(item));
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