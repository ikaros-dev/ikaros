# Ikaros 开发要点总结

## 关于添加新的初始化字段
初始化的逻辑在：`cn.liguohao.ikaros.service.OptionService.initPresetOptionItems`

添加新的初始化字段步骤：
1. 在`cn.liguohao.ikaros.common.constants.OptionConstants.Init` 中添加新的 `interface`
2. 在`cn.liguohao.ikaros.common.constants.OptionConstants.Category` 中添加新的分类名称
3. 在包`cn.liguohao.ikaros.model.option`下添加新的`Model`，需要注意的是，一个分类对应一个模型