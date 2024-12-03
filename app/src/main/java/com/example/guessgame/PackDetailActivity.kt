package com.example.guessgame

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import android.widget.ImageView
import android.widget.TextView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import kotlin.math.sqrt

class PackDetailActivity : AppCompatActivity() {

    private var isScrolling = false
    private val scrollHandler = Handler(Looper.getMainLooper())
    private val scrollRunnable = Runnable { isScrolling = false }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pack_detail)

        val images = intent.getParcelableArrayListExtra<Image>("images") ?: arrayListOf()
        val packName = intent.getStringExtra("packName") ?: "Pack"
        title = packName

        val toHomeActivityButton: Button = findViewById(R.id.toHomeActivityButton)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = ImageAdapter(images)

        toHomeActivityButton.setOnClickListener {
            // Переход на новую активность HomeActivity
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

        // Добавляем слушатель прокрутки для RecyclerView
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING || newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    isScrolling = true
                    scrollHandler.removeCallbacks(scrollRunnable)
                    scrollHandler.postDelayed(scrollRunnable, 200) // задержка для сброса прокрутки
                }
            }
        })
    }

    class ImageAdapter(private val images: List<Image>) :
        RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_image, parent, false)
            return ImageViewHolder(view)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val image = images[position]
            holder.bind(image)
        }

        override fun getItemCount(): Int = images.size

        class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val imageView: ImageView = itemView.findViewById(R.id.imageView)
            private val textView: TextView = itemView.findViewById(R.id.textView)
            private val crossImageView: ImageView = itemView.findViewById(R.id.crossImageView)

            private var isDimmed = false
            private var dialogFragment: NewImageDialogFragment? = null
            private val handler = Handler(Looper.getMainLooper())
            private var isLongPress = false
            private lateinit var longPressRunnable: Runnable

            private var initialX = 0f
            private var initialY = 0f
            private var isMoving = false

            fun bind(image: Image) {
                textView.text = image.Name
                val imageUrl = Constants.BASE_URL + "/api/download/images/${image.ID}"
                Picasso.get().load(imageUrl).into(imageView)

                imageView.setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = event.x
                            initialY = event.y
                            isMoving = false

                            // Начинаем задержку для долгого нажатия
                            longPressRunnable = Runnable {
                                // Проверяем, не был ли список прокручен
                                if (!(v.context as PackDetailActivity).isScrolling && !isMoving) {
                                    dialogFragment = NewImageDialogFragment.newInstance(imageUrl)
                                    dialogFragment?.show((itemView.context as AppCompatActivity).supportFragmentManager, "dialog")
                                    isLongPress = true
                                }
                            }
                            handler.postDelayed(longPressRunnable, 500) // задержка на 500ms
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = event.x - initialX
                            val dy = event.y - initialY
                            if (sqrt((dx * dx + dy * dy).toDouble()) > 10) {
                                isMoving = true
                                handler.removeCallbacks(longPressRunnable)
                            }
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            handler.removeCallbacks(longPressRunnable)
                            if (isLongPress) {
                                dialogFragment?.dismiss()
                                dialogFragment = null
                            } else {
                                toggleImageState()
                            }
                            isLongPress = false
                            true
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            handler.removeCallbacks(longPressRunnable)
                            dialogFragment?.dismiss()
                            dialogFragment = null
                            if (isDimmed) {
                                imageView.alpha = 1f
                                isDimmed = false
                            }
                            isLongPress = false
                            true
                        }
                        else -> false
                    }
                }
            }

            private fun toggleImageState() {
                if (isDimmed) {
                    imageView.alpha = 1f
                    isDimmed = false
                    crossImageView.visibility = View.GONE
                } else {
                    imageView.alpha = 0.2f
                    isDimmed = true
                    crossImageView.visibility = View.VISIBLE
                }
            }
        }
    }
}
