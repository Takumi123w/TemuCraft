import java.util.Hashtable;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.m3g.Appearance;
import javax.microedition.m3g.Background;
import javax.microedition.m3g.Camera;
import javax.microedition.m3g.CompositingMode;
import javax.microedition.m3g.Fog;
import javax.microedition.m3g.Graphics3D;
import javax.microedition.m3g.Image2D;
import javax.microedition.m3g.Light;
import javax.microedition.m3g.Material;
import javax.microedition.m3g.PolygonMode;
import javax.microedition.m3g.Texture2D;
import javax.microedition.m3g.Transform;
import javax.microedition.m3g.TriangleStripArray;
import javax.microedition.m3g.VertexArray;
import javax.microedition.m3g.VertexBuffer;
import java.util.Random;

final class Renderer {
    
static int RW = 120, RH = 80;

static void setResolution(int w, int h) {
    RW = w;
    RH = h;
}

    static final float FOV = 90f;
    static final float RENDER_DIST = 16f;
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
    static Pigs pigs;

    static boolean hoverValid;               
    static int hovX, hovY, hovZ;
    static boolean newHoverValid;
    static int newHovX, newHovY, newHovZ;
    
    static int nrmX, nrmY, nrmZ;

    

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

final class M3GRenderer {
    static final int CH = 16;

    static int fov = 60;
    static final float NEAR = 0.2f;
static final float FAR = 16f;
    static final float DRAW_R = 16f;
static final float FOG_NEAR = 12f;
    private final float[] lc = new float[3];
private final VertexBuffer sVb, sVbHurt;
private final TriangleStripArray sTsa;
private final VertexBuffer pigVb, pigVbHurt;
private final TriangleStripArray pigTsa;
private final VertexBuffer torchVb;
private final TriangleStripArray torchTsa;
private final Transform torchT = new Transform();
private final VertexBuffer fenceVb;
private final TriangleStripArray fenceTsa;
private final Transform fenceT = new Transform();
private final VertexBuffer[] fenceVbs = new VertexBuffer[16];
private final TriangleStripArray[] fenceTsas = new TriangleStripArray[16];
private final VertexBuffer signVb, doorVb, doorOpenVb, plaqueVb;
private final TriangleStripArray signTsa, doorTsa, doorOpenTsa, plaqueTsa;
private final Transform thinT = new Transform();
private final Transform plaqueT = new Transform();
private final Hashtable plaqueCache = new Hashtable();
private final Appearance signApp, doorApp, fenceApp;
static final float TORCH_DRAW2 = 24f * 24f;   
private static final float SKY_D = 28f;           
private static final float[] WHITE = { 1f, 1f, 1f };
private final VertexBuffer sunVb, moonVb;
private final VertexArray sunCa, moonCa;              
private final TriangleStripArray skyTsa;
private final Appearance sunApp, moonApp;
private final Transform skyT = new Transform();
private final float[] skyM = new float[16];
private final byte[] skyBuf = new byte[16];
static boolean heldTorch;
static final int MAX_TORCH_LIGHTS = 4;     
static final float TORCH_LIGHT_R2 = 10f * 10f;
private final Appearance stoneApp;
private final VertexBuffer slabVb;
private final TriangleStripArray slabTsa;
private final VertexBuffer[] stairVb = new VertexBuffer[4];
private final TriangleStripArray stairTsa;
private static final float[][] STAIR_UPPER = {
    { -0.5f, 0.5f, 0.5f, 1f, -0.5f, 0f },   
    { 0f, 0.5f, 0.5f, 1f, -0.5f, 0.5f },    
    { -0.5f, 0.5f, 0.5f, 1f, 0f, 0.5f },    
    { -0.5f, 0f, 0.5f, 1f, -0.5f, 0.5f },   
};
private static final float[] STAIR_LOWER = { -0.5f, 0.5f, 0f, 0.5f, -0.5f, 0.5f };
static final int TORCH_LIT_R = 5;

static final int LIT_BIT = 1 << 16;
static final int LIT_MASK = LIT_BIT - 1;
private final Light ambLight = new Light();
private final Light torchLight = new Light();
private final Transform[] torchLT = new Transform[MAX_TORCH_LIGHTS];
private final boolean[] litUsed = new boolean[World.MAX_TORCH];
private final Material blockMat = new Material();
    static final boolean FOG_ON = false;
    static final int MAX_BUILD = 2;
    static final int NKEY = (Items.MAX_ID + 1) * 6;
    private Image glowImg;

private static Image makeGlow(int n) {
    int[] px = new int[n * n];
    for (int y = 0; y < n; y++) {
        for (int x = 0; x < n; x++) {
            float dx = x - n / 2 + 0.5f, dy = y - n / 2 + 0.5f;
            float d = (float) Math.sqrt(dx * dx + dy * dy) / (n / 2f);
            float a = d >= 1f ? 0f : (1f - d) * (1f - d);
            px[y * n + x] = ((int) (a * 150f) << 24) | 0xFF9020;
        }
    }
    return Image.createRGBImage(px, n, n, true);
}

private void drawPlacedGlows(Graphics g, int W, int H) {
    if (Renderer.dimC[0] > 0.9f) return;          
    int cnt = World.torchCount();
    if (cnt == 0) return;
    if (glowImg == null) glowImg = makeGlow(40);
    for (int i = 0; i < cnt; i++) {
        toCam(World.torchX(i) + 0.5f, World.torchY(i) + 0.3f, World.torchZ(i) + 0.5f, pa3);
        float z = pa3[2];
        if (z < 0.3f || z > 20f) continue;
        int sx = (int) (W / 2f + pa3[0] / (z * tanH) * (W / 2f));
        int sy = (int) (H / 2f - pa3[1] / (z * tanV) * (H / 2f));
        if (sx < -30 || sx > W + 30 || sy < -30 || sy > H + 30) continue;
        g.drawImage(glowImg, sx, sy, Graphics.HCENTER | Graphics.VCENTER);
    }
}

    
private static final int[][] DIRS = {
    { 1, -1, 0, 255 },
    { 1,  1, 2, 255 },
    { 0,  1, 1, 255 },
    { 0, -1, 1, 255 },
    { 2,  1, 1, 255 },
    { 2, -1, 1, 255 },
};

private static VertexBuffer pigMeshVb(boolean hurt) {
    int boxes = 7;
    int totalVerts = boxes * 24;
    short[] pos = new short[totalVerts * 3];
    byte[] col = new byte[totalVerts * 4];

    int bodyColor  = hurt ? 0xE05050 : 0xF0A0A0;
    int headColor  = hurt ? 0xD04040 : 0xF4B0B0;
    int snoutColor = hurt ? 0xB03030 : 0xE07F8A;
    int legColor   = hurt ? 0x803030 : 0xD88888;

    int v = 0;
    v = zBox(pos, col, v, -0.35f, 0.35f, 0.30f, 0.90f, -0.50f, 0.50f, bodyColor);
    v = zBox(pos, col, v, -0.25f, 0.25f, 0.40f, 1.00f, -0.75f, -0.50f, headColor);
    v = zBox(pos, col, v, -0.12f, 0.12f, 0.50f, 0.75f, -0.85f, -0.75f, snoutColor);
    v = zBox(pos, col, v, -0.30f, -0.15f, 0.00f, 0.30f, -0.40f, -0.20f, legColor);
    v = zBox(pos, col, v,  0.15f,  0.30f, 0.00f, 0.30f, -0.40f, -0.20f, legColor);
    v = zBox(pos, col, v, -0.30f, -0.15f, 0.00f, 0.30f,  0.20f,  0.40f, legColor);
    v = zBox(pos, col, v,  0.15f,  0.30f, 0.00f, 0.30f,  0.20f,  0.40f, legColor);

    VertexArray pa = new VertexArray(totalVerts, 3, 2);
    pa.set(0, totalVerts, pos);
    VertexArray ca = new VertexArray(totalVerts, 4, 1);
    ca.set(0, totalVerts, col);

    VertexBuffer vb = new VertexBuffer();
    vb.setPositions(pa, 0.01f, null);
    vb.setColors(ca);
    return vb;
}

private static VertexBuffer sheep3dMeshVb(boolean hurt) {
    int totalVerts = 6 * 24;
    short[] pos = new short[totalVerts * 3];
    byte[] col = new byte[totalVerts * 4];

    int woolColor = hurt ? 0xFA7D7D : 0xE8E8E8;
    int headColor = hurt ? 0xD48D91 : 0xD2B48C;
    int legColor  = hurt ? 0x444444 : 0x444444;

    int v = 0;
    
    v = zBox(pos, col, v, -0.35f, 0.35f, 0.40f, 0.90f, -0.40f, 0.40f, woolColor);
    
    v = zBox(pos, col, v, -0.20f, 0.20f, 0.60f, 1.00f, -0.60f, -0.40f, headColor);
    
    v = zBox(pos, col, v, -0.30f, -0.15f, 0.00f, 0.40f, -0.30f, -0.15f, legColor);
    
    v = zBox(pos, col, v,  0.15f,  0.30f, 0.00f, 0.40f, -0.30f, -0.15f, legColor);
    
    v = zBox(pos, col, v, -0.30f, -0.15f, 0.00f, 0.40f,  0.15f,  0.30f, legColor);
    
    v = zBox(pos, col, v,  0.15f,  0.30f, 0.00f, 0.40f,  0.15f,  0.30f, legColor);

    VertexArray pa = new VertexArray(totalVerts, 3, 2);
    pa.set(0, totalVerts, pos);
    VertexArray ca = new VertexArray(totalVerts, 4, 1);
    ca.set(0, totalVerts, col);

    VertexBuffer vb = new VertexBuffer();
    vb.setPositions(pa, 0.01f, null);
    vb.setColors(ca);
    return vb;
}

    private final int[] hb = new int[3];
    private final float[] hc = new float[3];
    private final float[] pa3 = new float[3], pb3 = new float[3];
    private final float[] outlineQ = new float[3];

    
    private void drawOutline(Graphics g, int W, int H) {
        if (!Renderer.hoverValid) return;
        hb[0] = Renderer.hovX;
        hb[1] = Renderer.hovY;
        hb[2] = Renderer.hovZ;
        hc[0] = Renderer.px;
        hc[1] = Renderer.py;
        hc[2] = Renderer.pz;

        g.setColor(0x000000);
        for (int e = 0; e < 3; e++) {                 
            int u = (e + 1) % 3, v = (e + 2) % 3;
            for (int s = 0; s < 4; s++) {
                int bu = s & 1, bv = s >> 1;
                
                boolean fu = (hc[u] - (hb[u] + bu)) * (bu == 1 ? 1 : -1) > 0;
                boolean fv = (hc[v] - (hb[v] + bv)) * (bv == 1 ? 1 : -1) > 0;
                if (!fu && !fv) continue;

                float[] q = outlineQ;
                q[e] = hb[e];
                q[u] = hb[u] + bu;
                q[v] = hb[v] + bv;
                toCam(q[0], q[1], q[2], pa3);
                q[e] = hb[e] + 1;
                toCam(q[0], q[1], q[2], pb3);
                edge2d(g, W, H);
            }
        }
    }

    
    private void toCam(float gx, float gy, float gz, float[] out) {
        float rx = gx - Renderer.px;
        float ry = Renderer.py - gy;                  
        float rz = gz - Renderer.pz;
        out[0] = rx * m[0] + ry * m[4] + rz * m[8];
        out[1] = rx * m[1] + ry * m[5] + rz * m[9];
        out[2] = -(rx * m[2] + ry * m[6] + rz * m[10]);
    }

