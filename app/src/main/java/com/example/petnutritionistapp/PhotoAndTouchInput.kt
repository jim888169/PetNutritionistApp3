package com.example.petnutritionistapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.petnutritionistapp.api.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.storageMetadata
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream

class PhotoAndTouchInputActivity : AppCompatActivity() {

    private lateinit var imgPreview: ImageView
    private lateinit var btnTakePhoto: Button
    private lateinit var spinnerRibs: Spinner
    private lateinit var spinnerWaist: Spinner
    private lateinit var spinnerStomach: Spinner
    private lateinit var btnAnalyze: Button

    // Firebase Storage
    private val storageRef by lazy { FirebaseStorage.getInstance().reference }
    private var uploadedImageUrl: String? = null

    // ===== Retrofit =====
    private val BASE_URL = "https://asia-east1-petnutritionist-f1f8d.cloudfunctions.net/api/" // 末尾要有 /
    private val api by lazy { provideApi(BASE_URL) }

    private fun provideApi(baseUrl: String): ApiService {
        val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder().addInterceptor(logger).build()
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(ApiService::class.java)
    }

    // ===== Camera launchers =====
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bmp = result.data?.extras?.get("data") as? Bitmap
            if (bmp != null) {
                imgPreview.setImageBitmap(bmp)
                // 拍完立即上傳，完成後才允許分析
                uploadBitmapToStorage(bmp) { url ->
                    uploadedImageUrl = url
                    btnAnalyze.isEnabled = url != null
                    if (url == null) {
                        Toast.makeText(this, "圖片上傳失敗（看上方錯誤訊息）", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "圖片已上傳完成", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "無法取得拍攝影像", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCamera()
        else Toast.makeText(this, "未授權相機，無法拍照", Toast.LENGTH_LONG).show()
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            cameraLauncher.launch(intent)
        } else {
            Toast.makeText(this, "此裝置不支援相機", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_and_touch_input)

        // ===== Firebase 初始化 +（選擇性）App Check Debug Provider =====
        FirebaseApp.initializeApp(this)
        runCatching {
            val appCheck = FirebaseAppCheck.getInstance()
            appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
        }

        imgPreview = findViewById(R.id.imgPreview)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        spinnerRibs = findViewById(R.id.spinnerRibs)
        spinnerWaist = findViewById(R.id.spinnerWaist)
        spinnerStomach = findViewById(R.id.spinnerStomach)
        btnAnalyze = findViewById(R.id.btnAnalyze)

        // ✅ 立刻允許拍照（不再因登入 gating）
        btnTakePhoto.isEnabled = true

        // （可留可拿掉）匿名登入：失敗只提示，不影響拍照
                // 初始化下拉
        ArrayAdapter.createFromResource(this, R.array.ribs_array, android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerRibs.adapter = it
        }
        ArrayAdapter.createFromResource(this, R.array.waist_array, android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerWaist.adapter = it
        }
        ArrayAdapter.createFromResource(this, R.array.stomach_array, android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerStomach.adapter = it
        }

        // 尚未上傳完成前，先鎖分析鍵
        btnAnalyze.isEnabled = false

        // 拍照：先請求權限，通過後開啟相機
        btnTakePhoto.setOnClickListener {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }

        // 分析：需要已經有上傳成功的圖片 URL
        btnAnalyze.setOnClickListener {
            val url = uploadedImageUrl
            if (url.isNullOrEmpty()) {
                Toast.makeText(this, "請先拍照並等待圖片上傳完成", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val ribsText = spinnerRibs.selectedItem?.toString() ?: return@setOnClickListener
            val waistText = spinnerWaist.selectedItem?.toString() ?: return@setOnClickListener
            val stomachText = spinnerStomach.selectedItem?.toString() ?: return@setOnClickListener
            callBcsApi(url, ribsText, waistText, stomachText)
        }
    }

    private fun uploadBitmapToStorage(bitmap: Bitmap, onComplete: (String?) -> Unit) {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
        val data = baos.toByteArray()

        val fileName = "bcs_${System.currentTimeMillis()}.jpg"
        val ref = storageRef.child("bcsUploads/$fileName")

        val metadata = storageMetadata {
            contentType = "image/jpeg"
        }

        ref.putBytes(data, metadata)
            .addOnSuccessListener {
                ref.downloadUrl
                    .addOnSuccessListener { uri -> onComplete(uri.toString()) }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "取得下載連結失敗：${e.message}", Toast.LENGTH_LONG).show()
                        onComplete(null)
                    }
            }
            .addOnFailureListener { e ->
                // 顯示更完整錯誤（如果是 StorageException 會包含 httpCode 等）
                val msg = buildString {
                    append(e.message ?: "未知錯誤")
                    if (e is StorageException) {
                        append(" [code=${e.errorCode}, http=${e.httpResultCode}]")
                    }
                }
                Toast.makeText(this, "上傳失敗：$msg", Toast.LENGTH_LONG).show()
                onComplete(null)
            }
    }

    private fun callBcsApi(imageUrl: String, ribsText: String, waistText: String, stomachText: String) {
        val req = BcsRequest(
            imageUrl = imageUrl,
            ribs = ribsText,
            waist = waistText,
            stomach = stomachText
        )

        val progress = ProgressBar(this).apply { isIndeterminate = true }
        val dlg = android.app.AlertDialog.Builder(this)
            .setTitle("分析中…")
            .setView(progress)
            .setCancelable(false)
            .show()

        api.getBcsScore(req).enqueue(object : Callback<BcsResponse> {
            override fun onResponse(call: Call<BcsResponse>, resp: Response<BcsResponse>) {
                dlg.dismiss()
                val body = resp.body()
                if (!resp.isSuccessful || body == null) {
                    Toast.makeText(this@PhotoAndTouchInputActivity, "分析失敗：${resp.code()}", Toast.LENGTH_LONG).show()
                    return
                }

                val finalBCS = body.finalBcs
                val breed = intent.getStringExtra("DOG_BREED") ?: ""

                val go = Intent(this@PhotoAndTouchInputActivity, MainActivity::class.java)
                go.putExtra("TARGET_FRAGMENT", "BCS_RESULT")
                go.putExtra("FINAL_BCS_SCORE", finalBCS)
                go.putExtra("DOG_BREED", breed)
                go.putExtra("IMAGE_URL", imageUrl)
                startActivity(go)
                finish()
            }

            override fun onFailure(call: Call<BcsResponse>, t: Throwable) {
                dlg.dismiss()
                Toast.makeText(this@PhotoAndTouchInputActivity, "呼叫後端失敗：${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}
