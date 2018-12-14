package com.jiangkang.ktools

import android.app.ActivityManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import com.jiangkang.tools.device.DeviceUtils
import com.jiangkang.tools.device.ScreenUtils
import com.jiangkang.tools.service.WatchingTopActivityService
import com.jiangkang.tools.utils.AppUtils
import com.jiangkang.tools.utils.NetworkUtils
import com.jiangkang.tools.utils.ShellUtils
import com.jiangkang.tools.widget.KDialog
import kotlinx.android.synthetic.main.activity_device.*
import org.jetbrains.anko.activityManager
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.toast

class DeviceActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device)
        title = "Device"
        handleClick()
    }


    private fun handleClick() {

        btnCheckNetworkInfo.onClick {
            val builder = StringBuilder()
            builder.append(String.format("网络类型: %s\n", NetworkUtils.getNetWorkType()))
                    .append(String.format("Mac地址: %s\n", NetworkUtils.getMacAddress()))
            KDialog.showMsgDialog(this@DeviceActivity, builder.toString())
        }

        btnScreenBrightness.onClick {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(this@DeviceActivity)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))
                    toast("授权用来修改系统屏幕亮度").show()
                } else {
                    val currentScreenBrightness = ScreenUtils.getScreenBrightness(this@DeviceActivity)
                    ScreenUtils.setScreenBrightness(this@DeviceActivity, 255)
                    KDialog.showMsgDialog(this@DeviceActivity, "原来的屏幕亮度为：$currentScreenBrightness \n现在屏幕亮度已经设置到最大")
                }
            }

        }

        btnCurrentWindowBrightness.onClick {
            ScreenUtils.setCurrentWindowScreenBrightness(this@DeviceActivity, 125)
            toast("将亮度修改到了125,只对当前页面有效").show()
        }


        btnCheckCurrentActivity.onClick {
//            KDialog.showMsgDialog(this@DeviceActivity, AppUtils.currentActivity)
            startService(Intent(this@DeviceActivity,WatchingTopActivityService::class.java))
        }



        btnAdbWireless.onClick {
            doAsync {
                val command = "setprop service.adb.tcp.port 5555 && stop adbd && start adbd"
                val result = ShellUtils.execCmd(command, true)
                runOnUiThread {
                    val ip = DeviceUtils.getIPAddress(this@DeviceActivity)
                    KDialog.showMsgDialog(this@DeviceActivity, "adb connect " + ip!!)
                }
            }
        }

        btnGetMaxMemory.onClick {
            val maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024)
            val memoryInfo = getAvailableMemory()

            val lowMemory = memoryInfo.lowMemory
            val availableMemory = memoryInfo.availMem / (1024 * 1024)
            val totalMem = memoryInfo.totalMem / (1024 * 1024)
            val threshold = memoryInfo.threshold / (1024 * 1024)

            val msg = "分配给App的最大堆内存为：$maxMemory M \n" +
                    "是否处于低内存状态：$lowMemory \n" +
                    "系统可用内存：$availableMemory M\n" +
                    "系统总运行内存： $totalMem M\n" +
                    "低内存阈值：$threshold M"
            KDialog.showMsgDialog(this@DeviceActivity,msg)
        }

    }

    private fun getAvailableMemory(): ActivityManager.MemoryInfo {
        var memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo
    }


    override fun onDestroy() {
        super.onDestroy()
    }


    companion object {
        private val TAG = DeviceActivity::class.java.simpleName
    }


}