    private void edge2d(Graphics g, int W, int H) {
        final float Z = 0.1f;
        if (pa3[2] < Z && pb3[2] < Z) return;
        if (pa3[2] < Z) clipNear(pa3, pb3, Z);
        else if (pb3[2] < Z) clipNear(pb3, pa3, Z);

        int x1 = (int) (W / 2f + pa3[0] / (pa3[2] * tanH) * (W / 2f));
        int y1 = (int) (H / 2f - pa3[1] / (pa3[2] * tanV) * (H / 2f));
        int x2 = (int) (W / 2f + pb3[0] / (pb3[2] * tanH) * (W / 2f));
        int y2 = (int) (H / 2f - pb3[1] / (pb3[2] * tanV) * (H / 2f));
        g.drawLine(x1, y1, x2, y2);
        g.drawLine(x1 + 1, y1, x2 + 1, y2);           
    }

    
    private static void clipNear(float[] a, float[] b, float zMin) {
        float t = (zMin - a[2]) / (b[2] - a[2]);
        a[0] += (b[0] - a[0]) * t;
        a[1] += (b[1] - a[1]) * t;
        a[2] = zMin;
    }

    
    private static final class Section {
        final VertexBuffer[] vbs = new VertexBuffer[NKEY];
        final TriangleStripArray[] tsas = new TriangleStripArray[NKEY];
    }

    private final Graphics3D g3d;
    private final Appearance[] apps = new Appearance[NKEY];
    
    private final Appearance[] darkApps = new Appearance[NKEY];
    private final Fog fog;
    private final Background bg;
    private final Camera cam;
    
static int texDist = 24;
    
    private final Camera dimCam;
    private final VertexBuffer dimVb;
    private final VertexArray dimColors;
    private final TriangleStripArray dimTsa;
    private final Appearance dimApp;
    private final byte[] dimBytes = new byte[16];
private final Appearance plainApp;   
    private final Transform camT = new Transform();
    private final Transform identity = new Transform();
    private final float[] m = new float[16];


private final VertexBuffer zVb, zVbHurt;
private final TriangleStripArray zTsa;
private final Transform zT = new Transform();
private final float[] zm = new float[16];

    
    private final int[] mask = new int[CH * CH];
    private final int[] cc = new int[3];
    private final int[] org = new int[3];
    private final int[] p = new int[3];
    private int[] rec = new int[9 * 512];   
    private int nrec;

