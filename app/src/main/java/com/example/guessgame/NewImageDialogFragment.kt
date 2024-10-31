package com.example.guessgame

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.squareup.picasso.Picasso

class NewImageDialogFragment : DialogFragment() {

    private var imageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imageUrl = it.getString(IMAGE_URL_KEY)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_full_image, container, false)
        val imageView: ImageView = view.findViewById(R.id.fullImageView)

        imageUrl?.let {
            Picasso.get()
                .load(it)
                .resize(1200, 1200) // Задайте нужные размеры
                .centerInside() // Или centerCrop(), если хотите, чтобы изображение заполнило весь экран
                .into(imageView)
        }


        return view
    }

    companion object {
        private const val IMAGE_URL_KEY = "image_url"

        fun newInstance(imageUrl: String) = NewImageDialogFragment().apply {
            arguments = Bundle().apply {
                putString(IMAGE_URL_KEY, imageUrl)
            }
        }
    }
}
