package com.introid.newsapp.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.introid.newsapp.R
import com.introid.newsapp.models.Article

import kotlinx.android.synthetic.main.item_article_preview.view.*
import kotlinx.android.synthetic.main.item_news.view.*
import kotlinx.android.synthetic.main.item_news_short.view.*

/*
 change made
*/

class NewsAdapter(
    val isShort : Boolean = false
) : RecyclerView.Adapter<NewsAdapter.ArticleViewHolder>() {

    inner class ArticleViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    private val differCallback = object : DiffUtil.ItemCallback<Article>() {
        override fun areItemsTheSame(oldItem: Article, newItem: Article): Boolean {
            return oldItem.url == newItem.url
        }

        override fun areContentsTheSame(oldItem: Article, newItem: Article): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val layoutToUse = if (isShort){
            R.layout.item_news_short
        }else {
            R.layout.item_news
        }

        return ArticleViewHolder(
            LayoutInflater.from(parent.context).inflate(
                layoutToUse,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    private var onItemClickListener: ((Article) -> Unit)? = null

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val article = differ.currentList[position]
        if (isShort){
            holder.itemView.apply {
                Glide.with(this).load(article.urlToImage)
                    .placeholder(R.drawable.bg_placeholder)
                    .into(itemArticleImageViewS)
                itemArticleTitleTVS.text = article.title
                article.source?.name.let {
                    if (it!!.isEmpty()) itemNewsSourceTVS.text = "${article.source?.name}"
                }
                setOnClickListener {
                    onItemClickListener?.let {
                        it(article)
                    }
                }
            }

        }else {
            holder.itemView.apply {
                Glide.with(this).load(article.urlToImage)
                    .placeholder(R.drawable.bg_placeholder)
                    .into(itemArticleImageView)
                itemArticleTitleTV.text = article.title
                itemArticleDescTV.text = article.description
                article.source?.name.let {
                    if(it!!.isNotEmpty()) itemNewsSourceTV.text = "Source: ${article.source?.name}"
                }
                itemActionButton.setOnClickListener {
                    onItemClickListener?.let {
                        it(article)
                    }
                }
            }

        }
    }

    fun setOnItemClickListener(listener: (Article) -> Unit) {
        onItemClickListener = listener
    }
}