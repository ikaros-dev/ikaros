package cn.liguohao.ikaros.service;

import cn.liguohao.ikaros.entity.ItemDataEntity;
import cn.liguohao.ikaros.file.ItemDataHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author li-guohao
 * @date 2022/06/19
 */
@Service
public class ItemDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemDataService.class);
    private final ItemDataEntity itemDataEntity;
    private ItemDataHandler itemDataHandler;

    public ItemDataService(ItemDataEntity itemDataEntity) {
        this.itemDataEntity = itemDataEntity;
    }


    public String upload() {
        return null;
    }
}
