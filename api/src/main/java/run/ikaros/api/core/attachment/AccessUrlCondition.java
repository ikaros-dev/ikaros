package run.ikaros.api.core.attachment;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 附件访问地址条件参数定义.
 * 描述一个插件支持的某个可传入参数.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AccessUrlCondition {
    /**
     * 参数名称（对应conditions map的key）.
     */
    private String name;

    /**
     * 参数类型: string / boolean / integer / select.
     */
    private String type;

    /**
     * 展示标签（前端显示用）.
     */
    private String label;

    /**
     * 是否必填.
     */
    private boolean required;

    /**
     * 默认值.
     */
    private String defaultValue;

    /**
     * 参数说明.
     */
    private String description;

    /**
     * 当type=select时，可选值列表.
     */
    private List<String> options;
}
