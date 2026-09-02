package run.ikaros.common;

/** Storage Provider 当前无法提供读取能力。 */
public class StorageUnavailableException extends RuntimeException {
    public StorageUnavailableException(String message) {
        super(message);
    }
}
