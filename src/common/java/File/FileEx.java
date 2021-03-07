package common.java.File;

import sun.misc.Unsafe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FileEx<T extends FileEx> {
    protected static final int MAX_BLOCK_LENGTH = 0xffffffff;
    protected final File file;
    public long readPoint = 0;
    public long writePoint = 0;
    private FileInputStream inStream;
    private FileOutputStream outStream;
    private MappedByteBuffer[] fileMap;
    private Consumer<File> func;

    protected FileEx(File file) {
        this.file = file;
    }

    protected void error_handle() {
        if (func != null) {
            func.accept(this.file);
            throw new RuntimeException("file failed!");
        }
    }

    public T setErrorHanle(Consumer<File> func) {
        this.func = func;
        return (T) this;
    }

    /**
     * 新建文件
     */
    public T create() {
        boolean rb;
        try {
            rb = this.file.createNewFile();
        } catch (Exception e) {
            rb = false;
        }
        if (!rb) {
            error_handle();
        }
        return (T) this;
    }

    /**
     * 文件是否存在
     */
    public boolean exists() {
        return this.file.exists();
    }

    /**
     * 文件移动
     */
    public T move(File path) {
        boolean rb;
        try {
            rb = this.file.renameTo(path);
        } catch (Exception e) {
            rb = false;
        }
        if (!rb) {
            error_handle();
        }
        return (T) this;
    }

    /**
     * 文件复制
     */
    public T copy(File path) {
        try {
            Files.copy(this.file.toPath(), path.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            error_handle();
        }
        return (T) this;
    }

    /**
     * 删除文件
     */
    public T delete() {
        boolean rb;
        try {
            rb = this.file.delete();
        } catch (Exception e) {
            rb = false;
        }
        if (!rb) {
            error_handle();
        }
        return (T) this;
    }

    /**
     * 比较文件是否一致
     */
    public boolean equal(File file) {
        boolean rb;
        try {
            rb = Files.isSameFile(this.file.toPath(), file.toPath());
        } catch (Exception e) {
            rb = false;
        }
        return rb;
    }

    /**
     * 获得文件大小
     */
    public long size() {
        return this.file.length();
    }

    /**
     * 获得文件路径
     */
    public String path() {
        return file.getAbsolutePath();
    }

    /**
     * 获得文件名
     */
    public String fileName() {
        return file.getName();
    }

    /**
     * 获得文件最后修改时间
     */
    public long lastModified() {
        return file.lastModified();
    }

    /**
     * 获得文件最后修改时间
     */
    public T setLastModified(long unixtime) {
        boolean rb;
        try {
            rb = file.setLastModified(unixtime);
        } catch (Exception e) {
            rb = false;
        }
        if (!rb) {
            error_handle();
        }
        return (T) this;
    }

    protected FileInputStream getInputStream() {
        if (inStream == null) {
            try {
                inStream = new FileInputStream(this.file);
            } catch (Exception e) {
                inStream = null;
                error_handle();
            }
        }
        return this.inStream;
    }

    protected FileOutputStream getOutputStream() {
        if (outStream == null) {
            try {
                outStream = new FileOutputStream(this.file);
            } catch (Exception e) {
                outStream = null;
                error_handle();
            }
        }
        return this.outStream;
    }

    protected void release() {
        this.unmapAll();
        try {
            if (this.inStream != null) {
                this.inStream.close();
                this.inStream = null;
            }
            if (this.outStream != null) {
                this.outStream.flush();
                this.outStream.close();
                this.outStream = null;
            }
        } catch (Exception e) {
        }
    }

    protected void unmap(MappedByteBuffer fmap) {
        AccessController.doPrivileged((PrivilegedAction) () -> {
            try {
                Method getCleanerMethod = fmap.getClass().getMethod("cleaner");
                getCleanerMethod.setAccessible(true);
                Unsafe.getUnsafe().invokeCleaner(fmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    private void unmapAll() {
        if (fileMap != null) {
            for (MappedByteBuffer fmap : fileMap) {
                if (fmap != null && fmap.isLoaded()) {
                    this.unmap(fmap);
                }
            }
        }
    }

    protected MappedByteBuffer[] getFileBuffer(long offset, int length) {
        int beginIdx = FilePointer.getBlockIdx(offset);
        int endIdx = FilePointer.getBlockIdx(offset + length);
        List<MappedByteBuffer> mapList = new ArrayList<>();
        for (int i = beginIdx; i <= endIdx; i++) {
            MappedByteBuffer fmap = fileMap[i];
            mapList.add(fmap);
            if (!fmap.isLoaded()) {
                fmap.load();
            }
        }
        return mapList.toArray(new MappedByteBuffer[0]);
    }

    protected MappedByteBuffer getFileBuffer(long offset) {
        int blockIdx = FilePointer.getBlockIdx(offset);
        MappedByteBuffer fmap = fileMap[blockIdx];
        if (!fmap.isLoaded()) {
            fmap.load();
        }
        return fmap;
    }

    protected MappedByteBuffer getFileBuffer(int idx) {
        return fileMap[idx];
    }

    protected MappedByteBuffer[] getFileBuffer() {
        if (fileMap == null) {
            List<MappedByteBuffer> mapList = new ArrayList<>();
            try {
                FileInputStream fis = this.getInputStream();
                FileChannel fc = fis.getChannel();
                int blockSize = (int) Math.ceil(fc.size() / MAX_BLOCK_LENGTH);
                long tCmp, tSize;
                for (int i = 0; i < blockSize; i++) {
                    tCmp = fc.size() - i * MAX_BLOCK_LENGTH;
                    tSize = (tCmp > MAX_BLOCK_LENGTH) ? MAX_BLOCK_LENGTH : tCmp;
                    mapList.add(fc.map(FileChannel.MapMode.READ_WRITE, i * MAX_BLOCK_LENGTH, tSize));
                }
                fileMap = mapList.toArray(new MappedByteBuffer[0]);
            } catch (Exception e) {
                fileMap = null;
            }
        } else {
            for (MappedByteBuffer mappedByteBuffer : fileMap) {
                mappedByteBuffer.reset();
            }
        }
        return fileMap;
    }

}
