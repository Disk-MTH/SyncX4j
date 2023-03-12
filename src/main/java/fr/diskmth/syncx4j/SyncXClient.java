package fr.diskmth.syncx4j;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SyncXClient
{
    protected final FTPClient client;
    protected final String hostname;
    protected final int port;
    protected final String username;
    protected final String password;

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

    public List<SyncXFile> getFilesToDownload(List<SyncXFile> remoteFiles, List<SyncXFile> localFiles)
    {
        final List<SyncXFile> filesToDownload = new ArrayList<>(remoteFiles);

        for (SyncXFile localFile : localFiles)
        {
            for (SyncXFile remoteFile : remoteFiles)
            {
                if (localFile.getName().equals(remoteFile.getName()) && localFile.getSize() == remoteFile.getSize())
                {
                    filesToDownload.remove(remoteFile);
                    break;
                }
            }
        }

        return filesToDownload;
    }


    //TODO change for a better algorithm
    public List<SyncXFile> getFilesToDelete(List<SyncXFile> remoteFiles, List<SyncXFile> localFiles, String remoteDir)
    {
        final List<SyncXFile> filesToDelete = new ArrayList<>();

        for (SyncXFile localFile : localFiles)
        {
            boolean fileExistsRemotely = false;

            final String localFilePath = localFile.getPath() + "/" + localFile.getName();

            for (SyncXFile remoteFile : remoteFiles)
            {
                final String remoteFilePath = (remoteFile.getPath() + "/" + remoteFile.getName()).replace(remoteDir + "/", "");

                if (localFilePath.contains(remoteFilePath) && localFile.getSize() == remoteFile.getSize())
                {
                    fileExistsRemotely = true;
                    break;
                }
            }

            if (!fileExistsRemotely)
            {
                filesToDelete.add(localFile);
            }
        }

        return filesToDelete;
    }

    //TODO add progress bar, clear empty directories, logger, multithreading
    public void sync(String remoteDir, String localDir) throws IOException
    {
        final List<SyncXFile> remoteFiles = getRemoteFiles(remoteDir);
        final List<SyncXFile> localFiles = getLocalFiles(localDir);

        final List<SyncXFile> filesToDownload = getFilesToDownload(remoteFiles, localFiles);
        final List<SyncXFile> filesToDelete = getFilesToDelete(remoteFiles, localFiles, remoteDir);

        for (SyncXFile file : filesToDelete)
        {
            String localFilePath = file.getPath().replace(remoteDir, "") + "/" + file.getName();
            Path localPath = Paths.get(localFilePath);
            try
            {
                Files.delete(localPath);
                System.out.println("Deleted file: " + localFilePath);
            }
            catch (IOException e)
            {
                System.err.println("Failed to delete file: " + localFilePath);
                e.printStackTrace();
            }
        }

        for (SyncXFile file : filesToDownload)
        {
            String remoteFilePath = file.getPath() + "/" + file.getName();
            String localFilePath = localDir + "/" + file.getPath().replace(remoteDir, "") + "/" + file.getName();

            System.out.println("Downloading file: " + remoteFilePath + " to " + localFilePath);

            new File(localDir + "/" + file.getPath().replace(remoteDir, "")).mkdirs();

            if (client instanceof FTPSClient)
            {
                ((FTPSClient) client).execPROT("P");
            }

            client.retrieveFile(remoteFilePath, new FileOutputStream(localFilePath));
        }
    }
}