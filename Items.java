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
    static final int MAX_ID = IRON_AXE;
    static final int STACK = 64;

    static final String[] NAMES = { "", "Grass", "Dirt", "", "Stone", "Bricks", "", "Wood", "Leaves", "Scenery",
        "Planks", "Crafting Table", "Cobblestone", "Coal Ore", "Iron Ore", "Stick", "Coal", "Wooden Sword", "Stone Sword",
        "Wool", "Bed", "Wooden Pickaxe", "Stone Pickaxe", "Wooden Axe", "Stone Axe", "Torch", "Bed",
        "TNT", "Iron Block", "Apple", "Iron Ingot", "Iron Sword", "Iron Pickaxe", "Iron Axe" };

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
        // Iron: no furnace yet, so ore + coal is smelted in the crafting grid.
        new Recipe(1, 2, new int[] { IRON_ORE, COAL }, IRON_INGOT, 2),
        new Recipe(1, 1, new int[] { IRON_BLOCK }, IRON_INGOT, 4),
        new Recipe(2, 2, new int[] { IRON_INGOT, IRON_INGOT, IRON_INGOT, IRON_INGOT }, IRON_BLOCK, 1),
        new Recipe(2, 2, new int[] { COAL, COAL, COAL, COAL }, TNT, 1),
        new Recipe(1, 3, new int[] { IRON_INGOT, IRON_INGOT, STICK }, IRON_SWORD, 1),
        new Recipe(3, 3, new int[] { IRON_INGOT, IRON_INGOT, IRON_INGOT, 0, STICK, 0, 0, STICK, 0 }, IRON_PICKAXE, 1),
        new Recipe(2, 3, new int[] { IRON_INGOT, IRON_INGOT, IRON_INGOT, STICK, 0, STICK }, IRON_AXE, 1),
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

    /** Only real blocks can be placed. Sticks, tools, coal, food and ingots are items. */
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
                return true;
            default:
                return false;
        }
    }

    static boolean isSword(int id) {
        return id == WOOD_SWORD || id == STONE_SWORD || id == IRON_SWORD;
    }

    /** 0 = none, 1 = wood, 2 = stone, 3 = iron. */
    static int pickLevel(int id) {
        switch (id) {
            case WOOD_PICKAXE: return 1;
            case STONE_PICKAXE: return 2;
            case IRON_PICKAXE: return 3;
            default: return 0;
        }
    }

    /** 0 = none, 1 = wood, 2 = stone, 3 = iron. */
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

    static String name(int id) {
        return id > 0 && id <= MAX_ID ? NAMES[id] : "";
    }

    static int dropOf(int block) {
        if (block == World.STONE) return COBBLE;
        if (block == COAL_ORE) return COAL;
        if (block == BED_HEAD) return BED;
        return block;
    }
}
