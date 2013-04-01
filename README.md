## fonten ##

## why fonten?
The online and real time fonten builds a much smaller size font file that contains only specified characters. One runs a fonten server that hosts all his or her favorite fonts and builds subset font file on the fly. For Chinese fonts, the subset font file built by fonten is only a few KB, which is less than 1/1000 of the original size (10~20MB).

## Example Usage
```
@font-face{ 
  font-family: myfont; 
  src: url('/font?id=opensans&text=SomeCharaters&format=eot'); 
}
```

Visit [http://fonten-demo.appspot.com](http://fonten-demo.appspot.com) for installation guide, APIs, jQuery Plugin and Demo.

## Powered by
This project is developed based on [sfntly](https://code.google.com/p/sfntly/) and [Google App Engine/Java](https://developers.google.com/appengine/docs/java/overview).

## Author
Shih-Wen Su

sushi@summeridea.com

## License
Apache 2.0





## 為什麼使用 fonten?
fonten是一個即時線上切割字型檔案的工具，在你安裝了fonten server之後，你可以先上傳字型檔，之後根據需要的文字即時產生對應的字型檔。尤其像是中文，字型檔動輒10~20MB，如果我們只需要轉換幾個字，那切出來的檔案大概只有幾KB，是原本的千分之一的大小，因為只有指定的文字的字型被包在新的字型檔裡。這個專案是開源的，你可以自由的改造他來符合你的需求。

請至[http://fonten-demo.appspot.com](http://fonten-demo.appspot.com/index.zh_TW.html)查看詳細資訊