    M3GRenderer() {
        g3d = Graphics3D.getInstance();
        identity.setIdentity();

PolygonMode pm = new PolygonMode();
pm.setCulling(PolygonMode.CULL_NONE);
pm.setShading(PolygonMode.SHADE_SMOOTH);
pm.setPerspectiveCorrectionEnable(false);   

        CompositingMode opaque = new CompositingMode();
        CompositingMode cutout = new CompositingMode();
        CompositingMode disc = new CompositingMode();
disc.setAlphaThreshold(0.5f);      
disc.setDepthWriteEnable(false);   
sunCa = new VertexArray(4, 4, 1);
moonCa = new VertexArray(4, 4, 1);
sunVb = skyQuadVb(sunCa);
moonVb = skyQuadVb(moonCa);
skyTsa = new TriangleStripArray(0, new int[] { 4 });
sunApp = discApp(true, pm, disc);
moonApp = discApp(false, pm, disc);
tintSky(WHITE, sunCa);
tintSky(WHITE, moonCa);
        cutout.setAlphaThreshold(0.5f);   
        sVb = sheep3dMeshVb(false);
sVbHurt = sheep3dMeshVb(true);
 int[] sStrips = new int[36]; 
 for (int i = 0; i < sStrips.length; i++) sStrips[i] = 4;
 sTsa = new TriangleStripArray(0, sStrips);
 pigVb = pigMeshVb(false);
pigVbHurt = pigMeshVb(true);
int[] pStrips = new int[42];
for (int i = 0; i < pStrips.length; i++) pStrips[i] = 4;
pigTsa = new TriangleStripArray(0, pStrips);

        fog = new Fog();
        fog.setMode(Fog.LINEAR);
        fog.setLinear(FOG_NEAR, FAR);

blockMat.setVertexColorTrackingEnable(true);
blockMat.setShininess(0f);

ambLight.setMode(Light.AMBIENT);
ambLight.setColor(0xD9D9D9);             

torchLight.setMode(Light.OMNI);
torchLight.setColor(0xFFB060);           
torchLight.setAttenuation(1f, 0f, 0.08f);

for (int i = 0; i < MAX_TORCH_LIGHTS; i++) torchLT[i] = new Transform();
        
        for (int id = 1; id <= Items.MAX_ID; id++) {
            if (Textures.NAMES[id].length() == 0) continue;   
            for (int band = 0; band < 3; band++) {
                apps[id * 3 + band] = makeApp(Textures.atlas, id, band, pm, opaque);
                if (!sameTile(id, band)) {
                    darkApps[id * 3 + band] = makeApp(Textures.darkAtlas, id, band, pm, opaque);
                }
            }
        }

        bg = new Background();
        bg.setColorClearEnable(true);

        cam = new Camera();

        
        dimCam = new Camera();
        dimCam.setParallel(2f, 1f, 0.1f, 10f);

        VertexArray dimPos = new VertexArray(4, 3, 2);
        dimPos.set(0, 4, new short[] { -1, -1, -1, 1, -1, -1, -1, 1, -1, 1, 1, -1 });
        dimColors = new VertexArray(4, 4, 1);
        dimVb = new VertexBuffer();
        dimVb.setPositions(dimPos, 1f, null);
        dimVb.setColors(dimColors);
        dimTsa = new TriangleStripArray(0, new int[] { 4 });

        CompositingMode modulate = new CompositingMode();
        modulate.setBlending(CompositingMode.MODULATE);
        modulate.setDepthTestEnable(false);
        modulate.setDepthWriteEnable(false);
        dimApp = new Appearance();
        dimApp.setCompositingMode(modulate);
        dimApp.setPolygonMode(pm);
        plainApp = new Appearance();
plainApp.setPolygonMode(pm);
plainApp.setCompositingMode(opaque);
if (FOG_ON) plainApp.setFog(fog);


zVb = zombie3dMeshVb(false);
zVbHurt = zombie3dMeshVb(true);
 int[] zStrips = new int[36]; 
 for (int i = 0; i < zStrips.length; i++) zStrips[i] = 4;
 zTsa = new TriangleStripArray(0, zStrips);

torchVb = torchMeshVb();
int[] tStrips = new int[12];
for (int i = 0; i < tStrips.length; i++) tStrips[i] = 4;
torchTsa = new TriangleStripArray(0, tStrips);

fenceVb = fenceMeshVb();
int[] fStrips = new int[18];
for (int i = 0; i < fStrips.length; i++) fStrips[i] = 4;
fenceTsa = new TriangleStripArray(0, fStrips);
for (int m = 0; m < 16; m++) {
    float[][] fb = fenceBoxes(m);
    int[] fc = new int[fb.length];
    for (int q = 0; q < fc.length; q++) fc[q] = 0x8B5A2B;
    fenceVbs[m] = texBoxesVb(fb, fc);
    fenceTsas[m] = boxesTsa(fb.length);
}

signVb = texBoxesVb(new float[][] {
    { -0.04f, 0.04f, 0.00f, 0.30f, -0.04f, 0.04f },
    { -0.45f, 0.45f, 0.30f, 0.85f, -0.03f, 0.03f } },
    new int[] { 0x8B5A2B, 0xC8A060 });
signTsa = boxesTsa(2);
doorVb = texBoxesVb(new float[][] {
    { -0.50f, 0.50f, 0.00f, 2.00f, -0.06f, 0.06f } },
    new int[] { 0x9C6D3C });
doorTsa = boxesTsa(1);
doorOpenVb = texBoxesVb(new float[][] {
    { 0.44f, 0.50f, 0.00f, 2.00f, -0.50f, 0.50f } },
    new int[] { 0x9C6D3C });
doorOpenTsa = boxesTsa(1);
plaqueVb = plaqueMeshVb();
plaqueTsa = new TriangleStripArray(0, new int[] { 4 });
Image itemSheet;
try {
    itemSheet = Image.createImage("/tex/items.png");
} catch (java.io.IOException e) {
    throw new IllegalStateException("missing /tex/items.png: " + e);
}
signApp = tileApp(itemSheet, 0, 0);
doorApp = tileApp(itemSheet, 1, 0);
fenceApp = tileApp(itemSheet, 3, 0);
 Image blocksImg;
try {
    blocksImg = Image.createImage("/tex/blocks.png");
} catch (java.io.IOException e) {
    throw new IllegalStateException("missing /tex/blocks.png: " + e);
}
stoneApp = tileApp(blocksImg, 8, 0);   
slabVb = texBoxesVb(new float[][] { { -0.5f, 0.5f, 0.5f, 1f, -0.5f, 0.5f } }, new int[] { 0xFFFFFF });
slabTsa = boxesTsa(1);
for (int f = 0; f < 4; f++) {
    stairVb[f] = texBoxesVb(new float[][] { STAIR_LOWER, STAIR_UPPER[f] }, new int[] { 0xFFFFFF, 0xFFFFFF });
}
stairTsa = boxesTsa(2);
        zm[0] = 1;
        zm[5] = 1;
        zm[10] = 1;
        zm[15] = 1;
    }
    
private static VertexBuffer torchMeshVb() {
    int totalVerts = 2 * 24;
    short[] pos = new short[totalVerts * 3];
    byte[] col = new byte[totalVerts * 4];
    int v = 0;
    v = zBox(pos, col, v, -0.04f, 0.04f, 0.00f, 0.50f, -0.04f, 0.04f, 0x8B5A2B); 
    v = zBox(pos, col, v, -0.07f, 0.07f, 0.50f, 0.72f, -0.07f, 0.07f, 0xFFB020); 

    VertexArray pa = new VertexArray(totalVerts, 3, 2);
    pa.set(0, totalVerts, pos);
    VertexArray ca = new VertexArray(totalVerts, 4, 1);
    ca.set(0, totalVerts, col);

    VertexBuffer vb = new VertexBuffer();
    vb.setPositions(pa, 0.01f, null);
    vb.setColors(ca);
    return vb;
}
    
private static VertexBuffer fenceMeshVb() {
    int totalVerts = 3 * 24;
    short[] pos = new short[totalVerts * 3];
    byte[] col = new byte[totalVerts * 4];
    int wood = 0x8B5A2B;
    int v = 0;
    v = zBox(pos, col, v, -0.06f, 0.06f, 0.00f, 0.90f, -0.06f, 0.06f, wood);
    v = zBox(pos, col, v, -0.50f, 0.50f, 0.30f, 0.38f, -0.03f, 0.03f, wood);
    v = zBox(pos, col, v, -0.50f, 0.50f, 0.62f, 0.70f, -0.03f, 0.03f, wood);

    VertexArray pa = new VertexArray(totalVerts, 3, 2);
    pa.set(0, totalVerts, pos);
    VertexArray ca = new VertexArray(totalVerts, 4, 1);
    ca.set(0, totalVerts, col);

    VertexBuffer vb = new VertexBuffer();
    vb.setPositions(pa, 0.01f, null);
    vb.setColors(ca);
    return vb;
}


private static float[][] fenceBoxes(int mask) {
    int n = 1;
    for (int k = 0; k < 4; k++) if ((mask & (1 << k)) != 0) n += 2;
    float[][] out = new float[n][];
    out[0] = new float[] { -0.06f, 0.06f, 0.00f, 0.90f, -0.06f, 0.06f };
    int c = 1;
    for (int k = 0; k < 4; k++) {
        if ((mask & (1 << k)) == 0) continue;
        for (int h = 0; h < 2; h++) {
            float y0 = h == 0 ? 0.30f : 0.62f;
            float y1 = y0 + 0.08f;
            if (k == 0) out[c] = new float[] { 0.00f, 0.50f, y0, y1, -0.03f, 0.03f };
            else if (k == 1) out[c] = new float[] { -0.50f, 0.00f, y0, y1, -0.03f, 0.03f };
            else if (k == 2) out[c] = new float[] { -0.03f, 0.03f, y0, y1, 0.00f, 0.50f };
            else out[c] = new float[] { -0.03f, 0.03f, y0, y1, -0.50f, 0.00f };
            c++;
        }
    }
    return out;
}


private static VertexBuffer texBoxesVb(float[][] boxes, int[] rgb) {
    int totalVerts = boxes.length * 24;
    short[] pos = new short[totalVerts * 3];
    byte[] col = new byte[totalVerts * 4];
    int v = 0;
    for (int i = 0; i < boxes.length; i++) {
        float[] b = boxes[i];
        v = zBox(pos, col, v, b[0], b[1], b[2], b[3], b[4], b[5], 0xFFFFFF);
    }
    short[] uv = new short[totalVerts * 2];
    for (int i = 0; i < totalVerts; i++) {
        uv[i * 2] = (short) (i & 1);
        uv[i * 2 + 1] = (short) ((i >> 1) & 1);
    }
    VertexArray pa = new VertexArray(totalVerts, 3, 2);
    pa.set(0, totalVerts, pos);
    VertexArray ta = new VertexArray(totalVerts, 2, 2);
    ta.set(0, totalVerts, uv);
    VertexBuffer vb = new VertexBuffer();
    vb.setPositions(pa, 0.01f, null);
    vb.setTexCoords(0, ta, 1f, null);
    return vb;
}


private static Appearance tileApp(Image sheet, int col, int band) {
    Image tile = Image.createImage(sheet, col * Textures.TEX, band * Textures.TEX,
            Textures.TEX, Textures.TEX, 0);
    Texture2D t = new Texture2D(new Image2D(Image2D.RGB, tile));
    t.setFiltering(Texture2D.FILTER_NEAREST, Texture2D.FILTER_NEAREST);
    t.setWrapping(Texture2D.WRAP_REPEAT, Texture2D.WRAP_REPEAT);
    t.setBlending(Texture2D.FUNC_REPLACE);
    PolygonMode p = new PolygonMode();
    p.setCulling(PolygonMode.CULL_NONE);
    Appearance a = new Appearance();
    a.setTexture(0, t);
    a.setPolygonMode(p);
    return a;
}

private static VertexBuffer boxesVb(float[][] boxes, int[] rgb) {
    int totalVerts = boxes.length * 24;
    short[] pos = new short[totalVerts * 3];
    byte[] col = new byte[totalVerts * 4];
    int v = 0;
    for (int i = 0; i < boxes.length; i++) {
        float[] b = boxes[i];
        v = zBox(pos, col, v, b[0], b[1], b[2], b[3], b[4], b[5], rgb[i]);
    }
    VertexArray pa = new VertexArray(totalVerts, 3, 2);
    pa.set(0, totalVerts, pos);
    VertexArray ca = new VertexArray(totalVerts, 4, 1);
    ca.set(0, totalVerts, col);
    VertexBuffer vb = new VertexBuffer();
    vb.setPositions(pa, 0.01f, null);
    vb.setColors(ca);
    return vb;
}

private static TriangleStripArray boxesTsa(int n) {
    int[] s = new int[n * 6];
    for (int i = 0; i < s.length; i++) s[i] = 4;
    return new TriangleStripArray(0, s);
}


private static VertexBuffer plaqueMeshVb() {
    short[] pos = { -42, 36, 4,   42, 36, 4,   -42, 80, 4,   42, 80, 4 };
    short[] tex = { 0, 1,   1, 1,   0, 0,   1, 0 };
    VertexArray pa = new VertexArray(4, 3, 2);
    pa.set(0, 4, pos);
    VertexArray ta = new VertexArray(4, 2, 2);
    ta.set(0, 4, tex);
    VertexBuffer vb = new VertexBuffer();
    vb.setPositions(pa, 0.01f, null);
    vb.setTexCoords(0, ta, 1f, null);
    return vb;
}

private Appearance plaqueApp(String text) {
    Appearance cached = (Appearance) plaqueCache.get(text);
    if (cached != null) return cached;
    if (plaqueCache.size() >= 16) plaqueCache.clear();
    Image img = Image.createImage(128, 32);
    Graphics ig = img.getGraphics();
    ig.setColor(0x3A2A18);
    ig.fillRect(0, 0, 128, 32);
    ig.setColor(0xFFF4D0);
    ig.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
    int nl = text.indexOf('\n');
    String l1 = nl < 0 ? text : text.substring(0, nl);
    String l2 = nl < 0 ? "" : text.substring(nl + 1);
    ig.drawString(l1, 4, 2, Graphics.TOP | Graphics.LEFT);
    if (l2.length() > 0) ig.drawString(l2, 4, 16, Graphics.TOP | Graphics.LEFT);
    Texture2D tex = new Texture2D(new Image2D(Image2D.RGB, img));
    tex.setFiltering(Texture2D.FILTER_NEAREST, Texture2D.FILTER_NEAREST);
    tex.setWrapping(Texture2D.WRAP_CLAMP, Texture2D.WRAP_CLAMP);
    tex.setBlending(Texture2D.FUNC_REPLACE);
    PolygonMode p = new PolygonMode();
    p.setCulling(PolygonMode.CULL_NONE);
    Appearance a = new Appearance();
    a.setTexture(0, tex);
    a.setPolygonMode(p);
    plaqueCache.put(text, a);
    return a;
}

private static VertexBuffer zombie3dMeshVb(boolean hurt) {
    int totalVerts = 6 * 24;
    short[] pos = new short[totalVerts * 3];
    byte[] col = new byte[totalVerts * 4];

    int headColor  = hurt ? 0xE04040 : 0x5C9A4A;
    int skinColor  = hurt ? 0xC02020 : 0x468232;
    int shirtColor = hurt ? 0xC01818 : 0x2E6FA4;
    int pantsColor = hurt ? 0x801010 : 0x3F3151;

    int v = 0;
    
    v = zBox(pos, col, v, -0.25f, 0.25f, 1.30f, 1.80f, -0.25f, 0.25f, headColor);
    
    v = zBox(pos, col, v, -0.25f, 0.25f, 0.65f, 1.30f, -0.125f, 0.125f, shirtColor);
    
    v = zBox(pos, col, v, -0.25f, 0.00f, 0.00f, 0.65f, -0.125f, 0.125f, pantsColor);
    
    v = zBox(pos, col, v,  0.00f, 0.25f, 0.00f, 0.65f, -0.125f, 0.125f, pantsColor);
    
    v = zBox(pos, col, v, -0.40f, -0.25f, 0.90f, 1.15f, -0.65f, -0.125f, skinColor);
    
    v = zBox(pos, col, v,  0.25f,  0.40f, 0.90f, 1.15f, -0.65f, -0.125f, skinColor);

    VertexArray pa = new VertexArray(totalVerts, 3, 2);
    pa.set(0, totalVerts, pos);
    VertexArray ca = new VertexArray(totalVerts, 4, 1);
    ca.set(0, totalVerts, col);

    VertexBuffer vb = new VertexBuffer();
    vb.setPositions(pa, 0.01f, null);
    vb.setColors(ca);
    return vb;
}

private static int zBox(short[] pos, byte[] col, int baseIndex, float x0, float x1, float y0, float y1, float z0, float z1, int rgb) {
    float[][] f = {
        { x1, y0, z0, x1, y1, z0, x1, y0, z1, x1, y1, z1 }, 
        { x0, y0, z1, x0, y1, z1, x0, y0, z0, x0, y1, z0 }, 
        { x0, y1, z0, x1, y1, z0, x0, y1, z1, x1, y1, z1 }, 
        { x0, y0, z1, x1, y0, z1, x0, y0, z0, x1, y0, z0 }, 
        { x0, y0, z1, x1, y0, z1, x0, y1, z1, x1, y1, z1 }, 
        { x1, y0, z0, x0, y0, z0, x1, y1, z0, x0, y1, z0 }, 
    };
    int[] shade = { 200, 160, 255, 110, 185, 150 };
    int r0 = (rgb >> 16) & 0xFF, g0 = (rgb >> 8) & 0xFF, b0 = rgb & 0xFF;
    for (int k = 0; k < 6; k++) {
        for (int c = 0; c < 4; c++) {
            int idx = baseIndex + k * 4 + c;
            pos[idx * 3]     = (short) toHundredths(f[k][c * 3]);
            pos[idx * 3 + 1] = (short) toHundredths(f[k][c * 3 + 1]);
            pos[idx * 3 + 2] = (short) toHundredths(f[k][c * 3 + 2]);
            col[idx * 4]     = (byte) (r0 * shade[k] / 255);
            col[idx * 4 + 1] = (byte) (g0 * shade[k] / 255);
            col[idx * 4 + 2] = (byte) (b0 * shade[k] / 255);
            col[idx * 4 + 3] = (byte) 255;
        }
    }
    return baseIndex + 24;
}

private static int toHundredths(float v) {
    return (int) (v * 100f + (v < 0f ? -0.5f : 0.5f));
}
    
    
private static VertexBuffer zombieBillboardVb() {
    short[] pos = new short[] {
        -40,   0, 0,
         40,   0, 0,
        -40, 180, 0,
         40, 180, 0
    };
    short[] uv = new short[] { 0, 1, 1, 1, 0, 0, 1, 0 };
    byte[] col = new byte[] {
        (byte)255, (byte)255, (byte)255, (byte)255,
        (byte)255, (byte)255, (byte)255, (byte)255,
        (byte)255, (byte)255, (byte)255, (byte)255,
        (byte)255, (byte)255, (byte)255, (byte)255
    };
    VertexArray pa = new VertexArray(4, 3, 2);
    pa.set(0, 4, pos);
    VertexArray ta = new VertexArray(4, 2, 2);
    ta.set(0, 4, uv);
    VertexArray ca = new VertexArray(4, 4, 1);
    ca.set(0, 4, col);

    VertexBuffer vb = new VertexBuffer();
    vb.setPositions(pa, 0.01f, null);
    vb.setTexCoords(0, ta, 1f, null);
    vb.setColors(ca);
    return vb;
}

private static Image2D loadPngOrFallback(String name, int fallbackRgb) {
    try {
        Image img = Image.createImage(name);
        int w = img.getWidth(), h = img.getHeight();
        int[] rgb = new int[w * h];
        img.getRGB(rgb, 0, w, 0, 0, w, h);
        byte[] px = new byte[w * h * 4];
        for (int i = 0; i < w * h; i++) {
            int c = rgb[i];
            px[i * 4]     = (byte) ((c >> 16) & 0xFF);
            px[i * 4 + 1] = (byte) ((c >> 8) & 0xFF);
            px[i * 4 + 2] = (byte) (c & 0xFF);
            px[i * 4 + 3] = (byte) ((c >> 24) & 0xFF);
        }
        return new Image2D(Image2D.RGBA, w, h, px);
    } catch (Throwable t) {
        byte[] px = new byte[16 * 16 * 4];
        byte r = (byte) ((fallbackRgb >> 16) & 0xFF);
        byte g = (byte) ((fallbackRgb >> 8) & 0xFF);
        byte b = (byte) (fallbackRgb & 0xFF);
        for (int i = 0; i < 16 * 16; i++) {
            px[i * 4]     = r;
            px[i * 4 + 1] = g;
            px[i * 4 + 2] = b;
            px[i * 4 + 3] = (byte) 255;
        }
        return new Image2D(Image2D.RGBA, 16, 16, px);
    }
}

    
    private static VertexBuffer zombieVb() {
        short[] pos = new short[48 * 3];
        byte[] col = new byte[48 * 4];
        zBox(pos, col, 0, -0.3f, 0.3f, 0f, 1.3f, -0.3f, 0.3f, 0x3E7F2E);
        zBox(pos, col, 24, -0.22f, 0.22f, 1.3f, 1.8f, -0.22f, 0.22f, 0x5C9A4A);
        VertexArray pa = new VertexArray(48, 3, 2);
        pa.set(0, 48, pos);
        VertexArray ca = new VertexArray(48, 4, 1);
        ca.set(0, 48, col);
        VertexBuffer vb = new VertexBuffer();
        vb.setPositions(pa, 0.01f, null);
        vb.setColors(ca);
        return vb;
    }
    
    
    static boolean available() {
        System.out.println("[m3g] microedition.m3g.version = " + System.getProperty("microedition.m3g.version"));
        try {
            Class.forName("javax.microedition.m3g.Graphics3D");
            Graphics3D.getInstance();
            System.out.println("[m3g] Graphics3D OK");
            return true;
        } catch (Throwable t) {
            System.out.println("[m3g] unavailable: " + t);
            return false;
        }
    }

    
    private static boolean sameTile(int id, int band) {
        int n = Textures.TEX * Textures.TEX;
        int base = id * Textures.BLOCK + band * n;
        for (int i = 0; i < n; i++) {
            if (Textures.atlas[base + i] != Textures.darkAtlas[base + i]) return false;
        }
        return true;
    }

