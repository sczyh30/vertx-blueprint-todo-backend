# Vert.x 蓝图 - Todo Backend 开发教程

## 踏入Vert.x之门

朋友，欢迎来到Vert.x的世界！初次听说Vert.x，你一定会非常好奇：这是啥？让我们来看一下Vert.x的官方解释：

> Vert.x is a tool-kit for building reactive applications on the JVM.

(⊙o⊙)哦哦。。Vert.x是一个在JVM上构建响应式应用的一个工具集。

## 我们的应用 - 待办事项服务

## Vert.x Web

**注：**若国内用户出现用Gradle解析依赖非常缓慢的情况，可以尝试使用开源中国Maven镜像代替默认的镜像。只要在`build.gradle`中配置即可：
```groovy
repositories {
    maven {
            url 'http://maven.oschina.net/content/groups/public/'
        }
    mavenLocal() 
}
```
