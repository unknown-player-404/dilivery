# 从零手写 SpringCloud 外卖项目（分阶段实战教程）

> 这篇教程从**一个空文件夹**开始，带你按真实开发顺序把整个项目写出来：
> 建目录 → 写父工程 → 加依赖 → 写 Result / 异常处理类 → 写 CRUD → 接入 Nacos → 接入 OpenFeign → 接入 Seata → 接入 Sentinel → 接入 RocketMQ。
> 每个阶段都给**完整源码**和**验收标准**，写完一个阶段就测试一个阶段，绝不攒到最后一起调。
>
> 和另一篇文档的分工：
> - 本篇《从零手写教程》= 动手写，管"先写哪个文件、怎么验收"；
> - [SpringCloud外卖项目教程.md](SpringCloud外卖项目教程.md) = 讲原理，管"为什么这么写、底层怎么运转"。本篇里遇到想深挖的点，我会指过去对应章节。

## 开始之前

**1. 建议新建一个空文件夹来写**，比如 `C:\Users\86138\Desktop\code\delivery-from-zero`。
不要直接改成品项目，把 `delivery` 留作"标准答案"，卡住时可以对照。

**2. 组件（Nacos/Seata/Sentinel/RocketMQ）用成品项目里的脚本启动**，都在运行就什么都不用做：

```powershell
cd C:\Users\86138\Desktop\code\delivery\scripts
.\start-nacos.ps1   # 8848（控制台 8080）
.\start-seata.ps1   # 8091
.\start-sentinel.ps1 # 8858
.\start-rocketmq.ps1 # 9876 / 10911
```

**3. 学习期间先把成品项目的两个微服务停掉**（IDEA 里点红色停止按钮，或任务管理器结束 java 进程），
因为手写版要用同样的 8081/8082 端口。**组件不用停**。

**4. 数据库直接用现成的 `takeout` 库**：如果之前已按成品初始化过，本篇的建表 SQL 都可以跳过；从零开始就把每阶段的 SQL 执行一遍。账号密码 `root/123456`。

## 总路线：9 个阶段

| 阶段 | 你将要做的 | 验收标准 |
| --- | --- | --- |
| 1 | 建父工程 + 两个模块骨架 | IDEA 里 Maven 面板无报错，能打包 |
| 2 | food-service：连数据库，写 Result/异常处理/CRUD | curl 五连测试菜品增删改查全通过 |
| 3 | 接入 Nacos | Nacos 控制台出现 food-service |
| 4 | order-service：订单 CRUD（价格先 mock） | 能下单、能查单 |
| 5 | 接入 OpenFeign | 下单总价来自菜品服务的真实价格 |
| 6 | 接入 Seata | 库存不足时订单和库存一起回滚 |
| 7 | 接入 Sentinel | 快速连发请求出现"限流"提示 |
| 8 | 接入 RocketMQ | 下单后菜品销量自动增加 |
| 9 | 整体验收 + 收尾 | 四步验收法全部通过 |

**每个阶段的节奏都是：① 写代码 → ② 启动 → ③ 验收 → （卡住）查另一篇教程第 12 章报错手册。**

---

## 阶段 1：创建父工程与两个模块骨架

**本阶段目标**：一个空文件夹，变成"三个 pom 文件 + 两个能编译的 Spring Boot 骨架"。

### 1.1 在空文件夹里建出如下目录

```
delivery-from-zero
├── pom.xml                        # 父工程
├── food-service
│   ├── pom.xml
│   └── src/main/java/com/delivery/food/FoodServiceApplication.java
└── order-service
    ├── pom.xml
    └── src/main/java/com/delivery/order/OrderServiceApplication.java
```

### 1.2 根目录 `pom.xml`（父工程）

父工程**不写业务代码**，只做两件事：把两个模块组织在一起，统一 Java 版本和编码。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- 聚合父工程：一个模块都还没写业务，先把"组织关系"立起来 -->
    <groupId>com.delivery</groupId>
    <artifactId>delivery-cloud</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>
    <name>delivery-cloud</name>

    <modules>
        <module>food-service</module>
        <module>order-service</module>
    </modules>

    <properties>
        <!-- 编译成 Java 17 字节码：JDK 17/21/24 都能跑 -->
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
</project>
```

### 1.3 `food-service/pom.xml`（骨架版：先只有 web）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- 注意：parent 是 Spring Boot 官方父工程（不是上面那个聚合工程） -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.16</version>
        <!-- relativePath 留空：告诉 Maven 不要找上一级目录当父工程，去仓库下载 -->
        <relativePath/>
    </parent>

    <groupId>com.delivery</groupId>
    <artifactId>food-service</artifactId>
    <version>1.0.0</version>
    <name>food-service</name>

    <properties>
        <java.version>17</java.version>
        <!--
            覆盖 Spring Boot 内置的 Jackson 版本：Boot 3.5.16 自带的 Jackson 2.21.4
            有两个 DoS 漏洞公告（GHSA-7hhh-6rmp-j9qf / GHSA-p6pp-m3f8-5c89，IDEA 扫描会标黄），
            用同名属性覆盖为修复版 2.21.7。这是"覆盖传递依赖版本"的标准手法，详见阶段 2.12。
        -->
        <jackson-bom.version>2.21.7</jackson-bom.version>
    </properties>

    <dependencies>
        <!-- 第一个依赖：Web 能力（Controller 注解 / 内嵌 Tomcat / JSON） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- 打出可以直接 java -jar 运行的可执行 jar -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 1.4 `order-service/pom.xml`（和上面一模一样，只改两个地方）

复制 food-service 的 pom，把这两处改掉：

- `<artifactId>food-service</artifactId>` → `order-service`
- `<name>food-service</name>` → `order-service`

其余全部相同（现在两个模块都是"只会 web 的骨架"，后面各自加料）。

### 1.5 两个裸启动类

**文件：** `food-service/src/main/java/com/delivery/food/FoodServiceApplication.java`

```java
package com.delivery.food;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FoodServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodServiceApplication.class, args);
    }
}
```

**文件：** `order-service/src/main/java/com/delivery/order/OrderServiceApplication.java`

```java
package com.delivery.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

### 1.6 用 IDEA 打开并验收

1. IDEA → File → Open → 选择 `delivery-from-zero` 文件夹 → Trust Project；
2. 等右下角 Maven 同步完成（首次下载依赖几分钟）；
3. 右侧 Maven 面板 → 根工程 `delivery-cloud` → Lifecycle → 双击 `package`；
4. 看到 `BUILD SUCCESS` 即本阶段完成。

> 现在别急着运行——还没有端口配置，两个应用都会抢 8080。下一阶段开始就都配好了。
>
> **为什么要父工程？** 以后在根目录执行一次 Maven 命令就能构建全部模块；IDEA 里也是一个工程管理所有服务，和真实公司的多模块项目结构一致。
>
> ❓ **常见疑问：IDEA 的"新建 Spring Boot 项目"向导里只有 4.x，没有 3.x？**
> 正常现象：向导（start.spring.io）只列出仍在官方维护期的版本，Boot 3.5 的维护期已结束所以不显示；
> 但 **3.5.16 的构件在 Maven 中央仓库里永久可下载**，本项目就是用它编译和验证的。
> 结论：**这个项目不要用新建向导**（它还会生成一堆用不到的东西），按本教程手动建文件夹 + 粘贴 pom.xml 的方式做，版本完全由你掌控。
> 如果已经用向导生成了 4.x 骨架：把 pom.xml 里 `<parent>` 的 `<version>` 改成 `3.5.16`、`<java.version>` 改成 `17` 即可；
> 但更推荐直接删掉重建，避免两套结构混杂。
>
> 如果你确实想走"向导 + 修改"这条路，按这个修正清单来（可以，但要改干净）：
> ① 生成时**依赖项一个都不要勾**（避免混入 4.x 特有的坐标），也不要勾"下载 JDK"；
> ② 改 pom：`<parent>` 版本 → `3.5.16`，加一行 `<relativePath/>`，`<java.version>` → `17`；
> ③ 包名/目录对齐成 `com.delivery.food`（向导生成的 `com.example.xxx` 要改，否则教程代码的 package 全对不上）；
> ④ 删掉 `src/test`、`HELP.md`、`.mvn`/`mvnw`/`mvnw.cmd`（可选，留着只是显乱）；
> ⑤ order-service 不用再生成：复制 food-service 文件夹，改文件夹名、pom 的 artifactId、包名即可。
>
> 💡 **省事小技巧**：IDEA 里新建文件时，文件名框直接粘贴完整相对路径（如
> `food-service/src/main/java/com/delivery/food/FoodServiceApplication.java`），IDEA 会自动创建所有中间目录——
> 所以"手动建文件夹"其实只需几次粘贴，比走向导再清理更快更省心。

