
final class Renderer {
    
static int RW = 120, RH = 80;

static void setResolution(int w, int h) {
    RW = w;
    RH = h;
}

    static final float FOV = 90f;
    static final float RENDER_DIST = 80f;
    static final float PLAYER_REACH = 5f;
    static final boolean SHADOWS = true;

    
    static float px, py, pz;                 
    static float sinYaw, cosYaw, sinPitch, cosPitch;
    static float lx, ly, lz;                 
    static final float[] sunC = new float[3];
    static final float[] ambC = new float[3];
    static final float[] skyC = new float[3];
    
    static final float[] dimC = { 1f, 1f, 1f };
    
    static Zombies zombies;
    static Sheeps sheeps;

    
    static boolean hoverValid;               
    static int hovX, hovY, hovZ;
    static boolean newHoverValid;
    static int newHovX, newHovY, newHovZ;
    
    static int nrmX, nrmY, nrmZ;

    private Renderer() {}

    static void renderFrame(int[] fb) {
        newHoverValid = false;
        for (int sy = 0; sy < RH; sy++) {
            for (int sx = 0; sx < RW; sx++) {
                fb[sy * RW + sx] = trace(sx, sy);
            }
        }
        hoverValid = newHoverValid;
        hovX = newHovX;
        hovY = newHovY;
        hovZ = newHovZ;
    }

    


    
    private static int zombieColour(boolean head, int axis, float dAxis) {
        float ln = axis == 0 ? lx : (axis == 1 ? ly : lz);
        float li = dAxis > 0f ? -ln : ln;
        float light = (1 + li) / 2f;
        int base = head ? 0x5C9A4A : 0x3E7F2E;
        float r = ((base >> 16) & 0xFF) / 255f;
        float g = ((base >> 8) & 0xFF) / 255f;
        float b = (base & 0xFF) / 255f;
        r *= ambC[0] + (sunC[0] - ambC[0]) * light;
        g *= ambC[1] + (sunC[1] - ambC[1]) * light;
        b *= ambC[2] + (sunC[2] - ambC[2]) * light;
        return (Calc.channel(r) << 16) | (Calc.channel(g) << 8) | Calc.channel(b);
    }

