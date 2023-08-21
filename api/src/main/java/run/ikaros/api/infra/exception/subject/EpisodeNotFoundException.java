package run.ikaros.api.infra.exception.subject;

import run.ikaros.api.infra.exception.NotFoundException;

public class EpisodeNotFoundException extends NotFoundException {
    public EpisodeNotFoundException(String message) {
        super(message);
    }
}
