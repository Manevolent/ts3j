package com.github.manevolent.ts3j.protocol;

public class RingQueue {
    private static final int
            NOT_SET = 0x00,
            SET = 0x01,
            OUT_OF_WINDOW = 0x00,
            IN_WINDOW = 0x10;

    private boolean[] ringBufferSet;

    private final int initialBufferSize = 16;
    private int currentStart, currentLength;

    private int mappedBaseOffset;
    private final int mappedMod;
    private int generation;

    private final int maxBufferSize;

    public RingQueue(int maxBufferSize, int mod) {
        if (maxBufferSize == -1)
        {
            this.maxBufferSize = (mod / 2) - 1;
        }
        else
        {
            if (maxBufferSize >= mod)
                throw new IllegalArgumentException("Modulo must be bigger than buffer size");

            this.maxBufferSize = maxBufferSize;
        }

        int setBufferSize = Math.min(initialBufferSize, maxBufferSize);
        ringBufferSet = new boolean[setBufferSize];
        mappedMod = mod;

        clear();
    }

    private void bufferSet(int index) {
        bufferExtend(index);
        int local = indexToLocal(index);
        int newLength = local - currentStart + 1 + (local >= currentStart ? 0 : ringBufferSet.length);
        currentLength = Math.max(currentLength, newLength);
        ringBufferSet[local] = true;
    }

    private boolean stateGet(int index) {
        bufferExtend(index);
        int local = indexToLocal(index);
        return ringBufferSet[local];
    }

    private void bufferPop() {
        ringBufferSet[currentStart] = false;

        currentStart = (currentStart + 1) % ringBufferSet.length;
        currentLength --;
    }

    private void bufferExtend(int index) {
        if (index < ringBufferSet.length)
            return;
        if (index >= maxBufferSize)
            throw new IllegalArgumentException("the index does not fit into the maximal buffer size");
        int extendto = index < ringBufferSet.length * 2
                ? Math.min(ringBufferSet.length * 2, maxBufferSize)
                : Math.min(index + ringBufferSet.length, maxBufferSize);

        Object[] extRingBuffer = new Object[extendto];
        boolean[] extRingBufferSet = new boolean[extendto];

        System.arraycopy(ringBufferSet, currentStart, extRingBufferSet, 0, ringBufferSet.length - currentStart);
        System.arraycopy(ringBufferSet, 0, extRingBufferSet, ringBufferSet.length - currentStart, currentStart);

        currentStart = 0;
        ringBufferSet = extRingBufferSet;
    }

    private int indexToLocal(int index) {
        return (currentStart + index) % ringBufferSet.length;
    }

    public void clear()
    {
        currentStart = 0;
        currentLength = 0;
        for (int i = 0; i < ringBufferSet.length; i ++) ringBufferSet[i] = false;
        mappedBaseOffset = 0;
        generation = 0;
    }

    public void set(int mappedValue) {
        int index = mappedToIndex(mappedValue);

        if (isSetIndex(index) != ItemSetStatus.IN_WINDOW_NOT_SET)
            throw new IllegalStateException("object cannot be set");

        bufferSet(index);
    }

    private int mappedToIndex(int mappedValue) {
        if (mappedValue >= mappedMod)
            throw new ArrayIndexOutOfBoundsException("mappedValue");

        if (isNextGen(mappedValue)) {
            return (mappedValue + mappedMod) - mappedBaseOffset;
        } else {
            return mappedValue - mappedBaseOffset;
        }
    }

    public boolean isNextGen(int mappedValue) {
        return mappedBaseOffset > mappedMod - maxBufferSize && mappedValue < maxBufferSize;
    }

    public int getGeneration(int mappedValue) {
        return generation + (isNextGen(mappedValue) ? 1 : 0);
    }

    private ItemSetStatus isSetIndex(int index) {
        if (index < 0)
            return ItemSetStatus.OUT_OF_WINDOW_SET;
        else if (index > currentLength && index < maxBufferSize)
            return ItemSetStatus.IN_WINDOW_NOT_SET;
        else if (index >= maxBufferSize)
            return ItemSetStatus.OUT_OF_WINDOW_NOT_SET;

        return stateGet(index) ? ItemSetStatus.IN_WINDOW_SET : ItemSetStatus.IN_WINDOW_NOT_SET;
    }

    public int getCount() {
        return currentLength;
    }

    public boolean isSet(int packetId) {
        return isSetIndex(packetId) == ItemSetStatus.IN_WINDOW_SET;
    }

    public enum ItemSetStatus {
        OUT_OF_WINDOW_NOT_SET(0x00),
        OUT_OF_WINDOW_SET(0x01),
        IN_WINDOW_NOT_SET(0x10),
        IN_WINDOW_SET(0x11);

        private final int index;

        ItemSetStatus(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public static ItemSetStatus fromId(int index) {
            for (ItemSetStatus value : values())
                if (value.getIndex() == index) return value;

            throw new IllegalArgumentException("invalid index: " + index);
        }
    }
}
