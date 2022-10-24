package run.ikaros.server.service;

import run.ikaros.server.entity.BoxEntity;
import run.ikaros.server.repository.BoxRepository;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author guohao
 * @date 2022/10/21
 */
@Service
@Transactional(rollbackOn = Exception.class)
public class BoxService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BoxService.class);

    private final BoxRepository repository;

    public BoxService(BoxRepository repository) {
        this.repository = repository;
    }

    public void save(BoxEntity boxEntity) {

    }
}