    private static byte[] loadPixels(int[] src, int id, int band) {
        int n = Textures.TEX;
        byte[] px = new byte[n * n * 4];
        for (int i = 0; i < n * n; i++) {
            int c = src[id * Textures.BLOCK + band * n * n + i];
            px[i * 4] = (byte) (c >> 16);
            px[i * 4 + 1] = (byte) (c >> 8);
            px[i * 4 + 2] = (byte) c;
            px[i * 4 + 3] = (byte) (c >>> 24);
        }
        return px;
    }
    
    

private static boolean nearTorch(int x, int y, int z) {
    int n = World.torchCount();
    for (int i = 0; i < n; i++) {
        int dx = Math.abs(World.torchX(i) - x), dy = Math.abs(World.torchY(i) - y), dz = Math.abs(World.torchZ(i) - z);
        if (dx <= TORCH_LIT_R && dy <= TORCH_LIT_R && dz <= TORCH_LIT_R) return true;
    }
    return false;
}

private static float sectionDist2(World.Chunk c, int sy) {
    float dx = c.cx * CH + CH / 2f - Renderer.px;
    float dy = sy * CH + CH / 2f - Renderer.py;
    float dz = c.cz * CH + CH / 2f - Renderer.pz;
    return dx * dx + dy * dy + dz * dz;
}

private Appearance makeApp(int[] src, int id, int band, PolygonMode pm, CompositingMode cm) {
    byte[] px = loadPixels(src, id, band);
    Texture2D tex = new Texture2D(new Image2D(Image2D.RGBA, Textures.TEX, Textures.TEX, px));
    tex.setFiltering(Texture2D.FILTER_NEAREST, Texture2D.FILTER_NEAREST);
    tex.setWrapping(Texture2D.WRAP_REPEAT, Texture2D.WRAP_REPEAT);
    tex.setBlending(Texture2D.FUNC_MODULATE);

    Appearance a = new Appearance();
    a.setTexture(0, tex);
    a.setPolygonMode(pm);
    a.setCompositingMode(cm);
    a.setMaterial(blockMat);
    if (FOG_ON) a.setFog(fog);
    return a;
}

private void setLights() {
    g3d.resetLights();
    g3d.addLight(ambLight, identity);
    int n = 0;

    if (heldTorch) {
        placeLight(torchLT[n], Renderer.px, Renderer.py - 0.5f, Renderer.pz);
        g3d.addLight(torchLight, torchLT[n]);
        n++;
    }

    int count = World.torchCount();
    for (int i = 0; i < count; i++) litUsed[i] = false;
    while (n < MAX_TORCH_LIGHTS) {
        int best = -1;
        float bestD = TORCH_LIGHT_R2;
        for (int i = 0; i < count; i++) {
            if (litUsed[i]) continue;
            float dx = World.torchX(i) + 0.5f - Renderer.px;
            float dy = World.torchY(i) + 0.5f - Renderer.py;
            float dz = World.torchZ(i) + 0.5f - Renderer.pz;
            float d = dx * dx + dy * dy + dz * dz;
            if (d < bestD) { bestD = d; best = i; }
        }
        if (best < 0) break;
        litUsed[best] = true;
        placeLight(torchLT[n], World.torchX(best) + 0.5f, World.torchY(best) + 0.5f, World.torchZ(best) + 0.5f);
        g3d.addLight(torchLight, torchLT[n]);
        n++;
    }
}


private static void placeLight(Transform t, float x, float y, float z) {
    t.setIdentity();
    t.postTranslate(x, -y, z);
}


private boolean buildUrgent() {
    boolean did = false;
    for (int i = 0; i < World.chunkCount(); i++) {
        World.Chunk c = World.chunkAt(i);
        for (int sy = 0; sy < World.SECTIONS; sy++) {
            if (!c.urgent[sy]) continue;
            c.urgent[sy] = false;
            if (!visible(c, sy)) continue;     

            if (bc != null) {                  
                bc.dirty[bsy] = true;
                bc = null;
                bsec = null;
            }
            bc = c;
            bsy = sy;
            c.dirty[sy] = false;
            org[0] = c.cx * CH; org[1] = sy * CH; org[2] = c.cz * CH;
            nrec = 0; bd = 0; bsl = 0; bsec = null;
            while (bc != null) stepSection();
            did = true;
        }
    }
    return did;
}

    
private static int lastW = -1, lastH = -1, lastFov = -1;
    private static float tanH, tanV;
    private static float sqH, sqV;
    private static float fwX, fwY, fwZ;
    private static float rtX, rtY, rtZ;
    private static float upX, upY, upZ;

