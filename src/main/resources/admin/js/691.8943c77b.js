"use strict";(self["webpackChunkvue_antd_pro"]=self["webpackChunkvue_antd_pro"]||[]).push([[691],{1691:function(t,e,n){n.r(e),n.d(e,{default:function(){return f}});var r=function(){var t=this,e=t._self._c;return e("page-header-wrapper",{attrs:{title:!1}},[e("a-row",{attrs:{gutter:12,align:"middle",type:"flex"}},[e("a-col",{staticClass:"pb-3",attrs:{span:24}},[e("div",{staticClass:"table-page-search-wrapper"},[e("a-form",{attrs:{layout:"inline"}},[e("a-row",{attrs:{gutter:48}},[e("a-col",{attrs:{md:8,sm:24}},[e("a-form-item",{attrs:{label:"中文标题："}},[e("a-input",{on:{keyup:function(e){return!e.type.indexOf("key")&&t._k(e.keyCode,"enter",13,e.key,"Enter")?null:t.handleQuery()}},model:{value:t.list.params.titleCn,callback:function(e){t.$set(t.list.params,"titleCn",e)},expression:"list.params.titleCn"}})],1)],1),e("a-col",{attrs:{md:8,sm:24}},[e("a-form-item",{attrs:{label:"标题："}},[e("a-input",{on:{keyup:function(e){return!e.type.indexOf("key")&&t._k(e.keyCode,"enter",13,e.key,"Enter")?null:t.handleQuery()}},model:{value:t.list.params.title,callback:function(e){t.$set(t.list.params,"title",e)},expression:"list.params.title"}})],1)],1),e("a-col",{attrs:{md:8,sm:24}},[e("span",{staticClass:"table-page-search-submitButtons"},[e("a-space",[e("a-button",{attrs:{type:"primary"},on:{click:function(e){return t.handleQuery()}}},[t._v("查询")]),e("a-button",{on:{click:t.handleResetParam}},[t._v("重置")]),e("a-button",{on:{click:t.toAnimeSavePage}},[t._v("新增")]),e("a-button",{on:{click:function(e){t.animeAddfleetlyModal.visible=!0}}},[t._v("快速新增")])],1)],1)])],1)],1)],1)]),e("a-col",{attrs:{span:24}},[e("a-spin",{attrs:{spinning:t.list.loading}},[e("div",[e("a-row",{attrs:{gutter:16}},t._l(t.list.data,(function(n,r){return e("div",{key:r},[e("a-col",{staticClass:"col-anime-item",attrs:{xs:24,sm:12,md:8,lg:8,xl:6}},[e("a-card",{attrs:{bordered:!1,title:""===n.titleCn?n.title:n.titleCn}},[e("a",{attrs:{slot:"extra",href:"#"},on:{click:function(e){return t.handleAnimeItemClick(n)}},slot:"extra"},[t._v("编辑")]),e("img",{attrs:{slot:"cover",alt:n.originalTitle,src:n.coverUrl},on:{click:t.openAnimeDetailModal},slot:"cover"})])],1)],1)})),0)],1)])],1)],1),e("div",{staticClass:"page-wrapper"},[e("a-pagination",{staticClass:"pagination",attrs:{current:t.pagination.page,defaultPageSize:t.pagination.size,pageSizeOptions:["8","16","32","64","128"],total:t.pagination.total,showLessItems:"",showSizeChanger:""},on:{change:t.handlePageChange,showSizeChange:t.handlePageSizeChange}})],1),e("a-modal",{attrs:{title:"快速新增动漫",visible:t.animeAddfleetlyModal.visible,footer:null},on:{cancel:function(e){t.animeAddfleetlyModal.visible=!1}}},[e("a-form-model",{attrs:{layout:"inline",model:t.animeAddFleetlyForm},nativeOn:{submit:function(t){t.preventDefault()}}},[e("a-form-model-item",[e("a-select",{staticStyle:{"min-width":"150px"},attrs:{placeholder:"请选择类型"},model:{value:t.animeAddFleetlyForm.type,callback:function(e){t.$set(t.animeAddFleetlyForm,"type",e)},expression:"animeAddFleetlyForm.type"}},[e("a-select-option",{attrs:{value:"originalTitle",disabled:""}},[t._v(" 原始标题 ")]),e("a-select-option",{attrs:{value:"bgmtvId"}},[t._v(" 番组计划的条目ID ")])],1)],1),e("a-form-model-item",[e("a-input",{attrs:{placeholder:"请输入对应类型的值"},model:{value:t.animeAddFleetlyForm.value,callback:function(e){t.$set(t.animeAddFleetlyForm,"value",e)},expression:"animeAddFleetlyForm.value"}})],1),e("a-form-model-item",[e("a-button",{attrs:{type:"primary","html-type":"submit",loading:t.animeAddfleetlyModal.confirmLoading,disabled:""===t.animeAddFleetlyForm.type||""===t.animeAddFleetlyForm.value},on:{click:t.handleAnimeAddFleetlyFormSubmit}},[t._v(" 查询 ")])],1)],1)],1)],1)},a=[],i=n(6835),o=n(48534),l=(n(92222),n(37498)),s=n(26745),u={name:"AnimeList",computed:{pagination:function(){return{page:this.list.params.page,size:this.list.params.size,total:this.list.total}}},data:function(){return{list:{data:[],loading:!1,total:0,hasNext:!1,hasPrevious:!1,params:{page:1,size:8,title:void 0,originalTitle:void 0},current:{}},animeAddfleetlyModal:{visible:!1,confirmLoading:!1},animeAddFleetlyForm:{type:"bgmtvId",value:""}}},created:function(){this.handleResetParam()},methods:{handleQuery:function(){this.$log.debug("params",this.list.params),this.handlePageChange()},handleResetParam:function(){this.list.params.title=void 0,this.list.params.originalTitle=void 0,this.handlePageChange()},handlePageChange:function(){var t=arguments.length>0&&void 0!==arguments[0]?arguments[0]:1;this.list.params.page=t,this.handleListAnimes()},handlePageSizeChange:function(t,e){this.$log.debug("Current: ".concat(t,", PageSize: ").concat(e)),this.list.params.page=1,this.list.params.size=e,this.handleListFiles()},handleListAnimes:function(){var t=this;return(0,o.Z)((0,i.Z)().mark((function e(){var n;return(0,i.Z)().wrap((function(e){while(1)switch(e.prev=e.next){case 0:return e.prev=0,t.list.loading=!0,e.next=4,(0,l.tp)(t.list.params);case 4:if(n=e.sent,!(0===n.result.content.length&&t.list.params.page>0)){e.next=10;break}return t.list.params.page--,e.next=9,t.handleListAnimes();case 9:return e.abrupt("return");case 10:t.list.data=n.result.content,t.list.total=n.result.total,t.list.hasNext=n.result.hasNext,t.list.hasPrevious=n.result.hasPrevious,e.next=19;break;case 16:e.prev=16,e.t0=e["catch"](0),t.$log.error(e.t0);case 19:return e.prev=19,t.list.loading=!1,e.finish(19);case 22:case"end":return e.stop()}}),e,null,[[0,16,19,22]])})))()},handleAnimeItemClick:function(t){var e=this,n=t.id;(0,l.NC)(n).then((function(t){e.$router.push({name:"AnimeSave",params:{anime:t.result}})})).catch((function(t){e.$log.error("find animeDTO fail, err: ",t),e.$message.error("查询番剧详情失败, ID="+n)}))},toAnimeSavePage:function(){this.$router.push("/Anime/save")},handleAnimeAddFleetlyFormSubmit:function(){var t=this,e=this.animeAddFleetlyForm.type;if(""!==e){var n=this.animeAddFleetlyForm.value;if(""!==n){if("bgmtvId"===e){this.animeAddfleetlyModal.confirmLoading=!0;var r=parseInt(n);(0,s.Z)(r).then((function(e){t.animeAddfleetlyModal.confirmLoading=!1,t.animeAddfleetlyModal.visible=!1,t.handlePageChange()})).catch((function(e){t.$log.error("request bgmtv subject metadata to add Anime fleetly fail, err: ",e),t.$message.error("请求番组计划快速新增番剧失败, ID="+r),t.animeAddfleetlyModal.confirmLoading=!1}))}}else this.$message.success("请输入对应类型的值")}else this.$message.success("请选择类型")},openAnimeDetailModal:function(){this.$log.debug("run")}}},c=u,d=n(1001),h=(0,d.Z)(c,r,a,!1,null,"1d1cd66e",null),f=h.exports},37498:function(t,e,n){n.d(e,{NC:function(){return l},cK:function(){return i},tp:function(){return o}});var r=n(46945),a={basic:"/Anime",seasonWithAnimeId:"/Anime/season/animeId",listDTOS:"/Anime/dtos",findDTOById:"/Anime/dto/id"};function i(t){return(0,r.ZP)({url:a.basic,method:"put",data:t})}function o(t){return(0,r.ZP)({url:a.listDTOS,method:"get",params:t})}function l(t){return(0,r.ZP)({url:a.findDTOById+"/"+t,method:"get"})}},26745:function(t,e,n){n.d(e,{U:function(){return o},Z:function(){return i}});var r=n(46945),a={basic:"/network",reqBgmtvBangumiMetadata:"/network/metadata/bgmTv/subject",testProxyConnect:"/network/proxy/connect/test"};function i(t){return(0,r.ZP)({url:a.reqBgmtvBangumiMetadata,method:"put",params:{id:t}})}function o(){return(0,r.ZP)({url:a.testProxyConnect,method:"get"})}},48534:function(t,e,n){n.d(e,{Z:function(){return a}});n(41539);function r(t,e,n,r,a,i,o){try{var l=t[i](o),s=l.value}catch(u){return void n(u)}l.done?e(s):Promise.resolve(s).then(r,a)}function a(t){return function(){var e=this,n=arguments;return new Promise((function(a,i){var o=t.apply(e,n);function l(t){r(o,a,i,l,s,"next",t)}function s(t){r(o,a,i,l,s,"throw",t)}l(void 0)}))}}},6835:function(t,e,n){n.d(e,{Z:function(){return a}});n(82526),n(41817),n(41539),n(32165),n(78783),n(33948),n(72443),n(39341),n(73706),n(10408),n(30489),n(54747),n(68309),n(68304),n(47042);function r(t){return r="function"==typeof Symbol&&"symbol"==typeof Symbol.iterator?function(t){return typeof t}:function(t){return t&&"function"==typeof Symbol&&t.constructor===Symbol&&t!==Symbol.prototype?"symbol":typeof t},r(t)}function a(){
/*! regenerator-runtime -- Copyright (c) 2014-present, Facebook, Inc. -- license (MIT): https://github.com/facebook/regenerator/blob/main/LICENSE */
a=function(){return t};var t={},e=Object.prototype,n=e.hasOwnProperty,i="function"==typeof Symbol?Symbol:{},o=i.iterator||"@@iterator",l=i.asyncIterator||"@@asyncIterator",s=i.toStringTag||"@@toStringTag";function u(t,e,n){return Object.defineProperty(t,e,{value:n,enumerable:!0,configurable:!0,writable:!0}),t[e]}try{u({},"")}catch(C){u=function(t,e,n){return t[e]=n}}function c(t,e,n,r){var a=e&&e.prototype instanceof f?e:f,i=Object.create(a.prototype),o=new F(r||[]);return i._invoke=function(t,e,n){var r="suspendedStart";return function(a,i){if("executing"===r)throw new Error("Generator is already running");if("completed"===r){if("throw"===a)throw i;return _()}for(n.method=a,n.arg=i;;){var o=n.delegate;if(o){var l=A(o,n);if(l){if(l===h)continue;return l}}if("next"===n.method)n.sent=n._sent=n.arg;else if("throw"===n.method){if("suspendedStart"===r)throw r="completed",n.arg;n.dispatchException(n.arg)}else"return"===n.method&&n.abrupt("return",n.arg);r="executing";var s=d(t,e,n);if("normal"===s.type){if(r=n.done?"completed":"suspendedYield",s.arg===h)continue;return{value:s.arg,done:n.done}}"throw"===s.type&&(r="completed",n.method="throw",n.arg=s.arg)}}}(t,n,o),i}function d(t,e,n){try{return{type:"normal",arg:t.call(e,n)}}catch(C){return{type:"throw",arg:C}}}t.wrap=c;var h={};function f(){}function m(){}function p(){}var v={};u(v,o,(function(){return this}));var y=Object.getPrototypeOf,g=y&&y(y(P([])));g&&g!==e&&n.call(g,o)&&(v=g);var b=p.prototype=f.prototype=Object.create(v);function w(t){["next","throw","return"].forEach((function(e){u(t,e,(function(t){return this._invoke(e,t)}))}))}function x(t,e){function a(i,o,l,s){var u=d(t[i],t,o);if("throw"!==u.type){var c=u.arg,h=c.value;return h&&"object"==r(h)&&n.call(h,"__await")?e.resolve(h.__await).then((function(t){a("next",t,l,s)}),(function(t){a("throw",t,l,s)})):e.resolve(h).then((function(t){c.value=t,l(c)}),(function(t){return a("throw",t,l,s)}))}s(u.arg)}var i;this._invoke=function(t,n){function r(){return new e((function(e,r){a(t,n,e,r)}))}return i=i?i.then(r,r):r()}}function A(t,e){var n=t.iterator[e.method];if(void 0===n){if(e.delegate=null,"throw"===e.method){if(t.iterator["return"]&&(e.method="return",e.arg=void 0,A(t,e),"throw"===e.method))return h;e.method="throw",e.arg=new TypeError("The iterator does not provide a 'throw' method")}return h}var r=d(n,t.iterator,e.arg);if("throw"===r.type)return e.method="throw",e.arg=r.arg,e.delegate=null,h;var a=r.arg;return a?a.done?(e[t.resultName]=a.value,e.next=t.nextLoc,"return"!==e.method&&(e.method="next",e.arg=void 0),e.delegate=null,h):a:(e.method="throw",e.arg=new TypeError("iterator result is not an object"),e.delegate=null,h)}function k(t){var e={tryLoc:t[0]};1 in t&&(e.catchLoc=t[1]),2 in t&&(e.finallyLoc=t[2],e.afterLoc=t[3]),this.tryEntries.push(e)}function L(t){var e=t.completion||{};e.type="normal",delete e.arg,t.completion=e}function F(t){this.tryEntries=[{tryLoc:"root"}],t.forEach(k,this),this.reset(!0)}function P(t){if(t){var e=t[o];if(e)return e.call(t);if("function"==typeof t.next)return t;if(!isNaN(t.length)){var r=-1,a=function e(){for(;++r<t.length;)if(n.call(t,r))return e.value=t[r],e.done=!1,e;return e.value=void 0,e.done=!0,e};return a.next=a}}return{next:_}}function _(){return{value:void 0,done:!0}}return m.prototype=p,u(b,"constructor",p),u(p,"constructor",m),m.displayName=u(p,s,"GeneratorFunction"),t.isGeneratorFunction=function(t){var e="function"==typeof t&&t.constructor;return!!e&&(e===m||"GeneratorFunction"===(e.displayName||e.name))},t.mark=function(t){return Object.setPrototypeOf?Object.setPrototypeOf(t,p):(t.__proto__=p,u(t,s,"GeneratorFunction")),t.prototype=Object.create(b),t},t.awrap=function(t){return{__await:t}},w(x.prototype),u(x.prototype,l,(function(){return this})),t.AsyncIterator=x,t.async=function(e,n,r,a,i){void 0===i&&(i=Promise);var o=new x(c(e,n,r,a),i);return t.isGeneratorFunction(n)?o:o.next().then((function(t){return t.done?t.value:o.next()}))},w(b),u(b,s,"Generator"),u(b,o,(function(){return this})),u(b,"toString",(function(){return"[object Generator]"})),t.keys=function(t){var e=[];for(var n in t)e.push(n);return e.reverse(),function n(){for(;e.length;){var r=e.pop();if(r in t)return n.value=r,n.done=!1,n}return n.done=!0,n}},t.values=P,F.prototype={constructor:F,reset:function(t){if(this.prev=0,this.next=0,this.sent=this._sent=void 0,this.done=!1,this.delegate=null,this.method="next",this.arg=void 0,this.tryEntries.forEach(L),!t)for(var e in this)"t"===e.charAt(0)&&n.call(this,e)&&!isNaN(+e.slice(1))&&(this[e]=void 0)},stop:function(){this.done=!0;var t=this.tryEntries[0].completion;if("throw"===t.type)throw t.arg;return this.rval},dispatchException:function(t){if(this.done)throw t;var e=this;function r(n,r){return o.type="throw",o.arg=t,e.next=n,r&&(e.method="next",e.arg=void 0),!!r}for(var a=this.tryEntries.length-1;a>=0;--a){var i=this.tryEntries[a],o=i.completion;if("root"===i.tryLoc)return r("end");if(i.tryLoc<=this.prev){var l=n.call(i,"catchLoc"),s=n.call(i,"finallyLoc");if(l&&s){if(this.prev<i.catchLoc)return r(i.catchLoc,!0);if(this.prev<i.finallyLoc)return r(i.finallyLoc)}else if(l){if(this.prev<i.catchLoc)return r(i.catchLoc,!0)}else{if(!s)throw new Error("try statement without catch or finally");if(this.prev<i.finallyLoc)return r(i.finallyLoc)}}}},abrupt:function(t,e){for(var r=this.tryEntries.length-1;r>=0;--r){var a=this.tryEntries[r];if(a.tryLoc<=this.prev&&n.call(a,"finallyLoc")&&this.prev<a.finallyLoc){var i=a;break}}i&&("break"===t||"continue"===t)&&i.tryLoc<=e&&e<=i.finallyLoc&&(i=null);var o=i?i.completion:{};return o.type=t,o.arg=e,i?(this.method="next",this.next=i.finallyLoc,h):this.complete(o)},complete:function(t,e){if("throw"===t.type)throw t.arg;return"break"===t.type||"continue"===t.type?this.next=t.arg:"return"===t.type?(this.rval=this.arg=t.arg,this.method="return",this.next="end"):"normal"===t.type&&e&&(this.next=e),h},finish:function(t){for(var e=this.tryEntries.length-1;e>=0;--e){var n=this.tryEntries[e];if(n.finallyLoc===t)return this.complete(n.completion,n.afterLoc),L(n),h}},catch:function(t){for(var e=this.tryEntries.length-1;e>=0;--e){var n=this.tryEntries[e];if(n.tryLoc===t){var r=n.completion;if("throw"===r.type){var a=r.arg;L(n)}return a}}throw new Error("illegal catch attempt")},delegateYield:function(t,e,n){return this.delegate={iterator:P(t),resultName:e,nextLoc:n},"next"===this.method&&(this.arg=void 0),h}},t}}}]);