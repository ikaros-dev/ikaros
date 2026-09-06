alter table resource add constraint resource_type_ck
    check (resource_type in ('VIDEO', 'COMIC', 'BOOK', 'MUSIC', 'PHOTO', 'ARTICLE', 'DOCUMENT', 'GAME', 'ARCHIVE', 'OTHER'));

alter table resource add constraint resource_lifecycle_ck
    check (lifecycle in ('ACTIVE', 'ARCHIVED', 'TRASHED'));

alter table blob add constraint blob_availability_ck
    check (availability in ('AVAILABLE', 'REMOTE', 'PROCESSING', 'RESTORING', 'MISSING', 'CORRUPTED'));

alter table attachment add constraint attachment_kind_ck
    check (attachment_kind in ('ORIGINAL', 'DERIVED', 'COVER', 'SUBTITLE'));

alter table blob_placement add constraint blob_placement_state_ck
    check (placement_state in ('ACTIVE', 'VERIFYING', 'UNAVAILABLE', 'DELETING'));
