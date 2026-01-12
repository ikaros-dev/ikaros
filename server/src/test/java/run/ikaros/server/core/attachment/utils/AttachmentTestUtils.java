package run.ikaros.server.core.attachment.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.server.store.entity.AttachmentEntity;

public class AttachmentTestUtils {
    /**
     * 对比对象集合是否相同.
     */
    public static boolean attIsEquals(List<AttachmentEntity> oldAttachments,
                                List<Attachment> newAttachments) {
        ArrayList<AttachmentEntity> objects = new ArrayList<>();
        for (Attachment newAttachment : newAttachments) {
            objects.add(AttachmentEntity.builder()
                .id(newAttachment.getId())
                .name(newAttachment.getName())
                .type(newAttachment.getType())
                .parentId(newAttachment.getParentId())
                .size(newAttachment.getSize())
                .updateTime(newAttachment.getUpdateTime())
                .build());
        }
        return entitiesIsEquals(oldAttachments, objects);
    }

    /**
     * 对比对象集合是否相同.
     */
    public static boolean attIsEqualsWithoutOrder(List<AttachmentEntity> oldAttachments,
                                List<Attachment> newAttachments) {
        ArrayList<AttachmentEntity> objects = new ArrayList<>();
        for (Attachment newAttachment : newAttachments) {
            objects.add(AttachmentEntity.builder()
                .id(newAttachment.getId())
                .name(newAttachment.getName())
                .type(newAttachment.getType())
                .parentId(newAttachment.getParentId())
                .size(newAttachment.getSize())
                .updateTime(newAttachment.getUpdateTime())
                .build());
        }
        return entitiesIsEqualsWithoutOrder(oldAttachments, objects);
    }

    /**
     * 对比对象集合是否相同.
     */
    public static boolean entitiesIsEquals(List<AttachmentEntity> oldAttachments,
                                     List<AttachmentEntity> newAttachments) {
        if (oldAttachments.size() != newAttachments.size()) {
            return false;
        }

        boolean result = true;
        for (int i = 0; i < oldAttachments.size(); i++) {
            AttachmentEntity oldEntity = oldAttachments.get(i);
            AttachmentEntity newEntity = newAttachments.get(i);
            if (!oldEntity.getId().equals(newEntity.getId())) {
                result = false;
                break;
            }
            if (!oldEntity.getName().equals(newEntity.getName())) {
                result = false;
                break;
            }
            if (!oldEntity.getType().equals(newEntity.getType())) {
                result = false;
                break;
            }
            if (!oldEntity.getParentId().equals(newEntity.getParentId())) {
                result = false;
                break;
            }
            if (!oldEntity.getSize().equals(newEntity.getSize())) {
                result = false;
                break;
            }
        }
        return result;
    }


    /**
     * 对比对象集合是否相同.
     */
    public static boolean entitiesIsEqualsWithoutOrder(List<AttachmentEntity> oldAttachments,
                                     List<AttachmentEntity> newAttachments) {
        if (oldAttachments.size() != newAttachments.size()) {
            return false;
        }

        Map<UUID, AttachmentEntity> oldAttIdMap = new HashMap<>();
        for (AttachmentEntity oldAttachment : oldAttachments) {
            oldAttIdMap.putIfAbsent(oldAttachment.getId(), oldAttachment);
        }

        boolean result = true;
        for (AttachmentEntity newEntity : newAttachments) {
            UUID newEntityId = newEntity.getId();
            if (!oldAttIdMap.containsKey(newEntityId)) {
                result = false;
                break;
            }
            AttachmentEntity oldEntity = oldAttIdMap.get(newEntityId);
            if (!oldEntity.getName().equals(newEntity.getName())) {
                result = false;
                break;
            }
            if (!oldEntity.getType().equals(newEntity.getType())) {
                result = false;
                break;
            }
            if (!oldEntity.getParentId().equals(newEntity.getParentId())) {
                result = false;
                break;
            }
            if (!oldEntity.getSize().equals(newEntity.getSize())) {
                result = false;
                break;
            }
        }
        return result;
    }
}
