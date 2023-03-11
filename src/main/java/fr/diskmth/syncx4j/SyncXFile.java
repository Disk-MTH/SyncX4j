package fr.diskmth.syncx4j;

public class SyncXFile
{
    private final String path;
    private final String name;
    private final long size;

    public SyncXFile(String path, String name, long size)
    {
        this.path = path;
        this.name = name;
        this.size = size;
    }

    public String getPath()
    {
        return path;
    }

    public String getName()
    {
        return name;
    }

    public long getSize()
    {
        return size;
    }
}
