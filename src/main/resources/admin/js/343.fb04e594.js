"use strict";(self["webpackChunkvue_antd_pro"]=self["webpackChunkvue_antd_pro"]||[]).push([[343],{72030:function(e,t,i){i.d(t,{Z:function(){return g}});var a=function(){var e=this,t=e._self._c;return t("a-modal",{attrs:{destroyOnClose:"",width:"65%",title:"文件匹配"},model:{value:e.modalVisible,callback:function(t){e.modalVisible=t},expression:"modalVisible"}},[t("a-row",{attrs:{gutter:12,align:"middle",type:"flex"}},[t("a-col",{staticClass:"pb-3",attrs:{span:24}},[t("div",{staticClass:"table-page-search-wrapper"},[t("a-form",{attrs:{layout:"inline"}},[t("a-row",{attrs:{gutter:48}},[t("a-col",{attrs:{md:6,sm:24}},[t("a-form-item",{attrs:{label:"关键词："}},[t("a-input",{attrs:{placeholder:"请输入文件名称关键词"},on:{keyup:function(t){return!t.type.indexOf("key")&&e._k(t.keyCode,"enter",13,t.key,"Enter")?null:e.handleQuery()}},model:{value:e.list.params.keyword,callback:function(t){e.$set(e.list.params,"keyword",t)},expression:"list.params.keyword"}})],1)],1),t("a-col",{attrs:{md:6,sm:24}},[t("a-form-item",{attrs:{label:"存储位置："}},[t("a-select",{attrs:{loading:e.places.loading,allowClear:""},on:{change:function(t){return e.handleQuery()}},model:{value:e.list.params.place,callback:function(t){e.$set(e.list.params,"place",t)},expression:"list.params.place"}},e._l(e.places.data,(function(i){return t("a-select-option",{key:i,attrs:{value:i}},[e._v(" "+e._s(e._f("fileTypePlace")(i))+" ")])})),1)],1)],1),t("a-col",{attrs:{md:6,sm:24}},[t("a-form-item",{attrs:{label:"文件类型："}},[t("a-select",{attrs:{loading:e.types.loading,allowClear:""},on:{change:function(t){return e.handleQuery()}},model:{value:e.list.params.type,callback:function(t){e.$set(e.list.params,"type",t)},expression:"list.params.type"}},e._l(e.types.data,(function(i,a){return t("a-select-option",{key:a,attrs:{value:i}},[e._v(e._s(e._f("fileTypeText")(i))+" ")])})),1)],1)],1),t("a-col",{attrs:{md:6,sm:24}},[t("span",{staticClass:"table-page-search-submitButtons"},[t("a-space",[t("a-button",{attrs:{type:"primary"},on:{click:function(t){return e.handleQuery()}}},[e._v("查询")]),t("a-button",{on:{click:function(t){return e.handleResetParam()}}},[e._v("重置")]),t("a-button",{attrs:{icon:"cloud-upload",type:"primary"},on:{click:function(t){e.upload.visible=!0}}},[e._v("上传")])],1)],1)])],1)],1)],1)]),t("a-col",{attrs:{span:24}},[t("a-table",{attrs:{"row-selection":{selectedRowKeys:e.fileTableSelectedRowKeys,onChange:e.fileTableOnSelectChange,onSelect:e.fileTableOnSingleRowSelect},columns:e.fileTableColumns,"data-source":e.list.data,pagination:!1,size:"small"}})],1)],1),t("div",{staticClass:"page-wrapper"},[t("a-pagination",{staticClass:"pagination",attrs:{current:e.pagination.page,defaultPageSize:e.pagination.size,pageSizeOptions:["12","24","36","48","72"],total:e.pagination.total,showLessItems:"",showSizeChanger:""},on:{change:e.handlePageChange,showSizeChange:e.handlePageSizeChange}})],1),t("template",{slot:"footer"},[t("a-button",{attrs:{type:"primary",disabled:e.copyFieldIdButtonDisable},on:{click:e.handleCopyFieldIdButtonClick}},[e._v("复制ID")]),t("a-button",{attrs:{type:"primary",disabled:e.copyFieldUrlButtonDisable},on:{click:e.handleCopyFieldUrlButtonClick}},[e._v("复制URL")]),t("a-button",{attrs:{type:"primary",loading:e.batchMatchingButtonLoading,disabled:e.batchMatchingButtonDisable},on:{click:e.handleBatchMatchingClick}},[e._v("批量匹配")]),t("a-button",{attrs:{type:"primary",loading:e.singleMatchingButtonLoading,disabled:e.singleMatchingButtonDisable},on:{click:e.handleSingleMatchingClick}},[e._v("单个匹配")])],1),t("FileUploadModal",{attrs:{visible:e.upload.visible},on:{"update:visible":function(t){return e.$set(e.upload,"visible",t)},fileUploadModalClose:e.onFileUploadModalClose}})],2)},n=[],s=i(6835),l=i(48534),o=(i(34553),i(41539),i(54747),i(92222),i(57327),i(86700)),r=i(30627),d=i(51274),c={name:"FileMatchingModal",components:{FileUploadModal:o.Z},props:{visible:{type:Boolean,default:!1},receiveSeasonId:{type:String},receiveEpisodeId:{type:String},copyFieldId:{type:Boolean,default:!1},copyFieldUrl:{type:Boolean,default:!1}},beforeMount:function(){this.receiveSeasonId&&(this.seasonId=this.receiveSeasonId,this.batchMatchingButtonDisable=!1),this.receiveEpisodeId&&(this.episodeId=this.receiveEpisodeId,this.singleMatchingButtonDisable=!1,this.singleSelectMode=!0),this.copyFieldId&&(this.singleSelectMode=!0,this.copyFieldIdButtonDisable=!1),this.copyFieldUrl&&(this.singleSelectMode=!0,this.copyFieldUrlButtonDisable=!1)},computed:{modalVisible:{get:function(){return this.visible},set:function(e){this.$emit("update:visible",e)}},pagination:function(){return{page:this.list.params.page,size:this.list.params.size,total:this.list.total}},selectPreviousButtonDisabled:function(){var e=this,t=this.list.data.findIndex((function(t){return t.id===e.list.current.id}));return 0===t&&!this.list.hasPrevious},selectNextButtonDisabled:function(){var e=this,t=this.list.data.findIndex((function(t){return t.id===e.list.current.id}));return t===this.list.data.length-1&&!this.list.hasNext}},data:function(){return{fileTableColumns:[{title:"名称",dataIndex:"name"},{title:"URL",dataIndex:"url"}],seasonId:"",episodeId:"",fileTableSelectedRowKeys:[],list:{data:[],loading:!1,total:0,hasNext:!1,hasPrevious:!1,params:{page:1,size:12,keyword:void 0,type:void 0,place:void 0},current:{}},types:{data:[],loading:!1},places:{data:[],loading:!1},upload:{visible:!1},batchMatchingButtonLoading:!1,batchMatchingButtonDisable:!0,singleMatchingButtonLoading:!1,singleMatchingButtonDisable:!0,singleSelectMode:!1,copyFieldIdButtonDisable:!0,copyFieldUrlButtonDisable:!0}},created:function(){this.handleResetParam()},methods:{handleResetParam:function(){this.list.params.keyword=void 0,this.list.params.type=void 0,this.list.params.place=void 0,this.handlePageChange(),this.handleListTypes(),this.handleListPlaces()},handleListFiles:function(){var e=this;return(0,l.Z)((0,s.Z)().mark((function t(){var i;return(0,s.Z)().wrap((function(t){while(1)switch(t.prev=t.next){case 0:return t.prev=0,e.list.loading=!0,t.next=4,(0,r.bc)(e.list.params);case 4:if(i=t.sent,!(0===i.result.content.length&&e.list.params.page>0)){t.next=10;break}return e.list.params.page--,t.next=9,e.handleListFiles();case 9:return t.abrupt("return");case 10:e.list.data=i.result.content,e.list.data.forEach((function(e){e.key=e.id})),e.list.total=i.result.total,e.list.hasNext=i.result.hasNext,e.list.hasPrevious=i.result.hasPrevious,t.next=20;break;case 17:t.prev=17,t.t0=t["catch"](0),e.$log.error(t.t0);case 20:return t.prev=20,e.list.loading=!1,t.finish(20);case 23:case"end":return t.stop()}}),t,null,[[0,17,20,23]])})))()},handleQuery:function(){this.$log.debug("params",this.list.params),this.handlePageChange()},handlePageChange:function(){var e=arguments.length>0&&void 0!==arguments[0]?arguments[0]:1;this.list.params.page=e,this.handleListFiles()},handlePageSizeChange:function(e,t){this.$log.debug("Current: ".concat(e,", PageSize: ").concat(t)),this.list.params.page=1,this.list.params.size=t,this.handleListFiles()},handleListTypes:function(){var e=this;return(0,l.Z)((0,s.Z)().mark((function t(){var i;return(0,s.Z)().wrap((function(t){while(1)switch(t.prev=t.next){case 0:return t.prev=0,e.types.loading=!0,t.next=4,(0,r.Z9)();case 4:i=t.sent,e.types.data=i.result,t.next=11;break;case 8:t.prev=8,t.t0=t["catch"](0),e.$log.error(t.t0);case 11:return t.prev=11,e.types.loading=!1,t.finish(11);case 14:case"end":return t.stop()}}),t,null,[[0,8,11,14]])})))()},handleListPlaces:function(){var e=this;return(0,l.Z)((0,s.Z)().mark((function t(){var i;return(0,s.Z)().wrap((function(t){while(1)switch(t.prev=t.next){case 0:return t.prev=0,e.places.loading=!0,t.next=4,(0,r.U4)();case 4:i=t.sent,e.places.data=i.result,t.next=11;break;case 8:t.prev=8,t.t0=t["catch"](0),e.$log.error(t.t0);case 11:return t.prev=11,e.places.loading=!1,t.finish(11);case 14:case"end":return t.stop()}}),t,null,[[0,8,11,14]])})))()},onFileUploadModalClose:function(){this.handlePageChange(),this.handleListTypes(),this.handleListPlaces()},handleFileItemClick:function(e){this.isItemSelect(e)?this.list.current={}:this.list.current=e},closeFileMathcingModal:function(){this.$log.debug("run"),this.modalVisible=!1},fileTableOnSelectChange:function(e){this.fileTableSelectedRowKeys=e},fileTableOnSingleRowSelect:function(e){this.singleSelectMode&&(this.fileTableSelectedRowKeys=[e.id])},handleBatchMatchingClick:function(){var e=this;this.seasonId?(this.batchMatchingButtonLoading=!0,(0,d.q3)(this.seasonId,this.fileTableSelectedRowKeys).then((function(t){e.$message.success("匹配成功"),e.$emit("dataHasUpdated"),e.modalVisible=!1})).catch((function(t){e.$message.error("匹配失败, 错误信息:"+t),e.$log.debug("matching episode url by file ids fail, error:",t)})).finally((function(){e.batchMatchingButtonLoading=!1}))):this.$message.error("季度ID不存在，取消批量匹配操作")},handleSingleMatchingClick:function(){var e=this;if(this.episodeId)if(1===this.fileTableSelectedRowKeys.length){this.singleMatchingButtonLoading=!0;var t=this.fileTableSelectedRowKeys[0];this.$log.debug("fileId",t),(0,d.ZO)(this.episodeId,t).then((function(t){e.$message.success("匹配成功"),e.$emit("dataHasUpdated"),e.modalVisible=!1})).catch((function(t){e.$message.error("匹配失败, 错误信息:"+t),e.$log.error("matching episode url by file id fail, error:",t)})).finally((function(){e.singleMatchingButtonLoading=!1}))}else this.$message.error("选中了多个文件，操作取消，单个匹配只能选中一个文件");else this.$message.error("剧集ID不存在，取消单个匹配操作")},getListDataFileById:function(e){var t=this.list.data.filter((function(t){return t.id===e}));return t[0]},handleCopyFieldIdButtonClick:function(){1===this.fileTableSelectedRowKeys.length?(this.$emit("sendSelectedFileFieldValue",this.fileTableSelectedRowKeys[0]),this.modalVisible=!1):this.$message.error("未选择或选择了多个, 操作取消, 请只选择一个")},handleCopyFieldUrlButtonClick:function(){if(1===this.fileTableSelectedRowKeys.length){var e=this.getListDataFileById(this.fileTableSelectedRowKeys[0]);this.$log.debug("file",e),this.$emit("sendSelectedFileFieldValue",e.url),this.modalVisible=!1}else this.$message.error("未选择或选择了多个, 操作取消, 请只选择一个")}}},u=c,h=i(1001),p=(0,h.Z)(u,a,n,!1,null,"41014814",null),g=p.exports},37498:function(e,t,i){i.d(t,{NC:function(){return u},Sz:function(){return p},cK:function(){return d},ne:function(){return h},tp:function(){return c}});var a=i(46945),n=i(98269),s=i.n(n),l=i(55743),o=i.n(l),r={basic:"/anime",seasonWithAnimeId:"/anime/season/animeId",listDTOS:"/anime/dtos",findDTOById:"/anime/dto/id",deleteAnimeById:"/anime/id",deleteWithBatchByIds:"anime/ids"};function d(e){return(0,a.ZP)({url:r.basic,method:"put",data:e})}function c(e){return(0,a.ZP)({url:r.listDTOS,method:"get",params:e})}function u(e){return(0,a.ZP)({url:r.findDTOById+"/"+e,method:"get"})}function h(e){return(0,a.ZP)({url:r.deleteAnimeById+"/"+e,method:"delete"})}function p(e){var t=JSON.stringify(e),i=s().stringify(o().parse(t));return(0,a.ZP)({url:r.deleteWithBatchByIds+"/"+i,method:"delete"})}},51274:function(e,t,i){i.d(t,{Ee:function(){return o},Rl:function(){return l},Uc:function(){return s},ZO:function(){return d},q3:function(){return r}});var a=i(46945),n={basic:"/season",types:"/season/types",matchingEpisodesUrlByFileIds:"/season/matching/episodes",matchingEpisodeUrlByFileId:"/season/matching/episode"};function s(e){return(0,a.ZP)({url:n.basic,method:"delete",params:{id:e}})}function l(e){return(0,a.ZP)({url:n.basic,method:"put",data:e})}function o(){return(0,a.ZP)({url:n.types,method:"get"})}function r(e,t){return(0,a.ZP)({url:n.matchingEpisodesUrlByFileIds,method:"put",data:{seasonId:e,fileIdList:t}})}function d(e,t){return(0,a.ZP)({url:n.matchingEpisodeUrlByFileId,method:"put",data:{episodeId:e,fileId:t}})}}}]);