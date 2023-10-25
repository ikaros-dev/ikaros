package run.ikaros.api.infra.exception.subject;

import run.ikaros.api.infra.exception.IkarosNotFoundException;

public class EpisodeIkarosNotFoundException extends IkarosNotFoundException {
    public EpisodeIkarosNotFoundException(String message) {
        super(message);
    }
}
