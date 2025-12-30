package run.ikaros.api.core.attachment;

import java.util.UUID;

public interface AttachmentConst {
    String V_ROOT_DIRECTORY_PARENT_ID = "019b715a-82bd-7d04-ac3e-0709a097de84";
    UUID ROOT_DIRECTORY_PARENT_ID = UUID.fromString(V_ROOT_DIRECTORY_PARENT_ID);
    String V_ROOT_DIRECTORY_ID = "019b715b-08c7-7509-ab14-2abe47f440f3";
    UUID ROOT_DIRECTORY_ID = UUID.fromString(V_ROOT_DIRECTORY_ID);
    String V_COVER_DIRECTORY_ID = "019b715b-5cb5-7407-b571-6688c9e61e5a";
    UUID COVER_DIRECTORY_ID = UUID.fromString(V_COVER_DIRECTORY_ID);
    String V_DOWNLOAD_DIRECTORY_ID = "019b715b-97dc-72dd-9e5a-0f714efc89d9";
    UUID DOWNLOAD_DIRECTORY_ID = UUID.fromString(V_DOWNLOAD_DIRECTORY_ID);
    String ROOT_DIR_NAME = "/";
    String COVER_DIR_NAME = "Covers";
    String DOWNLOAD_DIR_NAME = "Downloads";
    String DRIVER_STATIC_RESOURCE_PREFIX = "/driver/static";
}
