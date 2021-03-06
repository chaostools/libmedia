package libmedia

import android.R
import android.app.Activity
import android.content.Context
import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.AudioManager
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import java.io.*


@Suppress("unused")
class Media(private val activity: Activity) {

    private val LOG_TAG = "libMedia"
    inner class Classes {

        inner class FocusManager {
            private var mAudioFocusChangeListener: AudioFocusChangeListenerImpl? = null
            var hasFocus: Boolean = false
            private var focusChanged: Boolean = false
            private val TAG = "FocusManager"

            fun request(): Boolean {
                if (hasFocus) return true
                mAudioFocusChangeListener = AudioFocusChangeListenerImpl()
                val result = audioManager.requestAudioFocus(
                    mAudioFocusChangeListener,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
                )

                when (result) {
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> hasFocus = true
                    AudioManager.AUDIOFOCUS_REQUEST_FAILED -> hasFocus = false
                }

                val message = "Focus request " + if (hasFocus) "granted" else "failed"
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
                Log.i(TAG, message)
                return hasFocus
            }

            fun release(): Boolean {
                if (!hasFocus) return true
                val result = audioManager.abandonAudioFocus(mAudioFocusChangeListener)
                when (result) {
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> hasFocus = false
                    AudioManager.AUDIOFOCUS_REQUEST_FAILED -> hasFocus = true
                }
                val message = "Abandon focus request " + if (!hasFocus) "granted" else "failed"
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
                Log.i(TAG, message)
                return !hasFocus
            }

            private inner class AudioFocusChangeListenerImpl : AudioManager.OnAudioFocusChangeListener {

                override fun onAudioFocusChange(focusChange: Int) {
                    focusChanged = true
                    Log.i(TAG, "Focus changed")

                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            Log.i(TAG, "AUDIOFOCUS_GAIN")
                            Toast.makeText(activity, "Focus GAINED", Toast.LENGTH_LONG).show()
                            play()
                        }
                        AudioManager.AUDIOFOCUS_LOSS -> {
                            Log.i(TAG, "AUDIOFOCUS_LOSS")
                            Toast.makeText(activity, "Focus LOST", Toast.LENGTH_LONG).show()
                            pause()
                        }
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                            Log.i(TAG, "AUDIOFOCUS_LOSS_TRANSIENT")
                            Toast.makeText(activity, "Focus LOST TRANSIENT", Toast.LENGTH_LONG).show()
                        }
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                            Log.i(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK")
                            Toast.makeText(activity, "Focus LOST TRANSIENT CAN DUCK", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        inner class assetsManager {

            fun copyAssetFolder(assetManager: AssetManager): Boolean = copyAssetFolder(assetManager, null, ASSETS)

            fun copyAssetFolder(
                assetManager: AssetManager,
                toPath: String
            ): Boolean = copyAssetFolder(assetManager, null, toPath)

            fun copyAssetFolder(
                assetManager: AssetManager,
                fromAssetPath: String?,
                toPath: String
            ): Boolean {
                val files: Array<String>? =
                    assetManager.list(if (fromAssetPath.isNullOrBlank()) "" else fromAssetPath)
                if (files == null) return false else if (files.isEmpty()) return false
                Log.i(LOG_TAG, "obtained a list of assets")
                File(toPath).mkdirs()
                var res = true
                files.forEach {
                    // attempting to open a directory will return a filenotfound exception
                    var f = ""
                    f = if (fromAssetPath.isNullOrBlank()) it
                    else "$fromAssetPath/$it"
                    res = try {
                        val dummy = assetManager.open(f)
                        // if this point has been reached, we found a file
                        dummy.close()
                        res and copyAsset(assetManager, f, "$toPath/$it")
                    } catch (e: IOException) {
                        // we didnt find a file, assume it must be a folder
                        res and copyAssetFolder(assetManager, f, "$toPath/$it")
                    }
                }
                return res
            }

            fun copyAsset(
                assetManager: AssetManager,
                fromAssetPath: String, toPath: String
            ): Boolean {
                var `in`: InputStream? = null
                var out: OutputStream? = null
                Log.i(LOG_TAG, "copying \"$fromAssetPath\" to \"$toPath\"")
                try {
                    `in` = assetManager.open(fromAssetPath)
                } catch (e: Exception) {
                    e.printStackTrace()
                    return false
                }
                File(toPath).createNewFile()
                out = FileOutputStream(toPath)
                copyFile(`in`!!, out)
                `in`.close()
                `in` = null
                out.flush()
                out.close()
                out = null
                return true
            }

            @Throws(IOException::class)
            fun copyFile(`in`: InputStream, out: OutputStream) {
                val buffer = ByteArray(1024)
                var read = `in`.read(buffer)
                while (read != -1) {
                    out.write(buffer, 0, read)
                    read = `in`.read(buffer)
                }
            }
        }

        inner class ListnerCallback {
            var play: () -> Unit = {}
            var pause: () -> Unit = {}
            var stop: () -> Unit = {}
        }

        inner class loop(var name: String, var start: Long, var end: Long, var timing: Int) {};

        inner class LooperTiming() {
            val nanoseconds = 1
            val microseconds = 2
            val milliseconds = 3
            val seconds = 4
            val minutes = 5
            val hours = 6
        }

        inner class WaveformViewOptions__ {

            var drawLines: Boolean = true
            var highlightSilence: Boolean = false
            var stretchToScreenHeight: Boolean = true;
            var stretchToScreenWidth: Boolean = true;

        }

        inner class WaveformView_ : ConstraintLayout {

            lateinit var imageView: ImageView
            private var media: Media? = null
            private var height_ = 0;
            private var width_ = 0;

            constructor(context: Context, height: Int, width: Int) :
                    super(context) {
                height_ = height
                width_ = width
                initView()
            }

            constructor(context: Context, height: Int, width: Int, media: Media) :
                    super(context) {
                height_ = height
                width_ = width
                this.media = media
                initView()
            }


            constructor(context: Context, height: Int, width: Int, attrs: AttributeSet) :
                    super(context, attrs) {
                height_ = height
                width_ = width
                initView()
            }

            constructor(context: Context, height: Int, width: Int, media: Media, attrs: AttributeSet) :
                    super(context, attrs) {
                height_ = height
                width_ = width
                this.media = media
                initView()
            }


            constructor(context: Context, height: Int, width: Int, attrs: AttributeSet, defStyleAttr: Int) :
                    super(context, attrs, defStyleAttr) {
                height_ = height
                width_ = width
                initView()
            }

            constructor(
                context: Context,
                height: Int,
                width: Int,
                media: Media,
                attrs: AttributeSet,
                defStyleAttr: Int
            ) :
                    super(context, attrs, defStyleAttr) {
                height_ = height
                width_ = width
                this.media = media
                initView()
            }

            fun initView() {
                imageView = ImageView(context)
                Log.i("WaveformView", "getting file location")
                val file = Oboe_getWaveformLocation()
                Log.i("WaveformView", "got file location")
                Log.i("WaveformView", "file: $file")
                loadFile(file)
                addView(imageView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
                if (media != null) addView(
                    MyView__(context, height_).also {
                        Thread {
                            var currentFrame = 0
                            while (true) {
                                val previousFrame = currentFrame
                                currentFrame = currentFrame(width_)
                                if (currentFrame != previousFrame) {
                                    activity.runOnUiThread {
                                        it.left = currentFrame
                                    }
                                }
                            }
                        }.start()
                    }
                )
            }

            // TODO: have jni callback to load image on availability
            fun loadFile(file: String) {
                val imgFile = File(file)

                if (imgFile.exists()) {
                    Log.i("WaveformView", "file exists")
                    val myBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                    imageView.setImageBitmap(myBitmap)
                } else {
                    Log.i("WaveformView", "file does not exist")
                }
            }
        }

        inner class MyView__(context: Context, val height_: Int) : View(context) {

            var paint: Paint? = null

            init {
                paint = Paint()
                paint!!.color = Color.GREEN
                paint!!.strokeWidth = 2f
                paint!!.style = Paint.Style.STROKE
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val offset: Float = 0f
                canvas.drawRect(offset, 0f, offset, height_.toFloat(), paint!!)
            }
        }
    }
    val classes = Classes()

    // TODO: vst support (plugins): generator, effects, graphical

    lateinit var currentTemporyMediaFilesDirectory: String
    lateinit var currentAssetFilesDirectory: String
    lateinit var audioManager: AudioManager
    var audioManagerObtained = false
    private val focusManager = classes.FocusManager()
    lateinit var ASSETS: String
    lateinit var CACHE: String

    fun `init`(): Media {
        // If this method is called more than once with the same library name, the second and
        // subsequent calls are ignored.
        System.loadLibrary("AudioEngine")
        val filesDir = activity.filesDir.absolutePath;
        ASSETS = "$filesDir/ASSETS"
        setAssetFilesDirectory(ASSETS)
        CACHE = "$filesDir/CACHE"
        setTemporyMediaFilesDirectory(CACHE)
        Log.i(LOG_TAG, "copying assets folder")
        classes.assetsManager().copyAssetFolder(activity.assets)
        val binDir = "$ASSETS/usr/bin"
        if (File(binDir).exists()) {
            Chmod.main(arrayOf("-R", "a=rwx", binDir))
        }

        audioManager = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManagerObtained = true

        // library is loaded at application startup
        Oboe_Init(
            audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE).toInt(),
            audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER).toInt()
        )
        return this
    }

    fun requestAudioFocus(): Media {
        focusManager.request()
        return this
    }

    fun releaseAudioFocus(): Media {
        focusManager.release()
        return this
    }

    fun setAssetFilesDirectory(dir: String): Media {
        currentAssetFilesDirectory = dir
        Log.e(LOG_TAG, "setting Temporary Files Directory to $dir")
        Oboe_SetAssetsDir(dir)
        return this
    }

    fun setTemporyMediaFilesDirectory(dir: String): Media {
        currentTemporyMediaFilesDirectory = dir
        Log.e(LOG_TAG, "setting Temporary Files Directory to $dir")
        Oboe_SetTempDir(dir)
        return this
    }

    var isInBackground: Boolean = false

    fun foreground(): Media {
        isInBackground = false
        return this
    }

    fun background(): Media {
        isInBackground = true
        return this
    }

    fun destroy(): Media {
        focusManager.release()
        Oboe_Cleanup()
        return this
    }

    fun loadMediaRes(data: Int): Media {
        // Files under res/raw are not zipped, just copied into the APK.
        // Get the offset and length to know where our file is located.
        val fd = activity.resources.openRawResourceFd(data)
        val fileOffset = fd.startOffset.toInt()
        val fileLength = fd.length.toInt()
        try {
            fd.parcelFileDescriptor.close()
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Close error.")
        }
        // get path to APK package
        val path = activity.packageResourcePath
        return this
    }

    lateinit var file: ByteArray
    lateinit var data: ByteArray
    var size: Int = 0

    fun loadMediaPath(path: String): Media {
//        val p = PermissionManager(activity).Permissions("android.permission.READ_EXTERNAL_STORAGE")
//        p.requestAllRemaining()
//        while (!p.checkAll()) Thread.sleep(1)
        Oboe_LoadTrackFromPath(path)
        return this
    }

    fun loadMediaAsset(asset: String): Media {
        Oboe_LoadTrackFromAssets(asset, activity.assets)
        return this
    }

    fun loadMediaAssetAsFile(asset: String): Media {
        Oboe_LoadTrackFromPath("$ASSETS/$asset")
        return this
    }

    val Listner = classes.ListnerCallback()

    var isPlaying = false
    var isPaused = false
    var isStopped = true
    var isLooping = false

    fun togglePlayback(): Media {
        when {
            isStopped || isPaused -> play()
            isPlaying -> play()
            else -> Log.e(LOG_TAG, "Unknown Playback State")
        }
        return this
    }

    fun play(): Media {
        focusManager.request()
        Oboe_Play()
        isPlaying = true
        isPaused = false
        isStopped = false
        Listner.play()
        return this
    }

    fun pause(): Media {
        focusManager.release()
        Oboe_Pause()
        isPlaying = false
        isPaused = true
        isStopped = false
        Listner.pause()
        return this
    }

    fun stop(): Media {
        focusManager.release()
        Oboe_Stop()
        isPlaying = false
        isPaused = false
        isStopped = true
        Listner.stop()
        return this
    }

    fun loop(value: Boolean): Media {
        Oboe_Loop(value)
        return this
    }

    private val looper: MutableList<Classes.loop> = mutableListOf()
    private var currentLooper: Classes.loop? = null
    private var stopLooper = true
    
    private fun looperStart() {
        Oboe_Looper(currentLooper!!.start, currentLooper!!.end, currentLooper!!.timing)
        isLooping = true
    }

    private fun looperStop() {
        currentLooper = null
        isLooping = false
    }

    fun addLooper(name: String, start: Int, end: Int, timing: Int): Media = addLooper(
        name,
        start.toLong(),
        end.toLong(),
        timing
    )

    fun addLooper(name: String, start: Long, end: Long, timing: Int): Media {
        looper.add(classes.loop(name, start, end, timing))
        return this
    }

    fun setLooper(name: String?): Media {
        if (name == null) looperStop()
        else {
            val l = looper.find { it.name == name }
            if (l != null) {
                currentLooper = l
                looperStart()
            }
            else Log.e(LOG_TAG, "Looper not found: $name")
        }
        return this
    }

    fun currentFrame(width: Int): Int = Oboe_CurrentFrame(width);

    fun WaveformView(context: Context, height: Int, width: Int): ConstraintLayout =
        classes.WaveformView_(context, height, width)

    fun WaveformView(context: Context, height: Int, width: Int, media: Media): ConstraintLayout =
        classes.WaveformView_(context, height, width, media)


    val samples: ShortArray get() {
        val a = mutableListOf<Short>()
        // this WILL read incur a buffer overrun by twice its size
        // | [] [] [] [] | -> | [] [] [] [] | [] [] [] [] |
        // |    DATA     | -> |    DATA     | RANDOM DATA |
        // a.size == Oboe_SampleCount()*2
        for (i in 0 until ((Oboe_SampleCount()*2)-1)) a.add(Oboe_SampleIndex(i))
        return a.toTypedArray().toShortArray()
    }
    val sampleRate get() = Oboe_SampleRate()
    val channelCount get() = Oboe_ChannelCount()

    // Functions implemented in the native library.

    external fun Oboe_underrunCount(): Int
    external fun Oboe_previousUnderrunCount(): Int
    external fun Oboe_framesPerBurst(): Int
    external fun Oboe_bufferSize(): Int
    external fun Oboe_bufferCapacity(): Int
    external fun Oboe_getCurrentTime(): String
    external fun Oboe_getAudioTimingNANO(): Long
    external fun Oboe_getAudioTimingMICRO(): Long
    external fun Oboe_getAudioTimingMILLI(): Long
    external fun Oboe_getAudioTimingFormatNANO(): Long
    external fun Oboe_getAudioTimingFormatMICRO(): Long
    external fun Oboe_getAudioTimingFormatMILLI(): Long
    external fun Oboe_getAudioTimingChronoNANO(): Long
    external fun Oboe_getAudioTimingChronoMICRO(): Long
    external fun Oboe_getAudioTimingChronoMILLI(): Long
    external fun Oboe_getCurrentFrame(): Long

    private external fun Oboe_Init(sampleRate: Int, framesPerBurst: Int)
    private external fun Oboe_SetAssetsDir(dir: String)
    private external fun Oboe_SetTempDir(dir: String)
    private external fun Oboe_LoadTrackFromAssets(asset: String, assetManager: AssetManager)
    private external fun Oboe_LoadTrackFromPath(path: String)
    private external fun Oboe_getWaveformLocation(): String
    private external fun Oboe_Play()
    private external fun Oboe_Pause()
    private external fun Oboe_Stop()
    private external fun Oboe_Loop(value: Boolean)
    private external fun Oboe_Looper(start: Long, end: Long, timing: Int)
    private external fun Oboe_CurrentFrame(width: Int): Int
    private external fun Oboe_Cleanup()
    private external fun Oboe_SampleIndex(index: Long): Short
    private external fun Oboe_SampleCount(): Long
    private external fun Oboe_SampleRate(): Int
    private external fun Oboe_ChannelCount(): Int

    val WaveformViewOptions = classes.WaveformViewOptions__()
}