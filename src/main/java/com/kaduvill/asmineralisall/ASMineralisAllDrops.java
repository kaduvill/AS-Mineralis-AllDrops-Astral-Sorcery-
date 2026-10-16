package com.kaduvill.asmineralisall;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = ASMineralisAllDrops.MOD_ID, name = ASMineralisAllDrops.MOD_NAME,
        version = ASMineralisAllDrops.VERSION, acceptedMinecraftVersions = "[" + Tags.MC_VERSION + "]",
        dependencies = "required-after:astralsorcery@[1.10.27];required-after:jei@[4.15,5);",
        clientSideOnly = true, acceptableRemoteVersions = "*")
public final class ASMineralisAllDrops {
    public static final String MOD_ID = "asmineralisall";
    public static final String MOD_NAME = "AS Mineralis - All Drops";
    public static final String VERSION = Tags.VERSION;
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);
}
