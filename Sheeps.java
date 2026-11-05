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

    void update(double dayTime) {
        boolean day = dayTime >= 0 && dayTime < 13000;
        if (day && --spawnCool <= 0) {
            spawnCool = SPAWN_FRAMES;
            spawn();
        }

        for (int i = 0; i < MAX; i++) {
            if (!alive[i]) continue;

            if (x[i] < 0 || z[i] < 0 || x[i] >= World.AREA || z[i] >= World.AREA) {
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

            if (!fleeing && rnd.nextInt(100) == 0) {      
                int bx = Calc.floor(x[i]);
                int bz = Calc.floor(z[i]);
                if (World.get(bx, g, bz) == World.GRASS) {
                    World.set(bx, g, bz, World.DIRT);
                    targetX[i] = x[i];
                    targetZ[i] = z[i];
                }
            }
        }
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
if (!World.passable(World.get(tx, targetGround - 1, tz))) return false;
if (!World.passable(World.get(tx, targetGround - 2, tz))) return false;
        return true;
    }

    int target(float ex, float ey, float ez, float fx, float fy, float fz) {
        int best = -1;
        float bestT = Float.MAX_VALUE;
        for (int i = 0; i < MAX; i++) {
            if (!alive[i]) continue;
            tn = 0f;
            tf = MAX_RANGE;
            
            if (!slab(ex, fx, x[i] - HALF, x[i] + HALF)) continue;
            if (!slab(ey, fy, y[i] - BOX_H, y[i] + 0.05f)) continue;
            if (!slab(ez, fz, z[i] - HALF, z[i] + HALF)) continue;
            if (tn >= bestT) continue;

            
            float tm = (tn + tf) * 0.5f;
            if (!visible(ex, ey, ez, ex + fx * tm, ey + fy * tm, ez + fz * tm)) continue;

            bestT = tn;
            best = i;
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
if (id > World.AIR && id != Items.TORCH) return false;
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