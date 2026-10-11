package net.cengiz1.uxmskyblock.island;

import net.cengiz1.uxmskyblock.config.SettingsManager;

import java.util.concurrent.atomic.AtomicInteger;

public class IslandGrid {

    private final SettingsManager settings;
    private final AtomicInteger nextIndex = new AtomicInteger(0);

    public IslandGrid(SettingsManager settings) {
        this.settings = settings;
    }

    public int reserveIndex() {
        return this.nextIndex.getAndIncrement();
    }

    public void setNextIndex(int nextIndex) {
        this.nextIndex.set(nextIndex);
    }

    public int getCenterX(int index) {
        return spiral(index)[0] * settings.getIslandDistance();
    }

    public int getCenterZ(int index) {
        return spiral(index)[1] * settings.getIslandDistance();
    }

    private final java.util.List<int[]> spiralStates = new java.util.ArrayList<>();

    private int[] spiral(int index) {
        synchronized (spiralStates) {
            if (spiralStates.isEmpty())
                spiralStates.add(new int[]{0, 0, 0, -1});
            while (spiralStates.size() <= index) {
                int[] last = spiralStates.get(spiralStates.size() - 1);
                int x = last[0];
                int z = last[1];
                int dx = last[2];
                int dz = last[3];
                if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
                    int temp = dx;
                    dx = -dz;
                    dz = temp;
                }
                spiralStates.add(new int[]{x + dx, z + dz, dx, dz});
            }
            int[] state = spiralStates.get(index);
            return new int[]{state[0], state[1]};
        }
    }
}
