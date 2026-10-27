package com.kaduvill.asjeialldrops.jei;

/** Fixed for one JEI category registration; grows from one to four output rows. */
final class OreTableLayout {
    static final int WIDTH = 162, SLOT = 18, COLUMNS = 4, PAGE_SIZE = 16;
    static final int INPUT_X = 10, ARROW_X = 47, OUTPUT_X = 84, HIDDEN = -10000;
    final int height, footerY, inputY, arrowY, perkY, pages;
    private final int outputTop;

    OreTableLayout(int count, boolean perk) {
        if (count < 0) throw new IllegalArgumentException("Negative output count");
        int outputHeight = Math.max(1, (Math.min(count, PAGE_SIZE) + COLUMNS - 1) / COLUMNS) * SLOT;
        int arrowHeight = perk ? 17 + 2 + 9 : 17;
        int bodyHeight = Math.max(outputHeight, arrowHeight);
        inputY = 2 + (bodyHeight - SLOT) / 2;
        arrowY = 2 + (bodyHeight - arrowHeight) / 2;
        perkY = perk ? arrowY + 17 + 2 : -1;
        outputTop = 2 + (bodyHeight - outputHeight) / 2;
        footerY = bodyHeight + 4;
        pages = count == 0 ? 1 : 1 + (count - 1) / PAGE_SIZE;
        height = footerY + (pages > 1 ? 13 : 0);
    }
    int outputX(int index) { return OUTPUT_X + index % COLUMNS * SLOT; }
    int outputY(int index) { return outputTop + index / COLUMNS * SLOT; }
    static int visible(int count, int page) { return Math.max(0, Math.min(PAGE_SIZE, count - page * PAGE_SIZE)); }
}
