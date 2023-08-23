package com.example.projectparkinson

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.graphics.Color
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Chronometer
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.projectparkinson.databinding.ActivityMainBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE = 100
    }

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var output: String
    private var pauseOffset: Long = 0
    private lateinit var binding: ActivityMainBinding

    private lateinit var chronometer: Chronometer

    private lateinit var selectedFileUri: Uri
    private lateinit var selectedFilePath: String

    private val REQUEST_PERMISSION_CODE = 100
    private val SAMPLE_RATE = 44100
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    private var isRecording = false
    private var audioRecord: AudioRecord? = null
    private var outputStream: FileOutputStream? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        val btnRecord = findViewById<ImageButton>(R.id.btn_record)
        val textSonuc = findViewById<TextView>(R.id.sonuc)
        val btnWarning = findViewById<ImageButton>(R.id.btn_warning)

        textSonuc.text="Ses Kaydı Gönderin"

        val REQUEST_PERMISSION = 100
        val READ_EXTERNAL_STORAGE = android.Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this,  android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this,  android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this,  android.Manifest.permission.INTERNET)
            != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(this,
                arrayOf( android.Manifest.permission.READ_EXTERNAL_STORAGE,  android.Manifest.permission.WRITE_EXTERNAL_STORAGE ,  android.Manifest.permission.RECORD_AUDIO,  android.Manifest.permission.INTERNET),
                REQUEST_PERMISSION)
        }

        warning()
        btnWarning.setOnClickListener {
            warning()
        }

        kronometre()

        }

    private fun warning(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Uyarı")
        builder.setMessage(
            "1. Bu uygulama parkinson hastalığının ses ile tahmini için yapılmıştır.\n" +
                    "2. Sonuçlar sadece bir tahmindir. Eğer bu hastalığa dair şüpheniz varsa en yakın zamanda doktorunuza başvurunuz. \n" +
                    "3. Tahminin doğruluğunu arttırmak için gürültüsüz ve temiz bir ortamda sesinizi kaydediniz.\n" +
                    "4. Tahminin doğruluğunu arttırmak için tek bir ünlü harfi (a,e,i,ı,o,ö,u,ü) en az 5 saniye olmak üzere konuşunuz.\n" +
                    "5. Uygulama 5 saniye altındaki ses kayıtlarına da sonuç verecektir fakat bu sonuçların doğruluğu çok daha düşüktür.\n" +
                    "6. Ses kaydınızı örnekteki gibi kaydetmeye çalışınız. \n" +
                    "7. Sonuç alabilmek için internet bağlantınızın çalıştığından emin olunuz.")

        builder.setPositiveButton(
            "Tamam"
        ) { dialog, id -> durdur() }
        builder.setNegativeButton("Örnek Ses Kaydı", null)
        val dialog = builder.create()
        dialog.show()

        val startButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

        startButton.setOnClickListener {
            oynat()
        }
    }

    private fun oynat(){
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer()
                val afd: AssetFileDescriptor = resources.openRawResourceFd(R.raw.parkinsonsample)
                mediaPlayer?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                mediaPlayer?.prepare()
            }

            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun durdur(){
        mediaPlayer?.release()
        mediaPlayer = null
    }
    private fun kronometre(){

        val btnRecord = findViewById<ImageButton>(R.id.btn_record)
        val textSonuc = findViewById<TextView>(R.id.sonuc)

        val colorAnim = ValueAnimator.ofArgb(
            ContextCompat.getColor(this, R.color.yesil3),
            Color.RED
        )
        val colorAnim2 = ValueAnimator.ofArgb(

            Color.RED,
            ContextCompat.getColor(this, R.color.yesil3),
        )
        val colorAnim3 = ValueAnimator.ofArgb(

            ContextCompat.getColor(this, R.color.yesil3),
            ContextCompat.getColor(this, R.color.yesil2),
            ContextCompat.getColor(this, R.color.yesil3),
        )
        colorAnim.duration = 1000
        colorAnim2.duration = 1000
        colorAnim3.duration = 1000
        colorAnim3.repeatCount = ValueAnimator.INFINITE

        binding.btnRecord.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                val chronoMeter = binding.timer
                colorAnim.addUpdateListener { animator ->
                    val color = animator.animatedValue as Int   // Animasyonun geçerli değeri
                    chronoMeter.setTextColor(color)   // TextView'in yazı rengi animasyonlu şekilde değiştiriliyor
                }
                colorAnim2.addUpdateListener { animator ->
                    val color = animator.animatedValue as Int   // Animasyonun geçerli değeri
                    chronoMeter.setTextColor(color)   // TextView'in yazı rengi animasyonlu şekilde değiştiriliyor
                }
                colorAnim3.addUpdateListener { animator ->
                    val color = animator.animatedValue as Int   // Animasyonun geçerli değeri
                    chronoMeter.setTextColor(color)   // TextView'in yazı rengi animasyonlu şekilde değiştiriliyor
                }
                isRecording = if (!isRecording) {
                    textSonuc.text="Ses Kaydı Alınıyor..."

                    try {
                        startRecording()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    btnRecord.setImageResource(R.drawable.recording)
                    chronoMeter.base = SystemClock.elapsedRealtime()

                    colorAnim2.start()
                    colorAnim3.start()
                    chronoMeter.start()

                    chronoMeter.setFormat("%S");

                    true
                }
                else {
                    stopRecording()
                    val context = applicationContext
                    val client = OkHttpClient()
                    val file = File(Environment.getExternalStorageDirectory().absolutePath + "/recordingParkinson.pcm")
                    val requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", file.name, file.asRequestBody("audio/pcm".toMediaTypeOrNull()))
                        .build()
                    val request = Request.Builder()
                        .url("http://192.168.1.110:5000/upload")
                        .post(requestBody)
                        .build()
                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            Log.e("HTTP isteği", "Bağlantı hatası")
                            runOnUiThread {
                                textSonuc.text="Ses Kaydı Gönderilemedi, Bağlantı Hatası Yaşandı"
                            }
                            textSonuc.text="Ses Kaydı Gönderilemedi, Bağlantı Hatası Yaşandı"
                        }

                        override fun onResponse(call: Call, response: Response) {
                            val responseText = response.body?.string() ?: ""
                            Log.i("HTTP isteği", "Sunucudan gelen cevap: $responseText")
                            runOnUiThread {
                                colorAnim2.start()
                                chronoMeter.setBase(SystemClock.elapsedRealtime());
                                chronoMeter.stop();
                                textSonuc.text="Sonuç: "+responseText
                            }

                        }
                    })

                    colorAnim3.cancel()
                    colorAnim.start()

                    btnRecord.setImageResource(R.drawable.record)
                    chronoMeter.stop()
                    false
                }

            }
        })

    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        isRecording = true

        val filePath = Environment.getExternalStorageDirectory().absolutePath + "/recordingParkinson.pcm"
        val file = File(filePath)

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        outputStream = FileOutputStream(file)

        audioRecord?.startRecording()

        thread {
            val buffer = ByteArray(bufferSize)

            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, bufferSize)
                if (read != null && read > 0) {
                    outputStream?.write(buffer, 0, read)
                }
            }

            outputStream?.close()
        }
    }

    private fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val selectedFile = data?.data
            val filePath = selectedFile?.path
            if (filePath != null) {
                selectedFilePath = filePath
                println(selectedFilePath)
            }

        }
    }



    private fun startPlaying() {

        var player = MediaPlayer()
        try {
            // Kaydedilen ses dosyasını çal
            //println(selectedFilePath)
            player.setDataSource(output)
            player.prepare()
            player.start()

            player.setOnCompletionListener {

                player.release()
            }



        } catch (e: IOException) {
            // Dosya okuma hatası durumunda buraya düşeriz
            e.printStackTrace()
        }
    }

    override fun onStop() {
        super.onStop()
        stopRecording()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        if (isRecording) {
            stopRecording()
        }
    }
}