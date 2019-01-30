package com.jiangkang.ktools

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.os.Environment
import android.provider.ContactsContract
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.View
import com.jiangkang.ktools.service.AIDLDemoActivity
import com.jiangkang.ktools.share.ShareActivity
import com.jiangkang.tools.permission.RxPermissions
import com.jiangkang.tools.struct.JsonGenerator
import com.jiangkang.tools.system.ContactHelper
import com.jiangkang.tools.utils.ClipboardUtils
import com.jiangkang.tools.utils.ShellUtils
import com.jiangkang.tools.utils.SpUtils
import com.jiangkang.tools.utils.ToastUtils
import dalvik.system.DexClassLoader
import kotlinx.android.synthetic.main.activity_system.*
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import org.jetbrains.anko.wallpaperManager
import org.json.JSONException
import org.json.JSONObject
import java.io.File


/**
 * Created by jiangkang on 2017/9/5.
 * description：与系统相关的Demo
 * 1.打开通讯录，选择联系人，获得联系人姓名和手机号
 * 2.获取联系人列表
 * 3.设置文本到剪贴板，从剪贴板中取出文本
 */

@SuppressLint("LogUtilsNotUsed")
class SystemActivity : AppCompatActivity() {

    private var jsonObject: JSONObject? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system)
        title = "System"
        handleClick()

    }

    private fun handleClick() {

        btn_open_contacts.onClick {
            val rxPermissions = RxPermissions(this@SystemActivity)
            rxPermissions.request(Manifest.permission.READ_CONTACTS)
                    .subscribe { granted ->
                        if (granted!!) {
                            gotoContactPage()
                        } else {
                            ToastUtils.showShortToast("权限被拒绝")
                        }
                    }
        }


        btn_get_all_contacts.onClick {
            onBtnGetAllContactsClicked()
        }

        btn_set_clipboard.onClick {
            onBtnSetClipboardClicked()
        }

        btn_exit_app.onClick {
            onClickBtnExitApp()
        }

        btnQuickSettings.setOnClickListener {

            runBlocking {
                val result = async {
                    ShellUtils.execCmd("adb shell setprop debug.layout true", false)
                }
                async {
                    ShellUtils.execCmd("adb shell am start com.android.settings/.DevelopmentSettings", false)
                }
                toast("result : $result")
            }

        }

    }


    private fun gotoContactPage() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        startActivityForResult(intent, REQUEST_PICK_CONTACT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_PICK_CONTACT) {
            if (resultCode == Activity.RESULT_OK) {
                if (data ==
                        null) {
                    Log.d(TAG, "onActivityResult: 什么东西都没有选")
                } else {
                    Log.d(TAG, "onActivityResult: 有返回")
                    val cursor = contentResolver.query(
                            data.data!!, null, null, null, null
                    )
                    val contact = handleCursor(cursor)
                    try {
                        AlertDialog.Builder(this)
                                .setTitle("选择的联系人信息")
                                .setMessage(contact.toString(4))
                                .setNegativeButton("关闭") { dialog, which -> dialog.dismiss() }
                                .create()
                                .show()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleCursor(data: Cursor?): JSONObject {
        var result = JSONObject()
        if (data != null && data.moveToFirst()) {
            do {
                val name = data.getString(
                        data.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                )
                val id = data.getInt(
                        data.getColumnIndex(ContactsContract.Contacts._ID)
                )

                //指定获取NUMBER这一列数据
                val phoneProjection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)

                val cursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        phoneProjection,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + id, null, null
                )
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        val number = cursor.getString(
                                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        )

                        result = JsonGenerator()
                                .put("name", name)
                                .put("tel", number)
                                .gen()
                    } while (cursor.moveToNext())
                }

            } while (data.moveToNext())
        }

        return result
    }

    private fun onBtnGetAllContactsClicked() {
        val rxPermissions = RxPermissions(this)
        rxPermissions.request(Manifest.permission.READ_CONTACTS)
                .subscribe { aBoolean ->
                    if (aBoolean!!) {
                        getContactList()
                    } else {
                        ToastUtils.showShortToast("权限被拒绝")
                    }
                }
    }

    private fun getContactList() {
        val helper = ContactHelper(this)
        Thread(Runnable {
            jsonObject = helper.queryContactList()
            runOnUiThread {
                try {
                    AlertDialog.Builder(this@SystemActivity)
                            .setTitle("通讯录")
                            .setMessage(jsonObject!!.toString(4))
                            .setPositiveButton("关闭") { dialog, which -> dialog.dismiss() }
                            .setNegativeButton("复制") { dialog, which ->
                                try {
                                    ClipboardUtils.putStringToClipboard(jsonObject!!.toString(4))
                                    ToastUtils.showShortToast("复制成功")
                                } catch (e: JSONException) {
                                    e.printStackTrace()
                                }

                                dialog.dismiss()
                            }
                            .show()
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }).start()

    }

    private fun onBtnSetClipboardClicked() {
        if (!TextUtils.isEmpty(et_content.text)) {
            val content = et_content.text.toString()
            ClipboardUtils.putStringToClipboard(content)
            ToastUtils.showShortToast("设置成功")
        } else {
            ToastUtils.showShortToast("内容不能为空")
        }

    }

    private fun hideVirtualNavbar(activity: Activity) {
        val decroView = activity.window.decorView
        val uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        decroView.systemUiVisibility = uiOptions
    }

    private fun hideStatusBar() {
        val decorView = window.decorView
        // Hide the status bar.
        val uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
        decorView.systemUiVisibility = uiOptions
        // Remember that you should never show the action bar if the
        // status bar is hidden, so hide that too if necessary.
        val actionBar = actionBar
        actionBar?.hide()
    }

    private fun changeLauncherIcon() {
        val packageManager = packageManager

        val componentName0 = ComponentName(baseContext, "com.jiangkang.ktools.MainActivity")
        val componentName1 = ComponentName(baseContext, "com.jiangkang.ktools.MainActivity1")

        if ("1" == SpUtils.getInstance(this, "Launcher").getString("icon_type", "0")) {

            packageManager.setComponentEnabledSetting(
                    componentName1,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
            )

            packageManager.setComponentEnabledSetting(
                    componentName0,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
            )

            SpUtils.getInstance(this, "Launcher")
                    .putString("icon_type", "0")

        } else {

            packageManager.setComponentEnabledSetting(
                    componentName0,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
            )

            packageManager.setComponentEnabledSetting(
                    componentName1,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
            )

            SpUtils.getInstance(this, "Launcher")
                    .putString("icon_type", "1")
        }

        ToastUtils.showShortToast("Icon替换成功，稍等一段时间方可看到效果")

    }

    private fun onClickBtnExitApp() {
        exitApp()
    }

    private fun exitApp() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        hideVirtualNavbar(this)
    }

    fun onBtnChangeIconClicked(view: View) {
        changeLauncherIcon()
    }

    fun onAIDLClicked(view: View) {
        AIDLDemoActivity.launch(this, null)
    }

    fun onHideAppIconClicked(view: View) {
        val manager = packageManager
        val componentName = ComponentName(this, MainActivity::class.java)
        val status = manager.getComponentEnabledSetting(componentName)
        if (status == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT || status == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            manager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        } else {
            manager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP)
        }
    }

    fun onBtnHideStatusBarClicked(view: View) {
        hideStatusBar()
    }

    fun onBtnHideVirtualNavbarClicked(view: View) {
        hideVirtualNavbar(this)
    }

    fun onBtnLoadDexClicked(view: View) {
        val jarFile = File(
                Environment.getExternalStorageDirectory().absolutePath + File.separator + "ktools",
                "hello_world_dex.jar"
        )

        if (!jarFile.exists()) {
            //todo:这里应该从assets中复制到sdcard中
            ToastUtils.showShortToast("文件不存在")
        } else {
            val loader = DexClassLoader(
                    jarFile.absolutePath,
                    externalCacheDir!!.absolutePath, null,
                    classLoader
            )
            try {

                val clazz = loader.loadClass("com.jiangkang.ktools.HelloWorld")

                val iSayHello = clazz.newInstance() as ISayHello

                ToastUtils.showLongToast("执行dex中方法：" + iSayHello.sayHello())

            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: InstantiationException) {
                e.printStackTrace()
            }

        }
    }

    fun onBtnGetClipboardClicked(view: View) {
        ToastUtils.showShortToast(ClipboardUtils.getStringFromClipboard())
    }

    fun onBtnChangeWallpaper(view: View) {
        wallpaperManager.setResource(R.raw.wallpaper)
        ToastUtils.showShortToast("壁纸更换成功！")
    }

    fun onBtnShare(view: View) {
        startActivity<ShareActivity>()
    }

    companion object {

        private val REQUEST_PICK_CONTACT = 1112
        private val TAG = SystemActivity::class.java.simpleName
    }
}
