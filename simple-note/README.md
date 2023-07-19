# Simple Note

简单笔记软件，开发目的是为局域网内部的多台设备进行信息同步，基于 Vert.x Web 开发，内容以 JSON 格式存储到文件中

运行: `mvn exec:java`

打包: `mvn package`

运行打包后的 jar 文件: `java -jar simple-note-1.0.0-SNAPSHOT-fat.jar --conf '{"filepath": "./"}'`

配置:

* filepath，文件保存路径，默认为 `/tmp`
* filename，文件名称，默认为 `note.json`
* port，监听端口，默认为 `8888`