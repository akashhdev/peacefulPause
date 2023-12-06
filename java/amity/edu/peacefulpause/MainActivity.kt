package amity.edu.peacefulpause

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import amity.edu.peacefulpause.databinding.ActivityMainBinding
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private var meditationDuration: Long = 0
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var vibrator: Vibrator
    private var isMeditationInProgress: Boolean = false
    private lateinit var binding: ActivityMainBinding
    private lateinit var instructionsTextView: TextView
    private val handler = Handler()
    private var countdownTimer: CountDownTimer? = null
    private var breathCyclePosition = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mediaPlayer = MediaPlayer()
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        instructionsTextView = findViewById(R.id.instructionsTextView)

        binding.meditationSeekBar.apply {
            max = 4 // Max value corresponds to 5 minutes, as each step represents 1 minute
            progress = 0 // Default progress corresponds to 1 minute
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    meditationDuration = (progress + 1) * 60 * 1000L // in milliseconds
                    binding.durationTextView.text = "${progress + 1} min"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }


        binding.startMeditationButton.setOnClickListener {
            if (!isMeditationInProgress) {
                startMeditation()
            } else {
                stopMeditation()
            }
        }

        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            onRadioButtonClicked(findViewById(checkedId))
        }

        // Schedule a runnable to update instructions every 5 seconds
        handler.postDelayed(updateInstructionsRunnable, 5000)
    }


    private fun onRadioButtonClicked(view: View) {
        val backgroundResourceId: Int
        val textColor: Int

        // Stop the current playback if it's ongoing
        stopAudio()

        when (view.id) {
            R.id.radioOcean -> {
                mediaPlayer = MediaPlayer.create(this, R.raw.ocean)
                backgroundResourceId = R.drawable.ocean_background
                textColor = Color.parseColor("#90000000")  // Set the default text color here, if needed
            }

            R.id.radioRain -> {
                mediaPlayer = MediaPlayer.create(this, R.raw.rain)
                backgroundResourceId = R.drawable.rain_background
                textColor = Color.parseColor("#90FFFFFF") // Set the desired text color for Rain
            }

            R.id.radioForest -> {
                mediaPlayer = MediaPlayer.create(this, R.raw.forest)
                backgroundResourceId = R.drawable.forest_background
                textColor = Color.parseColor("#90000000")
            }
            else -> return
        }

        // Start playing the selected audio
        mediaPlayer?.start()

        // Set the background of the whole app
        val rootLayout = findViewById<View>(R.id.toolbar) // replace with your root layout ID
        rootLayout.setBackgroundResource(backgroundResourceId)

        // Set the text color of appNameTextView
        val appNameTextView = findViewById<TextView>(R.id.appNameTextView) // replace with your TextView ID
        appNameTextView.setTextColor(textColor)

        // Set the text color of appNameTextView
        val instructionsTextView = findViewById<TextView>(R.id.instructionsTextView) // replace with your TextView ID
        appNameTextView.setTextColor(textColor)
    }

    private fun stopAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
    }




    // Function to get resource identifier dynamically
    private fun getIdentifier(name: String, type: String): Int {
        return resources.getIdentifier(name, type, packageName)
    }

    private val updateInstructionsRunnable = object : Runnable {
        override fun run() {
            updateInstructions()
            handler.postDelayed(this, 5000)
        }
    }

    private fun updateInstructions() {
        if (isMeditationInProgress) {
            val currentInstructions = binding.instructionsTextView.text.toString()
            if (currentInstructions == getString(R.string.breath_in)) {
                binding.instructionsTextView.text = getString(R.string.breath_out)
            } else {
                binding.instructionsTextView.text = getString(R.string.breath_in)
            }
        }
    }

    private fun startMeditation() {
        isMeditationInProgress = true
        binding.meditationSeekBar.isEnabled = false
        binding.radioGroup.isEnabled = false


        mediaPlayer!!.start()

        countdownTimer = object : CountDownTimer(meditationDuration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                binding.durationTextView.text = String.format("%02d:%02d", secondsRemaining / 60, secondsRemaining % 60)

                if (secondsRemaining % 5 == 0L) {
                    vibratePhone()
                }

                // Update breath text every 4 seconds
                if (secondsRemaining % 5 == 0L) {
                    updateBreathText()
                }
            }

            override fun onFinish() {
                stopMeditation()
            }
        }.start()

        // Update button text
        binding.startMeditationButton.text = getString(R.string.stop)

        // Update instructions text
        binding.instructionsTextView.text = ""

    }

    private fun stopMeditation() {
        isMeditationInProgress = false
        binding.meditationSeekBar.isEnabled = true
        binding.radioGroup.isEnabled = true
        binding.instructionsTextView.text = getString(R.string.select_time_and_start)

        mediaPlayer!!.pause()
        mediaPlayer!!.seekTo(0)

        // Reset timer to 00:00
        binding.durationTextView.text = "00:00"

        // Reset button text to "Start"
        binding.startMeditationButton.text = getString(R.string.start)

        // Cancel the countdown timer
        countdownTimer?.cancel()
        countdownTimer = null

        // Reset breath cycle position
        breathCyclePosition = 0

        // Reset breath text
        updateBreathText()

        // Update instructions text
        binding.instructionsTextView.text = getString(R.string.select_time_and_start)

    }

    private fun updateBreathText() {
        val breathText = if (breathCyclePosition % 2 == 0) {
            getString(R.string.breath_in)
        } else {
            getString(R.string.breath_out)
        }

        binding.instructionsTextView.text = breathText
        breathCyclePosition++

    }

    private fun vibratePhone() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer!!.release()
    }
}
