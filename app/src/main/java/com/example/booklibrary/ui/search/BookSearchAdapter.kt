package com.example.booklibrary.ui.search

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
import com.example.booklibrary.data.network.BookItem

class BookSearchAdapter(
    private val onAddClick: (BookItem) -> Unit
) : ListAdapter<BookItem, BookSearchAdapter.SearchViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<BookItem>() {
        override fun areItemsTheSame(oldItem: BookItem, newItem: BookItem) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: BookItem, newItem: BookItem) =
            oldItem == newItem
    }

    inner class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cover: ImageView = itemView.findViewById(R.id.iv_cover)
        private val title: TextView = itemView.findViewById(R.id.tv_title)
        private val author: TextView = itemView.findViewById(R.id.tv_author)
        private val genre: TextView = itemView.findViewById(R.id.tv_genre)
        private val description: TextView = itemView.findViewById(R.id.tv_description)

        fun bind(item: BookItem) {
            val info = item.volumeInfo
            title.text = info.title ?: "Без названия"
            author.text = info.authors?.joinToString(", ") ?: "Автор неизвестен"
            description.text = info.description ?: ""
            val genreText = info.categories?.firstOrNull()
            if (genreText != null) {
                genre.visibility = View.VISIBLE
                genre.text = genreText
            } else {
                genre.visibility = View.GONE
            }

            val imageUrl = info.imageLinks?.thumbnail?.replace("http://", "https://")
            if (imageUrl != null) {
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_book_placeholder)
                    .into(cover)
            } else {
                cover.setImageResource(R.drawable.ic_book_placeholder)
            }

            itemView.setOnClickListener { onAddClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book_search, parent, false)
        return SearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}