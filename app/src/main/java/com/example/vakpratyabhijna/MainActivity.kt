package com.example.vakpratyabhijna

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.vakpratyabhijna.databinding.ActivityMainBinding
import com.github.squti.androidwaverecorder.RecorderState
import com.github.squti.androidwaverecorder.WaveRecorder
import org.json.JSONObject
import java.io.*

class MainActivity : AppCompatActivity() {

    private val PERMISSIONS_REQUEST_RECORD_AUDIO = 77

    private lateinit var binding: ActivityMainBinding
    private lateinit var waveRecorder: WaveRecorder
    private lateinit var filePath: String
    private var isRecording = false
    private var isPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        filePath = externalCacheDir?.absolutePath + "/audioFile.wav"

        waveRecorder = WaveRecorder(filePath)
        waveRecorder.waveConfig.sampleRate = 16000
        waveRecorder.waveConfig.channels = AudioFormat.CHANNEL_IN_MONO
        waveRecorder.waveConfig.audioEncoding = AudioFormat.ENCODING_PCM_16BIT

        waveRecorder.onStateChangeListener = {
            when (it) {
                RecorderState.RECORDING -> startRecording()
                RecorderState.STOP -> stopRecording()
                //RecorderState.PAUSE -> pauseRecording()
            }
        }
        waveRecorder.onTimeElapsed = {
            Log.e(TAG, "onCreate: time elapsed $it")
            //timeTextView.text = formatTimeUnit(it * 1000)
        }

        binding.mic.setOnClickListener {

            if (!isRecording) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        PERMISSIONS_REQUEST_RECORD_AUDIO
                    )
                } else {
                    waveRecorder.startRecording()
                }
            } else {
                waveRecorder.stopRecording()
            }
        }
    }

    private fun startRecording() {
        Log.d(TAG, waveRecorder.audioSessionId.toString())
        isRecording = true
        isPaused = false
        //messageTextView.visibility = View.GONE
        //recordingTextView.text = "Recording..."
        //recordingTextView.visibility = View.VISIBLE
        //startStopRecordingButton.text = "STOP"
        binding.mic.setImageResource(R.drawable.mic_on);
        //pauseResumeRecordingButton.text = "PAUSE"
        //pauseResumeRecordingButton.visibility = View.VISIBLE
        //noiseSuppressorSwitch.isEnabled = false
    }

    private fun stopRecording() {
        isRecording = false
        isPaused = false
        //recordingTextView.visibility = View.GONE
        //messageTextView.visibility = View.VISIBLE
        //pauseResumeRecordingButton.visibility = View.GONE
        //showAmplitudeSwitch.isChecked = false
        Toast.makeText(this, "File saved at : $filePath", Toast.LENGTH_LONG).show()
        //startStopRecordingButton.text = "START"
        binding.mic.setImageResource(R.drawable.mic_off);
        //noiseSuppressorSwitch.isEnabled = true

        sendAPI()
    }

    private fun sendAPI() {
        var myExternalFile:File = File(externalCacheDir?.absolutePath,"audioFile.wav")
        //var myExternalFile:File = File(Environment.getExternalStorageDirectory().absolutePath,"audioFile.wav")
        val encodedString: String = "data:audio/wav;base64," + convertToBase64(myExternalFile)
        Log.i("Base64",encodedString)

        getSanskritData(encodedString)
    }

    private fun getSanskritData(encodedString: String) {
        val params = mutableMapOf<Any?, Any?>()
            params["filedata"] = "\""+ encodedString+"\""



        val queue = Volley.newRequestQueue(this)
        val url: String = "https://0aa0-103-104-47-8.in.ngrok.io/android-base64"

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, JSONObject(params),
            Response.Listener{ response ->
                Log.i("res",response.toString())
                val transcribed_text: String = response.getString("transcribed_text")
                binding.textArea.append(transcribed_text)
            },
            Response.ErrorListener{
                Toast.makeText(this,"Something Went Wrong", Toast.LENGTH_LONG).show()
            })
        queue.add(jsonObjectRequest)
    }


    companion object {
        private const val TAG = "MainActivity"
    }
    fun convertToBase64(attachment: File): String {
        return Base64.encodeToString(attachment.readBytes(), Base64.NO_WRAP)
    }

}
