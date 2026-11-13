package pl.lobbycore;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.Leaves;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Buduje lobby (wyspa z placem, fontanną z beaconem, dwoma pawilonami z portalami)
 * oraz arenę FFA. Wszystko proceduralnie, deterministycznie (stały seed).
 */
final class Builder {

    /** Punkty ważne dla pluginu po zbudowaniu lobby. */
    record LobbyLayout(Location spawn, Location npcAnarchia, Location npcFfa,
                       Location holoAnarchia, Location holoFfa, Location holoWelcome) {}

    record ArenaLayout(List<Location> spawns, Location holo) {}

    private static final int Y = 100;
    private final World w;
    private final Random rnd;

    private Builder(World w, long seed) {
        this.w = w;
        this.rnd = new Random(seed);
    }

    // ───────────────────────── prymitywy ─────────────────────────

    private void set(int x, int y, int z, Material m) { w.getBlockAt(x, Y + y, z).setType(m, false); }
    private void set(int x, int y, int z, BlockData d) { w.getBlockAt(x, Y + y, z).setBlockData(d, false); }
    private Material get(int x, int y, int z) { return w.getBlockAt(x, Y + y, z).getType(); }
    private static double d(double x, double z) { return Math.sqrt(x * x + z * z); }

    @SafeVarargs
    private <T> T pick(T... options) { return options[rnd.nextInt(options.length)]; }

    private Location at(double x, double y, double z, float yaw) { return new Location(w, x, Y + y, z, yaw, 0); }

