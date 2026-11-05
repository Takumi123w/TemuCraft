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
            if (x[i] < 0 || z[i] < 0 || x[i] >= World.AREA || z[i] >= World.AREA) {
                alive[i] = false;
                continue;
            }
            float dx = px - x[i], dz = pz - z[i];
            float d = (float) Math.sqrt(dx * dx + dz * dz);
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
if (id > World.AIR && id != Items.TORCH) return false;
        }
        return true;
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
        if (bx < 0 || bz < 0 || bx >= World.AREA || bz >= World.AREA) return;
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


    float getYaw(int i) {
        return yaw[i];
    }
}