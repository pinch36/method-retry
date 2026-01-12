# 方法重试框架
## 使用方法（未推送中心仓库，需要拉取到本地或者部署到私库）：
### 引入包retry-starter
```java
<dependency>
    <groupId>com.yun</groupId>
    <artifactId>retry-starter</artifactId>
    <version>0.0.1</version>
</dependency>
```
1. 在需要重试的方法加注解
2. 实现接口ReconsumeService，包含对数据库三个操作
* 写入（重试失败写入后异步重试）
* 读取（异步重试时获取方法重试需要的数据）
* 更新（重试后更新数据库状态）
3. 可写配置文件，前缀spring.retry，包含字段是否启动定时任务autoRetry，定时任务重试间隔interval（默认5min）
4. 如果需要使用定时任务设置autoRetry为true，必须对ReconsumeService三个方法都重写好
5. 不使用定时任务则只需要重写写入方法，手动调用RetryService的retry方法重试