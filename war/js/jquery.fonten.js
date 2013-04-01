/*
 * fonten - jQuery Plugin
 * Load dynamic web font for selected elements
 *
 * Copyright (c) 2013 Shih-Wen Su
 *
 * Version: 0.1 
 *
 * License under Apache 2.0
 */

;(function($){

	$.fonten = function(element, options){
		this.maxTextLength = 2000;
		this.init = function(element){
			var fontFace = "";
			options = $.extend($.fonten.defaultOptions, options);
			options.id = options.id || element.data('font-id') || return false;
			//create a unique font family name, so foten can be called multiple times without ruin previous changed different elements
			//note that if fonten is called with the same element again, previous font setting is overwritten
			this.fontFamily = this.fontFamily || "fonten-" + options.id + "-" + Math.random().toString().substr(-8);
			element.data('fonten-font', this.fontFamily);

			if(!options.text){
				options.text = element.text();
			}
			options.text = this._makeArraySortedAndUnique(options.text.replace(/\s+/g,'').split('')).join('');
			options.text = window.encodeURIComponent(options.text);
			if(options.text.length < this.maxTextLength){
				fontFace = this._composeFontFace({text:options.text});
				this._addCSS(fontFace);
				this._applyFont();
			}else{
				//use advanced api
				var postData = { text: options.text };
			    $.post( options.host + options.reservePath + '?callback=?', postData, function(data){
			    	var reservedToken = data.token;
					fontFace = this._composeFontFace({token:reservedToken});
					this._addCSS(fontFace);
					this._applyFont();
		    	}, 'json');		
			}
		};

		this._applyFont = function(){
			element.css("font-family", this.fontFamily + " " + element.css("font-family"));
			if(options.onload && typeof options.onload === 'function'){
				var img = new Image();
				img.src = this.fontUri;
				img.onerror = function(){
					options.onload();
				};
			}		
		};

		this._addCSS = function (fontFace){
			var newStyle = document.createElement('style');
			newStyle.appendChild(document.createTextNode(fontFace));
			document.head.appendChild(newStyle);
		};

		this._composeFontFace = function (params){
			var fontUriBaseParams = $.extend({
					id: options.id,
					strip: options.strip,
				}, params),
				fontUri = options.server + options.fontPath + "?" + $.param(fontUriBaseParams),
				fontFace = [
					"@font-face {",
						"\tfont-family: \"" + this.fontFamily + "\";",
						"\tsrc: url('" + fontUri + "&format=eot');",
						"\tsrc: local('☺'), url('" + fontUri + "&format=woff') format('woff'), url('" + fontUri + "');",
					"}"
				].join("\n");
				this.fontUri = fontUri;
			return fontFace;
		};

		this._makeArraySortedAndUnique = function (A){
			A.sort();
			for( var i = A.length; i--;){
				if(A[i] === A[i-1]){
					A.splice(i, 1);
				}
			}
			return A;
		};

		this.init(element);
	};

 	$.fn.fonten = function(options) {
		return this.each(function(){
			(new $.fonten($(this), options));
		});
	};

 	$.fonten.defaultOptions = {
	 	server: location.protocol + "//" + location.host,
		fontPath: "/font",
		reservePath: "/reserve",
		strip: true,
		onload: null
	};


})(jQuery);