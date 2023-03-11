package fr.diskmth.syncx4j;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

public class SyncXClient
{
    private FTPClient client;
    private final String hostname;
    private final int port;
    private final String username;
    private final String password;

    public SyncXClient(String hostname, int port, String username, String password, boolean useSSL)
    {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.password = password;
        this.client = useSSL ? new FTPSClient() : new FTPClient();
    }

    public void login() throws IOException
    {
        client.connect(hostname, port);
        client.login(username, password);
        client.enterLocalPassiveMode();
        if (client instanceof FTPSClient)
        {
            ((FTPSClient) client).execPBSZ(0);
        }
        client.setFileType(FTP.BINARY_FILE_TYPE);
    }

    public void logout() throws IOException
    {
        client.logout();
        client.disconnect();
    }

    public List<SyncXFile> getRemoteFiles(String remoteDir) throws IOException
    {
        final List<SyncXFile> remoteFiles = new ArrayList<>();

        if (client instanceof FTPSClient)
        {
            ((FTPSClient) client).execPROT("P");
        }

        final FTPFile[] ftpFiles = client.listFiles(remoteDir);

        if (ftpFiles == null || ftpFiles.length == 0)
        {
            //TODO : warn error
            System.out.println("No remoteFiles found in " + remoteDir);
            return remoteFiles;
        }

        for (FTPFile ftpFile : ftpFiles)
        {
            if (ftpFile.isFile())
            {
                remoteFiles.add(new SyncXFile(remoteDir, ftpFile.getName(), ftpFile.getSize()));
            }
            else if (ftpFile.isDirectory())
            {
                remoteFiles.addAll(getRemoteFiles(remoteDir + "/" + ftpFile.getName()));
            }
        }

        return remoteFiles;
    }

    public List<SyncXFile> getLocalFiles(String localDirPath)
    {
        final List<SyncXFile> localFiles = new ArrayList<>();
        final File[] rootFiles = new File(localDirPath).listFiles();

        if (rootFiles == null || rootFiles.length == 0)
        {
            //TODO : warn error
            System.out.println("No files found in " + localDirPath);
            return localFiles;
        }

        for (File file : rootFiles)
        {
            if (file.isFile())
            {
                localFiles.add(new SyncXFile(localDirPath, file.getName(), file.length()));
            }
            else if (file.isDirectory())
            {
                localFiles.addAll(getLocalFiles(localDirPath + "/" + file.getName()));
            }
        }

        return localFiles;
    }

    public List<SyncXFile> getFilesDiff(List<SyncXFile> files1, List<SyncXFile> files2, BiPredicate<SyncXFile, SyncXFile> comparator)
    {
        final List<SyncXFile> result = new ArrayList<>();

        for (SyncXFile file1 : files1)
        {
            boolean found = false;
            for (SyncXFile file2 : files2)
            {
                if (comparator.test(file1, file2))
                {
                    found = true;
                    break;
                }
            }
            if (!found)
            {
                result.add(file1);
            }
        }

        return result;
    }

    public List<SyncXFile> getFilesToDownload(List<SyncXFile> remoteFiles, List<SyncXFile> localFiles)
    {
        return getFilesDiff(remoteFiles, localFiles,
                (remoteFile, localFile) -> localFile.getName().equals(remoteFile.getName()) && localFile.getSize() == remoteFile.getSize());
    }

    public List<SyncXFile> getFilesToDelete(List<SyncXFile> remoteFiles, List<SyncXFile> localFiles)
    {
        return getFilesDiff(localFiles, remoteFiles,
                (localFile, remoteFile) -> remoteFile.getName().equals(localFile.getName()) && remoteFile.getSize() == localFile.getSize());
    }
}