package com.kaduvill.asmineralisall.jei;

/** Immutable category geometry, sized once from the largest loaded output group. */
final class ChaliceRecipeLayout {
    static final int WIDTH = 162;
    static final int SLOT = 18;
    static final int COLUMNS = 4;
    static final int PAGE_SIZE = 16;
    static final int INPUT_X = 10;
    static final int ARROW_X = 47;
    static final int OUTPUT_X = 84;
    static final int HIDDEN = -10000;
    private static final int PADDING = 2;
    private static final int INPUT_GAP = 4;
    private static final int FOOTER_HEIGHT = 13;

    final int maxOutputs;
    final int height;
    final int footerY;
    final int arrowY;
    private final int bodyHeight;
    private final int firstInputY;

    ChaliceRecipeLayout(int maxOutputs) {
        if (maxOutputs < 0) throw new IllegalArgumentException("Negative outcome count");
        this.maxOutputs = maxOutputs;
        int inputHeight = 2 * SLOT + INPUT_GAP;
        bodyHeight = Math.max(inputHeight, rows(maxOutputs) * SLOT);
        firstInputY = PADDING + (bodyHeight - inputHeight) / 2;
        arrowY = PADDING + (bodyHeight - 17) / 2;
        footerY = PADDING + bodyHeight + PADDING;
        height = footerY + (maxOutputs > PAGE_SIZE ? FOOTER_HEIGHT : 0);
    }

    private static int rows(int count) { return Math.max(1, (Math.min(count, PAGE_SIZE) + COLUMNS - 1) / COLUMNS); }
    static int pageCount(int count) { return count == 0 ? 1 : 1 + (count - 1) / PAGE_SIZE; }
    static int visibleCount(int count, int page) { return Math.max(0, Math.min(PAGE_SIZE, count - page * PAGE_SIZE)); }
    int inputY(int index) { return firstInputY + index * (SLOT + INPUT_GAP); }
    int outputX(int visible) { return OUTPUT_X + visible % COLUMNS * SLOT; }
    int outputY(int count, int visible) { return PADDING + (bodyHeight - rows(count) * SLOT) / 2 + visible / COLUMNS * SLOT; }
}
