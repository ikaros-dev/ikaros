package run.ikaros.server.core.profile;

import org.springframework.stereotype.Service;
import run.ikaros.server.store.repository.ProfileRepository;

@Service
public class ProfileServiceImpl implements ProfileService {
    private final ProfileRepository repository;

    public ProfileServiceImpl(ProfileRepository repository) {
        this.repository = repository;
    }


}
