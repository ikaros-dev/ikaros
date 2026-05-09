package run.ikaros.server.core.migration;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import run.ikaros.api.infra.utils.UuidV7Utils;

/**
 * Test data builder for migration tests.
 * Provides methods to create test data for various tables.
 */
public class MigrationTestDataBuilder {

    /**
     * Create a subject record map for testing.
     */
    public static Map<String, Object> createSubjectRecord(Long id, String name) {
        Map<String, Object> record = new HashMap<>();
        record.put("id", id);
        record.put("uuid", UuidV7Utils.generate());
        record.put("name", name);
        record.put("name_cn", name + "_cn");
        record.put("type", "ANIME");
        record.put("nsfw", false);
        record.put("air_time", LocalDateTime.now());
        record.put("cover", "https://example.com/cover.jpg");
        record.put("infobox", "test infobox");
        record.put("summary", "test summary");
        record.put("create_time", LocalDateTime.now());
        record.put("update_time", LocalDateTime.now());
        record.put("deleted", false);
        return record;
    }

    /**
     * Create an episode record map for testing.
     */
    public static Map<String, Object> createEpisodeRecord(Long id, Long subjectId, String name) {
        Map<String, Object> record = new HashMap<>();
        record.put("id", id);
        record.put("uuid", UuidV7Utils.generate());
        record.put("subject_id", subjectId);
        record.put("name", name);
        record.put("name_cn", name + "_cn");
        record.put("description", "test description");
        record.put("air_time", LocalDateTime.now());
        record.put("sequence", 1.0f);
        record.put("group", "MAIN");
        record.put("create_time", LocalDateTime.now());
        record.put("update_time", LocalDateTime.now());
        record.put("deleted", false);
        return record;
    }

    /**
     * Create an attachment record map for testing.
     */
    public static Map<String, Object> createAttachmentRecord(Long id, Long parentId, String name) {
        Map<String, Object> record = new HashMap<>();
        record.put("id", id);
        record.put("uuid", UuidV7Utils.generate());
        record.put("parent_id", parentId);
        record.put("type", "File");
        record.put("url", "https://example.com/" + name);
        record.put("path", "/" + name);
        record.put("fs_path", "/fs/" + name);
        record.put("name", name);
        record.put("size", 1024L);
        record.put("update_time", LocalDateTime.now());
        record.put("deleted", false);
        record.put("driver_id", null);
        record.put("sha1", "test-sha1-" + id);
        record.put("create_time", LocalDateTime.now());
        return record;
    }

    /**
     * Create an attachment directory record with special parent_id values.
     */
    public static Map<String, Object> createAttachmentDirectoryRecord(Long id, Long parentId,
                                                                      String name) {
        Map<String, Object> record = createAttachmentRecord(id, parentId, name);
        record.put("type", "Directory");
        return record;
    }

    /**
     * Create an attachment reference record for testing.
     */
    public static Map<String, Object> createAttachmentReferenceRecord(Long id, String type,
                                                                       Long attachmentId,
                                                                       Long referenceId) {
        Map<String, Object> record = new HashMap<>();
        record.put("id", id);
        record.put("uuid", UuidV7Utils.generate());
        record.put("type", type);
        record.put("attachment_id", attachmentId);
        record.put("reference_id", referenceId);
        return record;
    }

    /**
     * Create a user record map for testing.
     */
    public static Map<String, Object> createUserRecord(Long id, String username) {
        Map<String, Object> record = new HashMap<>();
        record.put("id", id);
        record.put("uuid", UuidV7Utils.generate());
        record.put("username", username);
        record.put("password", "$2a$10$encoded_password");
        record.put("nickname", username + "_nickname");
        record.put("email", username + "@example.com");
        record.put("enable", true);
        record.put("non_locked", true);
        record.put("delete_status", false);
        record.put("create_time", LocalDateTime.now());
        record.put("update_time", LocalDateTime.now());
        record.put("avatar", "https://example.com/avatar.jpg");
        record.put("introduce", "test introduce");
        record.put("site", "https://example.com");
        record.put("telephone", "1234567890");
        return record;
    }

    /**
     * Create a subject collection record for testing.
     */
    public static Map<String, Object> createSubjectCollectionRecord(Long id, Long userId,
                                                                     Long subjectId) {
        Map<String, Object> record = new HashMap<>();
        record.put("id", id);
        record.put("uuid", UuidV7Utils.generate());
        record.put("user_id", userId);
        record.put("subject_id", subjectId);
        record.put("type", "WISH");
        record.put("main_ep_progress", 0);
        record.put("score", 0);
        record.put("is_private", false);
        record.put("create_time", LocalDateTime.now());
        record.put("update_time", LocalDateTime.now());
        return record;
    }

    /**
     * Create an episode collection record for testing.
     */
    public static Map<String, Object> createEpisodeCollectionRecord(Long id, Long userId,
                                                                     Long episodeId) {
        Map<String, Object> record = new HashMap<>();
        record.put("id", id);
        record.put("uuid", UuidV7Utils.generate());
        record.put("user_id", userId);
        record.put("episode_id", episodeId);
        record.put("type", "WISH");
        record.put("main_ep_progress", 0);
        record.put("create_time", LocalDateTime.now());
        record.put("update_time", LocalDateTime.now());
        return record;
    }

    /**
     * Create a tag record for testing.
     */
    public static Map<String, Object> createTagRecord(Long id, Long masterId, String name) {
        Map<String, Object> record = new HashMap<>();
        record.put("id", id);
        record.put("uuid", UuidV7Utils.generate());
        record.put("master_id", masterId);
        record.put("name", name);
        record.put("create_time", LocalDateTime.now());
        record.put("update_time", LocalDateTime.now());
        return record;
    }

    /**
     * Create a role record for testing.
     */
    public static Map<String, Object> createRoleRecord(Long id, String name) {
        Map<String, Object> record = new HashMap<>();
        record.put("id", id);
        record.put("uuid", UuidV7Utils.generate());
        record.put("name", name);
        record.put("create_time", LocalDateTime.now());
        record.put("update_time", LocalDateTime.now());
        return record;
    }

    /**
     * Create an authority record for testing.
     */
    public static Map<String, Object> createAuthorityRecord(Long id, String authority) {
        Map<String, Object> record = new HashMap<>();
        record.put("id", id);
        record.put("uuid", UuidV7Utils.generate());
        record.put("allow", true);
        record.put("type", "API");
        record.put("target", "/api/v1/test");
        record.put("authority", authority);
        record.put("create_time", LocalDateTime.now());
        record.put("update_time", LocalDateTime.now());
        return record;
    }

    /**
     * Create a custom record for testing.
     */
    public static Map<String, Object> createCustomRecord(Long id, String name) {
        Map<String, Object> record = new HashMap<>();
        record.put("id", id);
        record.put("uuid", UuidV7Utils.generate());
        record.put("name", name);
        record.put("create_time", LocalDateTime.now());
        record.put("update_time", LocalDateTime.now());
        return record;
    }
}
