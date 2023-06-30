package run.ikaros.api.core.file;

import java.nio.file.Path;
import org.pf4j.ExtensionPoint;

public interface RemoteFileHandler extends ExtensionPoint {

    /**
     * delegation remote file handler remote.
     */
    String remote();

    /**
     * Push all chunk encrypted file to remote.
     *
     * @param path file that need push to remote
     * @return remote file id list
     */
    RemoteFileChunk push(Path path);

    /**
     * Pull all chunk encrypted file from remote.
     *
     * @param chunkFilePullDirPath all chunk encrypted file pull dir path
     * @param remoteFileId         remote file id
     */
    void pull(Path chunkFilePullDirPath, String remoteFileId);

    /**
     * Delete remote chunk file path.
     */
    void delete(String chunkFilePath);

    /**
     * Verify remote file handler is ready.
     */
    boolean ready();
}
