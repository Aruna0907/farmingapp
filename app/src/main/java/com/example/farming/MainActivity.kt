package com.example.farming

import android.Manifest
import android.accounts.AccountManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.farming.auth.AuthManager
import com.example.farming.data.db.FarmEntity
import com.example.farming.data.repository.RecommendationStatus
import com.example.farming.databinding.ActivityMainBinding
import com.example.farming.databinding.DialogAddFarmBinding
import com.example.farming.databinding.DialogLogIrrigationBinding
import com.example.farming.ui.FarmViewModel
import com.example.farming.ui.ForecastAdapter
import com.example.farming.ui.IrrigationLogAdapter
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: FarmViewModel by viewModels()

    private lateinit var authManager: AuthManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var googleSignInClient: GoogleSignInClient

    private val forecastAdapter = ForecastAdapter()
    private val logAdapter = IrrigationLogAdapter()

    private var farmsList: List<FarmEntity> = emptyList()
    private var tempDetectedLat: Double = 37.7749
    private var tempDetectedLon: Double = -122.4194

    private var dialogAddFarmBinding: DialogAddFarmBinding? = null

    private val accountPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrEmpty()) {
                authManager.setCustomUser(accountName)
                Toast.makeText(this, "Signed in as $accountName", Toast.LENGTH_SHORT).show()
                updateAuthDisplay()
            }
        }
    }

    private fun launchAccountPicker() {
        try {
            val intent = AccountManager.newChooseAccountIntent(
                null,
                null,
                arrayOf("com.google"),
                true,
                null,
                null,
                null,
                null
            )
            accountPickerLauncher.launch(intent)
        } catch (e: Exception) {
            showDeviceAccountsDialog()
        }
    }

    private fun showDeviceAccountsDialog() {
        try {
            val accountManager = AccountManager.get(this)
            val accounts = accountManager.getAccountsByType("com.google")
            val emails = accounts.map { it.name }.toTypedArray()

            if (emails.isNotEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("Select Google Account")
                    .setItems(emails) { _, which ->
                        val selectedEmail = emails[which]
                        authManager.setCustomUser(selectedEmail)
                        Toast.makeText(this, "Signed in as $selectedEmail", Toast.LENGTH_SHORT).show()
                        updateAuthDisplay()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                Toast.makeText(this, "No Google accounts found on this device", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Could not access device accounts", Toast.LENGTH_SHORT).show()
        }
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                authManager.setGoogleAccount(account)
                Toast.makeText(this, "Signed in as ${account.email}", Toast.LENGTH_SHORT).show()
                updateAuthDisplay()
            } else {
                launchAccountPicker()
            }
        } catch (e: ApiException) {
            launchAccountPicker()
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            fetchCurrentLocation()
        } else {
            Toast.makeText(this, "Location permission denied. Using default coordinates.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        authManager = AuthManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setupRecyclerViews()
        setupAuthUI()
        setupObservers()

        binding.btnAddFarm.setOnClickListener {
            showAddFarmDialog()
        }

        binding.btnLogIrrigation.setOnClickListener {
            showLogIrrigationDialog()
        }
    }

    private fun setupRecyclerViews() {
        binding.rvForecast.layoutManager = LinearLayoutManager(this)
        binding.rvForecast.adapter = forecastAdapter

        binding.rvIrrigationLogs.layoutManager = LinearLayoutManager(this)
        binding.rvIrrigationLogs.adapter = logAdapter
    }

    private fun setupAuthUI() {
        updateAuthDisplay()

        binding.btnAuthAction.setOnClickListener {
            if (authManager.isUserSignedIn) {
                googleSignInClient.signOut().addOnCompleteListener(this) {
                    authManager.signOut()
                    Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show()
                    updateAuthDisplay()
                }
            } else {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }
    }

    private fun updateAuthDisplay() {
        binding.tvUserDisplayName.text = String.format("User: %s", authManager.getUserDisplayName())
        binding.btnAuthAction.text = if (authManager.isUserSignedIn) getString(R.string.btn_sign_out) else getString(R.string.btn_sign_in_google)
    }

    private fun setupObservers() {
        viewModel.allFarms.observe(this) { farms ->
            farmsList = farms
            if (farms.isEmpty()) {
                // Pre-populate a default sample farm if none exists
                viewModel.addFarm(
                    name = "Green Valley Farm",
                    crop = "Wheat",
                    latitude = 37.7749,
                    longitude = -122.4194
                )
            } else {
                val farmNames = farms.map { "${it.name} (${it.crop})" }
                val spinnerAdapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    farmNames
                )
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerFarms.adapter = spinnerAdapter

                binding.spinnerFarms.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (position in farms.indices) {
                            viewModel.selectFarm(farms[position])
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

                if (viewModel.selectedFarm.value == null && farms.isNotEmpty()) {
                    viewModel.selectFarm(farms[0])
                }
            }
        }

        viewModel.adviceState.observe(this) { result ->
            if (result != null && result.isSuccess) {
                val advice = result.getOrNull()
                if (advice != null) {
                    val isIrrigate = advice.status == RecommendationStatus.IRRIGATE

                    if (isIrrigate) {
                        binding.tvStatusTitle.text = getString(R.string.status_irrigate)
                        binding.tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.status_irrigate))
                        binding.tvStatusExplanation.text = getString(R.string.status_irrigate_explanation)
                        binding.cardRecommendation.setCardBackgroundColor(ContextCompat.getColor(this, R.color.bg_irrigate_light))
                        binding.cardRecommendation.strokeColor = ContextCompat.getColor(this, R.color.border_irrigate)
                    } else {
                        binding.tvStatusTitle.text = getString(R.string.status_not_needed)
                        binding.tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.status_not_needed))
                        binding.tvStatusExplanation.text = getString(R.string.status_not_needed_explanation)
                        binding.cardRecommendation.setCardBackgroundColor(ContextCompat.getColor(this, R.color.bg_not_needed_light))
                        binding.cardRecommendation.strokeColor = ContextCompat.getColor(this, R.color.border_not_needed)
                    }

                    binding.tvEt0Today.text = String.format(Locale.getDefault(), "%.1f mm", advice.et0Today)
                    binding.tvRainfallToday.text = String.format(Locale.getDefault(), "%.1f mm", advice.rainfallToday)
                    binding.tvSoilMoisture.text = String.format(Locale.getDefault(), "%.2f", advice.soilMoistureToday)
                    binding.tvNetDeficit.text = String.format(Locale.getDefault(), "%.1f mm", advice.netDeficit)

                    forecastAdapter.submitList(advice.forecastItems)
                }
            } else if (result != null && result.isFailure) {
                Toast.makeText(this, "Weather fetch error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.irrigationLogs.observe(this) { logs ->
            logAdapter.submitList(logs ?: emptyList())
        }
    }

    private fun showAddFarmDialog() {
        val dialogBinding = DialogAddFarmBinding.inflate(layoutInflater)
        dialogAddFarmBinding = dialogBinding

        val crops = arrayOf("Wheat", "Corn", "Rice", "Cotton", "Tomato", "Sugarcane", "Potato", "Soybean")
        val cropAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, crops)
        cropAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerCrop.adapter = cropAdapter

        dialogBinding.tvLocationCoords.text = String.format(Locale.getDefault(), "Lat: %.4f, Lon: %.4f", tempDetectedLat, tempDetectedLon)

        dialogBinding.btnDetectLocation.setOnClickListener {
            checkLocationPermissionAndFetch()
        }

        AlertDialog.Builder(this)
            .setTitle("Add Farm")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val name = dialogBinding.etFarmName.text.toString().trim()
                val crop = dialogBinding.spinnerCrop.selectedItem?.toString() ?: "Wheat"

                if (name.isNotEmpty()) {
                    viewModel.addFarm(
                        name = name,
                        crop = crop,
                        latitude = tempDetectedLat,
                        longitude = tempDetectedLon
                    )
                    Toast.makeText(this, "Farm saved successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Please enter a farm name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkLocationPermissionAndFetch() {
        val fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            fetchCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @Suppress("MissingPermission")
    private fun fetchCurrentLocation() {
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        tempDetectedLat = location.latitude
                        tempDetectedLon = location.longitude
                        dialogAddFarmBinding?.tvLocationCoords?.text = String.format(
                            Locale.getDefault(),
                            "Lat: %.4f, Lon: %.4f",
                            tempDetectedLat,
                            tempDetectedLon
                        )
                        Toast.makeText(this, "Location detected!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Unable to get location. Using previous coordinates.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Location detection failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Location permission missing", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLogIrrigationDialog() {
        if (viewModel.selectedFarm.value == null) {
            Toast.makeText(this, "Please select or add a farm first", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogBinding = DialogLogIrrigationBinding.inflate(layoutInflater)

        AlertDialog.Builder(this)
            .setTitle("Log Irrigation Event")
            .setView(dialogBinding.root)
            .setPositiveButton("Save Log") { _, _ ->
                val durationStr = dialogBinding.etDurationMinutes.text.toString().trim()
                val duration = durationStr.toIntOrNull()

                if (duration != null && duration > 0) {
                    viewModel.logIrrigation(duration)
                    Toast.makeText(this, "Irrigation event logged!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Please enter a valid duration in minutes", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
