# 后端接口

- 所有请求的根路径为`/dws`，以下每个模块的url均代表相对路径，例如用户登录url为`/user/login`，完整url为`/dws/user/login`。
- 所有响应默认包含：`{statusCode: 0x00, message: "Success"}`，其中`statusCode`为状态码，只有0x00代表成功，其余情况均为异常，异常信息由`message`字段输出。下述模块仅列出除上述响应体外的字段。
- 文档中为阅读方便，将GET请求参数写作了JSON格式，实际构造请求时，请按GET请求规范附到url上。
- 默认所有请求均携带自定义Header：X-USER-TOKEN，该Header值来源于用户登录后返回的Cookie，即dwsToken。

## 一、用户相关

1. 用户登录
```json
{
  url: /user/login
  type: POST
  params: {
    username: "admin",
    password: "123"
  }
  response: {
    Cookie: dwsToken="2b59a6af-9486-3d42-bbe0-fad2b91c3b75-4fbebd6333c608650f8ffac889d4e0e8d3d631353a729153871b24f43a523c08426729773d142339042aaca3b774abd31e3374d7c698dc09fb096726319875039452a261fc3fc5642b2c888c0f8e386307b42ad2" // 返回cookie
  } 
}
```

2. 用户登出
```json
{
  url: /user/logout
  type: POST
  params: {
    username: "123"
  }
  response: {} 
}
```

3. 用户注册
```json
{
  url: /user/register
  type: POST
  params: {
    username: "dataUser",
    userNickname: "张三",
    systemId: "fbe90c27-d667-3268-b513-02846c67789d",
    functions: 0, // 0: file, 1: db, 2: all
  }
  response: {
    userPassword: "123456" // 登录口令
  }
}
```

## 二、接入业务系统相关（仅Admin调用）

1. 业务系统注册
```json
{
  url: /app/register
  type: POST
  params: {
    systemName: "dataOwner",
    systemNickname: "XX系统",
    dwUsage: 0, // 0: watermark, 1: fingerprint, 2: all
    functions: 0, // 0: file, 1: db, 2: all
    dbParams: {
      dbName: "dw", // 数据库名
      dbUser: "pkc", // 数据库用户名
      dbPassword: "123456", // 数据库口令
      dbIP: "127.0.0.1", 
      dbPort: "3306", 
      dbType: "MySQL" // 支持数据库种类
    }
  }
  response: {
    systemPassword: "123456" // 登录口令
    apiUser: "fbe90c27-d667-3268-b513-02846c67789d" // systemId，UUID的形式，业务系统通过该ID和authKey访问水印系统的接口
    apiAuthKey: "f200b417e9795811b926a42a66ab3e69" // authKey，16进制
  }
}
```

2. 获取已注册系统列表 
```json
{
  url: /app/list
  type: POST
  params: {}
  response: {
    systemCount: 1
    systemList: [
      {
        systemName: "dataOwner",
        systemNickname: "XX业务系统",
        systemId: "fbe90c27-d667-3268-b513-02846c67789d"
      }
    ]
  }
}
```
<!-- selectedAlgorithms: "算法1,算法2" // 水印嵌入算法，用英文逗号连接，可选参数 -->

## 三、数据库相关

### 3.1 基础信息
1. 获取数据库列表
```json
{
  url: /app/dbList
  type: GET
  params: {}
  response: {
    dbCount: // 数据库个数
    dbInfoList: [
      {
        dbName: "dw", // 数据库名
        default: true // 默认选中
      }, 
      {dbName: "test", default: false}
    ]
  }
}
```

2. 获取数据库表
```json
{
  url: /db/tableList
  type: GET
  params: {
    dbName: "dw"
  }
  response: {
    tableInfoList: [
      {
        tableName: "test_data", 
        tableFieldCount: 2 // 字段数
        tableFields: [
          {
            fieldName: "id_number", // 字段名
            fieldType: "TEXT" // 字段类型
            primaryKey: true // 是否为主键
          }, 
          {fieldName: "bills", fieldType: "DOUBLE", primaryKey: false}
        ],
        default: true // 默认选中
      }, 
    ]
  }
}
```

3. 获取表数据
```json
{
  url: /db/tableData
  type: GET
  params: {
    dbName: "dw",
    tableName: "test_data"
    page: 1 // 所选页，可选参数，用于分页
    pageCount: 50 // 每页展示数据数，可选参数，用于分页
  }
  response: {
    dataCount: 100 // 当前表数据总数
    dataList: [
      [
        "1000000000", // 表数据值，与字段顺序对应，例如，当前为"id_number"字段值，返回值均为字符串
        "123.45"
      ], 
      ["11111111", "32.23", ...]
    ]
  }
}
```

### 3.2 水印相关
1. 获取水印算法（非必须）
```json
{
  url: /db/algorithms
  type: GET
  params: {}
  response: {
    algorithmCount: 3,
    algorithmList: [
      {
        algorithmName: "模式搜索算法",
        supportType: "DOUBLE"
      }, 
      {
        algorithmName: "字符嵌入算法",
        supportType: "TEXT"
      }, 
    ]
  }
}
```

