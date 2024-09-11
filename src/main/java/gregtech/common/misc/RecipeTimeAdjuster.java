package gregtech.common.misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.server.MinecraftServer;

import org.apache.commons.io.IOUtils;

import gregtech.api.GregTech_API;
import gregtech.api.enums.ConfigCategories;
import gregtech.api.util.GT_Config;

public class RecipeTimeAdjuster {

    // tps over 15 minutes
    // so tps can change smoothly to avoid power loss

    static File tpsFile = new File(
        GT_Config.sConfigFileIDs.getConfigFile()
            .getParent() + File.separator + "tps_rec.txt");
    static final int DURATION = 18000;
    private static double[] tickTimeArray = new double[DURATION];

    private static double ticktimeSum = 0;

    // only used on loading
    static double multiplier = 20;

    public static boolean ENABLE = false;

    public static void init() {
        Arrays.fill(tickTimeArray, 0);
        loadTickTimeArray();
        ENABLE = GregTech_API.sSpecialFile.get(ConfigCategories.general, "AdjustRecipeByTPS", false);
        for (double l : tickTimeArray) {
            ticktimeSum += l;
        }
    }

    public static void loadTickTimeArray() {
        if (tpsFile.exists() && tpsFile.isFile()) {
            try (FileInputStream input = new FileInputStream(tpsFile)) {
                List<String> strings = IOUtils.readLines(input);
                for (int i = 0; i < strings.size(); i++) {
                    tickTimeArray[i] = Double.parseDouble(strings.get(i));
                }
            } catch (IOException e) {

            }
        }
    }

    public static void saveTickTimeArray() {
        try (FileOutputStream fileOutputStream = new FileOutputStream(tpsFile)) {
            IOUtils.writeLines(
                Arrays.stream(tickTimeArray)
                    .mapToObj(String::valueOf)
                    .collect(Collectors.toList()),
                "\n",
                fileOutputStream);
        } catch (IOException e) {}
    }

    public static double getMultiplierByMSPT() {
        if (ENABLE) {
            return multiplier;
        } else {
            return 1;
        }
    }

    public static void updateMSPT() {
        if (!ENABLE) return;
        int tickCounter = MinecraftServer.getServer()
            .getTickCounter() - 1;
        long tickTime = MinecraftServer.getServer().tickTimeArray[tickCounter % 100];
        int idx = tickCounter % DURATION;
        ticktimeSum = ticktimeSum + Math.max((tickTime / 1000000.0 - tickTimeArray[(idx + 1) % DURATION]), 0);
        tickTimeArray[idx] = tickTime / 1000000.0;
        multiplier = 20 / Math.min(1000 * DURATION / ticktimeSum, 20);
    }
}