    private static void updateFrustum(int W, int H) {
        float cp = Renderer.cosPitch, sp = Renderer.sinPitch;
        fwX = cp * Renderer.sinYaw;
        fwY = -sp;
        fwZ = cp * Renderer.cosYaw;

        float rx = -fwZ, rz = fwX;
        float lenSq = rx * rx + rz * rz;
        float invLen = lenSq < 1e-6f ? 1f : FastMath.invSqrt(lenSq);
        rtX = rx * invLen;
        rtY = 0f;
        rtZ = rz * invLen;

        upX = fwY * rtZ - fwZ * rtY;
        upY = fwZ * rtX - fwX * rtZ;
        upZ = fwX * rtY - fwY * rtX;

        if (W != lastW || H != lastH || fov != lastFov) {
            lastW = W;
            lastH = H;
            lastFov = fov;
            tanV = (float) Math.tan(Math.toRadians(fov * 0.5));
            tanH = tanV * (float) W / H;
            sqH = (float) Math.sqrt(1f + tanH * tanH);
            sqV = (float) Math.sqrt(1f + tanV * tanV);
        }
    }
    
static final int FRAME_MAX_MS = 90;   
static final int SLICE_MAX_MS = 40;
static final int SLICE_MIN_MS = 4;     
private long prevStart, prevWork;

private int budgetMs(long now) {
    long period = prevStart == 0 ? 0 : now - prevStart;
    prevStart = now;
    long base = period - prevWork;
    if (base < 0) base = 0;
    long b = FRAME_MAX_MS - base;
    if (b > SLICE_MAX_MS) b = SLICE_MAX_MS;
    if (b < SLICE_MIN_MS) b = SLICE_MIN_MS;
    return (int) b;
}


private World.Chunk bc;
private int bsy, bd, bsl, bKey;
private Section bsec;
private int[] bcnt;

private boolean pickSection() {
    World.Chunk best = null; int bestSy = 0; float bestD = 1e30f;
    for (int i = 0; i < World.chunkCount(); i++) {
        World.Chunk c = World.chunkAt(i);
        for (int sy = 0; sy < World.SECTIONS; sy++) {
            if (!c.dirty[sy] && c.view[sy] != null) continue;
            if (!visible(c, sy)) continue;
            if (sectionDist2(c, sy) > DRAW_R * DRAW_R) continue;
            float d = sectionDist2(c, sy);
            if (d < bestD) { bestD = d; best = c; bestSy = sy; }
        }
    }
    if (best == null) return false;
    bc = best; bsy = bestSy;
    best.dirty[bestSy] = false;
    org[0] = bc.cx * CH; org[1] = bsy * CH; org[2] = bc.cz * CH;
    nrec = 0; bd = 0; bsl = 0; bsec = null;
    return true;
}

private void buildTick(long deadline) {
    do {
        if (bc == null) {
            if (!pickSection()) return;
        } else if (World.find(bc.cx, bc.cz) != bc) {   
            bc = null; bsec = null;
            continue;
        }
        stepSection();
    } while (System.currentTimeMillis() < deadline);
}

private void stepSection() {
    if (bd < DIRS.length) {                       
        int a = DIRS[bd][0], sign = DIRS[bd][1], band = DIRS[bd][2], shade = DIRS[bd][3];
        int ua = (a + 1) % 3, va = (a + 2) % 3;
        int slice = org[a] + bsl;
        int plane = slice + (sign > 0 ? 1 : 0);
        for (int j = 0; j < CH; j++)
            for (int i = 0; i < CH; i++)
                mask[j * CH + i] = faceBlock(a, slice, ua, org[ua] + i, va, org[va] + j, sign);
        greedy(plane, shade, band, a, ua, va, sign);
        if (++bsl >= CH) { bsl = 0; bd++; }
        if (bd >= DIRS.length) {
            bsec = new Section();
            bcnt = new int[NKEY];
            for (int r = 0; r < nrec; r++) bcnt[rec[r * 9] * 6 + rec[r * 9 + 1]]++;
            bKey = 0;
        }
        return;
    }
    while (bKey < NKEY && bcnt[bKey] == 0) bKey++;   
    if (bKey < NKEY) { buildMesh(bsec, bKey, bcnt[bKey]); bKey++; }
    if (bKey >= NKEY) { bc.view[bsy] = bsec; bc = null; bsec = null; }
}
    
void render(Graphics g, int W, int H) {
    updateFrustum(W, H);

long t0 = System.currentTimeMillis();
int budget = budgetMs(t0);
if (buildUrgent()) budget = SLICE_MIN_MS;     
long t1 = System.currentTimeMillis();
World.tick(Long.MAX_VALUE);
buildTick(World.instantGen ? Long.MAX_VALUE : t1 + budget);
prevWork = System.currentTimeMillis() - t0;

        
        int sky = 0xFF000000
                | (Calc.channel(over(Renderer.skyC[0], Renderer.dimC[0])) << 16)
                | (Calc.channel(over(Renderer.skyC[1], Renderer.dimC[1])) << 8)
                | Calc.channel(over(Renderer.skyC[2], Renderer.dimC[2]));
        bg.setColor(sky);
        if (FOG_ON) fog.setColor(sky);

        cam.setPerspective((float) fov, (float) W / H, NEAR, FAR);
        setCameraTransform();

        g3d.bindTarget(g, true, 0);
        try {
            g3d.setViewport(0, 0, W, H);
g3d.clear(bg);
g3d.setCamera(cam, camT);
setLights();

float lim = DRAW_R + SECTION_R;
float lim2 = lim * lim;
for (int i = 0; i < World.chunkCount(); i++) {
    World.Chunk c = World.chunkAt(i);
    for (int sy = 0; sy < World.SECTIONS; sy++) {
        if (c.view[sy] == null || !visible(c, sy)) continue;
        if (sectionDist2(c, sy) > lim2) continue;
        Section s = (Section) c.view[sy];
        boolean night = Game.dayAmt < 0.5f;
        for (int k = 0; k < NKEY; k++) {
            if (s.vbs[k] != null) {
                int kid = k / 6, kb = k % 6, kband = kb % 3;
                boolean dark = night && kb < 3 && darkApps[kid * 3 + kband] != null;
                Appearance ka = dark ? darkApps[kid * 3 + kband] : apps[kid * 3 + kband];
                g3d.render(s.vbs[k], s.tsas[k], ka, identity);
            }
        }
    }
}
Zombies zs = Renderer.zombies;
            if (zs != null) {
                for (int i = 0; i < Zombies.MAX; i++) {
                    if (!zs.isAlive(i)) continue;

                    
                    zT.setIdentity();
                    zT.postTranslate(zs.getX(i), -zs.getY(i), zs.getZ(i));
                    zT.postRotate(zs.getYaw(i), 0, 1, 0); 
                    g3d.render(zs.isHurt(i) ? zVbHurt : zVb, zTsa, plainApp, zT);
                }
            }
  
            for (int i = 0; i < World.torchCount(); i++) {
    float tx = World.torchX(i) + 0.5f;
    float ty = World.torchY(i);
    float tz = World.torchZ(i) + 0.5f;
    float dx = tx - Renderer.px, dy = ty - Renderer.py, dz = tz - Renderer.pz;
    if (dx * dx + dy * dy + dz * dz > TORCH_DRAW2) continue;
    torchT.setIdentity();
    torchT.postTranslate(tx, -(ty + 1), tz);
    g3d.render(torchVb, torchTsa, plainApp, torchT);
}

            for (int i = 0; i < World.thinCount(); i++) {
                int tt = World.thinType(i);
                int bx = World.thinX(i), by = World.thinY(i), bz = World.thinZ(i);
                float tx = bx + 0.5f;
                float ty = by;
                float tz = bz + 0.5f;
                float dx = tx - Renderer.px, dy = ty - Renderer.py, dz = tz - Renderer.pz;
                if (dx * dx + dy * dy + dz * dz > TORCH_DRAW2) continue;
                thinT.setIdentity();
                float base = (tt == Items.DOOR || tt == Items.DOOR_OPEN) ? ty + 2 : ty + 1;
                thinT.postTranslate(tx, -base, tz);
                if (tt == Items.DOOR || tt == Items.DOOR_OPEN) {
    thinT.postRotate(90 * World.doorRot(bx, by, bz), 0, 1, 0);
}
                if (tt == Items.FENCE) {
                    int fm = 0;
                    if (World.peek(bx + 1, by, bz) == Items.FENCE) fm |= 1;
                    if (World.peek(bx - 1, by, bz) == Items.FENCE) fm |= 2;
                    if (World.peek(bx, by, bz + 1) == Items.FENCE) fm |= 4;
                    if (World.peek(bx, by, bz - 1) == Items.FENCE) fm |= 8;
                    g3d.render(fenceVbs[fm], fenceTsas[fm], fenceApp, thinT);
                } else if (tt == Items.SIGN) {
                    g3d.render(signVb, signTsa, signApp, thinT);
                    String st = World.signText(bx, by, bz);
                    if (st.length() > 0) {
                        Appearance pa = plaqueApp(st);
                        
                        
                        
                        g3d.render(plaqueVb, plaqueTsa, pa, thinT);
                        plaqueT.setIdentity();
                        plaqueT.postTranslate(tx, -(ty + 1), tz);
                        plaqueT.postRotate(180, 0, 1, 0);
                        g3d.render(plaqueVb, plaqueTsa, pa, plaqueT);
                        g3d.render(plaqueVb, plaqueTsa, pa, plaqueT);
                    }
                } else if (tt == Items.DOOR) {
                    g3d.render(doorVb, doorTsa, doorApp, thinT);
                } else if (tt == Items.SLAB) {
    g3d.render(slabVb, slabTsa, stoneApp, thinT);
} else if (tt >= Items.STAIR_N && tt <= Items.STAIR_W) {
    g3d.render(stairVb[tt - Items.STAIR_N], stairTsa, stoneApp, thinT);
} else if (tt == Items.DOOR_OPEN) {
                    g3d.render(doorOpenVb, doorOpenTsa, doorApp, thinT);
                }
            }

            Sheeps sps = Renderer.sheeps;
            if (sps != null) {
                for (int i = 0; i < Sheeps.MAX; i++) {
                    if (!sps.isAlive(i)) continue;
                    
                    zT.setIdentity();
                    zT.postTranslate(sps.getX(i), -sps.getY(i), sps.getZ(i));
                    zT.postRotate(sps.getYaw(i), 0, 1, 0); 
                    g3d.render(sps.isHurt(i) ? sVbHurt : sVb, sTsa, plainApp, zT);
                }
            }
            Pigs pgs = Renderer.pigs;
if (pgs != null) {
    for (int i = 0; i < Pigs.MAX; i++) {
        if (!pgs.isAlive(i)) continue;
        zT.setIdentity();
        zT.postTranslate(pgs.getX(i), -pgs.getY(i), pgs.getZ(i));
        zT.postRotate(pgs.getYaw(i), 0, 1, 0);
        g3d.render(pgs.isHurt(i) ? pigVbHurt : pigVb, pigTsa, plainApp, zT);
    }
}
            drawDim();
            drawSky();   
                } finally {
            g3d.releaseTarget();
        }
drawOutline(g, W, H);
drawPlacedGlows(g, W, H);
    }
    

private void drawSky() {
    g3d.setCamera(cam, camT);   
    float dx = Renderer.lx, dy = -Renderer.ly, dz = Renderer.lz;   
    float ex = Renderer.px, ey = -Renderer.py, ez = Renderer.pz;
}


private void placeSky(VertexBuffer vb, Appearance a, float ex, float ey, float ez,
                      float dx, float dy, float dz) {
    System.arraycopy(m, 0, skyM, 0, 16);
    skyM[3] = ex + dx * SKY_D;
    skyM[7] = ey + dy * SKY_D;
    skyM[11] = ez + dz * SKY_D;
    skyT.set(skyM);
    g3d.render(vb, skyTsa, a, skyT);
}

private void tintSky(float[] c, VertexArray ca) {
    int r = Calc.channel(c[0]), g = Calc.channel(c[1]), b = Calc.channel(c[2]);
    for (int v = 0; v < 4; v++) {
        skyBuf[v * 4] = (byte) r;
        skyBuf[v * 4 + 1] = (byte) g;
        skyBuf[v * 4 + 2] = (byte) b;
        skyBuf[v * 4 + 3] = (byte) 255;
    }
    ca.set(0, 4, skyBuf);
}


private static VertexBuffer skyQuadVb(VertexArray ca) {
    short[] pos = { -126, -126, 0, 126, -126, 0, -126, 126, 0, 126, 126, 0 };   
    short[] uv = { 0, 1, 1, 1, 0, 0, 1, 0 };
    VertexArray pa = new VertexArray(4, 3, 2);
    pa.set(0, 4, pos);
    VertexArray ta = new VertexArray(4, 2, 2);
    ta.set(0, 4, uv);
    VertexBuffer vb = new VertexBuffer();
    vb.setPositions(pa, 0.01f, null);
    vb.setTexCoords(0, ta, 1f, null);
    vb.setColors(ca);
    return vb;
}


private static Appearance discApp(boolean sun, PolygonMode pm, CompositingMode cm) {
    int n = 32;
    byte[] px = new byte[n * n * 4];
    for (int y = 0; y < n; y++) {
        for (int x = 0; x < n; x++) {
            float dx = x - 15.5f, dy = y - 15.5f;
            float d2 = dx * dx + dy * dy;
            int i = (y * n + x) * 4;
            if (d2 > 15.5f * 15.5f) {          
                px[i + 3] = 0;
                continue;
            }
            int r = 225, g = 225, b = 215;
            if (sun) {
                r = 255;
                g = d2 < 36f ? 255 : 210;
                b = d2 < 36f ? 230 : 70;
            } else if ((x - 10) * (x - 10) + (y - 11) * (y - 11) < 9
                    || (x - 21) * (x - 21) + (y - 20) * (y - 20) < 7
                    || (x - 20) * (x - 20) + (y - 7) * (y - 7) < 4) {
                r = 170;
                g = 170;
                b = 165;
            }
            px[i] = (byte) r;
            px[i + 1] = (byte) g;
            px[i + 2] = (byte) b;
            px[i + 3] = (byte) 255;
        }
    }
    Texture2D tex = new Texture2D(new Image2D(Image2D.RGBA, n, n, px));
    tex.setFiltering(Texture2D.FILTER_NEAREST, Texture2D.FILTER_NEAREST);
    tex.setWrapping(Texture2D.WRAP_CLAMP, Texture2D.WRAP_CLAMP);
    Appearance a = new Appearance();
    a.setTexture(0, tex);
    a.setPolygonMode(pm);
    a.setCompositingMode(cm);
    return a;
}

    private static float over(float sky, float dim) {
        float v = dim < 0.05f ? sky : sky / dim;
        return v > 1f ? 1f : v;
    }

    
private float lastDimR = -1f, lastDimG = -1f, lastDimB = -1f;

    private void drawDim() {
        float[] d = Renderer.dimC;
        if (d[0] > 0.995f && d[1] > 0.995f && d[2] > 0.995f) return;

        
        if (d[0] != lastDimR || d[1] != lastDimG || d[2] != lastDimB) {
            lastDimR = d[0]; lastDimG = d[1]; lastDimB = d[2];
            byte r = (byte) Calc.channel(d[0]);
            byte g = (byte) Calc.channel(d[1]);
            byte b = (byte) Calc.channel(d[2]);
            for (int i = 0; i < 4; i++) {
                dimBytes[i * 4]     = r;
                dimBytes[i * 4 + 1] = g;
                dimBytes[i * 4 + 2] = b;
                dimBytes[i * 4 + 3] = (byte) 255;
            }
            dimColors.set(0, 4, dimBytes);
        }
        g3d.setCamera(dimCam, identity);
        g3d.render(dimVb, dimTsa, dimApp, identity);
    }

    
    static final float SECTION_R = 14f;
    
private void setCameraTransform() {
        float ex = Renderer.px, ey = -Renderer.py, ez = Renderer.pz;   
        float cp = Renderer.cosPitch, sp = Renderer.sinPitch;
        float fx = cp * Renderer.sinYaw, fy = sp, fz = cp * Renderer.cosYaw;   

        float rx = -fz, rz = fx;
        float lenSq = rx * rx + rz * rz;
        float invLen = lenSq < 1e-6f ? 1f : FastMath.invSqrt(lenSq);
        rx *= invLen;
        rz *= invLen;

        float ux = -rz * fy;
        float uy = rz * fx - rx * fz;
        float uz = rx * fy;

        m[0] = rx;  m[1] = ux;  m[2] = -fx;  m[3] = ex;
        m[4] = 0f;  m[5] = uy;  m[6] = -fy;  m[7] = ey;
        m[8] = rz;  m[9] = uz;  m[10] = -fz; m[11] = ez;
        m[12] = 0;  m[13] = 0;  m[14] = 0;   m[15] = 1;
        camT.set(m);
    }
    
    private static boolean visible(World.Chunk c, int sy) {
        float cx = c.cx * CH + CH / 2f;
        float cy = sy * CH + CH / 2f;
        float cz = c.cz * CH + CH / 2f;
        float dx = cx - Renderer.px, dy = cy - Renderer.py, dz = cz - Renderer.pz;

        float depth = dx * fwX + dy * fwY + dz * fwZ;       
        if (depth < -SECTION_R || depth > FAR + SECTION_R) return false;

        float lat = dx * rtX + dy * rtY + dz * rtZ;         
        float vert = dx * upX + dy * upY + dz * upZ;        

        
        if (lat - tanH * depth > SECTION_R * sqH) return false;
        if (-lat - tanH * depth > SECTION_R * sqH) return false;
        if (vert - tanV * depth > SECTION_R * sqV) return false;
        if (-vert - tanV * depth > SECTION_R * sqV) return false;
        return true;
    }
    
private int faceBlock(int a, int slice, int ua, int u, int va, int v, int sign) {
    cc[a] = slice;
    cc[ua] = u;
    cc[va] = v;
    int id = World.peek(cc[0], cc[1], cc[2]);
    if (id <= World.AIR || id == Items.TORCH || World.isThin(id)) return 0;
    int lit = nearTorch(cc[0], cc[1], cc[2]) ? LIT_BIT : 0;
    cc[a] = slice + sign;
    int nb = World.peek(cc[0], cc[1], cc[2]);

    
    if (id == World.LEAVES && nb == World.LEAVES) return 0;
    
    if (nb == World.AIR || nb == World.UNLOADED || nb == Items.TORCH || World.isThin(nb)) return id | lit;
    return 0;
}

