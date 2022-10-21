package cn.liguohao.ikaros.service;

import cn.liguohao.ikaros.repository.BoxRepository;
import cn.liguohao.ikaros.repository.BoxTypeRepository;
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
    private final BoxTypeRepository typeRepository;

    public BoxService(BoxRepository repository, BoxTypeRepository typeRepository) {
        this.repository = repository;
        this.typeRepository = typeRepository;
    }


}
