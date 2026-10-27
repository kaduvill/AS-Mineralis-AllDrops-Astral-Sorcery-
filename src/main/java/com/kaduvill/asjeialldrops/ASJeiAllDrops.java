package com.kaduvill.asjeialldrops;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = ASJeiAllDrops.MOD_ID, name = ASJeiAllDrops.MOD_NAME,
        version = ASJeiAllDrops.VERSION, acceptedMinecraftVersions = "[" + Tags.MC_VERSION + "]",
        dependencies = "required-after:astralsorcery@[1.10.27];required-after:jei@[4.15,5);",
        clientSideOnly = true, acceptableRemoteVersions = "*")
public final class ASJeiAllDrops {
    public static final String MOD_ID = "asjeialldrops";
    public static final String MOD_NAME = "Astral Sorcery JEI Addon (All Drops)";
    public static final String VERSION = Tags.VERSION;
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);
}
