"use strict";(self["webpackChunkvue_antd_pro"]=self["webpackChunkvue_antd_pro"]||[]).push([[535],{10910:function(e,t,i){i.d(t,{Z:function(){return v}});i(68309);var s=function(){var e=this,t=e._self._c;return t("a-modal",{attrs:{width:"70%",title:"文件详情",destroyOnClose:!0},scopedSlots:e._u([{key:"footer",fn:function(){return[e._t("extraFooter"),t("a-popconfirm",{attrs:{cancelText:"取消",okText:"确定",title:"你确定要删除该文件？"},on:{confirm:e.handleDelete}},[t("a-button",{attrs:{type:"danger",loading:e.deleting}},[e._v("删除")])],1)]},proxy:!0}],null,!0),model:{value:e.modalVisible,callback:function(t){e.modalVisible=t},expression:"modalVisible"}},[t("a-row",{attrs:{gutter:24,type:"flex"}},[t("a-col",{attrs:{lg:15,md:24,sm:24,xl:15,xs:24}},[t("div",{staticClass:"attach-detail-img pb-3"},[e.isImage?t("a",{attrs:{href:e.file.url,target:"_blank"}},[t("img",{staticClass:"file-detail-preview-img",attrs:{src:e.file.url,loading:"lazy"}})]):e.isVideo?t("VideoDPlayer",{attrs:{url:e.file.url}}):e.isVoice?t("audio",{attrs:{controls:"",src:e.file.url}},[e._v(" 您的浏览器不支持这个格式的音频 ")]):t("div",[e._v("此文件不支持预览")])],1)]),t("a-col",{attrs:{lg:9,md:24,sm:24,xl:9,xs:24}},[t("a-list",{attrs:{itemLayout:"horizontal"}},[t("a-list-item",{staticStyle:{"padding-top":"0"}},[t("a-list-item-meta",{attrs:{description:e.file.id}},[t("span",{attrs:{slot:"title"},slot:"title"},[e._v("文件ID：")])])],1),t("a-list-item",[t("a-list-item-meta",[e.editable?t("template",{slot:"description"},[t("a-input",{ref:"nameInput",on:{blur:e.handleUpdateName,pressEnter:e.handleUpdateName},model:{value:e.file.name,callback:function(t){e.$set(e.file,"name",t)},expression:"file.name"}})],1):t("template",{slot:"description"},[e._v(e._s(e.file.name))]),t("span",{attrs:{slot:"title"},slot:"title"},[e._v(" 文件名： "),t("a-button",{staticClass:"!p-0",attrs:{type:"link"},on:{click:e.handleEditName}},[t("a-icon",{attrs:{type:"edit"}})],1)],1)],2)],1),t("a-list-item",[t("a-list-item-meta",[t("span",{attrs:{slot:"title"},slot:"title"},[e._v("文件类型：")]),t("span",{attrs:{slot:"description"},slot:"description"},[e._v(" "+e._s(e._f("fileTypeText")(e.file.type)))])])],1),t("a-list-item",[t("a-list-item-meta",{attrs:{description:e._f("fileTypePlace")(e.file.place)}},[t("span",{attrs:{slot:"title"},slot:"title"},[e._v("存储位置：")])])],1),t("a-list-item",[t("a-list-item-meta",[t("template",{slot:"description"},[e._v(" "+e._s(e._f("fileSizeFormat")(e.file.size))+" ")]),t("span",{attrs:{slot:"title"},slot:"title"},[e._v("文件大小：")])],2)],1),t("a-list-item",[t("a-list-item-meta",[t("template",{slot:"description"},[e._v(" "+e._s(e._f("moment")(e.file.createTime))+" ")]),t("span",{attrs:{slot:"title"},slot:"title"},[e._v("上传日期：")])],2)],1)],1)],1)],1)],1)},n=[],r=i(6835),a=i(48534),o=(i(23157),i(30627)),l=function(){var e=this,t=e._self._c;return t("d-player",{ref:"myVideoDPlayer",attrs:{options:e.options}})},u=[],d={name:"VideoDPlayer",props:{pic:{type:String,default:""},url:{type:String,default:""}},computed:{variablyUrl:function(){return"".concat(this.url)}},data:function(){return{options:{container:{},screenshot:!0,preload:"metadata",volume:.7,video:{pic:"",url:""}}}},beforeMount:function(){this.options.video.pic=this.pic,this.options.video.url=this.url},mounted:function(){this.$emit("player",this.$refs.myVideoDPlayer.dp)},watch:{variablyUrl:function(e,t){this.reRanderVideo(e)}},methods:{reRanderVideo:function(e){this.$log.debug("newVideoUrl",e),this.$refs.myVideoDPlayer.dp.pause(),this.$refs.myVideoDPlayer.dp.switchVideo({url:e})}}},c=d,m=i(1001),f=(0,m.Z)(c,l,u,!1,null,"1aca4086",null),b=f.exports,h={name:"FileDetailModal",components:{VideoDPlayer:b},props:{visible:{type:Boolean,default:!0},file:{type:Object,default:function(){return{}}}},data:function(){return{editable:!1,deleting:!1}},computed:{modalVisible:{get:function(){return this.visible},set:function(e){this.$emit("update:visible",e)}},isImage:function(){return!(!this.file||!this.file.type)&&this.file.type.startsWith("IMAGE")},isVideo:function(){return!(!this.file||!this.file.type)&&this.file.type.startsWith("VIDEO")},isVoice:function(){return!(!this.file||!this.file.type)&&this.file.type.startsWith("VOICE")}},methods:{handleDelete:function(){var e=this;return(0,a.Z)((0,r.Z)().mark((function t(){return(0,r.Z)().wrap((function(t){while(1)switch(t.prev=t.next){case 0:return t.prev=0,e.deleting=!0,t.next=4,(0,o.Jp)(e.file.id);case 4:e.$emit("delete",e.file),e.deleteErrored=!1,e.modalVisible=!1,t.next=13;break;case 9:t.prev=9,t.t0=t["catch"](0),e.$log.error(t.t0),e.deleteErrored=!0;case 13:return t.prev=13,setTimeout((function(){e.deleting=!1}),400),t.finish(13);case 16:case"end":return t.stop()}}),t,null,[[0,9,13,16]])})))()},handleEditName:function(){var e=this;this.editable=!this.editable,this.editable&&this.$nextTick((function(){e.$refs.nameInput.focus()}))},handleUpdateName:function(){var e=this;return(0,a.Z)((0,r.Z)().mark((function t(){return(0,r.Z)().wrap((function(t){while(1)switch(t.prev=t.next){case 0:if(e.file.name){t.next=3;break}return e.$notification["error"]({message:"提示",description:"文件名称不能为空！"}),t.abrupt("return");case 3:return t.prev=3,t.next=6,(0,o.Wd)(e.file.id,e.file.name);case 6:t.next=11;break;case 8:t.prev=8,t.t0=t["catch"](3),e.$log.error(t.t0);case 11:return t.prev=11,e.editable=!1,t.finish(11);case 14:case"end":return t.stop()}}),t,null,[[3,8,11,14]])})))()},handleCopyLink:function(e){var t=this;this.$copyText(e).then((function(e){t.$log.debug("copy",e),t.$message.success("复制成功！")})).catch((function(e){t.$log.debug("copy.err",e),t.$message.error("复制失败！")}))}}},p=h,g=(0,m.Z)(p,s,n,!1,null,"416117c0",null),v=g.exports},96955:function(e,t,i){i.r(t),i.d(t,{default:function(){return k}});i(68309);var s=function(){var e=this,t=e._self._c;return t("page-header-wrapper",{attrs:{title:!1}},[t("a-row",{attrs:{gutter:12}},[t("a-col",{attrs:{xs:24,sm:12,md:8,lg:6,xl:6}},[t("img",{staticStyle:{width:"100%","border-radius":"4px"},attrs:{src:e.anime.coverUrl,alt:"anime.title"}})]),t("a-col",{attrs:{xs:24,sm:12,md:16,lg:18,xl:18}},[t("h2",[e._v(e._s(e.anime.title))]),""!==e.anime.titleCn?t("p",[e._v("中文名："+e._s(e.anime.titleCn))]):e._e(),""!==e.anime.platform?t("p",[e._v("平台："+e._s(e.anime.platform))]):e._e(),""!==e.anime.bgmtvId?t("p",[e._v(" 番组计划： "),t("a",{attrs:{href:"https://bgm.tv/subject/"+e.anime.bgmtvId,target:"_blank"}},[e._v(e._s(e.anime.bgmtvId))])]):e._e(),t("p",[e._v("简介："+e._s(e.anime.overview))]),t("div",[t("h2",[e._v("订阅")]),t("p",[e._v("状态："+e._s(e.userSubStatus)+" "),t("br"),e._v(" 进度："+e._s(e._f("userSubProgressFilter")(e.userSubProgress)))]),t("a-button",{attrs:{type:e.userSubButton.type,icon:e.userSubButton.icon,loading:e.userSubButton.loading},on:{click:e.handleUserSubButtonClick}},[e._v(e._s(e.userSubButton.value)+" ")]),e._v("   "),e.userSubButton.isSub?t("span",[t("a-radio-group",{on:{change:e.onUserSubProgressChange},model:{value:e.userSubProgress,callback:function(t){e.userSubProgress=t},expression:"userSubProgress"}},[t("a-radio-button",{attrs:{value:"WISH"}},[e._v(" 想看 ")]),t("a-radio-button",{attrs:{value:"DOING"}},[e._v(" 在看 ")]),t("a-radio-button",{attrs:{value:"DONE"}},[e._v(" 看过 ")]),t("a-radio-button",{attrs:{value:"SHELVE"}},[e._v(" 搁置 ")]),t("a-radio-button",{attrs:{value:"DISCARD"}},[e._v(" 抛弃 ")])],1)],1):e._e()],1),t("br"),e.userSubAdditional?t("div",[t("h2",[e._v("特征")]),t("a-alert",{attrs:{message:"Ikaros会筛选出与当前名称特征最近似的剧集资源文件，一般是您订阅番剧时选择的第一集",banner:"",closable:""}}),t("p",[e._v(e._s(e.userSubAdditional))])],1):e._e()])],1),t("br"),t("a-row",[t("a-col",{attrs:{span:"24"}},e._l(e.anime.seasons,(function(i){return t("div",{key:i.id},[t("h2",[e._v(" "+e._s(e._f("seasonTypeFilter")(i.type)))]),t("span",[t("strong",[e._v("原始标题")]),e._v(": "+e._s(i.title))]),t("br"),t("span",[t("strong",[e._v("中文标题")]),e._v(": "+e._s(i.titleCn))]),t("br"),t("span",[t("strong",[e._v("当前集数")]),e._v(": "+e._s(i.episodes.length))]),t("br"),t("span",[t("strong",[e._v("季度简介")]),e._v(": "+e._s(i.overview))]),t("br"),t("a-table",{attrs:{bordered:"",columns:e.episodeTableColumns,pagination:!1,"data-source":i.episodes,rowKey:function(e){return e.seq}},scopedSlots:e._u([{key:"airTimeSlot",fn:function(i){return t("span",{},[e._v(" "+e._s(e._f("moment")(i))+" ")])}},{key:"resourceSlot",fn:function(i){return t("span",{},[t("span",i?[t("a-icon",{attrs:{type:"check-circle",theme:"twoTone","two-tone-color":"#52c41a"}}),t("a",{on:{click:function(t){return e.openFileDetailModal(i)}}},[e._v(e._s(i.name))])]:[t("a-icon",{attrs:{type:"close-circle",theme:"twoTone","two-tone-color":"red"}})],1)])}},{key:"operationSlot",fn:function(i,s){return t("span",{},[t("a-button",{attrs:{type:"dashed"},on:{click:function(t){return e.handleSingleMatchingButtonClick(s)}}},[e._v(" 单个匹配 ")])],1)}}],null,!0)},[t("span",{attrs:{slot:"customOperateTitleSlot"},slot:"customOperateTitleSlot"},[t("a-button",{attrs:{type:"dashed"},on:{click:function(t){return e.handleBatchMatchingButtonClick(i.id)}}},[e._v(" 批量匹配 ")])],1)])],1)})),0)],1),t("FileDetailModal",{attrs:{addToPhoto:!0,file:e.file,visible:e.fileDetailVisible},on:{"update:visible":function(t){e.fileDetailVisible=t}}}),t("FileMatchingModal",{key:e.fileMatchingModalKey,attrs:{"receive-episode-id":e.currentEpisodeIdStr,"receive-season-id":e.currentSeasonIdStr,visible:e.fileMatchingModalVisible},on:{"update:visible":function(t){e.fileMatchingModalVisible=t},dataHasUpdated:function(t){return e.reRenderTable(e.anime.id)}}}),t("AnimeSubscribeModal",{attrs:{visible:e.animeSubscribeModalVisible,animeId:e.anime.id,additional:e.userSubAdditional},on:{"update:visible":function(t){e.animeSubscribeModalVisible=t},"update:animeId":function(t){return e.$set(e.anime,"id",t)},"update:anime-id":function(t){return e.$set(e.anime,"id",t)},userSubProgressUpdated:e.handleUserSubProgressUpdated}})],1)},n=[],r=(i(60086),i(41539),i(54747),i(39714),i(30381)),a=i.n(r),o=i(37498),l=i(10910),u=i(72030),d=i(32900),c=function(){var e=this,t=e._self._c;return t("a-modal",{attrs:{width:"70%",afterClose:e.onModalClose,title:"动漫订阅"},model:{value:e.modalVisible,callback:function(t){e.modalVisible=t},expression:"modalVisible"}},[t("div",[t("a-radio-group",{model:{value:e.subscribe.progress,callback:function(t){e.$set(e.subscribe,"progress",t)},expression:"subscribe.progress"}},[t("a-radio-button",{attrs:{value:"WISH"}},[e._v(" 想看 ")]),t("a-radio-button",{attrs:{value:"DOING"}},[e._v(" 在看 ")]),t("a-radio-button",{attrs:{value:"DONE"}},[e._v(" 看过 ")]),t("a-radio-button",{attrs:{value:"SHELVE"}},[e._v(" 搁置 ")]),t("a-radio-button",{attrs:{value:"DISCARD"}},[e._v(" 抛弃 ")])],1)],1),t("template",{slot:"footer"},[t("a-button",{key:"back",on:{click:e.handleCancel}},[e._v(" 返回 ")]),t("a-button",{key:"submit",attrs:{type:"primary",loading:e.submitButtonLoading},on:{click:e.handleOk}},[e._v(" 提交订阅 ")])],1)],2)},m=[],f=(i(9653),i(69826),i(12921)),b={name:"AnimeSubscribeModal",components:{},props:{visible:{type:Boolean,default:!1},animeId:{type:Number,default:-1},additional:{type:String,default:""}},watch:{},data:function(){return{subscribe:{animeId:this.animeId,progress:"WISH",additional:this.additional},columns:[{title:"名称",dataIndex:"title"}],list:[],selectedRowKeys:[],submitButtonLoading:!1,resourceTableLoading:!1,searchKeyword:""}},computed:{modalVisible:{get:function(){return this.visible},set:function(e){this.$emit("update:visible",e)}}},methods:{findList:function(e){var t=this;this.resourceTableLoading=!0,(0,f.vs)(e,"1").then((function(e){var i=e.result;i.forEach((function(e){e.key=e.pubDate})),t.list=i,0===i.length&&t.$message.warn("未查询到资源")})).catch((function(e){t.$log.error("find dmhy rss items fail",e)})).finally((function(){t.resourceTableLoading=!1}))},onModalClose:function(){this.$emit("animeSubscribeModalClose")},onSelectChange:function(e){this.selectedRowKeys=e,this.subscribe.additional=this.getAdditionalByKey(e)},customRowClick:function(e){var t=this;return{on:{click:function(){t.$log.debug("record",e),t.subscribe.additional=e.name,t.selectedRowKeys=[],t.selectedRowKeys.push(e.key)}}}},getAdditionalByKey:function(e){var t=this.list.find((function(t){return t.key===e}));return t.title},handleOk:function(e){var t=this,i=this.animeId;(0,d.sL)(i,this.subscribe.progress).then((function(e){e.result&&(t.$message.success("订阅成功"),t.modalVisible=!1,t.$emit("userSubProgressUpdated",t.subscribe))})).catch((function(e){t.$log.error("sub anime fail, animeId="+i+", error msg: ",e),t.$message.error("sub anime fail, animeId="+i+", error msg: ",e)})).finally((function(){t.submitButtonLoading=!1}))},handleCancel:function(e){this.modalVisible=!1},onSearch:function(e){var t=this;this.$log.debug("searchKeyword",this.searchKeyword),this.searchKeyword&&(this.resourceTableLoading=!0,(0,f.hP)(this.searchKeyword).then((function(e){var i=e.result;i.forEach((function(e){e.key=e.pubDate})),t.list=i,0===i.length&&t.$message.warn("未查询到资源")})).catch((function(e){t.$error(e),t.$message.error("查询失败，异常：",e)})).finally((function(){t.resourceTableLoading=!1})))}}},h=b,p=i(1001),g=(0,p.Z)(h,c,m,!1,null,null,null),v=g.exports,S={name:"AnimeDetail",components:{AnimeSubscribeModal:v,FileDetailModal:l.Z,FileMatchingModal:u.Z},data:function(){return{anime:{},episodes:[],currentEpisodeIdStr:"",currentSeasonIdStr:"",episodeTableColumns:[{title:"序号",dataIndex:"seq",key:"seq"},{title:"标题",dataIndex:"title",key:"title"},{title:"中文标题",dataIndex:"titleCn",key:"titleCn"},{title:"放送时间",dataIndex:"airTime",key:"airTime",scopedSlots:{customRender:"airTimeSlot"}},{title:"资源",dataIndex:"file",key:"file",scopedSlots:{customRender:"resourceSlot"}},{dataIndex:"operate",key:"operate",slots:{title:"customOperateTitleSlot"},scopedSlots:{customRender:"operationSlot"}}],fileDetailVisible:!1,fileMatchingModalVisible:!1,fileMatchingModalKey:0,file:{},userSubButton:{isSub:!1,type:"dashed",icon:"close",loading:!1,value:"订阅"},userSubStatus:"未订阅",userSubProgress:"",animeSubscribeModalVisible:!1,userSubAdditional:""}},beforeMount:function(){if(this.$router.currentRoute.params.id){var e=this.$router.currentRoute.params.id;this.getAnimeDtoById(e)}else this.$set(this,"anime",{})},methods:{getAnimeDtoById:function(e){var t=this;(0,o.NC)(e).then((function(e){var i=e.result;i.airTime=a()(i.airTime),t.updateUserSubButton(i.sub),i.subscribe&&(t.$set(t,"userSubProgress",i.subscribe.progress),t.$set(t,"userSubAdditional",i.subscribe.additional)),t.$set(t,"anime",i)})).catch((function(i){t.$log.error("find animeDTO fail, err: ",i),t.$message.error("查询番剧详情失败, ID="+e)}))},dataTableAdapter:function(e){e.forEach((function(e){e.key=e.id}))},openFileDetailModal:function(e){this.file=e,this.fileDetailVisible=!0},handleSingleMatchingButtonClick:function(e){this.currentEpisodeIdStr=e.id.toString(),this.currentSeasonIdStr="",this.fileMatchingModalVisible=!0,this.fileMatchingModalKey+=1},handleBatchMatchingButtonClick:function(e){this.currentEpisodeIdStr="",this.currentSeasonIdStr=e.toString(),this.fileMatchingModalVisible=!0,this.fileMatchingModalKey+=1},reRenderTable:function(e){this.getAnimeDtoById(e)},reloadUserSubButton:function(){this.userSubButton.isSub?(this.userSubButton.type="default",this.userSubButton.icon="close",this.userSubButton.value="取消订阅",this.userSubStatus="已订阅"):(this.userSubButton.type="dashed",this.userSubButton.icon="check",this.userSubButton.value="订阅",this.userSubStatus="未订阅",this.userSubProgress="")},updateUserSubButton:function(e){this.userSubButton.isSub=e,this.reloadUserSubButton()},handleUserSubButtonClick:function(){this.userSubButton.isSub?this.cancelUserSub():this.addUserSub()},addUserSub:function(){this.animeSubscribeModalVisible=!0},cancelUserSub:function(){var e=this,t=this.anime.id;this.userSubButton.loading=!0,(0,d.Xf)(t).then((function(t){t.result&&(e.$message.success("取消订阅成功"),e.updateUserSubButton(!1),e.userSubProgress="",e.userSubAdditional="")})).catch((function(i){e.$log.error("sub anime fail, animeId="+t+", error msg: ",i),e.$message.error("sub anime fail, animeId="+t+", error msg: ",i)})).finally((function(){e.userSubButton.loading=!1}))},handleUserSubProgressUpdated:function(e){this.userSubProgress=e.progress,this.userSubAdditional=e.additional,this.updateUserSubButton(!0)},onUserSubProgressChange:function(){var e=this;(0,d.sL)(this.anime.id,this.userSubProgress).then((function(t){t.result?(e.$message.success("更新订阅进度成功"),e.$log.debug("update sub progress success")):(e.$message.warn("更新订阅进度失败"),e.$log.warn("update sub progress fail"))})).catch((function(t){e.$message.error("更新订阅进度失败, 异常信息：",t),e.$log.error("update sub progress fail, err: ",t)}))}}},y=S,_=(0,p.Z)(y,s,n,!1,null,"de5ecb68",null),k=_.exports},12921:function(e,t,i){i.d(t,{Zv:function(){return a},hP:function(){return l},nG:function(){return r},vs:function(){return o}});var s=i(46945),n={testQbittorrentConnect:"/tripartite/qbittorrent/connect/test",getBgmTvMe:"/tripartite/bgmtv/token/user/me",findDmhyRssItemsByAnimeId:"/tripartite/dmhy/rss/items/anime",findDmhyRssItems:"/tripartite/dmhy/rss/items"};function r(){return(0,s.ZP)({url:n.testQbittorrentConnect,mentions:"get"})}function a(){return(0,s.ZP)({url:n.getBgmTvMe,method:"get"})}function o(e,t){return(0,s.ZP)({url:n.findDmhyRssItemsByAnimeId+"/"+e,method:"get",params:{seq:t}})}function l(e){return(0,s.ZP)({url:n.findDmhyRssItems,method:"get",params:{keyword:e}})}}}]);