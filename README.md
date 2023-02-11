# 常用小工具

使用 Java(最低要求 JDK 17), Python 等语言实现, 运行 `./my-tools.sh` 查看帮助信息, `--debug=true` 打开调试模式

**TODO**

## 常用字符串转换工具

使用: `./my-tools.sh --tool=converts --cmd="upper test"`

支持功能（可通过 `|` 连接多个命令）:

* md5，生成指定字符串的 md5 值
* kindle，Kindle 笔记内容转换为 Markdown 文档
* upper/lower，字符串大小写转换
* hdate，时间戳转换为易于阅读的时间格式
* timesp，把时间转换为时间戳
* now，当前时间戳
* replace，字符串替换
* base64, 编码和解码
* ...

## Git 提交记录生成更新日志

使用: `./my-tools.sh --tool=gitCommits2Log --path=./`

## JSON 生成对象

使用: `./my-tools.sh --tool=json2POJO --path=./`

支持:

* Java class/record

## dhtml2text

该脚本可以下载指定页面下的所有a标签对应的链接，也可把下载下来的html页面合并为纯文本文件。

![dhtml2text](./imgs/dhtml2text-01.png)

脚本使用Python3, 网页转文本使用`html2text`完成。

使用:


### 安装依赖

```
pip3 install html2text
pip3 install chardet
```

### 运行

```
python3 dhtml2text.py
```

## listAllFiles

Node.js，列出当前目录下的所有文件，并读取内容。

## MenuGenerator

Node.js，根据OpenAPI规范，可提取Swagger生成的api.json文件中的关键字并写入文件。

## LinkCovertTool

Java，可把后缀为`.desktop`，`.webloc`的多个网页快捷文件提取到链接并输出到Markdown文件。

## Trello 导出的 JSON 文件导入到 Logseq

支持把从 Trello 看板导出的 JSON 文件导入到 Logseq

# 致谢

* SimpleHTTPServerWithUpload 来自 https://gist.github.com/UniIsland/3346170
