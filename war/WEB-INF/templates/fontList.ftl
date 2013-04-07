<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <title>fonten</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="have your own realtime font subsetter">
    <meta name="author" content="Shih-Wen Su">

    <!-- Le styles -->
    <link href="/css/bootstrap.min.css" rel="stylesheet">
    <style type="text/css">
      body {
        padding-top: 20px;
        padding-bottom: 40px;
      }

      /* Custom container */
      .container-narrow {
        margin: 0 auto;
        max-width: 800px;
      }
      .container-narrow > hr {
        margin: 30px 0;
      }

      /* Main marketing message and sign up button */
      .jumbotron {
        margin: 60px 0;
        text-align: center;
      }
      .jumbotron h1 {
        font-size: 72px;
        line-height: 1;
      }
      .jumbotron .btn {
        font-size: 21px;
        padding: 14px 24px;
      }

      /* Supporting marketing content */
      .marketing {
        margin: 60px 0;
      }
      .marketing p + h4 {
        margin-top: 28px;
      }
    </style>
    <link href="/css/bootstrap-responsive.min.css" rel="stylesheet">
  </head>
  <body>
    <div class="container-narrow">
        <div class="span8">
            <#if fonts?has_content>
            <div class="fonts">
            <h4>Font list</h4>
                <table class="table table-striped">
                    <thead>
                        <tr>
                            <th>ID</th><th>Name</th><th>Created</th>
                        </tr>
                    </thead>
                    <tbody>
                        <#list fonts as font>
                            <tr>
                                <td>${font.getKey().getName()}</td>
                                <td class="fonten" data-font-id="${font.getKey().getName()}">${font.getProperty('fontname')}</td>
                                <td>${font.getProperty('created')?string("hh:mm:ss MM/dd yyyy")}</td>
                            </tr>
                        </#list>
                    </tbody>
                </table>
            </div>
        </#if>
        </div>
        <div class="span8">
            <div class="upload-form">
            <h4>Upload New Font</h4>
                <form class="form-horizontal" action="${fontUploadUrl}" method="post" enctype="multipart/form-data">
                  <div class="control-group">
                    <label class="control-label">Font File* </label>
                    <div class="controls">
                    <input type="file" name="${FONT_FILE}"><br>
                    </div>
                  </div>
                  <div class="control-group">
                    <label class="control-label">Font Name* </label>
                    <div class="controls">
                    <input type="text" name="${FONT_NAME}"><br>
                    </div>
                  </div>
                  <div class="control-group">
                    <label class="control-label">Font ID* </label>
                    <div class="controls">
                    <input type="text" name="${FONT_ID}"><br>
                    </div>
                  </div>
                  <div class="control-group">
                    <div class="controls">
                    <input id="submit-new-font" class="btn" type="submit" value="Upload">
                    </div>
                  </div>
                </form>
                <div id="loading-msg" class="hide">
                  Uplaoding.... <br>
                  Please refresh and try again if request timeout.
                </div>
            </div>
        </div>
    </div>
  </body>
  <script src="//ajax.googleapis.com/ajax/libs/jquery/1.8.2/jquery.min.js"></script>
  <script src="/js/jquery.fonten.js"></script>
  <script type="text/javascript">
    $('.fonten').fonten();
    $('#submit-new-font').one('click',function(){
      $('#loading-msg').show();
    });
  </script>
</html>