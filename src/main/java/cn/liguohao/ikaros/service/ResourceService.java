package cn.liguohao.ikaros.service;

import cn.liguohao.ikaros.repository.ResourceRepository;
import cn.liguohao.ikaros.repository.ResourceTypeRepository;
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
public class ResourceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceService.class);

    private final ResourceRepository repository;
    private final ResourceTypeRepository typeRepository;

    public ResourceService(ResourceRepository repository,
                           ResourceTypeRepository typeRepository) {
        this.repository = repository;
        this.typeRepository = typeRepository;
    }


}
