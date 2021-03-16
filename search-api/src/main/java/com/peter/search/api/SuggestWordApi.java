package com.peter.search.api;

import com.peter.search.dto.Result;
import com.peter.search.dto.SuggestWordDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value="search-service", path = "search-service")
public interface SuggestWordApi {

    @GetMapping(value = "/suggest/generalSuggestWord")
    Result generalSuggestWord(@RequestBody SuggestWordDTO suggestWordDTO);
}
