package com.example.guessgame

import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import java.io.IOException
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.picasso.Picasso

data class Pack(val ID: Int, val Name: String, val Thumbnail: String, val Images: List<Image>)

data class Image(val ID: Int, val Name: String) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(ID)
        parcel.writeString(Name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Image> {
        override fun createFromParcel(parcel: Parcel): Image {
            return Image(parcel)
        }

        override fun newArray(size: Int): Array<Image?> {
            return arrayOfNulls(size)
        }
    }
}


class HomeActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private val gson = Gson()
    private val TAG = "HomeActivity" // Тег для логирования

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        Log.d(TAG, "onCreate: Запуск HomeActivity")
        fetchPacks() // Вызов функции для получения данных с сервера
    }

    private fun fetchPacks() {
        val url = "http://192.168.1.101:1323/api/packs" // URL для получения паков
        Log.d(TAG, "fetchPacks: Отправка запроса на $url")

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "fetchPacks: Ошибка при получении данных: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@HomeActivity, "Ошибка при получении данных: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        Log.d(TAG, "fetchPacks: Получен успешный ответ от сервера")
                        // Парсинг JSON ответа
                        val packType = object : TypeToken<List<Pack>>() {}.type
                        val packs: List<Pack> = gson.fromJson(responseBody, packType)
                        runOnUiThread {
                            displayPacks(packs) // Вызов функции для отображения паков
                        }
                    }
                } else {
                    Log.e(TAG, "fetchPacks: Ошибка на сервере: ${response.code}, ${response.message}")
                    runOnUiThread {
                        Toast.makeText(this@HomeActivity, "Ошибка: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun displayPacks(packs: List<Pack>) {
        val gridLayout: GridLayout = findViewById(R.id.gridLayout)
        gridLayout.removeAllViews()

        Log.d(TAG, "displayPacks: Отображение ${packs.size} паков на экране")

        val imageSizeInDp = 300 // Размер изображения в dp
        val scale = resources.displayMetrics.density
        val imageSizeInPx = (imageSizeInDp * scale + 0.5f).toInt() // Преобразование dp в px

        packs.forEach { pack ->
            // Создаем ImageView для отображения обложки (Thumbnail)
            val imageView = ImageView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = imageSizeInPx // Устанавливаем фиксированную ширину
                    height = imageSizeInPx // Устанавливаем фиксированную высоту
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(0, 100, 0, 0) // Нижний отступ
                }

                // URL для загрузки обложки
                val imageUrl = "http://192.168.1.101:1323/api/download/packThumbnails/${pack.ID}"
                Log.d(TAG, "displayPacks: Загружаем обложку с URL: $imageUrl")

                // Используем Picasso для загрузки обложки
                Picasso.get()
                    .load(imageUrl)
                    .resize(imageSizeInPx, imageSizeInPx) // Устанавливаем размер изображения
                    .centerCrop() // Центрируем изображение внутри ImageView
                    .into(this, object : com.squareup.picasso.Callback {
                        override fun onSuccess() {
                            Log.d(TAG, "Успешная загрузка обложки: ${pack.Thumbnail}")
                        }

                        override fun onError(e: Exception?) {
                            Log.e(TAG, "Ошибка загрузки обложки $imageUrl", e)
                        }
                    })
            }

            // Устанавливаем обработчик нажатия на ImageView
            imageView.setOnClickListener {
                // Создаем Intent для перехода в новое Activity
                val intent = Intent(this, PackDetailActivity::class.java)
                intent.putExtra("packName", pack.Name)
                intent.putParcelableArrayListExtra("images", ArrayList(pack.Images))
                // Передаем список изображений
                startActivity(intent) // Запускаем новое Activity
            }

            // Создаем TextView для отображения названия пака
            val textView = TextView(this).apply {
                text = pack.Name
                textSize = 30f // Увеличиваем размер шрифта
                gravity = android.view.Gravity.CENTER // Центрируем текст
                layoutParams = GridLayout.LayoutParams().apply {
                    width = GridLayout.LayoutParams.WRAP_CONTENT
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(0, 16, 0, 0) // Верхний отступ
                }
            }

            // Добавляем ImageView и TextView в GridLayout
            gridLayout.addView(imageView)
            gridLayout.addView(textView)
        }
    }

}
