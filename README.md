# Android 系统的权限申请管理

我是站在巨人的肩膀上修改的东西，感谢[tbruyelle](https://github.com/tbruyelle/RxPermissions)，我没有具体改动逻辑，我只是enchance

 我改动的地方，应该说添加的功能
 * 添加了特殊权限的使用，如android.permission.SYSTEM_ALERT_WINDOW和android.permission.WRITE_SETTINGS,使用方式还是一样
  * 统一处理了，申请被拒绝的情况，就是申请几次都被拒绝了的话，会有提示框，提示要到系统响应的地方开启
  
这个库目前来时，我觉得是比较好，应该说是非常好的Android权限处理方式，只需几行代码就可以处理权限申请，非常棒，具体使用请看[tbruyelle](https://github.com/tbruyelle/RxPermissions)