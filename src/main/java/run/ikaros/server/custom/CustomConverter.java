package run.ikaros.server.custom;


public interface CustomConverter {

    /**
     * Converts Custom to CustomDto.
     *
     * @param custom is a Custom to be converted.
     * @param <C> is Custom type.
     * @return an CustomDto.
     * @see CustomDto
     * @see Custom
     */
    <C> CustomDto convertTo(C custom);

    /**
     * Converts Custom from ExtensionStore.
     *
     * @param customType is Custom class type.
     * @param customDto is a CustomDto
     * @param <C> is Custom class type.
     * @return a Custom
     * @see CustomDto
     * @see Custom
     */
    <C> C convertFrom(Class<C> customType, CustomDto customDto);


}
