package common.java.file;

public class FilePointerEx {
    private final FilePointer[] pointerArray;
    private final int beginIdx;

    private FilePointerEx(long offset, long length) {
        FilePointer fp = FilePointer.build(offset);
        int beginIdx = fp.idx();
        int beginOffset = fp.point();
        int blockSize = (int) Math.ceil(length / FileEx.MAX_BLOCK_LENGTH);
        this.beginIdx = beginIdx;
        pointerArray = new FilePointer[blockSize];
        // 填充第一个
        pointerArray[0] = FilePointer.build(beginIdx, beginOffset, (int) (length > (long) FileEx.MAX_BLOCK_LENGTH ? FileEx.MAX_BLOCK_LENGTH - beginOffset : length));
        // 有超过1个
        int tSize = blockSize - 1;
        if (blockSize > 1) {
            for (int i = 1; i < tSize; i++) {
                pointerArray[i] = FilePointer.build(beginIdx + i, 0, FileEx.MAX_BLOCK_LENGTH);
            }
        }
        // 处理最后一个
        if (tSize > 0) {
            pointerArray[tSize] = FilePointer.build(beginIdx + tSize, 0, beginOffset);
        }
    }

    public static FilePointerEx build(long offset, long length) {
        return new FilePointerEx(offset, length);
    }

    public int size() {
        return pointerArray.length;
    }

    public FilePointer get(int idx) {
        return pointerArray[idx];
    }

    public FilePointer getByBlockID(int idx) {
        return pointerArray[idx - this.beginIdx];
    }
}
