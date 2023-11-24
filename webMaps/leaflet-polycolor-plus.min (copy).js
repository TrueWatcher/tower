!function(){"use strict";var globals="undefined"==typeof global?self:global;if("function"!=typeof globals.require){var modules={},cache={},aliases={},has={}.hasOwnProperty,expRe=/^\.\.?(\/|$)/,expand=function(root,name){for(var part,results=[],parts=(expRe.test(name)?root+"/"+name:name).split("/"),i=0,length=parts.length;i<length;i++)part=parts[i],".."===part?results.pop():"."!==part&&""!==part&&results.push(part);return results.join("/")},dirname=function(path){return path.split("/").slice(0,-1).join("/")},localRequire=function(path){return function(name){var absolute=expand(dirname(path),name);return globals.require(absolute,path)}},initModule=function(name,definition){var hot=hmr&&hmr.createHot(name),module={id:name,exports:{},hot:hot};return cache[name]=module,definition(module.exports,localRequire(name),module),module.exports},expandAlias=function(name){return aliases[name]?expandAlias(aliases[name]):name},_resolve=function(name,dep){return expandAlias(expand(dirname(name),dep))},require=function(name,loaderPath){null==loaderPath&&(loaderPath="/");var path=expandAlias(name);if(has.call(cache,path))return cache[path].exports;if(has.call(modules,path))return initModule(path,modules[path]);throw new Error("Cannot find module '"+name+"' from '"+loaderPath+"'")};require.alias=function(from,to){aliases[to]=from};var extRe=/\.[^.\/]+$/,indexRe=/\/index(\.[^\/]+)?$/,addExtensions=function(bundle){if(extRe.test(bundle)){var alias=bundle.replace(extRe,"");has.call(aliases,alias)&&aliases[alias].replace(extRe,"")!==alias+"/index"||(aliases[alias]=bundle)}if(indexRe.test(bundle)){var iAlias=bundle.replace(indexRe,"");has.call(aliases,iAlias)||(aliases[iAlias]=bundle)}};require.register=require.define=function(bundle,fn){if(bundle&&"object"==typeof bundle)for(var key in bundle)has.call(bundle,key)&&require.register(key,bundle[key]);else modules[bundle]=fn,delete cache[bundle],addExtensions(bundle)},require.list=function(){var list=[];for(var item in modules)has.call(modules,item)&&list.push(item);return list};var hmr=globals._hmr&&new globals._hmr(_resolve,require,modules,cache);require._cache=cache,require.hmr=hmr&&hmr.wrap,require.brunch=!0,globals.require=require}}(),function(){"undefined"==typeof window?this:window;require.register("leaflet-polycolor.js",function(exports,require,module){"use strict";Object.defineProperty(exports,"__esModule",{value:!0}), 
                                                                                                                                                                                                                                                                exports["default"]=function(L){
                                                                                                                                                                                                                                                                var Renderer=L.Renderer.RendererGradient=L.Canvas.extend({
                                                                                                                                                                                                                                                                _updatePoly:function(layer){ var options=layer.options;if(this._drawing){var i=void 0,j=void 0,len2=void 0,p=void 0,prev=void 0,parts=layer._parts,len=parts.length,serialCount=-1,ctx=this._ctx;if(len){if(this._layers[layer._leaflet_id]=layer,options.stroke&&0!==options.weight)for(i=0;i<len;i++,serialCount+=1)for(j=0,len2=parts[i].length-1;j<len2;j++)p=parts[i][j+1],prev=parts[i][j],serialCount+=1,ctx.beginPath(),ctx.moveTo(prev.x,prev.y),ctx.lineTo(p.x,p.y),this._stroke(ctx,layer,prev,p,serialCount);if(options.fill){for(ctx.beginPath(),i=0;i<len;i++)for(j=0,len2=parts[i].length-1;j<len2;j++)p=parts[i][j+1],prev=parts[i][j],0===j&&ctx.moveTo(prev.x,prev.y),ctx.lineTo(p.x,p.y);this._fill(ctx,layer,prev,p,j)}}}},
                                                                                                                                                                                                                                                                _fill:function(ctx,layer,prev,p,j){ var options=layer.options;options.fill&&(ctx.globalAlpha=options.fillOpacity,ctx.fillStyle=options.fillColor||options.color,ctx.fill(options.fillRule||"evenodd"))},
                                                                                                                                                                                                                                                                _stroke:function(ctx,layer,prev,p,j){
  var options=layer.options,dashed=options.breaks[j+1]?[options.weight,options.weight*2]:[];
  options.stroke&&0!==options.weight&&(ctx.setLineDash&&ctx.setLineDash(layer.options&&layer.options._dashArray||dashed||[]),ctx.globalAlpha=options.opacity,ctx.lineWidth=options.weight,ctx.strokeStyle=this._getStrokeGradient(ctx,layer,prev,p,j),ctx.lineCap=options.lineCap,ctx.lineJoin=options.lineJoin,ctx.stroke(),ctx.closePath())},
                                                                                                                                                                                                                                                                _getStrokeGradient:function(ctx,layer,prev,p,j){ 
  var options=layer.options,
  gradient=ctx.createLinearGradient(prev.x,prev.y,p.x,p.y), gradientStartRGB=options.colors[j]||options.color, gradientEndRGB=options.colors[j+1]||options.color;
  return gradient.addColorStop(0,gradientStartRGB),gradient.addColorStop(1,gradientEndRGB),gradient}
  
}),
Polycolor=L.Polycolor=L.Polyline.extend({
  
_colorParts:[],
options:{colors:[],renderer:new Renderer},
initialize:function(latlngs,options){L.Util.setOptions(this,options),this._setLatLngs(latlngs),this._colorParts=[]},
_clipPoints:function(){
  var bounds=this._renderer._bounds;
  this._parts=[],this._colorParts=[],this._pxBounds&&this._pxBounds.intersects(bounds)&&(this._parts=this._rings,this._colorParts=this.options.colors)},
_update:function(){this._map&&(this._clipPoints(),this._updatePath())}});
return L.polycolor=function(latlngs,options){return new L.Polycolor(latlngs,options)},Polycolor                                                                                                                                                                                                                                                                
                                                                                                                                                                                                                                                            }
  
}),require.register("___globals___",function(exports,require,module){})}(),require("___globals___");
