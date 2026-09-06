ALTER TABLE drive_sync_binding
    ADD CONSTRAINT fk_drive_sync_binding_device FOREIGN KEY (device_id) REFERENCES drive_device(id);
