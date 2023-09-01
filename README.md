# 常用小工具

使用 Java(最低要求 JDK 19, 推荐使用 `GraalVM`), Python 等语言实现, 运行 `./mytool.sh` 查看帮助信息, `--debug=true` 打开调试模式, `--help={COMMAND}` 查看帮助信息, 如 `--help=converts`

部分命令支持别名， 如 `--tool=converts --cmd=random --in=test` 的别名为 `--random=test`，具体可查看帮助文档

项目构建:

* `mvn package` 构建 jar 包
* `mvn package -Pnative` 构建本地代码

**TODO**

* Web 页面
* JavaFX

## 常用字符串转换工具

使用: `./mytool.sh --tool=converts --cmd="upper test"`

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

使用: `./mytool.sh --tool=gitCommits2Log --path=./`

## 简单 Web 文件服务器

使用 `./mytool.sh --tool=dirWebServer`

## JSON 生成对象

使用: `./mytool.sh --tool=json2POJO --path=./`

支持:

* Java class/record

## Trello 导出的 JSON 文件导入到 Logseq

支持把从 Trello 看板导出的 JSON 文件导入到 Logseq

##  文件重命名

使用: `./mytool.sh --tool=rename --path=./`

支持:

* order, 文件序号
* date, 日期
* datetime, 时间
* urlencoded, 解码 URL 编码
* replace, 字符串替换

## 阅读软件笔记、高亮导出为 markdown 文件

支持 Kindle、Moon+ Reader

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

## LinkCovertTool

Java，可把后缀为`.desktop`，`.webloc`的多个网页快捷文件提取到链接并输出到Markdown文件。

## 简单笔记

简单笔记软件，开发目的是为局域网内部的多台设备进行信息同步，基于 Vert.x Web 开发，内容以 JSON 格式存储到文件中

配置:

* filepath，文件保存路径，默认为当前目录
* filename，文件名称，默认为 `note.json`
* port，监听端口，默认为 `38080`

# JSON 格式化

使用: `./mytool.sh --tool=json [--json='{}'] [--path=dmeo.json]`

# 致谢

