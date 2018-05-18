# sqlline-drill
drill的sqlline代码

仅修改了sqlline/src/SQLline.java 这个文件的main方法
将原来直接调用start传入main的args修改为，读文件（用户名密码的敏感信息）和args拼接在一起传入。
class文件也一并上传方便后续使用。

如果需要打包：  解压drill/jars/3rdparty下的sqlline对应包，替换SQLLine.class文件，然后压缩替换原有包即可。
