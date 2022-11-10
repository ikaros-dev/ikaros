package run.ikaros.server.exceptions;

/**
 * @author li-guohao
 */
public class QbittorrentRequestException extends RuntimeIkarosException {
    public QbittorrentRequestException() {
    }

    public QbittorrentRequestException(String message) {
        super(message);
    }

    public QbittorrentRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public QbittorrentRequestException(Throwable cause) {
        super(cause);
    }

    public QbittorrentRequestException(String message, Throwable cause, boolean enableSuppression,
                                       boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
