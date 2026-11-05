import javax.microedition.lcdui.Image;


final class Textures {
    static final int TEX = 16;
    
    static final int BLOCK = TEX * TEX * 3;

    
    static int[] atlas;

    
    private static final String SHEET = "/tex/blocks.png";

    private Textures() {}

static void build() {
    atlas = new int[(Items.MAX_ID + 1) * BLOCK];
    for (int i = World.FLAT * BLOCK; i < (World.FLAT + 1) * BLOCK; i++) {
        atlas[i] = 0xFF4CAF2E;
    }
    loadSheet();
    buildTorch();
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

private static void loadSheet() {
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
        img = Image.createImage(SHEET);
    } catch (java.io.IOException e) {
        throw new IllegalStateException("missing texture sheet " + SHEET + ": " + e);
    }
    int w = img.getWidth(), h = img.getHeight();
    if (w < (headCol + 1) * n || h < 3 * n) {
        throw new IllegalStateException("texture sheet " + SHEET + " is " + w + "x" + h
                + ", needs at least " + ((headCol + 1) * n) + "x" + (3 * n));
    }
    int[] sheet = new int[w * h];
    img.getRGB(sheet, 0, w, 0, 0, w, h);
    System.out.println("[tex] loaded " + SHEET + " " + w + "x" + h);

    for (int k = 0; k < ids.length; k++) {
        for (int band = 0; band < 3; band++) {
            int sx = k * n, sy = band * n;
            int dst = ids[k] * BLOCK + band * n * n;
            for (int y = 0; y < n; y++) {
                System.arraycopy(sheet, (sy + y) * w + sx, atlas, dst + y * n, n);
            }
        }
    }

    
    for (int band = 0; band < 3; band++) {
        for (int y = 0; y < n; y++) {
            System.arraycopy(sheet, (band * n + y) * w + headCol * n,
                    atlas, Items.BED_HEAD * BLOCK + band * n * n + y * n, n);
        }
    }
    }

static final String[] NAMES = { "", "grass", "dirt", "", "stone", "bricks", "", "wood", "leaves", "flat",
    "planks", "craft", "cobble", "coal ore", "iron ore", "stick", "coal", "wood sword", "stone sword",
    "wool", "bed", "wood pickaxe", "stone pickaxe", "wood axe", "stone axe", "torch", "bed head" };
    
    
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
            default:
                return 0xFFFF00FF;
        }
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