package run.ikaros.server.service.impl;

import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author li-guohao
 */
@Service
@Transactional(rollbackOn = Exception.class)
public class SeasonService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeasonService.class);



}
