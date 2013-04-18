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

(function($){
    'use strict';
    var fonten = {

        init : function(options){
            this.options = $.extend($.fonten.defaultOptions, options);
            this.fallbackFonts = ['serif', 'sans-serif', 'monospace'];
            this.onloadTestString = this.options.success?'A':'';
        },

        applyFont : function(element){
            var fontID = element.data('font-id');
            element.css("font-family", this.fontFamilyDict[fontID] + "," + element.css("font-family"));
        },

        addCSS : function (fontFace){
            var newStyle = document.createElement('style');
            newStyle.appendChild(document.createTextNode(fontFace));
            document.head.appendChild(newStyle);
        },

        composeFontFace : function (params, fontFamily){
            var self = this,
                fontUriBaseParams = $.extend({
                    strip: self.options.strip
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

        makeArraySortedAndUnique : function (arr){
            arr.sort();
            for(var i = arr.length; i--;){
                if(arr[i] === arr[i-1]){
                    arr.splice(i, 1);
                }
            }
            return arr;
        },

        initPlayGround : function(fontFamily, text){
            var span = document.createElement('span'),
                div = document.createElement('div');
            span.innerHTML = text;
            div.id = fontFamily;
            $(div).css({
                position : 'absolute',
                top : '-10000px',
                left : '-10000px',
                width : '10000px',
                height: '10000px',
                visibility: 'hidden',
                'z-index': '-100'
            })
            .append(span)
            .appendTo('body');
        },

        isFontLoaded : function(fontFamily){
            var $pg = $('#'+fontFamily).find('span'),
                width,
                prevWidth;
            for(var i= this.fallbackFonts.length; i--;){
                $pg.css('font-family',fontFamily + ',' + this.fallbackFonts[i]);
                width = $pg.width();
                if(prevWidth && width !== prevWidth){
                    //font width is different, so a fallback font is applied
                    //which means our fontFamily is not yey loaded
                    return false;
                }
                prevWidth = width;
            }
            return true;
        },

        pollForChange : function(fontID, fontFamily){
            var self = this,
                start = (new Date()).getTime(),
                now,
                poll = window.setInterval(function(){
                    if(self.isFontLoaded(fontFamily)){
                        window.clearInterval(poll);
                        if(typeof self.options.success === 'function'){
                            self.options.success(fontID, fontFamily);
                        }
                    }else{
                        now = (new Date()).getTime();
                        if(now - start > 10000){
                            window.clearInterval(poll);
                            if(typeof self.options.error === 'function'){
                                self.options.error(fontID, fontFamily);
                            }
                        }
                    }
                }, 100);

        },

        onloadFont : function(fontID, fontFamily, text){
            this.initPlayGround(fontFamily, text);
            this.pollForChange(fontID, fontFamily);
        },

        callOnload : function(){
            var self = this;
            if( this.options.success !== undefined || this.options.error !== undefined ){
                $.each(this.fontFamilyDict, function(k, v){
                    self.onloadFont(k, v, window.decodeURIComponent(self.options.fontTextDict[k]));
                });
            }
        },

        main : function(){
            var self = this,
                fontFace,
                fontFamily,
                fontFamilyDict = {},
                maxTextLength = 2000;
            $.each(this.options.fontTextDict, function(k, v){
                //create a unique font family name, so foten can be called multiple times without ruin previous changed different elements
                //note that if fonten is called with the same element again, previous css font setting is overwritten
                fontFamily = "fonten-" + k + "-" + Math.random().toString().substr(-8);
                fontFamilyDict[k] = fontFamily;
                v = v + self.onloadTestString;
                var postData,
                    text = self.makeArraySortedAndUnique(v.replace(/\s+/g,'').split('')).join('');
                text = window.encodeURIComponent(text);
                self.options.fontTextDict[k] = text;
                if(text.length < maxTextLength){
                    fontFace = self.composeFontFace({id: k, text: text}, fontFamily);
                    self.addCSS(fontFace);
                }else{
                    //use advanced api
                    postData = {text: text};
                    $.ajax({
                        type: 'POST',
                        url: self.options.server + self.options.reservePath,
                        crossDomain: true,
                        data: postData,
                        dataType: 'json',
                        success: function(data){
                            var token = data.token;
                            fontFace = self.composeFontFace({id: k, token:token}, fontFamily);
                            self.addCSS(fontFace);
                        },
                        error: function(msg){
                            if(typeof self.options.error === 'function'){
                                self.options.error(msg);
                            }
                        }
                    }); 
                }
            });
            fonten.fontFamilyDict = fontFamilyDict;
        }
    };

    $.fonten = function(options){
        fonten.init(options);
        fonten.main();
        fonten.callOnload();
    };


    $.fn.fonten = function(options){

        fonten.init(options);
        options = fonten.options;

        if(options.id){
            //unified font
            options.fontTextDict[options.id] = this.text();
        }else{
            //indevidual font
            this.each(function(i,element){
                var $element = $(element),
                    fontid = $element.data('font-id');
                if(fontid !== undefined){
                    if(options.fontTextDict[fontid] !== undefined){
                        options.fontTextDict[fontid] += $element.text();
                    }else{
                        options.fontTextDict[fontid] = $element.text();
                    }
                }
            });
        }

        fonten.main();
        fonten.callOnload();
        

        this.each(function(i, element){
            fonten.applyFont($(element));
        });
        return this;
    };

    $.fonten.defaultOptions = {
        server: location.protocol + "//" + location.host,
        fontPath: "/font",
        reservePath: "/reserve",
        strip: true,
        fontTextDict: {},
        success: undefined,
        error: undefined
    };

}(jQuery)); 