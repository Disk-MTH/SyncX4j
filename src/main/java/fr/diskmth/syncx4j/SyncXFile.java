package fr.diskmth.syncx4j;

//TODO subdivide path in correct way
public class SyncXFile
{
    protected String path;
    protected String name;
    protected long size;

    public SyncXFile(String path, String name, long size)
    {
        this.path = path;
        this.name = name;
        this.size = size;
    }

    @Override
    public String toString()
    {
        return path + " -> " + name + " -> " + size;
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

    public SyncXFile setPath(String path)
    {
        this.path = path;
        return this;
    }

    public SyncXFile setName(String name)
    {
        this.name = name;
        return this;
    }

    public SyncXFile setSize(long size)
    {
        this.size = size;
        return this;
    }
}
