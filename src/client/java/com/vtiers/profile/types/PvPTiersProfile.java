package com.vtiers.profile.types;

import com.vtiers.misc.Mode;
import com.vtiers.profile.GameMode;
import net.minecraft.util.Identifier;

public class PvPTiersProfile extends SuperProfile {
    public static final Identifier PVPTIERS_IMAGE = Identifier.of("minecraft", "textures/pvptiers_logo.png");

    public PvPTiersProfile(String apiUrl, String uuid, String extra) {
        super();
        addGamemodes();
        buildRequest(apiUrl, uuid, extra);
    }

    public PvPTiersProfile(String json) {
        super();
        addGamemodes();
        parseJson(json);
    }

    private void addGamemodes() {
        gameModes.add(new GameMode(Mode.PVPTIERS_CRYSTAL, "crystal"));
        gameModes.add(new GameMode(Mode.PVPTIERS_SWORD, "sword"));
        gameModes.add(new GameMode(Mode.PVPTIERS_UHC, "uhc"));
        gameModes.add(new GameMode(Mode.PVPTIERS_POT, "pot"));
        gameModes.add(new GameMode(Mode.PVPTIERS_NETH_POT, "neth_pot"));
        gameModes.add(new GameMode(Mode.PVPTIERS_SMP, "smp"));
        gameModes.add(new GameMode(Mode.PVPTIERS_AXE, "axe"));
        gameModes.add(new GameMode(Mode.PVPTIERS_MACE, "mace"));
    }
}