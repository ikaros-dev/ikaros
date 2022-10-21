package cn.liguohao.ikaros.service;

import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.common.kit.SystemVarKit;
import cn.liguohao.ikaros.model.binary.Binary;
import cn.liguohao.ikaros.model.binary.BinaryStorge;
import cn.liguohao.ikaros.model.binary.LocalBinaryStorge;
import cn.liguohao.ikaros.model.entity.ResourceEntity;
import cn.liguohao.ikaros.repository.ResourceRepository;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * @author guohao
 * @date 2022/10/21
 */
@Service
@Transactional(rollbackOn = Exception.class)
public class ResourceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceService.class);

    private BinaryStorge binaryStorge = new LocalBinaryStorge();
    private final ResourceRepository repository;
    private final Environment environment;

    public ResourceService(ResourceRepository repository, Environment environment) {
        this.repository = repository;
        this.environment = environment;
    }

    public ResourceEntity save(ResourceEntity resourceEntity, byte[] bytes) {
        Assert.notNull(resourceEntity, "'resourceEntity' must not be null");
        String name = resourceEntity.getName();
        Assert.notBlank(name, "'name' must not be blank");


        // upload binary data and update url
        if (bytes != null) {
            Binary binary = new Binary()
                .setBytes(bytes)
                .setName(name);
            binary = binaryStorge.add(binary);
            binary.setUrl(path2url(binary.getUrl()));
            resourceEntity.setUrl(binary.getUrl());
        }

        return repository.saveAndFlush(resourceEntity);
    }


    private String path2url(String path) {
        String url = "";
        String currentAppDirPath = SystemVarKit.getCurrentAppDirPath();
        String ipAddress = SystemVarKit.getIPAddress();
        String port = environment.getProperty("local.server.port");
        String baseUrl = "http://" + ipAddress + ":" + port;
        url = path.replace(currentAppDirPath, baseUrl);
        // 如果是ntfs目录URL，则需要替换下 \ 为 /
        if (url.indexOf("\\") > 0) {
            url = url.replace("\\", "/");
        }
        return url;
    }

}
