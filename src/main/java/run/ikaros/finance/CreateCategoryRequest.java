package run.ikaros.finance; import jakarta.validation.constraints.NotBlank; import java.util.UUID; public record CreateCategoryRequest(@NotBlank String name,String kind,UUID parentId) {}