---

## 阶段 2：food-service —— 连数据库，写 Result、异常处理与 CRUD

**本阶段目标**：菜品服务完整的"增删改查"闭环，可以直接用 Apifox/curl 验证。

### 2.1 建菜品表

在你的 MySQL 里执行：

```sql
CREATE DATABASE IF NOT EXISTS takeout DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE takeout;

CREATE TABLE IF NOT EXISTS dish (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(50)   NOT NULL COMMENT '菜品名称',
    price       DECIMAL(10,2) NOT NULL COMMENT '价格（元）',
    stock       INT           NOT NULL DEFAULT 100 COMMENT '库存（份）',
    sales       INT           NOT NULL DEFAULT 0 COMMENT '累计销量（份）',
    description VARCHAR(255)  DEFAULT NULL COMMENT '描述',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品表';

-- 示例数据（只在第一次执行；重复执行会插入重复数据）
-- 黄焖鸡故意只给 5 份库存，第六阶段要用它演示"库存不足触发回滚"
INSERT INTO dish (name, price, stock, sales, description) VALUES
('黄焖鸡米饭', 18.00, 5, 0, '经典招牌，鸡肉嫩滑'),
('麻辣香锅', 32.00, 100, 0, '香辣过瘾，配菜丰富'),
('牛肉拉面', 15.50, 100, 0, '汤浓面劲道'),
('鲜榨橙汁', 8.00, 100, 0, '现点现榨，解腻神器');
```

> `stock` 和 `sales` 现在暂时用不上，是给第 6、8 阶段（Seata 扣库存 / MQ 加销量）预留的，一步到位省得后面改表。

### 2.2 给 `food-service/pom.xml` 加两个依赖

在 `<dependencies>` 里、web 依赖的下面加：

```xml
        <!-- MyBatis-Plus：注意 Spring Boot 3 必须用名字带 boot3 的 starter，别用老的 mybatis-plus-boot-starter -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>3.5.17</version>
        </dependency>

        <!-- MySQL 驱动（版本由 Boot 统一管理，这里不用写） -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
```

### 2.3 写 `food-service/src/main/resources/application.yml`

```yaml
server:
  port: 8081

spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    # serverTimezone 一定要带，否则中国时区会差 8 小时；allowPublicKeyRetrieval 是 MySQL 8 认证需要
    url: jdbc:mysql://localhost:3306/takeout?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 123456

mybatis-plus:
  configuration:
    # 打印 SQL：学习阶段强烈建议开着，控制台能看见每条 SQL
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### 2.4 更新启动类（加 @MapperScan）

**文件：** `food-service/src/main/java/com/delivery/food/FoodServiceApplication.java`（替换成）

```java
package com.delivery.food;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.delivery.food.mapper")
@SpringBootApplication
public class FoodServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodServiceApplication.class, args);
    }
}
```

> `@MapperScan` 漏了会怎样？启动报 `Field dishMapper ... required a bean ...`，见阶段 2.12。

### 2.5 统一返回结果类 `Result`

**文件：** `food-service/src/main/java/com/delivery/food/common/Result.java`

这是本项目所有接口的"统一语言"：`{code, message, data}`。第 5 阶段订单服务远程调用时，也靠 `code == 200` 判断成败。

```java
package com.delivery.food.common;

/**
 * 统一返回结果：所有接口都返回这个结构。
 * 泛型 T：data 字段的类型（查单个是 Dish，查列表是 List<Dish>）。
 */
public class Result<T> {

    private Integer code;      // 200 成功，500 失败
    private String message;    // 提示信息
    private T data;            // 业务数据

