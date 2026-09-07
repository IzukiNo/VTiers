package com.tiers.profile.types;

import com.tiers.misc.Mode;
import com.tiers.profile.GameMode;
import net.minecraft.util.Identifier;

public class VNListProfile extends SuperProfile {
    public static final Identifier VNLIST_IMAGE = Identifier.of("minecraft", "textures/vnlist_logo.png");

    public VNListProfile(String apiUrl, String uuid, String extra) {
        super();
        addGamemodes();
        buildRequest(apiUrl, uuid, extra);
    }

    public VNListProfile(String json) {
        super();
        addGamemodes();
        parseJson(json);
    }

    private void addGamemodes() {
        gameModes.add(new GameMode(Mode.VNLIST_VANILLA, "vanilla"));
        gameModes.add(new GameMode(Mode.VNLIST_SWORD, "sword"));
        gameModes.add(new GameMode(Mode.VNLIST_UHC, "uhc"));
        gameModes.add(new GameMode(Mode.VNLIST_POT, "pot"));
        gameModes.add(new GameMode(Mode.VNLIST_NETHOP, "nethop"));
        gameModes.add(new GameMode(Mode.VNLIST_SMP, "smp"));
        gameModes.add(new GameMode(Mode.VNLIST_AXE, "axe"));
        gameModes.add(new GameMode(Mode.VNLIST_MACE, "mace"));
    }
}