2. 嵌入水印
```json
{
  url: /db/embed/full
  type: POST
  params: {
    dbName: "dw",
    tableName: "test_data"
    selectedFields: [
      {
        fieldName: "bills",
        algorithm: "算法1" // 可选，默认为空
      }, 
      {fieldName: "count", algorithm: "算法2"}
    ] // 选择嵌入水印的字段
    shouldOutputToDB: true // 默认为true，表示水印数据输出到数据库中
    outputTable: "test_data_dw" // 水印数据输出表，可选参数，默认输出到随机生成的表中
    embeddedMessage: "admin" // 嵌入信息，可选参数，默认为用户Id。
  }
  response: {
    dataCount: 100 // 水印数据总数，若statusCode不为0x00，则字段为空
    dataList: [
      [
        "1000000000", // 表数据值，与字段顺序对应，例如，当前为"id_number"字段值
        123.45
      ], 
      ["11111111", 32.23, ...]
    ] // 若statusCode不为0x00，则字段为空
    dataId: "test_data_123456" // 数据ID，若statusCode不为0x00，则字段为空
  }
}
```

3. 导出CSV

下载水印数据CSV文件。
```json
{
  url: /db/embed/csv
  type: GET
  params: {
    dataId: "test_data_123456",
  }
  response: {
    Header: {
      Content-Type: "application/octet-stream" // 标准http相应类型
      File-Name: "xxx.csv" // 文件名
    } //http响应头
    Body: buffer // 文件字节流
  }
}
```

4. 提取水印（直连数据库方式，仅Admin调用）
```json
{
  url: /db/extract/table
  type: POST
  params: {
    systemId: "2b59a6af-9486-3d42-bbe0-fad2b91c3b75",
    dbName: "dw",
    tableName: "test_data"
    selectedFields: [
      {
        fieldName: "bills",
        algorithm: "算法1" // 可选，默认为空
      }, 
      {fieldName: "count", algorithm: "算法2"}
    ] // 嵌入水印的字段，可选参数
  }
  response: {
    extractedMessage: "123" // 提取出的信息，若statusCode不为0x00，则字段为空
  }
}
```

5. 提取水印（CSV文件，仅Admin调用）
```json
{
  url: /db/extract/csv
  type: POST
  params: {
    systemId: "2b59a6af-9486-3d42-bbe0-fad2b91c3b75",
    selectedFields: [
      {
        fieldName: "bills",
        fieldType: "DOUBLE", // 必选项，指定字段类型
        algorithm: "算法1" // 可选，默认为空
      }, 
    ] // 嵌入水印的字段，可选参数
    csvFile: abc.csv // 上传csv文件，Content-Type为 multipart/form-data 类型
    primaryKey: "id_number" // 必选项，指定主键
  }
  response: {
    extractedMessage: "123" // 提取出的信息，若statusCode不为0x00，则字段为空
  }
}
```

## 四、办公文件相关
1. 获取文件列表
```json
{
  url: /file/fileList
  type: GET
  params: {}
  response: {
    fileCount: 5 // 文件个数
    fileList: [
      {
        fileName: "abc.docx" 
        fileId: "123456"
        fileType: "docx"  
      }, 
    ]
  }
}
```
2. 上传文件（仅上传）
```json
{
  url: /file/upload
  type: POST
  params: {
    fileName: "abc.docx"
    file: abc.docx // 上传文件，Content-Type为 multipart/form-data 类型
  }
  response: {
    fileId: "123456"
  }
}
```

3. 下载文件
```json
{
  url: /file/download
  type: GET
  params: {
    fileName: "abc.docx"
    fileId: "123456"
  }
  response: {
    Header: {
      Content-Type: "application/octet-stream" // 标准http相应类型
    } //http响应头
    Body: buffer // 文件字节流
  }
}
```

4. 嵌入水印 
```json
{
  url: /file/embed
  type: POST
  params: {
    fileName: "abc.docx"
    file: abc.docx // 上传文件，Content-Type为 multipart/form-data 类型
    filePassword: "123456" // 文档加密口令，可选参数
    shouldReturnedFile: true // 是否立即返回嵌入水印的文件，默认为true
    embeddedMessage: "admin" // 嵌入信息，可选参数，默认为用户Id。
  }
  response: {
    dwFileId: "123456" // 嵌入水印的文件Id，当shouldReturnedFile字段为false时返回；为true时直接返回文件流。
    Header: {
      Content-Type: "application/octet-stream" // 标准http相应类型
    } //http响应头
    Body: buffer // 文件字节流
  }
}
```
5. 提取水印
```json
{
  url: /file/extract
  type: POST
  params: {
    systemId: "2b59a6af-9486-3d42-bbe0-fad2b91c3b75",
    fileName: "abc.docx"
    file: abc.docx // 上传水印文件，Content-Type为 multipart/form-data 类型
    filePassword: "123456" // 文档加密口令，可选参数
  }
  response: {
    extractedMessage: "123" // 提取出的信息，若statusCode不为0x00，则字段为空
  }
}
```