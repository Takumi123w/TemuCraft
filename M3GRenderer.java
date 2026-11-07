import java.util.Hashtable;
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


final class M3GRenderer {
    static final int CH = 16;

    static int fov = 60;
    static final float NEAR = 0.2f;
static final float FAR = 32f;
static final float FOG_NEAR = 18f;
    private final float[] lc = new float[3];
private final VertexBuffer sVb, sVbHurt;
private final TriangleStripArray sTsa;
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

                float[] q = new float[3];
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

float lim = FAR + SECTION_R;
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
    if (dy > 0.02f) {
        tintSky(Renderer.sunC, sunCa);     
        placeSky(sunVb, sunApp, ex, ey, ez, dx, dy, dz);
    }
    if (dy < -0.02f) {
        tintSky(WHITE, moonCa);
        placeSky(moonVb, moonApp, ex, ey, ez, -dx, -dy, -dz);
    }
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