package com.sakuya.navigation

const val SEARCH_ROUTE = "search"
const val SEARCH_RESULTS_BASE_ROUTE = "search/results"
const val SEARCH_RESULTS_ARG_QUERY = "query"
const val SEARCH_RESULTS_ROUTE = "$SEARCH_RESULTS_BASE_ROUTE?$SEARCH_RESULTS_ARG_QUERY={$SEARCH_RESULTS_ARG_QUERY}"
