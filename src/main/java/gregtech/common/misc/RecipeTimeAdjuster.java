package gregtech.common.misc;

import net.minecraft.server.MinecraftServer;

import gregtech.api.GregTech_API;
import gregtech.api.enums.ConfigCategories;

public class RecipeTimeAdjuster {

    // tps over 2 minutes
    static final int DURATION = 2400;
    public static long[] tickTimeArray = new long[DURATION];

    // only used on loading
    static int loadedTPS = 20;

    public static boolean ENABLE = false;

    public static void init() {
        ENABLE = GregTech_API.sSpecialFile.get(ConfigCategories.general, "AdjustRecipeByTPS", false);
    }

    public static void updateTickTimeArray() {
        if (!ENABLE) return;
        int tickCounter = MinecraftServer.getServer()
            .getTickCounter();
        long tickTime = MinecraftServer.getServer().tickTimeArray[tickCounter % 100];
        tickTimeArray[tickCounter % DURATION] = tickTime;
    }

    public static double getParallelismMultiplierByMSPT() {
        if (ENABLE) {
            // return 2.0;
            return 20.0 / loadedTPS;
        } else {
            return 1;
        }
    }

    public static void updateMSPT() {
        if (!ENABLE) return;
        int tickCounter = MinecraftServer.getServer()
            .getTickCounter();
        if (tickCounter % 600 != 0 || tickCounter == 0) {
            return;
        }

        double s = 0;
        for (long l : tickTimeArray) {
            s += l;
        }
        loadedTPS = Math.min((int) Math.ceil(1000 / (s / DURATION / 1000000)), 20);
        // loadedTPS = 10;
    }
}
