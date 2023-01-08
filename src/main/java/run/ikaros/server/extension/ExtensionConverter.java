package run.ikaros.server.extension;


import run.ikaros.server.store.entity.ExtensionEntity;

public interface ExtensionConverter {

    /**
     * Converts Extension to ExtensionStore.
     *
     * @param extension is an Extension to be converted.
     * @param <E> is Extension type.
     * @return an ExtensionStore.
     */
    <E> ExtensionEntity convertTo(E extension);

    /**
     * Converts Extension from ExtensionStore.
     *
     * @param type is Extension type.
     * @param extensionEntity is an ExtensionEntity
     * @param <E> is Extension type.
     * @return an Extension
     */
    <E> E convertFrom(Class<E> type, ExtensionEntity extensionEntity);

}
