package com.kaduvill.asjeialldrops.config;

import com.kaduvill.asjeialldrops.ASJeiAllDrops;
import net.minecraftforge.common.config.Config;

/** Client startup switches for this addon's JEI categories. */
@Config(modid = ASJeiAllDrops.MOD_ID, name = ASJeiAllDrops.MOD_ID, category = "jei")
public final class ASJeiAllDropsConfig {
    @Config.Name("Mineralis")
    @Config.Comment("Show the Mineralis Ritual category. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean mineralis = true;

    @Config.Name("ContainmentChalice")
    @Config.Comment("Show the Containment Chalice reactions category. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean containmentChalice = true;

    @Config.Name("StoneEnrichment")
    @Config.Comment("Show the Stone Enrichment category. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean stoneEnrichment = true;

    @Config.Name("TrashToTreasure")
    @Config.Comment("Show the Trash to Treasure category. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean trashToTreasure = true;

    @Config.Name("TreasureShrine")
    @Config.Comment("Show the Treasure Shrine category. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean treasureShrine = true;

    private ASJeiAllDropsConfig() {}
}
