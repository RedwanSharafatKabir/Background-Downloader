package com.background.download.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.background.download.R
import com.background.download.databinding.ActivityMainBinding
import com.background.download.utils.ConnectivityCheck
import es.dmoral.toasty.Toasty
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var downloadId: Long = -1
    private lateinit var progressBar: ProgressBar
    private lateinit var downloadManager: DownloadManager
    private lateinit var binding: ActivityMainBinding
    private var connectivityCheck: ConnectivityCheck = ConnectivityCheck()
    private val requestMultiplePermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach { _ ->
                Log.i("permission_status", permissions.toString())
            }
        }
    private val downloadReceiver = object : BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
                Toasty.success(this@MainActivity, "Video Downloaded", Toasty.LENGTH_LONG).show()
                binding.download.text = "Download"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressBar = binding.progressBar
        downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        requestPermission()

        binding.download.setOnClickListener(this)
    }

    @SuppressLint("SetTextI18n")
    override fun onClick(v: View) {
        when(v.id){
            R.id.download -> {
                if(connectivityCheck.checkInternet(this)){
                    binding.download.text = "Downloading..."
                    downloadFile()

                } else {
                    Toasty.error(this@MainActivity, "No internet", Toasty.LENGTH_LONG).show()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n", "UnspecifiedRegisterReceiverFlag")
    private fun downloadFile() {
        try {
            binding.progressBar.progress = 0
            binding.progressBar.visibility = View.VISIBLE
            binding.progressCount.text = ""

            val videoUrl = resources.getString(R.string.download_url)
            val request = DownloadManager.Request(Uri.parse(videoUrl))

            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, "Bandarban.mp4"
            )

            request.setMimeType("video/mp4")
            downloadId = downloadManager.enqueue(request)

            registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
            monitorDownloadProgress()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("Range", "SetTextI18n")
    private fun monitorDownloadProgress() {
        try {
            thread {
                var downloading = true

                while (downloading){
                    val query = DownloadManager.Query()
                    query.setFilterById(downloadId)
                    val cursor = downloadManager.query(query)

                    if(cursor.moveToFirst()){
                        val bytesDownloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val bytesTotal = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                        if(cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL){
                            downloading = false
                        }

                        val progress = ((bytesDownloaded * 100) / bytesTotal).toInt()

                        runOnUiThread {
                            if(progress==0){
                                progressBar.progress = 5
                                binding.progressCount.text = "5 %"

                            } else {
                                progressBar.progress = progress
                                binding.progressCount.text = "$progress %"
                            }
                        }

                        cursor.close()
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(downloadReceiver)
    }

    private fun requestPermission() {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ||
            (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {

            requestMultiplePermissions.launch(arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ))

        } else {
            Log.i("permission_status", "permission already granted")
        }
    }
}