    public Result() {
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success() {
        return new Result<>(200, "成功", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "成功", data);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
```

> 无参构造 + setter 一定不能少：第 5 阶段 Feign 把对方返回的 JSON 反序列化成 `Result<DishDTO>` 时就靠它们。

### 2.6 全局异常处理类

**文件：** `food-service/src/main/java/com/delivery/food/exception/GlobalExceptionHandler.java`

```java
package com.delivery.food.exception;

import com.delivery.food.common.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：接口里抛出的异常都会被这里接住，
 * 统一返回 {code:500, message:异常信息}，不把堆栈暴露给调用方。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result<Void> handle(Exception e) {
        return Result.fail(e.getMessage());
    }
}
```

### 2.7 实体类 Dish

**文件：** `food-service/src/main/java/com/delivery/food/entity/Dish.java`

```java
package com.delivery.food.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

/**
 * 菜品实体，对应数据库表 dish。
 * 金额必须用 BigDecimal，double/float 算钱会有精度误差。
 */
@TableName("dish")
public class Dish {

    @TableId(type = IdType.AUTO)   // 主键，数据库自增
    private Long id;

    private String name;

    private BigDecimal price;

    private Integer stock;   // 库存（第 6 阶段 Seata 用）

    private Integer sales;   // 销量（第 8 阶段 RocketMQ 用）

    private String description;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getSales() {
        return sales;
    }

    public void setSales(Integer sales) {
        this.sales = sales;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
```

### 2.8 Mapper 接口（体验 MyBatis-Plus 的"魔法"）

**文件：** `food-service/src/main/java/com/delivery/food/mapper/DishMapper.java`

```java
package com.delivery.food.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.delivery.food.entity.Dish;

/**
 * 继承 BaseMapper 后，单表增删改查方法全部自动生成（insert/selectById/updateById...），
 * 一行 SQL 都不用写。第 6、8 阶段会在这里加手写 SQL 的方法。
 */
public interface DishMapper extends BaseMapper<Dish> {
}
```

### 2.9 业务层：接口 + 实现

**文件：** `food-service/src/main/java/com/delivery/food/service/DishService.java`

```java
package com.delivery.food.service;

import com.delivery.food.entity.Dish;

import java.util.List;

public interface DishService {

    List<Dish> list();

    Dish getById(Long id);

    boolean add(Dish dish);

    boolean update(Dish dish);

    boolean delete(Long id);
}
```

**文件：** `food-service/src/main/java/com/delivery/food/service/impl/DishServiceImpl.java`

```java
package com.delivery.food.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.delivery.food.entity.Dish;
import com.delivery.food.mapper.DishMapper;
import com.delivery.food.service.DishService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DishServiceImpl implements DishService {

    // 构造器注入：Spring 官方推荐写法，字段 final 更安全
    private final DishMapper dishMapper;

    public DishServiceImpl(DishMapper dishMapper) {
        this.dishMapper = dishMapper;
    }

    @Override
    public List<Dish> list() {
        // LambdaQueryWrapper：用方法引用写查询条件，字段写错编译期就报
        return dishMapper.selectList(
                new LambdaQueryWrapper<Dish>().orderByAsc(Dish::getId)
        );
    }

    @Override
    public Dish getById(Long id) {
        return dishMapper.selectById(id);
    }

    @Override
    public boolean add(Dish dish) {
        return dishMapper.insert(dish) > 0;   // 返回受影响行数
    }

    @Override
    public boolean update(Dish dish) {
        return dishMapper.updateById(dish) > 0;
    }

    @Override
    public boolean delete(Long id) {
        return dishMapper.deleteById(id) > 0;
    }
}
```

### 2.10 控制器 DishController

**文件：** `food-service/src/main/java/com/delivery/food/controller/DishController.java`

```java
package com.delivery.food.controller;

import com.delivery.food.common.Result;
import com.delivery.food.entity.Dish;
import com.delivery.food.service.DishService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST 风格五件套（本服务跑在 8081）：
 *   GET    /dish/list  查全部
 *   GET    /dish/{id}  查单个   <- 第 5 阶段订单服务 Feign 调的就是它
 *   POST   /dish       新增
 *   PUT    /dish       修改
 *   DELETE /dish/{id}  删除
 */
@RestController
@RequestMapping("/dish")
public class DishController {

    private final DishService dishService;

    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    @GetMapping("/list")
    public Result<List<Dish>> list() {
        return Result.success(dishService.list());
    }

    @GetMapping("/{id}")
    public Result<Dish> getById(@PathVariable Long id) {
        Dish dish = dishService.getById(id);
        if (dish == null) {
            return Result.fail("菜品不存在：id=" + id);
        }
        return Result.success(dish);
    }

    @PostMapping
    public Result<Void> add(@RequestBody Dish dish) {
        if (dish.getName() == null || dish.getName().isBlank()) {
            return Result.fail("菜品名称不能为空");
        }
        if (dish.getPrice() == null || dish.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("菜品价格必须大于 0");
        }
        return dishService.add(dish) ? Result.success() : Result.fail("新增失败");
    }

    @PutMapping
    public Result<Void> update(@RequestBody Dish dish) {
        if (dish.getId() == null) {
            return Result.fail("id 不能为空");
        }
        return dishService.update(dish) ? Result.success() : Result.fail("更新失败（id 可能不存在）");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return dishService.delete(id) ? Result.success() : Result.fail("删除失败（id 可能不存在）");
    }
}
```

### 2.11 启动与验收（重点！）

1. IDEA 里运行 `FoodServiceApplication`，看到 `Tomcat started on port 8081`；
2. 打开 Apifox（或 PowerShell 的 curl），依次验证：

```powershell
# ① 查列表
curl.exe -s http://127.0.0.1:8081/dish/list
# 预期：{"code":200,"message":"成功","data":[ ...4条菜品... ]}

# ② 查单个
curl.exe -s http://127.0.0.1:8081/dish/1

# ③ 新增
curl.exe -s -X POST http://127.0.0.1:8081/dish -H "Content-Type: application/json" -d '{"name":"测试菜品","price":9.9,"stock":10,"description":"练习新增"}'
# 预期：{"code":200,...}，再去查列表能看到它（注意它会拿到 id=5）

# ④ 修改
curl.exe -s -X PUT http://127.0.0.1:8081/dish -H "Content-Type: application/json" -d '{"id":5,"name":"测试菜品-改","price":12.5}'

# ⑤ 删除
curl.exe -s -X DELETE http://127.0.0.1:8081/dish/5
```

另外看一眼控制台：每条操作都打印了 SQL（`==> Preparing: SELECT ...`），这是 MyBatis-Plus 的 SQL 日志，能帮你把"方法调用"和"真实 SQL"对上号。

### 2.12 本阶段常见坑

| 现象 | 原因 | 解决 |
| --- | --- | --- |
| 启动报 `Field dishMapper ... required a bean` | @MapperScan 没写或包名写错 | 检查启动类注解 |
| 连不上数据库 `Access denied` | 密码不对 | 改 application.yml |
| 启动报 `Unknown database 'takeout'` | 建库 SQL 没执行 | 先执行 2.1 |
| 依赖下载报错 | 版本名写错 | Boot 3 要用 `mybatis-plus-spring-boot3-starter` |
| IDEA 把 `spring-boot-starter-web` 标黄：传递依赖 jackson-core 有漏洞（Checkmarx 报告） | 不是你代码的问题，是 Spring Boot 内置的 Jackson 版本（2.21.4）旧了 | 阶段 1 的 pom 已预置 `<jackson-bom.version>2.21.7</jackson-bom.version>` 覆盖；若你漏了这行，补上后再点 Maven 刷新 |

> **补充：关于"传递依赖"与版本覆盖**——`jackson-core` 不是你直接写的依赖，而是 `spring-boot-starter-web → spring-boot-starter-json` 传递进来的；
> Boot 父工程用一个属性 `jackson-bom.version` 统一管理这组 Jackson 库（core / databind / 各 datatype）的版本，
> 在 `<properties>` 里写同名属性即可一次性整体覆盖，不会出现"只升一个模块、版本打架"的问题。
> 改完用 `mvn dependency:tree -Dincludes=com.fasterxml.jackson*` 可以验证实际解析到的版本。

**阶段 2 完成 ✅**。现在你有了一个标准的三层结构微服务（Controller → Service → Mapper），下面开始网"微服务"方向升级。

---

## 阶段 3：接入 Nacos（把服务"登记"到注册中心）

**本阶段目标**：food-service 启动时自动注册到 Nacos，在控制台里能看见它。这一步做完，"服务发现"的地基就有了。

> 原理不懂的看另一篇教程第 3 章（通讯录比喻、心跳、服务发现流程）。

### 3.1 先启动 Nacos

```powershell
cd C:\Users\86138\Desktop\code\delivery\scripts
.\start-nacos.ps1
```

浏览器打开控制台 **http://localhost:8080**（Nacos 3.x 控制台在 8080，不是 8848）确认能打开。

### 3.2 给 `food-service/pom.xml` 加依赖

```xml
        <!-- Nacos 服务注册与发现 -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
```

光有依赖还不够，Spring Cloud 全家桶的版本要统一管理，在 `</dependencies>` 后面加一段 **dependencyManagement**：

```xml
    <dependencyManagement>
        <dependencies>
            <!-- Spring Cloud 版本管理（BOM）：相关依赖从此不用写版本号 -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>2025.0.3</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <!-- Spring Cloud Alibaba 版本管理（Nacos/Seata/Sentinel 的版本由它指定） -->
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>2025.0.0.0</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
```

> 版本速记（这个组合是官方配套、本项目实测通过的）：
> Spring Boot 3.5.16 + Spring Cloud 2025.0.3 + Spring Cloud Alibaba 2025.0.0.0。**新手别自己升级**。

### 3.3 修改 `application.yml`（加服务名和注册中心地址）

在 `spring:` 下面加两段（和已有的 datasource 平级）：

```yaml
spring:
  application:
    # 服务名：注册到 Nacos 的名字，第 5 阶段 Feign 就靠它找我们
    name: food-service
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848   # 注册中心地址
```

### 3.4 启动类加一个注解

**文件：** `food-service/src/main/java/com/delivery/food/FoodServiceApplication.java`（替换成）

```java
package com.delivery.food;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@MapperScan("com.delivery.food.mapper")
@SpringBootApplication
public class FoodServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodServiceApplication.class, args);
    }
}
```

### 3.5 验收

1. 重启 `FoodServiceApplication`；
2. Nacos 控制台（http://localhost:8080）→ 服务列表 → 应该看到 `food-service`；
3. 也可以用命令行验证：

```powershell
curl.exe -s "http://127.0.0.1:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=10"
# 预期：{"count":1,"doms":["food-service"]}
```

4. 再随便调一个菜品接口（比如 `/dish/list`），确认接入 Nacos 后业务功能没被弄坏。

### 3.6 常见坑

| 现象 | 原因 | 解决 |
| --- | --- | --- |
| 启动报 `failed to req API /nacos/v1/ns/instance` | Nacos 没启动 | 先执行 3.1 |
| 控制台看不到服务 | 服务名没配 / 连错地址 | 检查 yml 里 `spring.application.name` 和 nacos 地址 |

**阶段 3 完成 ✅**。food-service 已经是一个"注册在案"的微服务了。

---

## 阶段 4：order-service —— 订单服务先"自己跑起来"

**本阶段目标**：第二个微服务能完成"创建订单 / 查询订单 / 订单列表"。注意：**下单要用菜价，但现在还不会调菜品服务**——所以本阶段先用一个假价格（mock）让流程跑通，下一阶段再用 OpenFeign 替换掉它。

> 这不是偷懒，而是真实开发中很常见的手法：先让主干流程能跑（可测试、可演示），再逐个替换临时实现。

### 4.1 建订单表

```sql
USE takeout;

CREATE TABLE IF NOT EXISTS orders (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_no    VARCHAR(32)   NOT NULL COMMENT '订单号',
    dish_id     BIGINT        NOT NULL COMMENT '菜品id（对应 dish 表主键）',
    quantity    INT           NOT NULL DEFAULT 1 COMMENT '购买数量',
    total_price DECIMAL(10,2) NOT NULL COMMENT '总价（元）',
    status      TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0待支付 1已支付',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';
```

> 表名为什么是 `orders` 不是 `order`？`order` 是 SQL 关键字（ORDER BY 那个），直接当表名会语法报错，加个 s 绕开。

### 4.2 `order-service/pom.xml` 升级成和 food-service 现在的样子

对照 food-service 的 pom，把这个模块补齐（web + mybatis-plus + mysql + nacos + 两个 BOM + 打包插件）。加完后两个模块的 pom 结构一致，只是 artifactId 不同。

### 4.3 `order-service/src/main/resources/application.yml`

```yaml
server:
  port: 8082

spring:
  application:
    name: order-service
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/takeout?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 123456

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### 4.4 启动类

**文件：** `order-service/src/main/java/com/delivery/order/OrderServiceApplication.java`（替换成）

```java
package com.delivery.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/* 后面阶段还会在这里加 @EnableFeignClients */
@EnableDiscoveryClient
@MapperScan("com.delivery.order.mapper")
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

### 4.5 公共类（直接复制 food-service 的，改包名）

把 `Result.java` 和 `GlobalExceptionHandler.java` 各复制一份过来，只把包名 `com.delivery.food` 改成 `com.delivery.order`：

- `order-service/src/main/java/com/delivery/order/common/Result.java`
- `order-service/src/main/java/com/delivery/order/exception/GlobalExceptionHandler.java`

> 为什么复制而不是共用？两个微服务是独立编译部署的，不能互相 import。真实项目会把这类公共类抽成第三个 common 模块，学习项目复制一份更直观。

### 4.6 实体 / Mapper / DTO / VO

**文件：** `order-service/src/main/java/com/delivery/order/entity/Orders.java`

```java
package com.delivery.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("orders")
public class Orders {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;         // 订单号

    private Long dishId;            // 点的什么菜（菜品服务 dish 表主键）

    private Integer quantity;       // 数量

    private BigDecimal totalPrice;  // 总价

    private Integer status;         // 0待支付 1已支付

    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getDishId() {
        return dishId;
    }

    public void setDishId(Long dishId) {
        this.dishId = dishId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
```

**文件：** `order-service/src/main/java/com/delivery/order/mapper/OrdersMapper.java`

```java
package com.delivery.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.delivery.order.entity.Orders;

public interface OrdersMapper extends BaseMapper<Orders> {
}
```

**文件：** `order-service/src/main/java/com/delivery/order/dto/OrderCreateDTO.java`（创建订单的请求参数）

```java
package com.delivery.order.dto;

public class OrderCreateDTO {

    private Long dishId;

    private Integer quantity;

    public Long getDishId() {
        return dishId;
    }

    public void setDishId(Long dishId) {
        this.dishId = dishId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
```

**文件：** `order-service/src/main/java/com/delivery/order/dto/OrderDetailVO.java`（订单详情：订单信息 + 菜品信息拼一起）

```java
package com.delivery.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderDetailVO {

    private Long id;
    private String orderNo;
    private Long dishId;
    private String dishName;      // 来自菜品服务（第 5 阶段起有值）
    private BigDecimal dishPrice; // 来自菜品服务（第 5 阶段起有值）
    private Integer quantity;
    private BigDecimal totalPrice;
    private Integer status;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getDishId() {
        return dishId;
    }

    public void setDishId(Long dishId) {
        this.dishId = dishId;
    }

    public String getDishName() {
        return dishName;
    }

    public void setDishName(String dishName) {
        this.dishName = dishName;
    }

    public BigDecimal getDishPrice() {
        return dishPrice;
    }

    public void setDishPrice(BigDecimal dishPrice) {
        this.dishPrice = dishPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
```

### 4.7 业务层（mock 价格版本）

**文件：** `order-service/src/main/java/com/delivery/order/service/OrdersService.java`

```java
package com.delivery.order.service;

import com.delivery.order.dto.OrderCreateDTO;
import com.delivery.order.dto.OrderDetailVO;
import com.delivery.order.entity.Orders;

import java.util.List;

public interface OrdersService {

    OrderDetailVO createOrder(OrderCreateDTO dto);

    OrderDetailVO getOrderDetail(Long id);

    List<Orders> list();
}
```

**文件：** `order-service/src/main/java/com/delivery/order/service/impl/OrdersServiceImpl.java`（先看 mock 版）

```java
package com.delivery.order.service.impl;

import com.delivery.order.dto.OrderCreateDTO;
import com.delivery.order.dto.OrderDetailVO;
import com.delivery.order.entity.Orders;
import com.delivery.order.mapper.OrdersMapper;
import com.delivery.order.service.OrdersService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class OrdersServiceImpl implements OrdersService {

    private final OrdersMapper ordersMapper;

    public OrdersServiceImpl(OrdersMapper ordersMapper) {
        this.ordersMapper = ordersMapper;
    }

    @Override
    public OrderDetailVO createOrder(OrderCreateDTO dto) {
        if (dto == null || dto.getDishId() == null) {
            throw new RuntimeException("菜品 id 不能为空");
        }
        int quantity = dto.getQuantity() == null ? 1 : dto.getQuantity();
        if (quantity <= 0) {
            throw new RuntimeException("数量必须大于 0");
        }

        Orders order = new Orders();
        order.setOrderNo(generateOrderNo());
        order.setDishId(dto.getDishId());
        order.setQuantity(quantity);
        // TODO 临时价格：下一阶段接入 OpenFeign 后，换成菜品服务返回的真实价格
        order.setTotalPrice(BigDecimal.ONE.multiply(BigDecimal.valueOf(quantity)));
        order.setStatus(0);
        order.setCreateTime(LocalDateTime.now());
        ordersMapper.insert(order);

        return toVO(order);
    }

    @Override
    public OrderDetailVO getOrderDetail(Long id) {
        Orders order = ordersMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在：id=" + id);
        }
        return toVO(order);
    }

    @Override
    public List<Orders> list() {
        return ordersMapper.selectList(null);
    }

    private String generateOrderNo() {
        int random = new Random().nextInt(9000) + 1000;
        return System.currentTimeMillis() + "" + random;
    }

    private OrderDetailVO toVO(Orders order) {
        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setDishId(order.getDishId());
        vo.setDishName("（mock：还没接入菜品服务）");
        vo.setQuantity(order.getQuantity());
        vo.setTotalPrice(order.getTotalPrice());
        vo.setStatus(order.getStatus());
        vo.setCreateTime(order.getCreateTime());
        return vo;
    }
}
```

### 4.8 控制器

**文件：** `order-service/src/main/java/com/delivery/order/controller/OrderController.java`

```java
package com.delivery.order.controller;

import com.delivery.order.common.Result;
import com.delivery.order.dto.OrderCreateDTO;
import com.delivery.order.dto.OrderDetailVO;
import com.delivery.order.entity.Orders;
import com.delivery.order.service.OrdersService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrdersService ordersService;

    public OrderController(OrdersService ordersService) {
        this.ordersService = ordersService;
    }

    @PostMapping("/create")
    public Result<OrderDetailVO> create(@RequestBody OrderCreateDTO dto) {
        return Result.success(ordersService.createOrder(dto));
    }

    @GetMapping("/{id}")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        return Result.success(ordersService.getOrderDetail(id));
    }

    @GetMapping("/list")
    public Result<List<Orders>> list() {
        return Result.success(ordersService.list());
    }
}
```

### 4.9 验收

```powershell
# 启动 OrderServiceApplication（8082）后：
curl.exe -s -X POST http://127.0.0.1:8082/order/create -H "Content-Type: application/json" -d '{"dishId":2,"quantity":2}'
# 预期成功，但 totalPrice=2.00（mock 价格），dishName 显示"（mock：还没接入菜品服务）"

curl.exe -s http://127.0.0.1:8082/order/1     # 订单详情
curl.exe -s http://127.0.0.1:8082/order/list  # 订单列表
```

Nacos 控制台里现在应该有 **两个** 服务：food-service、order-service。

**阶段 4 完成 ✅**。订单服务自己跑通了，缺口也很明确：**价格是假的**。下一阶段用 OpenFeign 解决。

---

## 阶段 5：接入 OpenFeign —— 订单服务调用菜品服务

**本阶段目标**：下单时真实调用 food-service 拿到菜价和菜名，替换掉 mock。

> 原理（动态代理、负载均衡、XID 后续会用到）看另一篇教程第 6 章。

### 5.1 `order-service/pom.xml` 加两个依赖

```xml
        <!-- OpenFeign：用"接口"的方式调用其他微服务 -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>

        <!-- 负载均衡器：Spring Cloud 2025 起 OpenFeign 不再自带它，必须显式引入，否则启动报
             "No Feign Client for loadBalancing defined" -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-loadbalancer</artifactId>
        </dependency>
```

### 5.2 启动类加 @EnableFeignClients

**文件：** `order-service/src/main/java/com/delivery/order/OrderServiceApplication.java`（替换成）

```java
package com.delivery.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@EnableDiscoveryClient
@MapperScan("com.delivery.order.mapper")
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

### 5.3 声明式客户端 DishClient（Feign 的灵魂）

**文件：** `order-service/src/main/java/com/delivery/order/client/DishClient.java`

```java
package com.delivery.order.client;

import com.delivery.order.common.Result;
import com.delivery.order.dto.DishDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 对照 food-service 的 DishController 写：
 *   @FeignClient 的 name  = 对方 spring.application.name
 *   方法上的路径和注解     = 对方接口的原样复制
 * 只写声明，不发请求；真正的 HTTP 调由 Spring 运行时代理完成。
 */
@FeignClient(name = "food-service")
public interface DishClient {

    @GetMapping("/dish/{id}")
    Result<DishDTO> getDishById(@PathVariable("id") Long id);
}
```

### 5.4 菜品"镜像类" DishDTO

两个服务不能共享 Java 类，在订单服务里建一个字段对得上的镜像类，JSON 才能反序列化进来。

**文件：** `order-service/src/main/java/com/delivery/order/dto/DishDTO.java`

```java
package com.delivery.order.dto;

import java.math.BigDecimal;

public class DishDTO {

    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private Integer sales;
    private String description;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getSales() {
        return sales;
    }

    public void setSales(Integer sales) {
        this.sales = sales;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
```

### 5.5 改造 OrdersServiceImpl：把 mock 换成真实调用

**文件：** `order-service/src/main/java/com/delivery/order/service/impl/OrdersServiceImpl.java`

改动点（其余不动）：

1. 注入 `DishClient`；
2. `createOrder` 里先远程查菜品，用真实价格算总价；
3. `getOrderDetail` 里也远程查菜品，把菜名单价拼进 VO；
4. `toVO` 加上菜品参数。

```java
@Service
public class OrdersServiceImpl implements OrdersService {

    private final OrdersMapper ordersMapper;
    private final DishClient dishClient;   // 注入 Feign 接口，调用它 = 调用菜品服务

    public OrdersServiceImpl(OrdersMapper ordersMapper, DishClient dishClient) {
        this.ordersMapper = ordersMapper;
        this.dishClient = dishClient;
    }

    @Override
    public OrderDetailVO createOrder(OrderCreateDTO dto) {
        if (dto == null || dto.getDishId() == null) {
            throw new RuntimeException("菜品 id 不能为空");
        }
        int quantity = dto.getQuantity() == null ? 1 : dto.getQuantity();
        if (quantity <= 0) {
            throw new RuntimeException("数量必须大于 0");
        }

        // 【替换 mock】远程查菜品：Feign 会自动去 Nacos 找 food-service 并发 HTTP GET /dish/{id}
        Result<DishDTO> result = dishClient.getDishById(dto.getDishId());
        if (result == null || result.getCode() != 200 || result.getData() == null) {
            throw new RuntimeException("菜品不存在，或菜品服务调用失败");
        }
        DishDTO dish = result.getData();

        Orders order = new Orders();
        order.setOrderNo(generateOrderNo());
        order.setDishId(dto.getDishId());
        order.setQuantity(quantity);
        order.setTotalPrice(dish.getPrice().multiply(BigDecimal.valueOf(quantity)));  // 真实价格
        order.setStatus(0);
        order.setCreateTime(LocalDateTime.now());
        ordersMapper.insert(order);

        return toVO(order, dish);
    }

    @Override
    public OrderDetailVO getOrderDetail(Long id) {
        Orders order = ordersMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在：id=" + id);
        }
        Result<DishDTO> result = dishClient.getDishById(order.getDishId());
        if (result == null || result.getCode() != 200 || result.getData() == null) {
            throw new RuntimeException("查询菜品信息失败");
        }
        return toVO(order, result.getData());
    }

    // list()、generateOrderNo() 不变，省略

    private OrderDetailVO toVO(Orders order, DishDTO dish) {
        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setDishId(order.getDishId());
        vo.setDishName(dish.getName());      // 菜名来自菜品服务
        vo.setDishPrice(dish.getPrice());    // 单价来自菜品服务
        vo.setQuantity(order.getQuantity());
        vo.setTotalPrice(order.getTotalPrice());
        vo.setStatus(order.getStatus());
        vo.setCreateTime(order.getCreateTime());
        return vo;
    }
}
```

需要的 import 换成：

```java
import com.delivery.order.client.DishClient;
import com.delivery.order.common.Result;
import com.delivery.order.dto.DishDTO;
import com.delivery.order.dto.OrderCreateDTO;
import com.delivery.order.dto.OrderDetailVO;
import com.delivery.order.entity.Orders;
import com.delivery.order.mapper.OrdersMapper;
import com.delivery.order.service.OrdersService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
```

### 5.6 验收

重启 order-service（food-service 和 Nacos 保持运行），再下单：

```powershell
curl.exe -s -X POST http://127.0.0.1:8082/order/create -H "Content-Type: application/json" -d '{"dishId":2,"quantity":2}'
# 预期：totalPrice=64.00，dishName="麻辣香锅"，dishPrice=32.00 —— 全部来自菜品服务！
```

**看到 64.00 这一刻，恭喜：你完成了一次真正的微服务间调用。** 调用链是：
`你 → order-service(8082) → Nacos 查地址 → food-service(8081) → MySQL`。

### 5.7 常见坑

| 现象 | 原因 | 解决 |
| --- | --- | --- |
| 启动报 `No Feign Client for loadBalancing defined` | 没加 loadbalancer 依赖 | 见 5.1 |
| 调用报 404 | 路径/服务名和对方不一致 | 对照 DishController 逐个字符核 |
| 报 `No instances available for food-service` | 对方没注册上 | 看 Nacos 服务列表 |

**阶段 5 完成 ✅**。

---

## 阶段 6：接入 Seata —— 让"下单 + 扣库存"跨服务一起成功 / 一起回滚

**本阶段目标**：下单时，订单服务写 `orders` 表 + 菜品服务扣 `dish.stock`，两个操作是一个全局事务；库存不足时两边一起回滚，不留脏数据。

> ⚠️ **顺序很重要**：加了 Seata 依赖后，微服务启动时会主动连接 Seata，连不上会**直接启动失败**。所以每一步测试前先确保 Seata 在运行。

### 6.1 启动 Seata

```powershell
cd C:\Users\86138\Desktop\code\delivery\scripts
.\start-seata.ps1
```

### 6.2 建 undo_log 表（Seata AT 模式的"反悔凭证"）

```sql
USE takeout;

CREATE TABLE IF NOT EXISTS undo_log (
    branch_id     BIGINT       NOT NULL COMMENT '分支事务 id',
    xid           VARCHAR(128) NOT NULL COMMENT '全局事务 id',
    context       VARCHAR(128) NOT NULL COMMENT 'undo_log context',
    rollback_info LONGBLOB     NOT NULL COMMENT '回滚数据',
    log_status    INT          NOT NULL COMMENT '状态：0正常 1已全局提交',
    log_created   DATETIME(6)  NOT NULL COMMENT '创建时间',
    log_modified  DATETIME(6)  NOT NULL COMMENT '修改时间',
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT 回滚日志表';
```

理解方式：Seata 在改数据前先把"旧值"记到这张表，回滚时照着旧值改回去。**每个参与全局事务的库都必须有它**。

### 6.3 两个服务的 pom 都加 Seata 依赖

`food-service/pom.xml` 和 `order-service/pom.xml` 的 `<dependencies>` 里都加：

```xml
        <!-- Seata：分布式事务（订单服务是发起方，菜品服务是参与方，依赖是一样的） -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
        </dependency>
```

### 6.4 两个服务的 yml 都加 Seata 配置

```yaml
seata:
  enabled: true
  application-id: ${spring.application.name}
  # 事务分组：两个服务必须一致，才在同一个"群"里
  tx-service-group: delivery-tx-group
  registry:
    # 学习项目用 file 模式（不用给 Seata 配注册中心），生产一般用 nacos
    type: file
    file:
      name: default
  service:
    grouplist:
      default: 127.0.0.1:8091   # Seata 服务端地址
    vgroup-mapping:
      delivery-tx-group: default
```

> 加了这一段后先重启两个服务试试能不能正常起来——能起来说明 Seata 连接配置对了，再写下面的代码。

### 6.5 food-service：新增"扣库存"接口

**① `DishMapper` 加一个手写 SQL 方法**（为什么手写：`stock >= #{num}` 这个条件 BaseMapper 写不出来，而且这条 UPDATE 本身是原子的，不会把库存扣成负数）：

**文件：** `food-service/src/main/java/com/delivery/food/mapper/DishMapper.java`（替换成）

```java
package com.delivery.food.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.delivery.food.entity.Dish;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface DishMapper extends BaseMapper<Dish> {

    /** 扣库存（原子操作）：库存不够时影响行数为 0，不会扣成负数 */
    @Update("UPDATE dish SET stock = stock - #{num} WHERE id = #{id} AND stock >= #{num}")
    int deductStock(@Param("id") Long id, @Param("num") Integer num);
}
```

**② `DishService` 加方法声明**：

```java
    /** 扣库存：库存不足时抛异常（触发全局事务回滚） */
    void deductStock(Long id, Integer quantity);
```

**③ `DishServiceImpl` 加实现**：

```java
    @Override
    public void deductStock(Long id, Integer quantity) {
        int rows = dishMapper.deductStock(id, quantity);
        if (rows <= 0) {
            // 抛异常 = 告诉全局事务"这个分支失败了"，Seata 会把订单和库存一起回滚
            throw new RuntimeException("菜品库存不足：id=" + id + "，下单数量 " + quantity + " 份");
        }
    }
```

**④ `DishController` 加一个内部接口**（加在类里，import 补上 `RequestParam`）：

```java
    /** 扣减库存（供订单服务内部调用）；库存不足抛异常 -> 全局异常处理器返回 code=500 */
    @PostMapping("/deductStock")
    public Result<Void> deductStock(@RequestParam("id") Long id, @RequestParam("quantity") Integer quantity) {
        dishService.deductStock(id, quantity);
        return Result.success();
    }
```

### 6.6 order-service：发全局事务 + 调用扣库存

**① `DishClient` 加方法**（import 补上 `PostMapping`、`RequestParam`）：

```java
    /** 扣库存（写操作：这条调用是全局事务的一个分支，失败会一起回滚） */
    @PostMapping("/dish/deductStock")
    Result<Void> deductStock(@RequestParam("id") Long id, @RequestParam("quantity") Integer quantity);
```

**② `OrdersServiceImpl` 的 `createOrder` 做两处修改**：

```java
import io.seata.spring.annotation.GlobalTransactional;   // 新增 import

    @Override
    @GlobalTransactional(timeoutMills = 300000, name = "create-order-tx")  // ← 新增注解
    public OrderDetailVO createOrder(OrderCreateDTO dto) {
        // ...前面的参数校验、Feign 查菜品、写订单表都不变...
        ordersMapper.insert(order);

        // ↓ 新增：远程扣库存（全局事务的第二个分支）
        //   库存不足时菜品服务返回 code=500，这里抛异常 -> 整个全局事务回滚
        Result<Void> deductResult = dishClient.deductStock(dto.getDishId(), quantity);
        if (deductResult == null || deductResult.getCode() != 200) {
            throw new RuntimeException(deductResult == null ? "扣减库存失败" : deductResult.getMessage());
        }

        return toVO(order, dish);
    }
```

> **XID 是怎么传到菜品服务的？** 不用你写任何代码——`spring-cloud-starter-alibaba-seata` 内置了 Feign 拦截器，自动把全局事务编号（XID）放进请求头传给对方。
> （本项目真实踩过的坑：我一开始手写了一个做同样事的拦截器，结果和内置的重名，order-service 直接启动失败。**框架已做好的事，先查再做。**）

### 6.7 验收：先成功、再回滚

```powershell
# ① 正常下单，看库存被扣
curl.exe -s -X POST http://127.0.0.1:8082/order/create -H "Content-Type: application/json" -d '{"dishId":2,"quantity":2}'
# 然后查数据库：SELECT id,name,stock FROM takeout.dish WHERE id=2;   （stock 应该 -2）

# ② 库存不足场景：黄焖鸡库存只有 5，买 99 份
curl.exe -s -X POST http://127.0.0.1:8082/order/create -H "Content-Type: application/json" -d '{"dishId":1,"quantity":99}'
# 预期返回：{"code":500,"message":"菜品库存不足：id=1，下单数量 99 份",...}
```

然后做**回滚验证**（这是整个项目最有说服力的一步）：

```sql
-- 订单表里不应该有 dish_id=1 的订单（说明订单插入被回滚了）
SELECT id, order_no, dish_id FROM takeout.orders;
-- 黄焖鸡库存应该还是 5（没被扣）
SELECT id, name, stock FROM takeout.dish WHERE id = 1;
-- undo_log 应该被清理干净（回滚完成后删掉了）
SELECT COUNT(*) FROM takeout.undo_log;
```

order-service 控制台里还能看到真实的回滚日志，出现这几行就说明分布式事务真的在工作：

```
transaction 192.168.x.x:8091:xxxx will be rollback
Branch Rollbacking: ... jdbc:mysql://localhost:3306/takeout
undo_log deleted with GlobalFinished
branch rollback success
rollback status: Rollbacked
```

### 6.8 常见坑

| 现象 | 原因 | 解决 |
| --- | --- | --- |
| 启动报 `can not connect to [127.0.0.1:8091]` | Seata 没启动 | 先执行 6.1 再启动服务 |
| 启动报 `BeanDefinitionOverrideException ... seataFeignRequestInterceptor` | 自己手写了 XID 拦截器和内置的冲突 | 删掉手写的（本项目踩过） |
| 回滚后库存没还原 | undo_log 表没建 | 执行 6.2 |
| 事务组不一致报错 | 两个服务 tx-service-group 不同 | 都写 `delivery-tx-group` |

**阶段 6 完成 ✅**。到这里，项目已经有两种"微服务难题"被解决了：找服务（Nacos）、跨服务数据一致性（Seata）。

---

## 阶段 7：接入 Sentinel —— 给下单接口"降压"

**本阶段目标**：下单接口配置每秒最多 1 次的限流，超出的请求返回友好提示而不是把服务压垮。

### 7.1 启动 Sentinel 控制台

```powershell
cd C:\Users\86138\Desktop\code\delivery\scripts
.\start-sentinel.ps1
```

浏览器打开 http://localhost:8858 ，账号密码 `sentinel / sentinel`。

### 7.2 `order-service/pom.xml` 加依赖

```xml
        <!-- Sentinel：流量控制 -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
        </dependency>
```

### 7.3 `application.yml` 加控制台地址

在 `spring.cloud` 下面加（和 nacos 平级）：

```yaml
    sentinel:
      transport:
        dashboard: 127.0.0.1:8858   # Sentinel 控制台地址
        port: 8719                  # 本服务与控制台通信的端口
```

### 7.4 注册切面 + 一个可选的演示开关

**文件：** `order-service/src/main/java/com/delivery/order/config/SentinelConfig.java`

```java
package com.delivery.order.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * ① sentinelResourceAspect：让 @SentinelResource 注解生效
 *    （Spring Cloud Alibaba 不会自动注册这个切面，必须手动加 Bean）
 * ② loadDemoFlowRule：可选的"代码方式限流"开关：
 *    启动参数加 --sentinel.demo.flow-qps=1 时给下单接口限流，不用控制台也能演示；默认关闭。
 */
@Configuration
public class SentinelConfig {

    private static final Logger log = LoggerFactory.getLogger(SentinelConfig.class);

    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }

    @Bean
    public CommandLineRunner loadDemoFlowRule(@Value("${sentinel.demo.flow-qps:0}") int qps) {
        return args -> {
            if (qps <= 0) {
                return;
            }
            FlowRule rule = new FlowRule("createOrder");
            rule.setGrade(RuleConstant.FLOW_GRADE_QPS);  // 按每秒请求数限流
            rule.setCount(qps);                          // 每秒最多几次
            FlowRuleManager.loadRules(List.of(rule));
            log.info(">>> 已加载 Sentinel 限流规则：createOrder 每秒最多 {} 次", qps);
        };
    }
}
```

### 7.5 给下单接口挂上资源名和兜底方法

**文件：** `order-service/src/main/java/com/delivery/order/controller/OrderController.java`（替换成）

```java
package com.delivery.order.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.delivery.order.common.Result;
import com.delivery.order.dto.OrderCreateDTO;
import com.delivery.order.dto.OrderDetailVO;
import com.delivery.order.entity.Orders;
import com.delivery.order.service.OrdersService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrdersService ordersService;

    public OrderController(OrdersService ordersService) {
        this.ordersService = ordersService;
    }

    /**
     * 创建订单：挂上 @SentinelResource 后，
     * 在控制台给资源名 createOrder 配限流规则，超出的请求会进入 blockHandler。
     */
    @PostMapping("/create")
    @SentinelResource(value = "createOrder", blockHandler = "createOrderBlockHandler")
    public Result<OrderDetailVO> create(@RequestBody OrderCreateDTO dto) {
        return Result.success(ordersService.createOrder(dto));
    }

    /**
     * 限流兜底方法：签名和原方法一致，末尾多一个 BlockException（Sentinel 规定）。
     */
    public Result<OrderDetailVO> createOrderBlockHandler(OrderCreateDTO dto, BlockException ex) {
        return Result.fail("当前下单人数过多，请稍后再试（Sentinel 限流已生效）");
    }

    @GetMapping("/{id}")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        return Result.success(ordersService.getOrderDetail(id));
    }

    @GetMapping("/list")
    public Result<List<Orders>> list() {
        return Result.success(ordersService.list());
    }
}
```

### 7.6 验收：两种方式二选一

**方式 A：控制台配规则（推荐体验一次）**

1. 重启 order-service，打开 Sentinel 控制台 http://localhost:8858 ；
2. 左侧「流控规则」→ 新建：
   - 资源名：`createOrder`
   - 阈值类型：QPS，单机阈值：`1`
3. 快速连发 5 次下单请求：

```powershell
1..5 | ForEach-Object { curl.exe -s -X POST http://127.0.0.1:8082/order/create -H "Content-Type: application/json" -d '{"dishId":3,"quantity":1}'; Write-Output "" }
```

预期：有的成功、有的返回"当前下单人数过多，请稍后再试（Sentinel 限流已生效）"。

**方式 B：代码方式（不打开控制台也能演示）**

用 IDEA 运行 order-service 时加 VM/Program 参数 `--sentinel.demo.flow-qps=1`，效果相同。

> 注意：控制台配置的规则**重启就丢**（生产要配持久化数据源），这是学习阶段的正常现象。

### 7.7 常见坑

| 现象 | 原因 | 解决 |
| --- | --- | --- |
| 配了规则不生效 | @SentinelResource 没生效 | 检查 SentinelConfig 切面 Bean 是否加上 |
| 控制台里看不到 order-service | dashboard 地址配错 / 没调用过接口 | Sentinel 是"懒加载"，先调用一次接口再看 |

**阶段 7 完成 ✅**。五个组件还剩最后一个：消息队列。

---

## 阶段 8：接入 RocketMQ —— 下单后异步更新销量

**本阶段目标**：下单成功后，订单服务往 RocketMQ 发一条消息；菜品服务消费这条消息，把对应菜品的销量 +N。下单和"更新销量"从此是两件互不阻塞的事。

### 8.1 启动 RocketMQ

```powershell
cd C:\Users\86138\Desktop\code\delivery\scripts
.\start-rocketmq.ps1
```

### 8.2 两个服务的 pom 都加客户端依赖

```xml
        <!-- RocketMQ 客户端：订单服务当生产者，菜品服务当消费者 -->
        <dependency>
            <groupId>org.apache.rocketmq</groupId>
            <artifactId>rocketmq-spring-boot-starter</artifactId>
            <version>2.3.6</version>
        </dependency>
```

### 8.3 两个服务的 yml 都加配置

```yaml
rocketmq:
  name-server: 127.0.0.1:9876   # NameServer 地址（消息队列的"地址簿"）
```

order-service 再多一块（它要当生产者）：

```yaml
rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    group: order-producer-group   # 生产者组
```

### 8.4 order-service：发消息

**`OrdersServiceImpl` 改动**：

1. 注入 `RocketMQTemplate`；
2. `createOrder` 扣库存成功之后，发一条消息。

```java
import org.apache.rocketmq.spring.core.RocketMQTemplate;          // 新增 import
import org.springframework.messaging.support.MessageBuilder;      // 新增 import

    private final RocketMQTemplate rocketMQTemplate;              // 新增字段

    public OrdersServiceImpl(OrdersMapper ordersMapper, DishClient dishClient,
                             RocketMQTemplate rocketMQTemplate) { // 构造器加参数
        this.ordersMapper = ordersMapper;
        this.dishClient = dishClient;
        this.rocketMQTemplate = rocketMQTemplate;
    }

    // createOrder 里，扣库存成功之后、return 之前，加：
        // 发送 RocketMQ 消息：菜品服务消费后更新销量
        String msg = "{\"dishId\":" + dto.getDishId() + ",\"quantity\":" + quantity + "}";
        rocketMQTemplate.syncSend("order-created-topic", MessageBuilder.withPayload(msg).build());
```

### 8.5 food-service：写消费者

**① `DishMapper` 加一个加销量的方法**：

```java
    /** 增加销量（由 RocketMQ 消费者调用） */
    @Update("UPDATE dish SET sales = sales + #{num} WHERE id = #{id}")
    int addSales(@Param("id") Long id, @Param("num") Integer num);
```

**② `DishService` 加声明、`DishServiceImpl` 加实现**：

```java
    // DishService 接口里：
    void addSales(Long id, Integer quantity);

    // DishServiceImpl 里：
    @Override
    public void addSales(Long id, Integer quantity) {
        dishMapper.addSales(id, quantity);
    }
```

**③ 消费者类（RocketMQ 的"灵魂"就这一个注解）**：

**文件：** `food-service/src/main/java/com/delivery/food/mq/OrderCreatedConsumer.java`

```java
package com.delivery.food.mq;

import com.delivery.food.service.DishService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 订单消息消费者：
 *   topic         监听的队列名，必须和生产端一致；
 *   consumerGroup 消费者组，同组多个实例会平分消息。
 */
@Component
@RocketMQMessageListener(topic = "order-created-topic", consumerGroup = "food-service-sales-consumer")
public class OrderCreatedConsumer implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedConsumer.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DishService dishService;

    public OrderCreatedConsumer(DishService dishService) {
        this.dishService = dishService;
    }

    @Override
    public void onMessage(String message) {
        // 消息格式：{"dishId":1,"quantity":2}
        try {
            JsonNode node = MAPPER.readTree(message);
            Long dishId = node.get("dishId").asLong();
            int quantity = node.get("quantity").asInt();
            dishService.addSales(dishId, quantity);
            log.info(">>> 收到订单消息并更新销量：dishId={}, +{} 份", dishId, quantity);
        } catch (Exception e) {
            log.error(">>> 消费订单消息失败：{}", e.getMessage());
        }
    }
}
```

### 8.6 验收

```powershell
# ① 记下当前销量
curl.exe -s http://127.0.0.1:8081/dish/3

# ② 下一单（牛肉拉面 x1）
curl.exe -s -X POST http://127.0.0.1:8082/order/create -H "Content-Type: application/json" -d '{"dishId":3,"quantity":1}'

# ③ 等两三秒再查销量
Start-Sleep -Seconds 3
curl.exe -s http://127.0.0.1:8081/dish/3
# 预期：sales 增加了 1
```

food-service 控制台里应该打印：`>>> 收到订单消息并更新销量：dishId=3, +1 份`。

### 8.7 常见坑与诚实说明

| 现象 | 原因 | 解决 |
| --- | --- | --- |
| 消息发出去了但销量没变 | 消费者没起来 / topic 名字不一致 | 看 food-service 日志；核对两处 topic |
| 启动报连不上 `127.0.0.1:9876` | RocketMQ 没启动 | 先执行 8.1 |
| 消费失败只打了日志 | 本项目消费者异常只做记录 | 生产环境要返回重试 / 进死信队列，进阶内容 |

另外要明白本项目的"缺陷"（面试常被追问）：消息是在全局事务**提交前**发的，理论上存在"消息发了但事务回滚"的窗口。生产项目要用**事务消息 / 本地消息表**保证一致——这正是你继续深入 RocketMQ 的入口（另一篇教程 9.5 有说明）。

**阶段 8 完成 ✅**。五大组件全部接入完毕！

---

## 阶段 9：整体验收与收尾

### 9.1 从头到脚的"四步验收法"

每次重启环境后，按这四步走一遍，全通过就说明项目是健康的：

1. **查菜品**：`curl http://127.0.0.1:8081/dish/list` → 4 条数据；
2. **正常下单**：`dishId=2, quantity=2` → totalPrice=64.00，库存 -2，销量 +2；
3. **回滚验证**：`dishId=1, quantity=99` → 报"库存不足"，订单表无脏数据、库存不变；
4. **限流验证**：给 `createOrder` 配 QPS=1，快速连发 → 出现"下单人数过多"。

### 9.2 环境依赖清单（重启电脑后的启动顺序）

```
MySQL（常驻）
 → Nacos / Seata / Sentinel / RocketMQ（scripts 四个脚本）
 → food-service（8081）→ order-service（8082）
```

微服务必须最后启动；组件没起完就启动微服务，会报连不上 Nacos / Seata（这就是真实踩过的坑）。

### 9.3 卡住了去哪查

每个阶段末尾都附了简短排错表；更全的报错手册在另一篇教程**第 12 章**（含本项目真实踩过的全部坑：`&&` 路径、LoadBalancer 缺失、Seata 拦截器重名、undo_log 缺失等）。

### 9.4 和"成品代码"对照

你手写的每一步，`C:\Users\86138\Desktop\code\delivery` 里的成品项目都是**标准答案**。两个建议：

- 写完某阶段后，用 IDEA 对比对应文件（包名、端口、路径完全一致）；
- 如果某处行为不一样，先对比 pom 和 yml——十有八九是依赖或配置差了一行。

### 9.5 下一步练什么

- 给菜品列表加 **Redis 缓存**（缓存穿透/击穿概念顺带入脑）；
- 加 **Gateway** 把 8081/8082 合成一个对外入口；
- 把 **Sentinel 规则持久化到 Nacos**（解决"重启即丢"）；
- 把 `orders` 表拆到独立数据库，体验"真·分库"后 Seata 依然生效；
- 用 JMeter 压测下单接口，亲手把 Sentinel 的 QPS 阈值调出来。

### 附：手写项目最终文件清单（按阶段）

| 阶段 | 新增/修改 |
| --- | --- |
| 1 | 根 pom、两个模块 pom（骨架）、两个启动类 |
| 2 | food：yml、Result、GlobalExceptionHandler、Dish、DishMapper、DishService(Impl)、DishController；SQL：dish 表 |
| 3 | food：pom(+nacos/BOM)、yml(+nacos)、启动类(+@EnableDiscoveryClient) |
| 4 | order：pom、yml、启动类、Result、异常处理、Orders、OrdersMapper、OrderCreateDTO、OrderDetailVO、OrdersService(Impl mock 版)、OrderController；SQL：orders 表 |
| 5 | order：pom(+feign/loadbalancer)、启动类(+@EnableFeignClients)、DishClient、DishDTO、OrdersServiceImpl 改造 |
| 6 | 两服务：pom(+seata)、yml(+seata)；food：deductStock 三处；order：DishClient+方法、@GlobalTransactional+扣库存调用；SQL：undo_log 表 |
| 7 | order：pom(+sentinel)、yml(+sentinel)、SentinelConfig、OrderController 注解改造 |
| 8 | 两服务：pom(+rocketmq)、yml(+rocketmq)；order：发消息；food：addSales、OrderCreatedConsumer |

> 至此，你已经从零把 Nacos + OpenFeign + Seata + Sentinel + RocketMQ 全部亲手落地了一遍。
> 下一步不是背，而是**讲**：把你的手写项目和另一篇教程对照着读一遍，试着不看文档把一次下单的完整链路画出来——画得出来，这个项目就真正属于你了。



