@file:Suppress("NotifyDataSetChanged")

package com.smarternote.core.ui.banner

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.smarternote.core.R
import net.lucode.hackware.magicindicator.MagicIndicator
import net.lucode.hackware.magicindicator.buildins.circlenavigator.CircleNavigator
import kotlin.math.abs

class Banner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    var lifecycleOwner: LifecycleOwner? = null
) : FrameLayout(context, attrs, defStyleAttr), DefaultLifecycleObserver {

    private var currentPage = 0
    private val maxDataSize = 8

    private val viewPager: ViewPager2
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var internalAdapter: BannerAdapter
    private val magicIndicator: MagicIndicator
    private var autoPlayRunnable = getAutoPlayRunnable()
    private val circleNavigator by lazy { CircleNavigator(context) }
    private var autoPlay = true

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.core_banner, this, true)
        viewPager = view.findViewById(R.id.viewPager)
        magicIndicator = view.findViewById(R.id.indicator)

        viewPager.setPageTransformer { page, position ->
            val absPos = abs(position)
            page.scaleY = (1 - absPos) * 0.15f + 0.85f
            page.alpha = 1 - absPos
            page.translationX = -page.width * position
            page.translationY = 30 * absPos
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                magicIndicator.onPageSelected(position)
            }
        })

        circleNavigator.circleColor = Color.RED
        circleNavigator.circleClickListener = CircleNavigator.OnCircleClickListener { index -> viewPager.setCurrentItem(index, true) }
        magicIndicator.navigator = circleNavigator

        lifecycleOwner?.lifecycle?.addObserver(this)

    }

    private fun getAutoPlayRunnable(): Runnable {
        return Runnable {
            currentPage = (currentPage + 1) % maxDataSize
            viewPager.setCurrentItem(currentPage, true)
            handler.postDelayed(autoPlayRunnable, 3000)
        }
    }

    fun setData(data: List<String>, onItemClickListener: (Int) -> Unit): Banner {
        if (!::internalAdapter.isInitialized) {
            circleNavigator.circleCount = data.size
            internalAdapter = BannerAdapter(data.take(maxDataSize), onItemClickListener)
            viewPager.isUserInputEnabled = data.size > 1
            viewPager.adapter = internalAdapter
            viewPager.offscreenPageLimit = 1
            viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        }
        return this
    }

    fun updateData(data: List<String>) {
        if (::internalAdapter.isInitialized) {
            internalAdapter.updateData(data)
        }
    }

    fun setPageTransformer(transformer: ViewPager2.PageTransformer): Banner {
        viewPager.setPageTransformer(transformer)
        return this
    }

    fun setAutoPlay(enable: Boolean): Banner {
        autoPlay = enable
        if (enable) {
            lifecycleOwner?.let { onStart(it) }
        } else {
            lifecycleOwner?.let { onStop(it) }
        }
        return this
    }


    fun setAdapter(adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>): Banner {
        viewPager.adapter = adapter
        return this
    }

    override fun onStart(owner: LifecycleOwner) {
        if (autoPlay) {
            handler.postDelayed(autoPlayRunnable, 3000)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        handler.removeCallbacks(autoPlayRunnable)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        handler.removeCallbacksAndMessages(null)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        when (ev?.action) {
            MotionEvent.ACTION_DOWN -> lifecycleOwner?.let { onStop(it) }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> lifecycleOwner?.let { onStart(it) }
        }
        return super.dispatchTouchEvent(ev)
    }

}
