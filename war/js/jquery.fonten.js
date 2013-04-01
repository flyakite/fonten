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

	$.fonten = {
		init : function(options){
			this.options = options;
		},

		_applyFont : function(element, fontFamilyDict){
			element.css("font-family", fontFamilyDict[element.data('font-id')] + "," + element.css("font-family"));
			if(this.options.onload && typeof this.options.onload === 'function'){
				var img = new Image();
				img.src = this.fontUri;
				img.onerror = function(){
					this.options.onload();
				};
			}
		},

		_addCSS : function (fontFace){
			var newStyle = document.createElement('style');
			newStyle.appendChild(document.createTextNode(fontFace));
			document.head.appendChild(newStyle);
		},

		_composeFontFace : function (params, fontFamily){
			var self = this,
				fontUriBaseParams = $.extend({
					strip: self.options.strip,
				}, params),
				fontUri = this.options.server + this.options.fontPath + "?" + $.param(fontUriBaseParams),
				fontFace = [
					"@font-face {",
						"\tfont-family: \"" + fontFamily + "\";",
						"\tsrc: url('" + fontUri + "&format=eot');",
						"\tsrc: local('☺'), url('" + fontUri + "&format=woff') format('woff'), url('" + fontUri + "');",
					"}"
				].join("\n");
				this.fontUri = fontUri;
			return fontFace;
		},

		_makeArraySortedAndUnique : function (A){
			A.sort();
			for( var i = A.length; i--;){
				if(A[i] === A[i-1]){
					A.splice(i, 1);
				}
			}
			return A;
		}
	};

 	$.fn.fonten = function(options) {
 		var fontFace = "", 
 			fontTextDict = {}, 
 			fontFamilyDict={},
 			maxTextLength = 2000;
		options = $.extend($.fonten.defaultOptions, options);
		$.fonten.init(options)

		if(options.id){
			//uni font
			fontTextDict[options.id] = element.text();
		}else{
			//indevidual font
			this.each(function(i,element){
				var $e = $(element),
					fontid = $e.data('font-id');
				if(fontid !== undefined){
					if(fontTextDict[fontid] !== undefined){
						fontTextDict[fontid] += $e.text();
					}else{
						fontTextDict[fontid] = $e.text();
					}
				}
			});
		}

		$.each(fontTextDict, function(k, v){
			//create a unique font family name, so foten can be called multiple times without ruin previous changed different elements
			//note that if fonten is called with the same element again, previous font setting is overwritten
			fontFamily = "fonten-" + k + "-" + Math.random().toString().substr(-8);
			fontFamilyDict[k] = fontFamily;
			var text = $.fonten._makeArraySortedAndUnique(v.replace(/\s+/g,'').split('')).join('');
			text = window.encodeURIComponent(text);
			fontTextDict[k] = text;
			if(text.length < maxTextLength){
				fontFace = $.fonten._composeFontFace({id: k, text: text}, fontFamily);
				$.fonten._addCSS(fontFace);
			}else{
				//use advanced api
				var postData = {text: text};
			    $.post( options.host + options.reservePath + '?callback=?', postData, function(data){
			    	var token = data.token;
					fontFace = $.fonten._composeFontFace({id: k, token:token}, fontFamily);
					$.fonten._addCSS(fontFace);
		    	}, 'json');	
			}
		});

		this.each(function(i, element){
			$.fonten._applyFont($(element), fontFamilyDict);
		});
		return this;
	};

 	$.fonten.defaultOptions = {
	 	server: location.protocol + "//" + location.host,
		fontPath: "/font",
		reservePath: "/reserve",
		strip: true,
		onload: null
	};

})(jQuery);	