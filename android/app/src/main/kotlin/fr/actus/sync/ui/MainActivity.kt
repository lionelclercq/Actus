package fr.actus.sync.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import fr.actus.sync.R
import fr.actus.sync.data.ConfigBackup
import fr.actus.sync.data.CookieRepository
import fr.actus.sync.data.CookieRepositoryHolder
import fr.actus.sync.data.SettingsRepository
import fr.actus.sync.data.WikiPusher
import fr.actus.sync.databinding.ActivityMainBinding
import fr.actus.sync.worker.SyncWorker

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: SettingsRepository

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { startSync() }

    private val loginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { updateSubscriptionStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settings = SettingsRepository(this)
        loadSettings()
        updateSubscriptionStatus()
        observeWork()

        binding.saveButton.setOnClickListener { saveSettings() }
        binding.exportConfigButton.setOnClickListener { exportConfig() }
        binding.importConfigButton.setOnClickListener { importConfig() }
        binding.syncButton.setOnClickListener { requestSync() }
        binding.loginLeMondeButton.setOnClickListener {
            openLogin(CookieRepository.PublisherSite.LEMONDE)
        }
        binding.loginCharenteButton.setOnClickListener {
            openLogin(CookieRepository.PublisherSite.CHARENTELIBRE)
        }
        binding.openWikiButton.setOnClickListener {
            val url = WikiPusher.wikiUrl(settings.githubRepo)
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    private fun openLogin(site: CookieRepository.PublisherSite) {
        CookieRepositoryHolder.init(this)
        val intent = Intent(this, LoginWebViewActivity::class.java)
            .putExtra(LoginWebViewActivity.EXTRA_SITE, site.name)
        loginLauncher.launch(intent)
    }

    private fun updateSubscriptionStatus() {
        CookieRepositoryHolder.init(this)
        val lines = CookieRepository.PublisherSite.entries.map { site ->
            val status = if (CookieRepositoryHolder.repository.isLoggedIn(site)) {
                getString(R.string.subscription_connected)
            } else {
                getString(R.string.subscription_disconnected)
            }
            "• ${site.label} : $status"
        }
        binding.subscriptionStatus.text = lines.joinToString("\n")
    }

    private fun loadSettings() {
        binding.geminiKeyInput.setText(settings.geminiApiKey)
        binding.githubTokenInput.setText(settings.githubToken)
        binding.githubRepoInput.setText(settings.githubRepo)
        if (settings.lastSyncMessage.isNotBlank()) {
            binding.logText.text = settings.lastSyncMessage
        }
    }

    private fun saveSettings() {
        settings.geminiApiKey = binding.geminiKeyInput.text?.toString().orEmpty()
        settings.githubToken = binding.githubTokenInput.text?.toString().orEmpty()
        settings.githubRepo = binding.githubRepoInput.text?.toString().orEmpty()
            .ifBlank { SettingsRepository.DEFAULT_REPO }
        Toast.makeText(this, "Configuration enregistrée", Toast.LENGTH_SHORT).show()
    }

    private fun exportConfig() {
        saveSettings()
        val backup = ConfigBackup.export(settings)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Actus Sync config", backup))
        binding.importConfigInput.setText(backup)
        Toast.makeText(this, R.string.export_config_copied, Toast.LENGTH_LONG).show()
    }

    private fun importConfig() {
        val raw = binding.importConfigInput.text?.toString().orEmpty().ifBlank {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
        }
        if (raw.isBlank()) {
            Toast.makeText(this, R.string.import_config_error, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            ConfigBackup.import(settings, raw)
            loadSettings()
            Toast.makeText(this, R.string.import_config_done, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, R.string.import_config_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestSync() {
        saveSettings()
        if (!settings.isConfigured()) {
            Toast.makeText(this, "Renseignez Gemini et GitHub", Toast.LENGTH_LONG).show()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        startSync()
    }

    private fun startSync() {
        binding.syncButton.isEnabled = false
        binding.statusText.setText(R.string.sync_running)
        val request = OneTimeWorkRequestBuilder<SyncWorker>().build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            SyncWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun observeWork() {
        WorkManager.getInstance(this)
            .getWorkInfosForUniqueWorkLiveData(SyncWorker.WORK_NAME)
            .observe(this) { infos ->
                val info = infos.firstOrNull() ?: return@observe
                when (info.state) {
                    WorkInfo.State.RUNNING -> {
                        binding.syncButton.isEnabled = false
                        binding.statusText.setText(R.string.sync_running)
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        binding.syncButton.isEnabled = true
                        binding.statusText.setText(R.string.sync_success)
                        binding.logText.text = settings.lastSyncMessage
                    }
                    WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                        binding.syncButton.isEnabled = true
                        binding.statusText.text = settings.lastSyncMessage.ifBlank {
                            getString(R.string.sync_idle)
                        }
                        binding.logText.text = settings.lastSyncMessage
                    }
                    else -> Unit
                }
            }
    }
}