    private void addRecord(int id, int band, int shade, int a, int plane, int u0, int v0, int w, int h) {
        if ((nrec + 1) * 9 > rec.length) {
            int[] r = new int[rec.length * 2];
            System.arraycopy(rec, 0, r, 0, nrec * 9);
            rec = r;
        }
        int o = nrec * 9;
        rec[o] = id;
        rec[o + 1] = band;
        rec[o + 2] = shade;
        rec[o + 3] = a;
        rec[o + 4] = plane;
        rec[o + 5] = u0;
        rec[o + 6] = v0;
        rec[o + 7] = w;
        rec[o + 8] = h;
        nrec++;
    }

    
    private void greedy(int plane, int shade, int band, int a, int ua, int va, int sign) {
        for (int j = 0; j < CH; j++) {
            for (int i = 0; i < CH; i++) {
                int id = mask[j * CH + i];
                if (id == 0) continue;

                int w = 1;
                while (i + w < CH && mask[j * CH + i + w] == id) w++;

                int h = 1;
                boolean grow = true;
                while (j + h < CH && grow) {
                    for (int k = 0; k < w; k++) {
                        if (mask[(j + h) * CH + i + k] != id) {
                            grow = false;
                            break;
                        }
                    }
                    if (grow) h++;
                }

                for (int dy = 0; dy < h; dy++) {
                    for (int dx = 0; dx < w; dx++) mask[(j + dy) * CH + i + dx] = 0;
                }

                int bid = id & LIT_MASK;
                int litBand = (id & LIT_BIT) != 0 ? 3 : 0;
                addRecord(bid, band + litBand, bid == World.FLAT ? 255 : shade, a + (sign > 0 ? 3 : 0), plane, org[ua] + i, org[va] + j, w, h);
            }
        }
    }

    
    private Section buildSection(World.Chunk c, int sy) {
        org[0] = c.cx * CH;
        org[1] = sy * CH;
        org[2] = c.cz * CH;
        nrec = 0;

        for (int d = 0; d < DIRS.length; d++) {
            int a = DIRS[d][0], sign = DIRS[d][1], band = DIRS[d][2], shade = DIRS[d][3];
            int ua = (a + 1) % 3, va = (a + 2) % 3;
            for (int sl = 0; sl < CH; sl++) {
                int slice = org[a] + sl;
                int plane = slice + (sign > 0 ? 1 : 0);
                for (int j = 0; j < CH; j++) {
                    for (int i = 0; i < CH; i++) {
                        mask[j * CH + i] = faceBlock(a, slice, ua, org[ua] + i, va, org[va] + j, sign);
                    }
                }
                greedy(plane, shade, band, a, ua, va, sign);
            }
        }

        
        Section sec = new Section();
        int[] cnt = new int[NKEY];
        for (int r = 0; r < nrec; r++) {
            cnt[rec[r * 9] * 6 + rec[r * 9 + 1]]++;
        }
        for (int k = 0; k < NKEY; k++) {
            if (cnt[k] > 0) buildMesh(sec, k, cnt[k]);
        }
        return sec;
    }

private void buildMesh(Section sec, int key, int nq) {
    int nv = nq * 4;
    short[] pos = new short[nv * 3];
    short[] uv = new short[nv * 2];
    byte[] nrm = new byte[nv * 3];
    byte[] col = new byte[nv * 4];

    int q = 0;
    for (int r = 0; r < nrec; r++) {
        int o = r * 9;
        if (rec[o] * 6 + rec[o + 1] != key) continue;

        int aRaw = rec[o + 3], plane = rec[o + 4];
        int u0 = rec[o + 5], v0 = rec[o + 6], w = rec[o + 7], h = rec[o + 8];
        int a = aRaw % 3;
        int sign = aRaw >= 3 ? 1 : -1;
        int ua = (a + 1) % 3, va = (a + 2) % 3;

        
        int nx = a == 0 ? sign : 0;
        int ny = a == 1 ? -sign : 0;
        int nz = a == 2 ? sign : 0;

        for (int c = 0; c < 4; c++) {
            int cu = c & 1, cv = c >> 1;
            p[a] = plane;
            p[ua] = u0 + cu * w;
            p[va] = v0 + cv * h;

            int vi = q * 4 + c;
            pos[vi * 3] = (short) (p[0] * 16);
            pos[vi * 3 + 1] = (short) (-p[1] * 16);
            pos[vi * 3 + 2] = (short) (p[2] * 16);

            nrm[vi * 3] = (byte) (nx * 127);
            nrm[vi * 3 + 1] = (byte) (ny * 127);
            nrm[vi * 3 + 2] = (byte) (nz * 127);

            uv[vi * 2] = (short) (cu * w);
            uv[vi * 2 + 1] = (short) (cv * h);

            
            col[vi * 4] = (byte) 255;
            col[vi * 4 + 1] = (byte) 255;
            col[vi * 4 + 2] = (byte) 255;
            col[vi * 4 + 3] = (byte) 255;
        }
        q++;
    }

    VertexArray pa = new VertexArray(nv, 3, 2);
    pa.set(0, nv, pos);
    VertexArray ta = new VertexArray(nv, 2, 2);
    ta.set(0, nv, uv);
    VertexArray na = new VertexArray(nv, 3, 1);
    na.set(0, nv, nrm);
    VertexArray ca = new VertexArray(nv, 4, 1);
    ca.set(0, nv, col);

    VertexBuffer vb = new VertexBuffer();
    vb.setPositions(pa, 1f / 16f, null);
    vb.setTexCoords(0, ta, 1f, null);
    vb.setNormals(na);
    vb.setColors(ca);

    int[] strips = new int[nq];
    for (int i = 0; i < nq; i++) strips[i] = 4;

    sec.vbs[key] = vb;
    sec.tsas[key] = new TriangleStripArray(0, strips);
}



    
    static void pick(float ex, float ey, float ez, float dx, float dy, float dz) {
        Renderer.hoverValid = false;
        int x = Calc.floor(ex), y = Calc.floor(ey), z = Calc.floor(ez);
        int sx = dx > 0 ? 1 : -1, sy = dy > 0 ? 1 : -1, sz = dz > 0 ? 1 : -1;

        float tdx = dx != 0 ? Math.abs(1f / dx) : 1e9f;
        float tdy = dy != 0 ? Math.abs(1f / dy) : 1e9f;
        float tdz = dz != 0 ? Math.abs(1f / dz) : 1e9f;
        float tmx = dx != 0 ? (dx > 0 ? x + 1 - ex : ex - x) * tdx : 1e9f;
        float tmy = dy != 0 ? (dy > 0 ? y + 1 - ey : ey - y) * tdy : 1e9f;
        float tmz = dz != 0 ? (dz > 0 ? z + 1 - ez : ez - z) * tdz : 1e9f;

        int nx = 0, ny = 0, nz = 0;
        float t = 0f;
        for (int i = 0; i < 32 && t <= Renderer.PLAYER_REACH; i++) {
            int id = World.get(x, y, z);
            if (id < 0) return;                  
            if (id != World.AIR) {
                Renderer.hoverValid = true;
                Renderer.hovX = x;
                Renderer.hovY = y;
                Renderer.hovZ = z;
                
                Renderer.nrmX = nx;
                Renderer.nrmY = ny;
                Renderer.nrmZ = nz;
                return;
            }
            if (tmx < tmy && tmx < tmz) {
                t = tmx;
                x += sx;
                tmx += tdx;
                nx = -sx; ny = 0; nz = 0;
            } else if (tmy < tmz) {
                t = tmy;
                y += sy;
                tmy += tdy;
                nx = 0; ny = -sy; nz = 0;
            } else {
                t = tmz;
                z += sz;
                tmz += tdz;
                nx = 0; ny = 0; nz = -sz;
            }
        }
    }

    private static int argb(float[] c) {
        return 0xFF000000 | (Calc.channel(c[0]) << 16) | (Calc.channel(c[1]) << 8) | Calc.channel(c[2]);
    }

}

final class Game extends Canvas implements Runnable, CommandListener {
    static int targetFps = 30;
    static final int PHYS_MS = 10;
    static boolean showInfo = false;
private Inventory box;   
static final int LAST_SLOT = Inventory.CONT + Inventory.CONT_MAX - 1;
    static boolean touchOn = false;
    static boolean touchSet = false;
    
    static boolean autoJump = true;
    private boolean grounded;

    static void autoTouch(boolean hasPointer) {
        if (!touchSet) touchOn = hasPointer;
    }

    
    
    private static final int T_UP = 0, T_DOWN = 1, T_LEFT = 2, T_RIGHT = 3, T_JUMP = 4,
            T_PAUSE = 5, T_COUNT = 6;
    private static final float LOOK_SENS = 0.012f;   
    private static final int TAP_MS = 250;
    private final int[] tr = new int[4];             
    private int touchBtn = -1;                       
    private boolean touchLook;                       
    private boolean touchDragged;
    private int touchX, touchY, touchDownX, touchDownY;
    private long touchDownAt;
    private Image touchImg, touchImgOn;
    private int touchImgSize;
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
    static float dayAmt = 1f; 
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
    private int memTick;
    private int mineX = -1, mineY = -1, mineZ = -1;

