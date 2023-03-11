package fr.diskmth.syncx4j;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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


    /*public List<SyncXFile> getRemoteFiles(String remoteDirPath) throws IOException
    {
        final List<SyncXFile> remoteFiles = new ArrayList<>();

        if (client instanceof FTPSClient)
        {
            ((FTPSClient) client).execPROT("P");
        }

        final FTPFile[] rootFiles = client.listFiles(remoteDirPath);

        if (rootFiles == null || rootFiles.length == 0)
        {
            //TODO : warn error
            System.out.println("No files found in " + remoteDirPath);
            return remoteFiles;
        }

        for (FTPFile file : rootFiles)
        {
            if (file.isFile())
            {
                if (!remoteFiles.containsKey(remoteDirPath))
                {
                    remoteFiles.put(remoteDirPath, new ArrayList<>());
                }
                remoteFiles.get(remoteDirPath).add(file);
            }
            else if (file.isDirectory())
            {
                remoteFiles.putAll(getRemoteFiles(remoteDirPath + "/" + file.getName()));
            }
        }

        return remoteFiles;
    }*/

    public HashMap<String, List<File>> getLocalFiles(String localDirPath)
    {
        final HashMap<String, List<File>> localFiles = new HashMap<>();
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
                if (!localFiles.containsKey(localDirPath))
                {
                    localFiles.put(localDirPath, new ArrayList<>());
                }
                localFiles.get(localDirPath).add(file);
            }
            else if (file.isDirectory())
            {
                localFiles.putAll(getLocalFiles(localDirPath + "/" + file.getName()));
            }
        }

        return localFiles;
    }

    public HashMap<String, Long> getFilesToDownload(HashMap<String, List<FTPFile>> remoteFiles, HashMap<String, List<File>> localFiles)
    {
        HashMap<String, Long> filesToDownload = new HashMap<>();

        for (String remotePath : remoteFiles.keySet())
        {
            List<FTPFile> remoteFileList = remoteFiles.get(remotePath);
            List<File> localFileList = localFiles.get(remotePath);

            if (localFileList == null)
            {
                // No local files found for this remote directory, download all remote files
                for (FTPFile remoteFile : remoteFileList)
                {
                    String remoteFilePath = remotePath + "/" + remoteFile.getName();
                    filesToDownload.put(remoteFilePath, remoteFile.getSize());
                }
            }
            else
            {
                // Check if each remote file needs to be downloaded
                for (FTPFile remoteFile : remoteFileList)
                {
                    String remoteFilePath = remotePath + "/" + remoteFile.getName();
                    boolean fileNeedsDownload = true;

                    for (File localFile : localFileList)
                    {
                        if (localFile.getName().equals(remoteFile.getName()) && localFile.length() == remoteFile.getSize())
                        {
                            // The local file exists and has the same size, skip download
                            fileNeedsDownload = false;
                            break;
                        }
                    }

                    if (fileNeedsDownload)
                    {
                        filesToDownload.put(remoteFilePath, remoteFile.getSize());
                    }
                }
            }
        }

        return filesToDownload;
    }

    public HashMap<String, Long> getFilesToDelete(HashMap<String, List<FTPFile>> remoteFiles, HashMap<String, List<File>> localFiles)
    {
        final HashMap<String, Long> filesToDelete = new HashMap<>();

        for (String localPath : localFiles.keySet())
        {
            List<File> localFileList = localFiles.get(localPath);
            List<FTPFile> remoteFileList = remoteFiles.get(localPath);

            if (remoteFileList == null)
            {
                // No remote files found for this local directory, delete all local files
                for (File localFile : localFileList)
                {
                    filesToDelete.put(localFile.getAbsolutePath(), localFile.length());
                }
            }
            else
            {
                // Check if each local file needs to be deleted
                for (File localFile : localFileList)
                {
                    boolean fileNeedsDeletion = true;

                    for (FTPFile remoteFile : remoteFileList)
                    {
                        if (remoteFile.getName().equals(localFile.getName()) && remoteFile.getSize() == localFile.length())
                        {
                            // The remote file exists and has the same size, skip deletion
                            fileNeedsDeletion = false;
                            break;
                        }
                    }

                    if (fileNeedsDeletion)
                    {
                        filesToDelete.put(localFile.getAbsolutePath(), localFile.length());
                    }
                }
            }
        }

        return filesToDelete;
    }


    /*public void syncDirectory(String localDirPath, HashMap<String, List<FTPFile>> remoteFiles) throws IOException
    {
        final HashMap<String, Long> filesToDownload = getFilesToDownload(remoteDirPath, localDirPath);
        final HashMap<String, Long> filesToDelete = getFilesToDelete(remoteDirPath, localDirPath);

        for (Map.Entry<String, Long> entry : filesToDownload.entrySet())
        {
            final String remoteFilePath = entry.getKey();
            final Long fileSize = entry.getValue();

            final File localFile = new File(localDirPath + "/" + (new File(remoteFilePath)).getName());

            client.retrieveFile(remoteFilePath, new FileOutputStream(localFile));

            if (localFile.length() != fileSize)
            {
                throw new IOException("Error downloading file " + remoteFilePath);
            }

            System.out.println("Downloaded: " + remoteFilePath);
        }

        for (Map.Entry<String, Long> entry : filesToDelete.entrySet())
        {
            final String localFilePath = entry.getKey();

            if (!new File(localFilePath).delete())
            {
                throw new IOException("Error deleting file " + localFilePath);
            }

            System.out.println("Deleted: " + localFilePath);
        }
    }

    /*private void syncDirectory(String remoteDirPath, String localDirPath) throws IOException
    {
        if (client instanceof FTPSClient)
        {
            ((FTPSClient) client).execPROT("P");
        }
        final FTPFile[] remoteFiles = client.listFiles(remoteDirPath);
        final File localDir = new File(localDirPath);
        localDir.mkdirs();
        final File[] localFiles = localDir.listFiles();

        List<String> remoteFileNames = new ArrayList<>();
        for (FTPFile remoteFile : remoteFiles)
        {
            remoteFileNames.add(remoteFile.getName());

            final String remoteFilePath = remoteDirPath + "/" + remoteFile.getName();
            final File localFile = new File(localDirPath + "/" + remoteFile.getName());

            if (remoteFile.isDirectory())
            {
                syncDirectory(remoteFilePath, localFile.getPath());
            }
            else
            {
                if (!localFile.exists() || localFile.length() != remoteFile.getSize())
                {
                    System.out.println("Downloaded: " + remoteFile.getName());
                    final OutputStream outputStream = new FileOutputStream(localFile.getPath());
                    if (client instanceof FTPSClient)
                    {
                        ((FTPSClient) client).execPROT("P");
                    }
                    client.retrieveFile(remoteFilePath, outputStream);
                    outputStream.close();
                }
                else
                {
                    System.out.println("Skipped: " + remoteFile.getName());
                }
            }
        }

        for (File localFile : localFiles)
        {
            String localFileName = localFile.getName();

            if (!remoteFileNames.contains(localFileName))
            {
                if (localFile.isDirectory())
                {
                    deleteDirectory(localFile);
                }
                else
                {
                    localFile.delete();
                }
            }
        }
    }*/

    private void deleteDirectory(File directory)
    {
        File[] files = directory.listFiles();
        for (File file : files)
        {
            if (file.isDirectory())
            {
                deleteDirectory(file);
            }
            else
            {
                file.delete();
            }
        }
        directory.delete();
    }
}