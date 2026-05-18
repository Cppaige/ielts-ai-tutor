# 测试错误码记录

## 认证测试

### 注册

后端添加限制：邮箱号有正确格式；密码大小写

> 成功：200
>
> 邮箱有人注册过
>
> ```json 
> {
>     "code": 409,
>     "message": "Email already registered"
> }
> ```

### 登陆

> 成功：200
>
> 用户名/密码错误
>
> ```json
> {
>     "code": 401,
>     "message": "Invalid credentials"
> }
> ```

## 认证边界测试

> 成功：200
>
> 不带token / 无效token
>
> ```json
> {
>     "code": 401,
>     "message": "Unauthorized"
> }
> ```
>
> 

## 口语话题查询

### 按 Part 查询口语话题列表

> 后端修改：不存在的part或者类别也会成功返回，例如part5
>
> 成功：200
>
> 不传part
>
> ```json
> {
>     "code": 500,
>     "message": "Internal server error"
> }
> ```
>
> 