    private static int trace(int sx, int sy) {
        boolean center = sx == RW / 2 && sy == RH / 2;

        float fx = ((sx - RW / 2) / FOV) / RW * 214f;
        float fy = ((sy - RH / 2) / FOV) / RH * 120f;

        
        float temp = cosPitch + fy * sinPitch;
        float dx = fx * cosYaw + temp * sinYaw;
        float dy = fy * cosPitch - sinPitch;
        float dz = temp * cosYaw - fx * sinYaw;
        float len = Calc.dist(dx, dy, dz);
        dx /= len;
        dy /= len;
        dz /= len;

        float sunDot = dx * lx + dy * ly + dz * lz;

        
        float pr = 0.525f, pg = 0.698f, pb = 0.996f;
        float fog = 0f;
        float light = 1f;
        
        float zd = zombies == null ? -1f : zombies.rayHit(px, py, pz, dx, dy, dz);
        boolean zHit = zd > 0f;
        float furthest = zHit ? Math.min(RENDER_DIST, zd) : RENDER_DIST;
        float hoverDist = PLAYER_REACH;
        float cx = -1, cy = -1, cz = -1;
        float ox = px, oy = py, oz = pz;
        boolean blockHit = false;

        for (int pass = 0; pass < (SHADOWS ? 2 : 1); pass++) {
            AXIS:
            for (int axis = 0; axis < 3; axis++) {
                float delta = axis == 0 ? dx : (axis == 1 ? dy : dz);
                float ad = Math.abs(delta);

                float rdx = dx / ad, rdy = dy / ad, rdz = dz / ad;

                float off = axis == 0 ? ox % 1f : (axis == 1 ? oy % 1f : oz % 1f);
                if (delta > 0) off = 1f - off;

                float travel = off / ad;
                float rx = ox + rdx * off;
                float ry = oy + rdy * off;
                float rz = oz + rdz * off;

                if (delta < 0) {
                    if (axis == 0) rx--;
                    else if (axis == 1) ry--;
                    else rz--;
                }

                while (travel < furthest) {
                    int bx = Calc.floor(rx), by = Calc.floor(ry), bz = Calc.floor(rz);

                    if (by < -2) break;                 
                    int id = World.peek(bx, by, bz);
                    if (id < 0) break;                  

                    if (id != World.AIR) {
                        int tx, ty;
                        if (axis == 1) {
                            tx = ((int) (rx * Textures.TEX)) & 15;
                            ty = ((int) (rz * Textures.TEX)) & 15;
                            if (rdy < 0f) ty += 2 * Textures.TEX;   
                        } else {
                            tx = ((int) ((rx + rz) * Textures.TEX)) & 15;
                            ty = (((int) (ry * Textures.TEX)) & 15) + Textures.TEX;
                        }

                        boolean outline = pass == 0 && hoverValid
                                && bx == hovX && by == hovY && bz == hovZ
                                && (tx == 0 || ty % Textures.TEX == 0
                                    || tx == Textures.TEX - 1 || ty % Textures.TEX == Textures.TEX - 1);

                        int tc = outline ? 0xFFFFFF : Textures.atlas[id * Textures.BLOCK + tx + ty * Textures.TEX];
                        boolean zero = (tc & 0xFFFFFF) == 0;

                        int direction = delta > 0f ? -1 : 1;

                        if (center && travel < hoverDist) {
                            if (pass == 0) {
                                newHoverValid = true;
                                newHovX = bx;
                                newHovY = by;
                                newHovZ = bz;
                                nrmX = 0;
                                nrmY = 0;
                                nrmZ = 0;
                                if (axis == 0) nrmX = direction;
                                else if (axis == 1) nrmY = direction;
                                else nrmZ = direction;
                            }
                            hoverDist = travel;
                        }

                        if (!zero) {
                            if (pass == 0) {
                                pr = ((tc >> 16) & 0xFF) / 255f;
                                pg = ((tc >> 8) & 0xFF) / 255f;
                                pb = (tc & 0xFF) / 255f;
                                fog = travel / RENDER_DIST;
                            } else {
                                light = 0.25f;     
                                break AXIS;
                            }

                            furthest = travel;
                            cx = rx;
                            cy = ry;
                            cz = rz;

                            float li = axis == 0 ? direction * lx : (axis == 1 ? direction * ly : direction * lz);
                            light = (1 + li) / 2f;
                            blockHit = true;
                        }
                    }

                    rx += rdx;
                    ry += rdy;
                    rz += rdz;
                    travel += 1f / ad;
                }
            }

            if (zHit && !blockHit) {
                
                int ax = zombies.hitAxis();
                return zombieColour(zombies.hitHead(), ax, ax == 0 ? dx : (ax == 1 ? dy : dz));
            }
            if (!blockHit) break;
            if (light <= 0.5f) break;

            
            ox = cx;
            oy = cy;
            oz = cz;
            dx = lx;
            dy = ly;
            dz = lz;
            furthest = RENDER_DIST;
        }

        if (blockHit) {
            
            pr = pr + (1f - pr) * fog;
            pg = pg + (1f - pg) * fog;
            pb = pb + (1f - pb) * fog;
            
            float lr = ambC[0] + (sunC[0] - ambC[0]) * light;
            float lg = ambC[1] + (sunC[1] - ambC[1]) * light;
            float lb = ambC[2] + (sunC[2] - ambC[2]) * light;
            pr *= lr;
            pg *= lg;
            pb *= lb;
        } else {
            float[] c = sunDot > 0.99f ? sunC : skyC;
            pr = c[0];
            pg = c[1];
            pb = c[2];
        }

        return (Calc.channel(pr) << 16) | (Calc.channel(pg) << 8) | Calc.channel(pb);
    }
}