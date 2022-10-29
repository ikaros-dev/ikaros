package run.ikaros.server.exceptions;

/**
 * @author li-guohao
 */
public class SeasonEpisodeMatchingFailException extends RuntimeIkarosException {
    public SeasonEpisodeMatchingFailException() {
    }

    public SeasonEpisodeMatchingFailException(String message) {
        super(message);
    }

    public SeasonEpisodeMatchingFailException(String message, Throwable cause) {
        super(message, cause);
    }

    public SeasonEpisodeMatchingFailException(Throwable cause) {
        super(cause);
    }

    public SeasonEpisodeMatchingFailException(String message, Throwable cause,
                                              boolean enableSuppression,
                                              boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
