package com.loffsfield.chess.block;

import net.minecraft.util.StringRepresentable;

public enum ChessWoodType implements StringRepresentable {
    ACACIA("acacia"),
    BAMBOO("bamboo"),
    BIRCH("birch"),
    CHERRY("cherry"),
    CRIMSON("crimson"),
    DARK_OAK("dark_oak"),
    JUNGLE("jungle"),
    MANGROVE("mangrove"),
    OAK("oak"),
    SPRUCE("spruce"),
    WARPED("warped");

    private final String serializedName;

    ChessWoodType(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public static ChessWoodType byName(String name) {
        for (ChessWoodType woodType : values()) {
            if (woodType.serializedName.equals(name)) {
                return woodType;
            }
        }
        return null;
    }
}
