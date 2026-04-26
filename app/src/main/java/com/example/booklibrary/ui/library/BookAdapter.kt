package com.example.booklibrary.ui.library

import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.booklibrary.R
import com.example.booklibrary.data.db.Book

class BookAdapter(
    private val onBookClick: (Book) -> Unit,
    private val onBookLongClick: (Book) -> Unit
) : ListAdapter<Book, BookAdapter.BookViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(oldItem: Book, newItem: Book) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Book, newItem: Book) = oldItem == newItem
    }

    private val coverColors = listOf(
        0xFF7986CB.toInt(),
        0xFF4DB6AC.toInt(),
        0xFFFF8A65.toInt(),
        0xFFA1887F.toInt(),
        0xFF90A4AE.toInt(),
        0xFFBA68C8.toInt(),
        0xFF4FC3F7.toInt(),
        0xFFAED581.toInt()
    )

    inner class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cover: ImageView = itemView.findViewById(R.id.book_cover)
        private val title: TextView = itemView.findViewById(R.id.book_title)
        private val author: TextView = itemView.findViewById(R.id.book_author)
        private val genre: TextView = itemView.findViewById(R.id.book_genre)

        fun bind(book: Book) {
            title.text = book.title
            author.text = book.author
            genre.text = book.genre

            if (book.thumbnail.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(book.thumbnail)
                    .placeholder(ColorDrawable(coverColors[book.id % coverColors.size]))
                    .error(ColorDrawable(coverColors[book.id % coverColors.size]))
                    .centerCrop()
                    .into(cover as ImageView)
            } else {
                cover.setBackgroundColor(coverColors[book.id % coverColors.size])
            }

            itemView.setOnClickListener { onBookClick(book) }
            itemView.setOnLongClickListener { onBookLongClick(book); true }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}