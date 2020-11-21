package common.java.file;

public class FilePointer {
    private final int BlockID;
    private final int BlockPointer;
    private int length;

    private FilePointer(long offset) {
        this.BlockID = FilePointer.getBlockIdx(offset);
        this.BlockPointer = FilePointer.getBlockPoint(offset);
    }

    private FilePointer(int blockID, int offset, int length) {
        this.BlockID = blockID;
        this.BlockPointer = offset;
        this.length = length;
    }

    public static final FilePointer build(int blockID, int offset, int length) {
        return new FilePointer(blockID, offset, length);
    }

    public static final FilePointer build(long offset) {
        return new FilePointer(offset);
    }

    public static final int getBlockIdx(long offset) {
        return (int) offset / FileEx.MAX_BLOCK_LENGTH;
    }

    public static final int getBlockPoint(long offset) {
        return (int) offset / FileEx.MAX_BLOCK_LENGTH;
    }

    public int idx() {
        return this.BlockID;
    }

    public int point() {
        return this.BlockPointer;
    }

    public int length() {
        return this.length;
    }
}
