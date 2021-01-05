## UnityNativeEdit v1.6
Unity Native Input Plugin for both iOS and Android (UGUI InputField &  ```TextmeshPro``` compatible).

This means you don't need a separate 'Unity' Input box and you can use all native text functions such as `Select`, `Copy` and `Paste`.

### Usage
1. Simply copy the files in `release/NativeEditPlugin` into your existing unity project asset folder.
OR
Install in Unity via UPM using the git url https://github.com/GoShikhar/UnityNativeEdit.git

2. Attach ```NativeEditBox``` script to your  ```UnityUI InputField``` or ```TMP_InputField``` object.

3. Build and run on your android or ios device!

4. The Return key is binded to the TextMeshPro's invokable function. So, just add a callback (optional) to your textmeshpro input field and the native edit box will fill the TextmeshPro input field with the current text.


### Building the Android plugin
If you want to tinker with the project yourself you need to build the Android project again in AndroidStudio (for iOS you can just modify the Objective-C code and it will get built at the same time as the Unity project). 

1. Open the `src/androidProj` directory in AndroidStudio.
2. Select View -> Tool Windows -> Gradle in AndroidStudio.
3. In Gradle run the :nativeeditplugin -> other -> makeJar task.
4. It's a bit confusing but the task seems to generate .aar files (even though it was called makeJar, not sure what's up with that) in the `src/androidProj/nativeeditplugin/build/outputs/aar` directory.
5. To test in the demo Unity project copy the `nativeeditplugin-release.aar` file (from the output directory) to the `release\NativeEditPlugin\Plugins\Android` directory. This file is symlinked to the Unity demo project.

### Etc
1. NativeEditBox will work with delegate defined in your Unity UI InputField, `On Value Change` and `End Edit`, if you're using TMP_InputField then the delegates are auto Invoked on your TMP_InputField.
2. It's open source and free to use/redistribute!

- - -
## UnityNativeEdit v1.6 中文说明
UnityNativeEdit是适用于Unity、支持iOS和Android的原生输入框插件，免去直接使用UGUI的InputField产生的键盘方面的不便，并且可以和原生应用一样方便地对输入文本进行选择、复制和粘贴等操作。

本repo的1.6版本针对[原版](https://github.com/YousicianGit/UnityNativeEdit/)进行了各种优化和bug修复，无需像原版那样要事先挂载`PluginMsgHandler`脚本，并且自2017年起被某国产知名二次元手机游戏使用。

### 使用方法
1. 直接拷贝`release/NativeEditPlugin`目录下的文件到你的项目中；
2. 在你的InputField对象上添加`NativeEditBox`脚本组件。

    添加后，如果要通过代码修改输入框的文本的话，请务必通过`NativeEditBox`脚本的`text`属性进行操作，否则将不会看到修改后的文本。
3. 发布到真机上试试吧！

### 生成插件
如果你想自行修改插件，对安卓平台而言，你需要在Android Studio中重新生成；而对iOS平台而言，只需修改 Objective-C 代码即可。

1. 在Android Studio中打开文件夹`src/androidProj`；
2. 选择 View -> Tool Windows -> Gradle ；
3. 运行 :nativeeditplugin -> other -> makeJar 任务；
4. 该任务会在`src/androidProj/nativeeditplugin/build/outputs/aar`下生成.aar文件（即使任务名字叫做makeJar）；
5. 拷贝`nativeeditplugin-release.aar`至`release\NativeEditPlugin\Plugins\Android`文件夹。

### Etc
1. 本插件可以响应同一GameObject上的InputField的`OnValueChanged`和`OnEndEdit`事件。
2. 本插件开源且可免费使用、分发。
