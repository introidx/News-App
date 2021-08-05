package com.introid.newsapp.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.URLUtil
import android.widget.AbsListView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.introid.newsapp.R
import com.introid.newsapp.adapters.NewsAdapter
import com.introid.newsapp.ui.NewsActivity
import com.introid.newsapp.ui.NewsViewModel
import com.introid.newsapp.util.Constants
import com.introid.newsapp.util.Constants.Companion.SEARCH_NEWS_TIME_DELAY
import com.introid.newsapp.util.Resource
import kotlinx.android.synthetic.main.category_bottom_sheet.*
import kotlinx.android.synthetic.main.fragment_search_news.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchNewsFragment : Fragment(R.layout.fragment_search_news) {

    lateinit var viewModel: NewsViewModel
    lateinit var newsAdapter: NewsAdapter
    private lateinit var newsCatt : String

    private lateinit var bottomSheetBehavior : BottomSheetBehavior<RelativeLayout>

    val TAG = "SearchNewsFragment"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = (activity as NewsActivity).viewModel
        setupRecyclerView()

        bottomSheetBehavior = BottomSheetBehavior.from(categoryBottomSheet)


        viewModel.newsCat.observe(viewLifecycleOwner) {
            newsCatt = it
            filterIcon.text =it
            Log.d(TAG, "filterListWithCategory: ${newsCatt}")
        }

        filterIcon.setOnClickListener {
            showHideBottomSheet()
        }


        newsAdapter.setOnItemClickListener {

            val isArticleEmpty = it.url.isNullOrEmpty()
            val isValidUrl = URLUtil.isValidUrl(it.url)
            Log.d(TAG, "onClick : isUrlEmpty : $isArticleEmpty  & isValidUrl : $isValidUrl")
            if(isArticleEmpty || !isValidUrl) {
                Toast.makeText(requireContext(), "Data Not Available!", Toast.LENGTH_SHORT).show()
                return@setOnItemClickListener
            }

            val bundle = Bundle().apply {
                putSerializable("article", it)
            }
            findNavController().navigate(
                R.id.action_searchNewsFragment2_to_articleFragment,
                bundle
            )
        }

        var job: Job? = null
        searchEditText.addTextChangedListener { editable ->
            job?.cancel()
            job = MainScope().launch {
                delay(SEARCH_NEWS_TIME_DELAY)
                editable?.let {
                    if(editable.toString().isNotEmpty()) {
                        viewModel.searchNews(editable.toString())
                    }
                }
            }
        }

        viewModel.searchNews.observe(viewLifecycleOwner) { response ->
            when(response) {
                is Resource.Success -> {
                    hideProgressBar()
                    response.data?.let { newsResponse ->
                        if (newsResponse.articles.isEmpty()){
                            Toast.makeText(requireContext(), "Response Empty" , Toast.LENGTH_SHORT).show()
                        }

                        newsAdapter.differ.submitList(newsResponse.articles.toList())
                        if (newsAdapter.differ.currentList.isEmpty()){
                            showAnimation(true)
                        }else{
                            showAnimation(false)
                        }
                        val totalPages = newsResponse.totalResults / Constants.QUERY_PAGE_SIZE + 2
                        isLastPage = viewModel.searchNewsPage == totalPages
                        if (isLastPage){
                            homeRecyclerView.setPadding(0, 0, 0, 0)
                        }
                    }
                }
                is Resource.Error -> {
                    hideProgressBar()
                    response.message?.let { message ->
                        Log.e(TAG, "An error occured: $message")
                        Toast.makeText(activity, "An Error occured: $message", Toast.LENGTH_SHORT).show()
                    }
                }
                is Resource.Loading -> {
                    showProgressBar()
                }
            }
        }

        // handling category here
        chipGroup?.apply {
            setOnCheckedChangeListener { group, checkedId ->
                val chip = group.findViewById<Chip>(checkedId)
                chip?.text?.let{
                    filterListWithCategory(it.toString())
                }
                showHideBottomSheet()
            }
        }
        closeBottomSheetIV?.apply {
            setOnClickListener {
                showHideBottomSheet()
            }
        }


    }


    private fun showAnimation(isShow : Boolean){
        if(isShow){
            homeAnimationView.visibility = View.VISIBLE
        }else{
            homeAnimationView.visibility = View.GONE
        }
    }


    private fun hideProgressBar() {
        homeProgressBar?.let {
            it.visibility = View.GONE
            isLoading = false
        }
    }

    private fun showProgressBar() {
        homeProgressBar?.let {
            it.visibility = View.VISIBLE
            isLoading = true
        }
    }

    var isLoading = false
    var isLastPage = false
    var isScrolling = false

    val scrollListener = object : RecyclerView.OnScrollListener() {

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)

            if(newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                isScrolling = true
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
            val visibleItemCount = layoutManager.childCount
            val totalItemCount = layoutManager.itemCount

            val isNotLoadingAndNotLastPage = !isLoading && !isLastPage
            val isAtLastItem = firstVisibleItemPosition + visibleItemCount >= totalItemCount
            val isNotAtBeginning = firstVisibleItemPosition >= 0
            val isTotalMoreThanVisible = totalItemCount >= Constants.QUERY_PAGE_SIZE

            val shouldPaginate = isNotLoadingAndNotLastPage && isAtLastItem && isNotAtBeginning &&
                    isTotalMoreThanVisible && isScrolling
            if(shouldPaginate) {
                viewModel.searchNews(newsCatt)
                isScrolling = false
            } else {

            }
        }


    }

    private fun setupRecyclerView() {
        newsAdapter = NewsAdapter(true)
        homeRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            val divider = DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
            addItemDecoration(divider)
            adapter = newsAdapter
            addOnScrollListener(this@SearchNewsFragment.scrollListener)
        }
    }

    private fun showHideBottomSheet() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        else
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }



    private fun filterListWithCategory(categoryOrQuery: String) {
        viewModel.apply {
            searchNewsResponse = null
            newsCat.postValue(categoryOrQuery)
            searchNewsPage = 1
            searchNews(categoryOrQuery)
        }

    }
}