    static final int MAX_HP = 10;
    private final Zombies zombies = new Zombies();
    private final Sheeps sheeps = new Sheeps();
    private final Pigs pigs = new Pigs();
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
        autoTouch(hasPointerEvents());
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
        Renderer.pigs = pigs;
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

private int doorRot() {
    
    return Math.abs(fwdX()) > Math.abs(fwdZ()) ? 1 : 0;
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
    if (invMode == 3 && s == Inventory.CONT + 2) {       
        if (inv.cursorCount() == 0) clickSlot(s);
        return;
    }
    if (invMode == 3 && inv.cursorCount() > 0) {          
        int want = s == Inventory.CONT ? Items.IRON_ORE : (s == Inventory.CONT + 1 ? Items.COAL : -1);
        if (want >= 0 && inv.cursorId() != want) return;
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
    if (box != null) {
        inv.storeBox(box, invMode == 3 ? 3 : 27);
        box = null;
        World.saveAll();
    }
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
    return invMode == 1 ? 9 : (invMode == 0 ? 4 : 0);
}

private boolean tall() {
    return invMode == 1 || invMode == 2;
}

private int mainY() {
    return tall() ? MAIN_Y3 : MAIN_Y;
}

private int hotY() {
    return tall() ? HOT_Y3 : HOT_Y;
}

private int panelH() {
    return tall() ? PH3 : PH;
}

private boolean shown(int s) {
    if (s == CLOSE_BTN) return true;
    if (s >= Inventory.CONT) {
        int c = s - Inventory.CONT;
        return invMode == 2 ? c < 27 : (invMode == 3 && c < 3);
    }
    if (s == Inventory.OUT) return invMode < 2;
    if (s >= Inventory.GRID && s < Inventory.OUT) return s - Inventory.GRID < gridCells();
    return true;
}

private int slotX(int s) {
    if (s == CLOSE_BTN) return PW - SZ - 2;
    if (s == Inventory.OUT) return invMode == 1 ? 4 + 6 * SZ : GX + 3 * SZ;
    if (s >= Inventory.CONT) {
        int c = s - Inventory.CONT;
        if (invMode == 3) return c == 2 ? GX + 3 * SZ : GX;
        return 4 + (c % 9) * SZ;
    }
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
    if (s >= Inventory.CONT) {
        int c = s - Inventory.CONT;
        if (invMode == 3) return c == 2 ? GY + SZ / 2 : GY + c * SZ;
        return GY + (c / 9) * SZ;
    }
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
        for (int s = 0; s <= LAST_SLOT; s++) { 
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

    

    private volatile boolean tapPending;             

    private int tsz() {
        int s = Math.min(getWidth(), getHeight()) / 6;
        if (s < 24) s = 24;
        if (s > 44) s = 44;
        return s;
    }

    private int hbX() {
        return (getWidth() - SZ * Inventory.HOTBAR) / 2;
    }

    private int hbY() {
        return getHeight() - SZ - 2;
    }

    
    private void touchRect(int i, int[] r) {
        int W = getWidth(), H = getHeight();
        int s = tsz(), g = 2;
        int floor = H - SZ - 14;                  
        int top = floor - 3 * s - 2 * g;          
        int c0 = 4, c1 = 4 + s + g, c2 = 4 + 2 * (s + g);
        int r0 = top, r1 = top + s + g, r2 = top + 2 * (s + g);
        int x = 0, y = 0, w = s, h = s;
        switch (i) {
            case T_UP: x = c1; y = r0; break;
            case T_DOWN: x = c1; y = r2; break;
            case T_LEFT: x = c0; y = r1; break;
            case T_RIGHT: x = c2; y = r1; break;
            case T_JUMP: x = c1; y = r1; break;                          
            default: w = h = s * 3 / 4; x = (W - w) / 2; y = 4; break;   
        }
        r[0] = x; r[1] = y; r[2] = w; r[3] = h;
    }

    private int touchAt(int px, int py) {
        for (int i = 0; i < T_COUNT; i++) {
            touchRect(i, tr);
            if (px >= tr[0] && px < tr[0] + tr[2] && py >= tr[1] && py < tr[1] + tr[3]) return i;
        }
        return -1;
    }

    private void touchHold(int b, boolean d) {
        switch (b) {
            case T_UP: kFwd = d; break;
            case T_DOWN: kBack = d; break;
            case T_LEFT: kLeft = d; break;
            case T_RIGHT: kRight = d; break;
            case T_JUMP: kJump = d; break;
            default: break;
        }
    }

    private void touchPress(int b) {
        if (b == T_PAUSE) {
            midlet.showMenu();
            return;
        }
        touchBtn = b;
        touchHold(b, true);
    }

    
    private boolean touchHotbar(int x, int y) {
        int hx = hbX(), hy = hbY();
        if (y < hy - 3) return false;
        if (x >= hx && x < hx + SZ * Inventory.HOTBAR) {
            held = (x - hx) / SZ;
            invBtn = false;
            repaint();
            return true;
        }
        int bx = hx + Inventory.HOTBAR * SZ + 2;
        if (x >= bx && x < bx + SZ) {
            openInv(0);
            repaint();
            return true;
        }
        return false;
    }

    private void touchRelease() {
        if (touchBtn >= 0) touchHold(touchBtn, false);
        touchBtn = -1;
        if (touchLook) {
            kMine = false;
            
            if (!touchDragged && System.currentTimeMillis() - touchDownAt < TAP_MS) tapPending = true;
        }
        touchLook = false;
        touchDragged = false;
    }

private boolean entityAhead() {
    return zombies.target(playerX, playerY, playerZ, fwdX(), fwdY(), fwdZ()) >= 0
            || sheeps.target(playerX, playerY, playerZ, fwdX(), fwdY(), fwdZ()) >= 0
            || pigs.target(playerX, playerY, playerZ, fwdX(), fwdY(), fwdZ()) >= 0;
}

    
    private void touchUpdate() {
        if (tapPending) {
            tapPending = false;
            if (entityAhead()) clickMine = true;
            else clickPlace = true;
        }
        if (touchLook && !touchDragged && !kMine && System.currentTimeMillis() - touchDownAt >= TAP_MS) {
            kMine = true;
            if (!touchLook) kMine = false;         
        }
    }

    private void drawTouch(Graphics g, int W, int H) {
        int s = tsz();
        if (touchImg == null || touchImgSize != s) {
            int[] a = new int[s * s], b = new int[s * s];
            for (int i = 0; i < a.length; i++) {
                a[i] = 0x66202020;
                b[i] = 0x99C0C0C0;
            }
            touchImg = Image.createRGBImage(a, s, s, true);
            touchImgOn = Image.createRGBImage(b, s, s, true);
            touchImgSize = s;
        }
        for (int i = 0; i < T_COUNT; i++) {
            touchRect(i, tr);
            int x = tr[0], y = tr[1], w = tr[2], h = tr[3];
            boolean on = i == touchBtn;
            if (w == s) {
                g.drawImage(on ? touchImgOn : touchImg, x, y, Graphics.TOP | Graphics.LEFT);
            } else {
                g.setColor(on ? 0xA0A0A0 : 0x303030);
                g.fillRect(x, y, w, h);
            }
            g.setColor(0xFFFFFF);
            g.drawRect(x, y, w - 1, h - 1);
            int cx = x + w / 2, cy = y + h / 2, a = s / 5;
            if (i == T_UP) {
                g.fillTriangle(cx, cy - a, cx - a, cy + a, cx + a, cy + a);
            } else if (i == T_DOWN) {
                g.fillTriangle(cx, cy + a, cx - a, cy - a, cx + a, cy - a);
            } else if (i == T_LEFT) {
                g.fillTriangle(cx - a, cy, cx + a, cy - a, cx + a, cy + a);
            } else if (i == T_RIGHT) {
                g.fillTriangle(cx + a, cy, cx - a, cy - a, cx - a, cy + a);
            } else if (i == T_JUMP) {
                int b = a * 2 / 3;                 
                g.fillTriangle(cx, cy - a, cx - a, cy - a + b + 2, cx + a, cy - a + b + 2);
                g.fillTriangle(cx, cy, cx - a, cy + b + 2, cx + a, cy + b + 2);
            } else {
                int bw = Math.max(2, w / 6);       
                g.fillRect(cx - bw - 1, cy - a, bw, 2 * a);
                g.fillRect(cx + 1, cy - a, bw, 2 * a);
            }
        }
    }

    protected void pointerDragged(int x, int y) {
        if (invOpen || !touchOn) return;
        if (touchLook) {
            if (!touchDragged && Math.abs(x - touchDownX) + Math.abs(y - touchDownY) < 8) return;
            if (!touchDragged) {
                touchDragged = true;
                kMine = false;                     
            }
            
            yaw -= (x - touchX) * LOOK_SENS;
            pitch -= (y - touchY) * LOOK_SENS;
            if (pitch < -1.57f) pitch = -1.57f;
            if (pitch > 1.57f) pitch = 1.57f;
            touchX = x;
            touchY = y;
        } else if (touchBtn >= 0 && touchBtn <= T_JUMP) {
            int b = touchAt(x, y);                 
            if (b != touchBtn) {
                touchHold(touchBtn, false);
                if (b >= 0 && b <= T_JUMP) {
                    touchBtn = b;
                    touchHold(b, true);
                } else {
                    touchBtn = -1;
                }
            }
        }
    }

    protected void pointerReleased(int x, int y) {
        if (invOpen || !touchOn) return;
        touchRelease();
    }

protected void pointerPressed(int x, int y) {
        if (!invOpen) {
            if (!touchOn) return;
            if (touchHotbar(x, y)) return;
            int b = touchAt(x, y);
            if (b >= 0) {
                touchPress(b);
            } else {
                touchLook = true;                  
                touchDragged = false;
                touchX = touchDownX = x;
                touchY = touchDownY = y;
                touchDownAt = System.currentTimeMillis();
            }
            return;
        }

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
        for (int s = 0; s <= LAST_SLOT; s++) {
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

        boolean moving = inputX != 0f || inputZ != 0f;
        boolean wasGrounded = grounded;
        boolean stepUp = false;
        grounded = false;

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
                    if (axis != 2) {
                        
                        if (autoJump && moving && wasGrounded
                                && freeAt(nx, ny - 1f, nz) && freeAt(playerX, playerY - 1f, playerZ)) {
                            stepUp = true;
                        }
                        continue OUTER;
                    }
                    if (velY > 0f) grounded = true;
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
        if (stepUp && velY >= 0f) velY = -0.1f;
    }

    
    private boolean freeAt(float x, float y, float z) {
        for (int c = 0; c < 12; c++) {
            int bx = Calc.floor(x + (c & 1) * 0.6f - 0.3f);
            int by = Calc.floor(y + ((c >> 2) - 1) * 0.8f + 0.65f);
            int bz = Calc.floor(z + ((c >> 1) & 1) * 0.6f - 0.3f);
            if (by < 0) continue;
            if (!World.passable(World.get(bx, by, bz)) || World.get(bx, by - 1, bz) == Items.FENCE) return false;
        }
        return true;
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
        
        
        float s = e * 4f;
        if (s < 0f) s = 0f;
        if (s > 1f) s = 1f;
        dayAmt = s;
        Renderer.dimC[0] = 1f + (0.28f + 0.72f * b - 1f) * s;
        Renderer.dimC[1] = 1f + ((0.28f + 0.72f * b) * (1f - 0.15f * tw) - 1f) * s;
        Renderer.dimC[2] = 1f + ((0.45f + 0.55f * b) * (1f - 0.35f * tw) - 1f) * s;
    }

    
    private void processClicks() {
        if (touchOn) touchUpdate();

        boolean hitEntity = false;
        if (clickMine) {
            int heldItem = inv.id(held);
            if (Items.isSword(heldItem)) {
                slashFrames = 6;
            }
            int tiZ = zombies.target(playerX, playerY, playerZ, fwdX(), fwdY(), fwdZ());
            int tiS = sheeps.target(playerX, playerY, playerZ, fwdX(), fwdY(), fwdZ());
            int tiP = pigs.target(playerX, playerY, playerZ, fwdX(), fwdY(), fwdZ());
            if (tiZ >= 0) {
                zombies.hit(tiZ, weaponDamage(), fwdX(), fwdZ());
                inv.damage(held);
                hitEntity = true;
            } else if (tiS >= 0) {
                sheeps.hit(tiS, weaponDamage(), inv);
                inv.damage(held);
                hitEntity = true;
            } else if (tiP >= 0) {
    pigs.hit(tiP, weaponDamage(), fwdX(), fwdZ());
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
} else if (hb == Items.CHEST) {
    openBox(2, Renderer.hovX, Renderer.hovY, Renderer.hovZ);
} else if (hb == Items.FURNACE) {
    openBox(3, Renderer.hovX, Renderer.hovY, Renderer.hovZ);
} else if (hb == Items.BED || hb == Items.BED_HEAD) {
    if (dayTime >= 12000 && dayTime < 23800) {
        dayTime = 0;
        wakeStart = System.currentTimeMillis();
        World.setSpawn(Renderer.hovX + 0.5f, Renderer.hovY - 2f, Renderer.hovZ + 0.5f);
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
        World.setDoorRot(d[0], d[1], d[2], doorRot());
        inv.take(held);
    }
} else if (item == Items.STAIR) {
    int[] d = bedDir();
    int facing = d[0] > 0 ? 1 : (d[0] < 0 ? 3 : (d[1] > 0 ? 2 : 0));
    if (inv.count(held) > 0 && World.get(tx, ty, tz) == World.AIR && !overlapsPlayer(tx, ty, tz)) {
        World.set(tx, ty, tz, Items.STAIR_N + facing);
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
    
private void openBox(int mode, int x, int y, int z) {
    box = World.box(x, y, z, mode == 3 ? 2 : 1);
    inv.loadBox(box, mode == 3 ? 3 : 27);
    openInv(mode);
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
            || b == Items.COAL_ORE || b == Items.IRON_ORE || b == Items.IRON_BLOCK
            || b == Items.FURNACE || b == Items.SLAB || (b >= Items.STAIR_N && b <= Items.STAIR_W);
}

    private void breakBlock(int bx, int by, int bz, int b) {
        int item = inv.id(held);
        int need = (b == Items.IRON_ORE || b == Items.IRON_BLOCK) ? 2 : 1;
        boolean canDrop = !isRock(b) || Items.pickLevel(item) >= need;
        boolean removed = !canDrop || inv.add(Items.dropOf(b), 1) == 0;
        if (!removed) return;
        World.dumpBox(bx, by, bz, inv);
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
    
    if (open && (overlapsPlayer(x, by, z) || overlapsPlayer(x, by + 1, z))) return;
    World.set(x, by, z, open ? Items.DOOR : Items.DOOR_OPEN);
    World.set(x, by + 1, z, open ? Items.DOOR_TOP : Items.DOOR_TOP_OPEN);
}

private int[] findDoorSpot(int x, int y, int z) {
    
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
                long dms = t0 - lastTime;
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

if ((++memTick & 127) == 0 && Runtime.getRuntime().freeMemory() < 300000L) {
    System.gc();
    if (Runtime.getRuntime().freeMemory() < 300000L) {
        try {
            saveInvToRms();
        } catch (Throwable e) {
            World.fileStatus = "RMS save failed";
        }

        
        World.purgeChunkMemory();
        System.gc();

        
        try {
            javax.microedition.rms.RecordStore rs =
                javax.microedition.rms.RecordStore.openRecordStore("mc4k_inv", false);
            if (rs.getNumRecords() > 0) {
                byte[] data = rs.getRecord(1);
                if (data != null && data.length > 0) {
                    inv.load(new String(data));
                }
            }
            rs.closeRecordStore();
        } catch (Throwable e) {}
    }
}
                clearPlayerBody();
                updateLighting();
                updateZombies();
                updateTnt();
                World.tickBoxes(dms, invOpen ? box : null);
                World.tickGrass();
                if (invOpen && invMode == 3) inv.tickFurnace(Inventory.CONT, dms);
                sheeps.update(playerX, playerZ, dayTime);
                pigs.update(playerX, playerZ);

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
    

private void writeInv(byte[] data) throws Exception {
    javax.microedition.rms.RecordStore rs =
        javax.microedition.rms.RecordStore.openRecordStore("mc4k_inv", true);
    try {
        if (rs.getNumRecords() == 0) rs.addRecord(data, 0, data.length);
        else rs.setRecord(1, data, 0, data.length);
    } finally {
        rs.closeRecordStore();
    }
}


private void saveInvToRms() throws Exception {
    byte[] data = inv.serialize().getBytes();
    try {
        writeInv(data);
    } catch (Throwable e) {
        try {
            javax.microedition.rms.RecordStore.deleteRecordStore("mc4k_inv");
        } catch (Throwable t) {}
        writeInv(data);
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
    if (dayAmt > 0.75f) return;
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
    g.drawString("X " + Calc.floor(playerX) + " Y " + Calc.floor(playerY) + " Z " + Calc.floor(playerZ),
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
        if (touchOn) drawTouch(g, W, H);
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
    g.drawString(invMode == 1 ? "Crafting Table" : invMode == 2 ? "Chest" : invMode == 3 ? "Furnace" : "Inventory",
        px + 4, py + 3, Graphics.TOP | Graphics.LEFT);

    g.setColor(0x000000);
    if (invMode == 1) {
        g.drawString("->", px + 4 + 5 * SZ + SZ / 2, py + GY + SZ + SZ / 2 - 6, Graphics.TOP | Graphics.HCENTER);
    } else if (invMode == 0) {
        g.drawString("->", px + 4 + 4 * SZ + SZ / 2, py + GY + SZ / 2 - 6, Graphics.TOP | Graphics.HCENTER);
    }
    
for (int s = 0; s <= LAST_SLOT; s++) {
    if (!shown(s)) continue;
    if (s == CLOSE_BTN) {
        drawCloseButton(g, px + slotX(s), py + slotY(s), s == invSel);
        continue;
    }
    drawSlot(g, px + slotX(s), py + slotY(s), s, s == invSel);
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

final class MenuScreen extends Canvas implements CommandListener {
    private static final int GAP = 4;

    private final Minecraft4kMidlet midlet;
    private final Command selectCmd = new Command("Select", Command.OK, 1);
    private final boolean canContinue;

    
    
    private int mode;
    private int sel;
    private int picked;                                   
    private final boolean[] worldHas = new boolean[World.MAX_WORLDS];
    
    private int scroll;

    
    private int pressIdx = -1;
    private int pressArrow = -1;                          
    private int pressY;
    private int lastY;
    private boolean dragging;
private static final String[] SPLASHES = {
    "Dig deep!",
    "Now with blocks!",
    "Embe-embe... sheep!",
    "Also try TNT!",
    "Wood is good!",
    "Sleep through it!",
    "Zombie Outbreak!",
    "Write in Signs!",
    "Hello, world!",
    "TakuruKagami",
    "Get the door!",
    "Also try Minecraft",
    "Also try Terraria",
    "Jamais Vu",
    "Jamais Vu",
    "Infinity!",
    "Technoblade Never Dies!",
    "Current now 541724!",
};
private final String splash;
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
        Game.autoTouch(hasPointerEvents());
        canContinue = World.hasSave(World.lastWorld());
        splash = SPLASHES[new Random().nextInt(SPLASHES.length)];
        addCommand(selectCmd);
        setCommandListener(this);
    }

    private void refreshWorlds() {
        for (int i = 0; i < worldHas.length; i++) worldHas[i] = World.hasSave(i);
    }

    private String heading() {
        if (mode == 1) return "Options";
        if (mode == 2) return "Select World";
        if (mode == 3) return "World " + (picked + 1);
        if (mode == 4) return "Delete World " + (picked + 1) + "?";
        return null;
    }

    private String[] items() {
        if (mode == 2) {
            String[] w = new String[World.MAX_WORLDS + 1];
            for (int i = 0; i < World.MAX_WORLDS; i++) {
                w[i] = "World " + (i + 1) + (worldHas[i] ? "" : " (new)");
            }
            w[World.MAX_WORLDS] = "Back";
            return w;
        }
        if (mode == 3) return new String[] { "Play", "Delete world", "Back" };
        if (mode == 4) return new String[] { "Yes, delete it", "Cancel" };
        if (mode == 1) {
            return new String[] {
                "Scenery: " + (World.sceneryOn ? "ON" : "OFF"),
                "Generation: " + (World.instantGen ? "Full" : "Light"),
                "Debug Info: " + (Game.showInfo ? "ON" : "OFF"),
                "FOV: " + M3GRenderer.fov,
                "Tex radius: " + (M3GRenderer.texDist >= 9999 ? "All" : String.valueOf(M3GRenderer.texDist)),
                "FPS limit: " + (Game.targetFps == 0 ? "Max" : String.valueOf(Game.targetFps)),
                "Touch buttons: " + (Game.touchOn ? "ON" : "OFF"),
                "Auto-jump: " + (Game.autoJump ? "ON" : "OFF"),
                "Done"
            };
        }
        return new String[] { canContinue ? "Continue" : "Play", "Worlds", "Settings", "Quit Game" };
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

    private int shownEnd() {
        return Math.min(items().length, scroll + visible());
    }

    
    private boolean upShown() {
        return scroll > 0 || (hasPointerEvents() && sel > 0);
    }

    private boolean downShown() {
        int n = items().length;
        return shownEnd() < n || (hasPointerEvents() && sel < n - 1);
    }

    private int upY() {
        return top() - hintFont.getHeight() - 2;
    }

    private int downY() {
        return btnY(shownEnd() - 1) + rowH() - 2;
    }

    
    private int arrowAt(int x, int y) {
        int half = Math.max(28, bw() / 6);
        if (x < getWidth() / 2 - half || x >= getWidth() / 2 + half) return -1;
        int hh = hintFont.getHeight();
        if (upShown() && y >= upY() - 6 && y < top()) return 0;
        int rowBottom = btnY(shownEnd() - 1) + bh();
        if (downShown() && y >= rowBottom && y < downY() + hh + 6) return 1;
        return -1;
    }

    
    private void step(int d) {
        int n = items().length;
        int ns = sel + d;
        if (ns < 0 || ns >= n) return;
        sel = ns;
        keepSelVisible();
        repaint();
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
        int sx = lx + lw - 6;
int sw = hintFont.stringWidth(splash);
if (sx + sw > W - 2) sx = W - 2 - sw;
g.drawString(splash, sx, logoY + titleFont.getHeight() - 4, Graphics.TOP | Graphics.LEFT);

        if (mode != 0) {
            drawShadowed(g, heading(), W / 2, logoY + titleFont.getHeight() + 8, 0xFFFFFF, 0x3F3F3F, 1,
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
        if (upShown()) {
            drawShadowed(g, "^", W / 2, upY(), pressArrow == 0 ? 0xFFFFA0 : 0xFFFFFF, 0x3F3F3F, 1,
                    Graphics.TOP | Graphics.HCENTER);
        }
        if (downShown()) {
            drawShadowed(g, "v", W / 2, downY(), pressArrow == 1 ? 0xFFFFA0 : 0xFFFFFF, 0x3F3F3F, 1,
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
        if (mode != 0 && k == '0') {
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
        pressArrow = arrowAt(x, y);
        pressY = y;
        lastY = y;
        dragging = false;
        if (pressArrow >= 0) repaint();           
    }

    protected void pointerDragged(int x, int y) {
        if (pressArrow >= 0) {
            if (arrowAt(x, y) != pressArrow) {    
                pressArrow = -1;
                repaint();
            }
            return;
        }
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
        
        int v = visible();
        if (sel < scroll) sel = scroll;
        if (sel >= scroll + v) sel = scroll + v - 1;
        repaint();
    }

    protected void pointerReleased(int x, int y) {
        if (pressArrow >= 0) {
            int a = pressArrow;
            pressArrow = -1;
            if (arrowAt(x, y) == a) step(a == 0 ? -1 : 1);
            repaint();
            return;
        }
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
        if (mode == 4) {
            mode = 3;
            sel = 1;
        } else if (mode == 3) {
            mode = 2;
            sel = picked;
        } else if (mode == 2) {
            mode = 0;
            sel = 1;
        } else {
            mode = 0;
            sel = 2;
        }
        scroll = 0;
        keepSelVisible();
        repaint();
    }

    private void go(int newMode, int newSel) {
        mode = newMode;
        sel = newSel;
        scroll = 0;
        repaint();
    }

    private void activate() {
    if (mode == 2) {
        if (sel >= World.MAX_WORLDS) {
            back();
        } else if (worldHas[sel]) {
            picked = sel;
            go(3, 0);
        } else {
            midlet.startGame(sel, true);      
        }
        return;
    }
    if (mode == 3) {
        if (sel == 0) midlet.startGame(picked, false);
        else if (sel == 1) go(4, 1);          
        else back();
        return;
    }
    if (mode == 4) {
        if (sel == 0) {
            World.deleteWorld(picked);
            refreshWorlds();
            go(2, picked);
        } else {
            back();
        }
        return;
    }
    if (mode == 1) {
        if (sel == 0) {
            World.setScenery(!World.sceneryOn);
        } else if (sel == 1) {
            World.setInstantGen(!World.instantGen);
        } else if (sel == 2) {
            Game.showInfo = !Game.showInfo;
            World.saveOptions();
        } else if (sel == 3) {
            M3GRenderer.fov = next(FOVS, M3GRenderer.fov);
            World.saveOptions();
        } else if (sel == 4) {
            M3GRenderer.texDist = next(TEXD, M3GRenderer.texDist);
            World.saveOptions();
        } else if (sel == 5) {
            Game.targetFps = next(FPSV, Game.targetFps);
            World.saveOptions();
        } else if (sel == 6) {
            Game.touchOn = !Game.touchOn;
            Game.touchSet = true;
            World.saveOptions();
        } else if (sel == 7) {
            Game.autoJump = !Game.autoJump;
            World.saveOptions();
        } else {
            back();
            return;
        }
        repaint();
        return;
    }
        switch (sel) {
            case 0: {
                int w = World.lastWorld();
                midlet.startGame(w, !canContinue);   
                break;
            }
            case 1:
                refreshWorlds();
                go(2, World.lastWorld());
                break;
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