    /** Czyści przestrzeń nad i pod wyspą (przy przebudowie). */
    private void clear(int r, int below, int above) {
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++)
            for (int y = -below; y <= above; y++) set(x, y, z, Material.AIR);
    }

    /** Pływająca wyspa: trawa na górze, ziemia i kamień zwężające się w dół, korzenie pod spodem. */
    private void island(int R, int maxDepth) {
        for (int x = -R - 1; x <= R + 1; x++) for (int z = -R - 1; z <= R + 1; z++) {
            double d = d(x, z);
            if (d > R + 0.5) continue;
            set(x, 0, z, Material.GRASS_BLOCK);
            double t = 1 - d / (R + 0.5);
            int depth = 2 + (int) (Math.pow(t, 0.55) * maxDepth) + rnd.nextInt(2);
            for (int dy = 1; dy <= depth; dy++) {
                Material m;
                if (dy <= 2) m = Material.DIRT;
                else {
                    double r = rnd.nextDouble();
                    m = r < 0.55 ? Material.STONE : r < 0.75 ? Material.ANDESITE : r < 0.88 ? Material.TUFF
                            : r < 0.95 ? Material.COBBLESTONE : Material.MOSSY_COBBLESTONE;
                }
                set(x, -dy, z, m);
            }
            if (d > R - 4 && rnd.nextDouble() < 0.35) set(x, -depth - 1, z, Material.HANGING_ROOTS);
        }
    }

    private void lamp(int x, int z) {
        set(x, 1, z, Material.STONE_BRICK_WALL);
        set(x, 2, z, Material.DARK_OAK_FENCE);
        set(x, 3, z, Material.DARK_OAK_FENCE);
        set(x, 4, z, Material.LANTERN);
    }

    private void hedge(int R) {
        for (int x = -R - 1; x <= R + 1; x++) for (int z = -R - 1; z <= R + 1; z++) {
            double d = d(x, z);
            if (d < R - 0.6 || d > R + 0.5) continue;
            if (get(x, 0, z) != Material.GRASS_BLOCK) continue;
            Material m = rnd.nextDouble() < 0.3 ? Material.FLOWERING_AZALEA_LEAVES : Material.AZALEA_LEAVES;
            Leaves leaves = (Leaves) m.createBlockData();
            leaves.setPersistent(true);
            set(x, 1, z, leaves);
        }
    }

    private void plants(int R, double chance, Material... pool) {
        for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++) {
            if (get(x, 0, z) != Material.GRASS_BLOCK || get(x, 1, z) != Material.AIR) continue;
            if (rnd.nextDouble() < chance) set(x, 1, z, pick(pool));
        }
    }

    private void tree(int x, int z, TreeType type) {
        if (get(x, 0, z) != Material.GRASS_BLOCK) return;
        set(x, 1, z, Material.AIR);
        w.generateTree(new Location(w, x, Y + 1, z), rnd, type);
    }

    // ───────────────────────── LOBBY ─────────────────────────

    static LobbyLayout lobby(World w) {
        return new Builder(w, 1337L).buildLobby();
    }

    private LobbyLayout buildLobby() {
        final int R = 26;
        clear(R + 6, 1, 34);
        island(R, 18);

        // Ścieżki do pawilonów (zachód = Anarchia, wschód = FFA)
        for (int s : new int[]{-1, 1}) for (int ax = 4; ax <= 14; ax++) for (int z = -10; z <= -6; z++) {
            int x = s * ax;
            if (d(x, z) < 9.4) continue;
            Material m = (z == -10 || z == -6) ? Material.STONE_BRICKS
                    : z == -8 ? Material.SMOOTH_STONE : Material.POLISHED_ANDESITE;
            set(x, 0, z, m);
        }

        // Plac: róża wiatrów z kwarcową obwódką
        for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++) {
            double d = d(x, z);
            if (d >= 9.4) continue;
            Material m;
            if (d >= 8.2) m = Material.SMOOTH_QUARTZ;
            else if (d >= 3.4 && d < 4.3) m = Material.POLISHED_DIORITE;
            else if (x == 0 || z == 0 || Math.abs(x) == Math.abs(z)) m = Material.SMOOTH_STONE;
            else m = Material.POLISHED_ANDESITE;
            set(x, 0, z, m);
        }

        // Fontanna z beaconem (złoty promień w niebo)
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            double d = d(x, z);
            if (d > 3.4) continue;
            if (d > 2.4) set(x, 1, z, Material.QUARTZ_BRICKS);
            else { set(x, 0, z, Material.PRISMARINE_BRICKS); set(x, 1, z, Material.WATER); }
        }
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) set(x, -1, z, Material.IRON_BLOCK);
        set(0, 0, 0, Material.BEACON);
        set(0, 1, 0, Material.YELLOW_STAINED_GLASS);
        for (int[] p : new int[][]{{3, 0}, {-3, 0}, {0, 3}, {0, -3}}) set(p[0], 2, p[1], Material.LANTERN);

        // Latarnie
        lamp(5, 5); lamp(-5, 5);
        for (int s : new int[]{-1, 1}) { lamp(7 * s, -11); lamp(11 * s, -11); lamp(7 * s, -5); lamp(11 * s, -5); }

        // Wyrzutnie na początku ścieżek
        for (int s : new int[]{-1, 1}) {
            set(5 * s, 0, -8, Material.GOLD_BLOCK);
            set(5 * s, 1, -8, Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
        }

        pavilion(-1);
        pavilion(1);

        hedge(R);
        plants(R, 0.2, Material.SHORT_GRASS, Material.SHORT_GRASS, Material.SHORT_GRASS, Material.SHORT_GRASS,
                Material.POPPY, Material.DANDELION, Material.CORNFLOWER, Material.ALLIUM, Material.OXEYE_DAISY,
                Material.AZURE_BLUET, Material.PINK_PETALS);
        int[][] trees = {{-14, 15}, {14, 15}, {-21, 4}, {21, 4}, {-9, -19}, {9, -19}, {-19, -17}, {19, -17}};
        for (int[] t : trees) tree(t[0], t[1], TreeType.CHERRY);

        return new LobbyLayout(
                at(0.5, 1, 7.5, 180),
                at(-16 + 0.5, 1, -8 + 3.5, 0),
                at(16 + 0.5, 1, -8 + 3.5, 0),
                at(-18 + 0.5, 5.6, -8 + 0.5, 0),
                at(18 + 0.5, 5.6, -8 + 0.5, 0),
                at(0.5, 4.2, -4.5, 0));
    }

    /** s = -1 Anarchia (zachód, czerwono-czarny), s = 1 FFA (wschód, morski). */
    private void pavilion(int s) {
        boolean anarchy = s < 0;
        int px = 16 * s, pz = -8;

        for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) {
            double d = d(dx, dz);
            if (d > 5.4) continue;
            Material m;
            if (anarchy) {
                if (d >= 4.4) m = Material.RED_NETHER_BRICKS;
                else {
                    double r = rnd.nextDouble();
                    m = r < 0.2 ? Material.CRACKED_POLISHED_BLACKSTONE_BRICKS
                            : r < 0.26 ? Material.GILDED_BLACKSTONE : Material.POLISHED_BLACKSTONE_BRICKS;
                }
            } else {
                if (d >= 4.4) m = Material.PRISMARINE_BRICKS;
                else m = (dx + dz) % 2 == 0 ? Material.DARK_PRISMARINE : Material.PRISMARINE_BRICKS;
                if (d < 1) m = Material.SEA_LANTERN;
            }
            set(px + dx, 0, pz + dz, m);
        }

        // Kolumny
        Material pillar = anarchy ? Material.POLISHED_BLACKSTONE : Material.PRISMARINE_BRICKS;
        Material light = anarchy ? Material.SHROOMLIGHT : Material.SEA_LANTERN;
        Material cap = anarchy ? Material.POLISHED_BLACKSTONE_WALL : Material.PRISMARINE_WALL;
        for (int dx : new int[]{-3, 3}) for (int dz : new int[]{-4, 4}) {
            for (int y = 1; y <= 4; y++) set(px + dx, y, pz + dz, pillar);
            set(px + dx, 5, pz + dz, light);
            set(px + dx, 6, pz + dz, cap);
        }
        // Belka za portalem
        for (int dz = -3; dz <= 3; dz++) set(px + 3 * s, 5, pz + dz, anarchy ? Material.POLISHED_BLACKSTONE_BRICKS : Material.DARK_PRISMARINE);

        // Portal (prostopadły do ścieżki)
        int fx = px + 2 * s;
        BlockData inner;
        if (anarchy) {
            Orientable portal = (Orientable) Material.NETHER_PORTAL.createBlockData();
            portal.setAxis(Axis.Z);
            inner = portal;
        } else inner = Material.END_GATEWAY.createBlockData();
        for (int dz = -2; dz <= 2; dz++) for (int y = 0; y <= 4; y++) {
            boolean frame = Math.abs(dz) == 2 || y == 0 || y == 4;
            boolean corner = Math.abs(dz) == 2 && (y == 0 || y == 4);
            if (frame) {
                Material m = anarchy ? (corner ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN)
                        : (corner ? Material.SEA_LANTERN : Material.DARK_PRISMARINE);
                set(fx, y, pz + dz, m);
            } else set(fx, y, pz + dz, inner);
        }

        // Ognisko z boku
        set(px, 1, pz - 3, anarchy ? Material.CAMPFIRE : Material.SOUL_CAMPFIRE);
    }

    // ───────────────────────── ARENA FFA ─────────────────────────

    static ArenaLayout arena(World w) {
        return new Builder(w, 7331L).buildArena();
    }

    private ArenaLayout buildArena() {
        final int R = 30;
        clear(R + 4, 1, 24);
        island(R, 14);

        // Mieszane podłoże
        for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++) {
            if (d(x, z) > R + 0.5) continue;
            double r = rnd.nextDouble();
            if (r < 0.12) set(x, 0, z, Material.COARSE_DIRT);
            else if (r < 0.2) set(x, 0, z, Material.MOSS_BLOCK);
            else if (r < 0.25) set(x, 0, z, Material.PODZOL);
        }

        // Mur + niewidzialna bariera
        for (int x = -R - 1; x <= R + 1; x++) for (int z = -R - 1; z <= R + 1; z++) {
            double d = d(x, z);
            if (d <= R - 1.2 || d > R + 0.5) continue;
            for (int y = 1; y <= 3; y++) {
                double r = rnd.nextDouble();
                set(x, y, z, r < 0.6 ? Material.STONE_BRICKS : r < 0.85 ? Material.MOSSY_STONE_BRICKS : Material.CRACKED_STONE_BRICKS);
            }
            set(x, 4, z, Material.POLISHED_ANDESITE);
            int from = 5;
            if (rnd.nextDouble() < 0.1) { set(x, 5, z, Material.LANTERN); from = 6; }
            for (int y = from; y <= 20; y++) set(x, y, z, Material.BARRIER);
        }

        // Wzgórze na środku
        for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
            double d = d(x, z);
            if (d <= 4.4) set(x, 1, z, Material.STONE_BRICKS);
            if (d <= 2.4) set(x, 2, z, Material.POLISHED_ANDESITE);
        }
        set(0, 3, 0, Material.CHISELED_STONE_BRICKS);

        // Spawny na okręgu
        List<int[]> spawnCells = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            spawnCells.add(new int[]{(int) Math.round(Math.cos(a) * 22), (int) Math.round(Math.sin(a) * 22)});
        }

        // Głazy
        for (int i = 0; i < 16; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, rad = 7 + rnd.nextDouble() * 18;
            int bx = (int) Math.round(Math.cos(a) * rad), bz = (int) Math.round(Math.sin(a) * rad);
            if (near(spawnCells, bx, bz, 4)) continue;
            double br = 1.3 + rnd.nextDouble() * 1.2;
            for (int dx = -3; dx <= 3; dx++) for (int dy = 0; dy <= 3; dy++) for (int dz = -3; dz <= 3; dz++) {
                if (Math.sqrt(dx * dx + dy * dy * 1.6 + dz * dz) > br) continue;
                set(bx + dx, 1 + dy, bz + dz, pick(Material.COBBLESTONE, Material.MOSSY_COBBLESTONE, Material.ANDESITE, Material.STONE));
            }
        }
        // Ruiny kolumn
        for (int i = 0; i < 8; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, rad = 9 + rnd.nextDouble() * 16;
            int bx = (int) Math.round(Math.cos(a) * rad), bz = (int) Math.round(Math.sin(a) * rad);
            if (near(spawnCells, bx, bz, 4)) continue;
            int h = 2 + rnd.nextInt(4);
            for (int y = 1; y <= h; y++) set(bx, y, bz, rnd.nextDouble() < 0.3 ? Material.CRACKED_STONE_BRICKS : Material.STONE_BRICKS);
            if (rnd.nextBoolean()) set(bx, h + 1, bz, Material.STONE_BRICK_WALL);
        }

        plants(R - 2, 0.16, Material.SHORT_GRASS, Material.SHORT_GRASS, Material.SHORT_GRASS, Material.FERN, Material.DANDELION, Material.POPPY);
        int[][] trees = {{-12, 6}, {12, -6}, {5, 15}, {-6, -15}, {17, 9}, {-16, -8}};
        for (int[] t : trees) if (!near(spawnCells, t[0], t[1], 4)) tree(t[0], t[1], rnd.nextBoolean() ? TreeType.TREE : TreeType.BIRCH);

        List<Location> spawns = new ArrayList<>();
        for (int[] c : spawnCells) {
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                set(c[0] + dx, 0, c[1] + dz, Material.GRASS_BLOCK);
                for (int y = 1; y <= 4; y++) set(c[0] + dx, y, c[1] + dz, Material.AIR);
            }
            float yaw = (float) Math.toDegrees(Math.atan2(c[0], -c[1]));
            spawns.add(at(c[0] + 0.5, 1, c[1] + 0.5, yaw));
        }
        return new ArenaLayout(spawns, at(0.5, 6, 0.5, 0));
    }

    private static boolean near(List<int[]> cells, int x, int z, double dist) {
        for (int[] c : cells) if (d(c[0] - x, c[1] - z) < dist) return true;
        return false;
    }
}
