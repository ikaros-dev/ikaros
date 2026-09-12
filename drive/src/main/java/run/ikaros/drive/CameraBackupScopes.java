package run.ikaros.drive;

import run.ikaros.common.ConflictException;

final class CameraBackupScopes {
    private static final String ALL_PHOTOS_SCOPE = "camera-roll:all-photos";
    private static final String ALBUM_SCOPE_PREFIX = "camera-roll:album:";

    private CameraBackupScopes() {}

    static String encode(CameraBackupScopeRequest request) {
        if (request.scopeKind() == CameraBackupScopeKind.ALL_PHOTOS) {
            if (request.albumId() != null && !request.albumId().trim().isEmpty()) {
                throw new ConflictException("全部照片范围不能同时指定相册");
            }
            return ALL_PHOTOS_SCOPE;
        }
        String albumId = request.albumId() == null ? "" : request.albumId().trim();
        if (albumId.isEmpty()) throw new ConflictException("指定相册范围必须填写相册 ID");
        if (albumId.contains("\n") || albumId.contains("\r")) {
            throw new ConflictException("相册 ID 不能包含换行符");
        }
        return ALBUM_SCOPE_PREFIX + albumId;
    }

    static boolean overlaps(String left, String right) {
        Parsed a = parse(left);
        Parsed b = parse(right);
        if (a == null || b == null) return false;
        if (a.kind() == CameraBackupScopeKind.ALL_PHOTOS || b.kind() == CameraBackupScopeKind.ALL_PHOTOS) return true;
        return a.albumId().equals(b.albumId());
    }

    private static Parsed parse(String value) {
        if (value == null) return null;
        String scope = value.trim();
        if (scope.equals("camera-roll") || scope.equals(ALL_PHOTOS_SCOPE)) {
            return new Parsed(CameraBackupScopeKind.ALL_PHOTOS, null);
        }
        if (scope.startsWith(ALBUM_SCOPE_PREFIX) && scope.length() > ALBUM_SCOPE_PREFIX.length()) {
            return new Parsed(CameraBackupScopeKind.ALBUM, scope.substring(ALBUM_SCOPE_PREFIX.length()));
        }
        return null;
    }

    private record Parsed(CameraBackupScopeKind kind, String albumId) {}
}
