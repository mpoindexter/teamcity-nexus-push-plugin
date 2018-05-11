/*
 * Copyright (c) 2018-Present Michael Poindexter.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package com.github.mpoindexter.teamcity.nexuspushplugin.nexus;

import java.util.List;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

public class SearchResponse {
    private static Moshi moshi = new Moshi.Builder().build();
    public static final JsonAdapter<SearchResponse> ADAPTER = moshi.adapter(SearchResponse.class);
    
    private List<ComponentBean> items;
    private String continuationToken;

    /**
     * @return the items
     */
    public List<ComponentBean> getItems() {
        return items;
    }

    /**
     * @param items the items to set
     */
    public void setItems(List<ComponentBean> items) {
        this.items = items;
    }

    /**
     * @return the continuationToken
     */
    public String getContinuationToken() {
        return continuationToken;
    }

    /**
     * @param continuationToken the continuationToken to set
     */
    public void setContinuationToken(String continuationToken) {
        this.continuationToken = continuationToken;
    }
}