(()=>{"use strict";function t(t,n,i){var r=n[1],s=n[0],e=r-s;return t===r&&i?t:((t-s)%e+e)%e+s}function n(t,n){if(!1===n)return t;var i=Math.pow(10,void 0===n?6:n);return Math.round(t*i)/i}Object.create;var i=Array.isArray||function(t){return"[object Array]"===Object.prototype.toString.call(t)};function r(t){return window["webkit"+t]||window["moz"+t]||window["ms"+t]}function s(t,n,i){this.x=i?Math.round(t):t,this.y=i?Math.round(n):n}window.requestAnimationFrame||r("RequestAnimationFrame"),window.cancelAnimationFrame||r("CancelAnimationFrame")||r("CancelRequestAnimationFrame");var e=Math.trunc||function(t){return t>0?Math.floor(t):Math.ceil(t)};function a(t,n,r){return t instanceof s?t:i(t)?new s(t[0],t[1]):null==t?t:"object"==typeof t&&"x"in t&&"y"in t?new s(t.x,t.y):new s(t,n,r)}function o(t,n){if(t)for(var i=n?[t,n]:t,r=0,s=i.length;r<s;r++)this.extend(i[r])}function h(t,n){return!t||t instanceof o?t:new o(t,n)}function u(t,n){if(t)for(var i=n?[t,n]:t,r=0,s=i.length;r<s;r++)this.extend(i[r])}function l(t,n){return t instanceof u?t:new u(t,n)}s.prototype={clone:function(){return new s(this.x,this.y)},add:function(t){return this.clone()._add(a(t))},_add:function(t){return this.x+=t.x,this.y+=t.y,this},subtract:function(t){return this.clone()._subtract(a(t))},_subtract:function(t){return this.x-=t.x,this.y-=t.y,this},divideBy:function(t){return this.clone()._divideBy(t)},_divideBy:function(t){return this.x/=t,this.y/=t,this},multiplyBy:function(t){return this.clone()._multiplyBy(t)},_multiplyBy:function(t){return this.x*=t,this.y*=t,this},scaleBy:function(t){return new s(this.x*t.x,this.y*t.y)},unscaleBy:function(t){return new s(this.x/t.x,this.y/t.y)},round:function(){return this.clone()._round()},_round:function(){return this.x=Math.round(this.x),this.y=Math.round(this.y),this},floor:function(){return this.clone()._floor()},_floor:function(){return this.x=Math.floor(this.x),this.y=Math.floor(this.y),this},ceil:function(){return this.clone()._ceil()},_ceil:function(){return this.x=Math.ceil(this.x),this.y=Math.ceil(this.y),this},trunc:function(){return this.clone()._trunc()},_trunc:function(){return this.x=e(this.x),this.y=e(this.y),this},distanceTo:function(t){var n=(t=a(t)).x-this.x,i=t.y-this.y;return Math.sqrt(n*n+i*i)},equals:function(t){return(t=a(t)).x===this.x&&t.y===this.y},contains:function(t){return t=a(t),Math.abs(t.x)<=Math.abs(this.x)&&Math.abs(t.y)<=Math.abs(this.y)},toString:function(){return"Point("+n(this.x)+", "+n(this.y)+")"}},o.prototype={extend:function(t){var n,i;if(!t)return this;if(t instanceof s||"number"==typeof t[0]||"x"in t)n=i=a(t);else if(n=(t=h(t)).min,i=t.max,!n||!i)return this;return this.min||this.max?(this.min.x=Math.min(n.x,this.min.x),this.max.x=Math.max(i.x,this.max.x),this.min.y=Math.min(n.y,this.min.y),this.max.y=Math.max(i.y,this.max.y)):(this.min=n.clone(),this.max=i.clone()),this},getCenter:function(t){return a((this.min.x+this.max.x)/2,(this.min.y+this.max.y)/2,t)},getBottomLeft:function(){return a(this.min.x,this.max.y)},getTopRight:function(){return a(this.max.x,this.min.y)},getTopLeft:function(){return this.min},getBottomRight:function(){return this.max},getSize:function(){return this.max.subtract(this.min)},contains:function(t){var n,i;return(t="number"==typeof t[0]||t instanceof s?a(t):h(t))instanceof o?(n=t.min,i=t.max):n=i=t,n.x>=this.min.x&&i.x<=this.max.x&&n.y>=this.min.y&&i.y<=this.max.y},intersects:function(t){t=h(t);var n=this.min,i=this.max,r=t.min,s=t.max,e=s.x>=n.x&&r.x<=i.x,a=s.y>=n.y&&r.y<=i.y;return e&&a},overlaps:function(t){t=h(t);var n=this.min,i=this.max,r=t.min,s=t.max,e=s.x>n.x&&r.x<i.x,a=s.y>n.y&&r.y<i.y;return e&&a},isValid:function(){return!(!this.min||!this.max)},pad:function(t){var n=this.min,i=this.max,r=Math.abs(n.x-i.x)*t,s=Math.abs(n.y-i.y)*t;return h(a(n.x-r,n.y-s),a(i.x+r,i.y+s))},equals:function(t){return!!t&&(t=h(t),this.min.equals(t.getTopLeft())&&this.max.equals(t.getBottomRight()))}},u.prototype={extend:function(t){var n,i,r=this._southWest,s=this._northEast;if(t instanceof f)n=t,i=t;else{if(!(t instanceof u))return t?this.extend(g(t)||l(t)):this;if(n=t._southWest,i=t._northEast,!n||!i)return this}return r||s?(r.lat=Math.min(n.lat,r.lat),r.lng=Math.min(n.lng,r.lng),s.lat=Math.max(i.lat,s.lat),s.lng=Math.max(i.lng,s.lng)):(this._southWest=new f(n.lat,n.lng),this._northEast=new f(i.lat,i.lng)),this},pad:function(t){var n=this._southWest,i=this._northEast,r=Math.abs(n.lat-i.lat)*t,s=Math.abs(n.lng-i.lng)*t;return new u(new f(n.lat-r,n.lng-s),new f(i.lat+r,i.lng+s))},getCenter:function(){return new f((this._southWest.lat+this._northEast.lat)/2,(this._southWest.lng+this._northEast.lng)/2)},getSouthWest:function(){return this._southWest},getNorthEast:function(){return this._northEast},getNorthWest:function(){return new f(this.getNorth(),this.getWest())},getSouthEast:function(){return new f(this.getSouth(),this.getEast())},getWest:function(){return this._southWest.lng},getSouth:function(){return this._southWest.lat},getEast:function(){return this._northEast.lng},getNorth:function(){return this._northEast.lat},contains:function(t){t="number"==typeof t[0]||t instanceof f||"lat"in t?g(t):l(t);var n,i,r=this._southWest,s=this._northEast;return t instanceof u?(n=t.getSouthWest(),i=t.getNorthEast()):n=i=t,n.lat>=r.lat&&i.lat<=s.lat&&n.lng>=r.lng&&i.lng<=s.lng},intersects:function(t){t=l(t);var n=this._southWest,i=this._northEast,r=t.getSouthWest(),s=t.getNorthEast(),e=s.lat>=n.lat&&r.lat<=i.lat,a=s.lng>=n.lng&&r.lng<=i.lng;return e&&a},overlaps:function(t){t=l(t);var n=this._southWest,i=this._northEast,r=t.getSouthWest(),s=t.getNorthEast(),e=s.lat>n.lat&&r.lat<i.lat,a=s.lng>n.lng&&r.lng<i.lng;return e&&a},toBBoxString:function(){return[this.getWest(),this.getSouth(),this.getEast(),this.getNorth()].join(",")},equals:function(t,n){return!!t&&(t=l(t),this._southWest.equals(t.getSouthWest(),n)&&this._northEast.equals(t.getNorthEast(),n))},isValid:function(){return!(!this._southWest||!this._northEast)}};var c=function(t){var n,i,r,s;for(i=1,r=arguments.length;i<r;i++)for(n in s=arguments[i])t[n]=s[n];return t}({},{latLngToPoint:function(t,n){var i=this.projection.project(t),r=this.scale(n);return this.transformation._transform(i,r)},pointToLatLng:function(t,n){var i=this.scale(n),r=this.transformation.untransform(t,i);return this.projection.unproject(r)},project:function(t){return this.projection.project(t)},unproject:function(t){return this.projection.unproject(t)},scale:function(t){return 256*Math.pow(2,t)},zoom:function(t){return Math.log(t/256)/Math.LN2},getProjectedBounds:function(t){if(this.infinite)return null;var n=this.projection.bounds,i=this.scale(t);return new o(this.transformation.transform(n.min,i),this.transformation.transform(n.max,i))},infinite:!1,wrapLatLng:function(n){var i=this.wrapLng?t(n.lng,this.wrapLng,!0):n.lng;return new f(this.wrapLat?t(n.lat,this.wrapLat,!0):n.lat,i,n.alt)},wrapLatLngBounds:function(t){var n=t.getCenter(),i=this.wrapLatLng(n),r=n.lat-i.lat,s=n.lng-i.lng;if(0===r&&0===s)return t;var e=t.getSouthWest(),a=t.getNorthEast();return new u(new f(e.lat-r,e.lng-s),new f(a.lat-r,a.lng-s))}},{wrapLng:[-180,180],R:6371e3,distance:function(t,n){var i=Math.PI/180,r=t.lat*i,s=n.lat*i,e=Math.sin((n.lat-t.lat)*i/2),a=Math.sin((n.lng-t.lng)*i/2),o=e*e+Math.cos(r)*Math.cos(s)*a*a,h=2*Math.atan2(Math.sqrt(o),Math.sqrt(1-o));return this.R*h}});function f(t,n,i){if(isNaN(t)||isNaN(n))throw new Error("Invalid LatLng object: ("+t+", "+n+")");this.lat=+t,this.lng=+n,void 0!==i&&(this.alt=+i)}function g(t,n,r){return t instanceof f?t:i(t)&&"object"!=typeof t[0]?3===t.length?new f(t[0],t[1],t[2]):2===t.length?new f(t[0],t[1]):null:null==t?t:"object"==typeof t&&"lat"in t?new f(t.lat,"lng"in t?t.lng:t.lon,t.alt):void 0===n?null:new f(t,n,r)}f.prototype={equals:function(t,n){return!!t&&(t=g(t),Math.max(Math.abs(this.lat-t.lat),Math.abs(this.lng-t.lng))<=(void 0===n?1e-9:n))},toString:function(t){return"LatLng("+n(this.lat,t)+", "+n(this.lng,t)+")"},distanceTo:function(t){return c.distance(this,g(t))},wrap:function(){return c.wrapLatLng(this)},toBounds:function(t){var n=180*t/40075017,i=n/Math.cos(Math.PI/180*this.lat);return l([this.lat-n,this.lng-i],[this.lat+n,this.lng+i])},clone:function(){return new f(this.lat,this.lng,this.alt)}}})();